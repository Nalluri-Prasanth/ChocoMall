package com.chocomall.core.experience;

import com.chocomall.common.api.ResultCode;
import com.chocomall.common.exception.ApiException;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.SnGenerator;
import com.chocomall.core.payment.PaymentService;
import com.chocomall.model.entity.ExperienceBooking;
import com.chocomall.model.entity.ExperienceSession;
import com.chocomall.model.entity.OrderSetting;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.Product;
import com.chocomall.model.enums.BookingStatus;
import com.chocomall.model.enums.PayMethod;
import com.chocomall.model.enums.PaymentStatus;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.enums.SessionStatus;
import com.chocomall.model.repository.ExperienceBookingRepository;
import com.chocomall.model.repository.ExperienceSessionRepository;
import com.chocomall.model.repository.OrderSettingRepository;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seats for tastings, pairing workshops and make-your-own-bark sessions. Seats are reserved with an atomic
 * conditional update (no overbooking), paid by card, and released again on cancellation or when the payment
 * window passes.
 */
@Service
@RequiredArgsConstructor
public class ExperienceBookingService {

    private final ExperienceSessionRepository sessions;
    private final ExperienceBookingRepository bookings;
    private final ProductRepository products;
    private final PaymentRepository payments;
    private final PaymentService paymentService;
    private final OrderSettingRepository settings;

    @Transactional
    public ExperienceBooking book(Long memberId, Long sessionId, int seats) {
        Asserts.isTrue(seats >= 1 && seats <= 10, "You can book between 1 and 10 seats at a time");
        ExperienceSession session = Asserts.found(sessions.findById(sessionId), "Session");
        Product product = Asserts.found(products.findById(session.getProductId()), "Experience");
        Asserts.state(product.getKind() == ProductKind.EXPERIENCE && product.isPublished(), "This experience is not open for booking");
        if (sessions.reserveSeats(sessionId, seats, LocalDateTime.now()) == 0) {
            throw new ApiException(ResultCode.CONFLICT, "Not enough seats left, or the session is closed");
        }
        BigDecimal unit = session.getPrice() != null ? session.getPrice() : product.getPrice();
        ExperienceBooking b = new ExperienceBooking();
        b.setBookingSn(SnGenerator.next("EXP"));
        b.setSessionId(sessionId);
        b.setMemberId(memberId);
        b.setSeats(seats);
        b.setAmount(unit.multiply(BigDecimal.valueOf(seats)));
        return bookings.save(b);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public ExperienceBooking pay(Long memberId, Long bookingId, String token) {
        ExperienceBooking b = Asserts.found(bookings.findByIdAndMemberId(bookingId, memberId), "Booking");
        if (b.getStatus() == BookingStatus.CONFIRMED) {
            return b;
        }
        Asserts.state(b.getStatus() == BookingStatus.PENDING_PAYMENT, "This booking was cancelled");
        Payment draft = paymentService.draft(b.getAmount(), PayMethod.CARD);
        draft.setBookingId(bookingId);
        paymentService.charge(draft, token);
        b.setStatus(BookingStatus.CONFIRMED);
        return b;
    }

    @Transactional
    public ExperienceBooking cancelByMember(Long memberId, Long bookingId) {
        ExperienceBooking b = Asserts.found(bookings.findByIdAndMemberId(bookingId, memberId), "Booking");
        ExperienceSession s = Asserts.found(sessions.findById(b.getSessionId()), "Session");
        int hours = settings.findAll().stream().findFirst().orElseGet(OrderSetting::new).getExperienceCancelHours();
        Asserts.state(LocalDateTime.now().plus(Duration.ofHours(hours)).isBefore(s.getStartsAt()) || b.getStatus() == BookingStatus.PENDING_PAYMENT,
                "Bookings can only be cancelled online up to " + hours + " hours before the session");
        return cancel(b);
    }

    /** Cancels a booking (idempotent): frees the seats and refunds a confirmed payment. */
    @Transactional
    public ExperienceBooking cancel(ExperienceBooking b) {
        if (b.getStatus() == BookingStatus.CANCELLED) {
            return b;
        }
        sessions.releaseSeats(b.getSessionId(), b.getSeats());
        if (b.getStatus() == BookingStatus.CONFIRMED) {
            payments.findByBookingId(b.getId()).stream().filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                    .forEach(p -> p.setStatus(PaymentStatus.REFUNDED));
        }
        b.setStatus(BookingStatus.CANCELLED);
        return b;
    }

    /** Cancels a session and every booking on it (e.g. the host is unavailable). */
    @Transactional
    public int cancelSession(Long sessionId) {
        ExperienceSession s = Asserts.found(sessions.findById(sessionId), "Session");
        List<ExperienceBooking> all = bookings.findBySessionId(sessionId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        int affected = 0;
        for (ExperienceBooking b : all) {
            if (b.getStatus() != BookingStatus.CANCELLED) {
                cancel(b);
                affected++;
            }
        }
        s.setStatus(SessionStatus.CANCELLED);
        return affected;
    }

    /** Frees seats held by bookings that were never paid. */
    @Transactional
    public int expirePending() {
        OrderSetting setting = settings.findAll().stream().findFirst().orElseGet(OrderSetting::new);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(setting.getNormalOrderTimeoutMinutes());
        List<ExperienceBooking> stale = bookings.findByStatusAndCreateTimeBefore(BookingStatus.PENDING_PAYMENT, cutoff);
        stale.forEach(this::cancel);
        return stale.size();
    }
}
