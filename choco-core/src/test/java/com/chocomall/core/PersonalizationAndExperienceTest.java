package com.chocomall.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chocomall.common.exception.ApiException;
import com.chocomall.core.experience.ExperienceBookingService;
import com.chocomall.core.payment.MockPaymentGateway;
import com.chocomall.core.personalization.PersonalizationService;
import com.chocomall.model.entity.ExperienceBooking;
import com.chocomall.model.entity.ExperienceSession;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.PersonalizationRequest;
import com.chocomall.model.entity.Product;
import com.chocomall.model.enums.BookingStatus;
import com.chocomall.model.enums.PaymentStatus;
import com.chocomall.model.enums.PersonalizationKind;
import com.chocomall.model.enums.PersonalizationStatus;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.enums.SessionStatus;
import com.chocomall.model.repository.ExperienceBookingRepository;
import com.chocomall.model.repository.ExperienceSessionRepository;
import com.chocomall.model.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = CoreTestApplication.class)
class PersonalizationAndExperienceTest {

    @Autowired PersonalizationService personalization;
    @Autowired ExperienceBookingService experiences;
    @Autowired ExperienceSessionRepository sessions;
    @Autowired ExperienceBookingRepository bookings;
    @Autowired PaymentRepository payments;
    @Autowired Fixtures fixtures;
    @Autowired JdbcTemplate jdbc;

    // ------------------------------------------------------------------------------- personalisation

    @Test
    void personalisationFollowsQuoteProofApprovalDepositProduction() {
        Member m = fixtures.member();
        PersonalizationRequest r = personalization.submit(m.getId(), PersonalizationKind.NAME_PLAQUE, "Happy Birthday Maya", 2,
                "Cake topper", LocalDate.now().plusDays(14));
        assertThat(r.getStatus()).isEqualTo(PersonalizationStatus.SUBMITTED);

        personalization.quote(r.getId(), new BigDecimal("40.00"), new BigDecimal("15.00"), 5, null);
        personalization.sendProof(r.getId(), "Flat plaque 12 cm, script font, v1", "staff");
        assertThat(personalization.getOwned(m.getId(), r.getId()).getStatus()).isEqualTo(PersonalizationStatus.PROOF_SENT);

        PersonalizationRequest approved = personalization.approveProof(m.getId(), r.getId(), 1);
        assertThat(approved.getStatus()).isEqualTo(PersonalizationStatus.PROOF_APPROVED);

        PersonalizationRequest paid = personalization.payDeposit(m.getId(), r.getId(), "tok_ok");
        assertThat(paid.getStatus()).isEqualTo(PersonalizationStatus.DEPOSIT_PAID);
        assertThat(payments.findByPersonalizationId(r.getId())).hasSize(1);

        personalization.startProduction(r.getId());
        personalization.markReady(r.getId());
        assertThat(personalization.complete(r.getId()).getStatus()).isEqualTo(PersonalizationStatus.COMPLETED);
        assertThat(personalization.proofs(r.getId())).hasSize(1).first().satisfies(p -> assertThat(p.isApprovedByMember()).isTrue());
    }

    @Test
    void theDepositCannotBePaidBeforeTheArtworkIsApproved() {
        Member m = fixtures.member();
        PersonalizationRequest r = personalization.submit(m.getId(), PersonalizationKind.MONOGRAM_MEDALLION, "AB", 10, null, null);
        personalization.quote(r.getId(), new BigDecimal("80.00"), new BigDecimal("20.00"), 7, null);

        assertThatThrownBy(() -> personalization.payDeposit(m.getId(), r.getId(), "tok_ok"))
                .isInstanceOf(ApiException.class).hasMessageContaining("Approve the artwork proof");
        personalization.sendProof(r.getId(), "v1", "staff");
        assertThatThrownBy(() -> personalization.payDeposit(m.getId(), r.getId(), "tok_ok")).isInstanceOf(ApiException.class);
    }

    @Test
    void onlyTheLatestProofVersionCanBeApproved() {
        Member m = fixtures.member();
        PersonalizationRequest r = personalization.submit(m.getId(), PersonalizationKind.NAME_PLAQUE, "Sam", 1, null, null);
        personalization.quote(r.getId(), new BigDecimal("10.00"), new BigDecimal("5.00"), 3, null);
        personalization.sendProof(r.getId(), "v1", "staff");
        personalization.requestChanges(m.getId(), r.getId(), "Make the letters bigger");
        personalization.sendProof(r.getId(), "v2 with larger letters", "staff");

        assertThatThrownBy(() -> personalization.approveProof(m.getId(), r.getId(), 1))
                .isInstanceOf(ApiException.class).hasMessageContaining("newer proof");
        assertThat(personalization.approveProof(m.getId(), r.getId(), 2).getStatus()).isEqualTo(PersonalizationStatus.PROOF_APPROVED);
        assertThat(personalization.proofs(r.getId())).hasSize(2);
    }

    @Test
    void aLogoNeedsARecordOfPermissionAndTextLengthIsLimited() {
        Member m = fixtures.member();
        PersonalizationRequest logo = personalization.submit(m.getId(), PersonalizationKind.LOGO_PLAQUE, "ACME Ltd logo", 50, null, null);

        assertThatThrownBy(() -> personalization.quote(logo.getId(), new BigDecimal("200.00"), new BigDecimal("50.00"), 10, " "))
                .isInstanceOf(ApiException.class).hasMessageContaining("permission");
        personalization.quote(logo.getId(), new BigDecimal("200.00"), new BigDecimal("50.00"), 10, "Written permission received 2026-10-01");

        assertThatThrownBy(() -> personalization.submit(m.getId(), PersonalizationKind.NAME_PLAQUE, "x".repeat(41), 1, null, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("40 characters");
    }

    @Test
    void customersCanCancelBeforeThePaidDepositButStaffCanRefundAfterwards() {
        Member m = fixtures.member();
        PersonalizationRequest early = personalization.submit(m.getId(), PersonalizationKind.NAME_PLAQUE, "Eve", 1, null, null);
        assertThat(personalization.cancelByMember(m.getId(), early.getId()).getStatus()).isEqualTo(PersonalizationStatus.CANCELLED);

        PersonalizationRequest late = personalization.submit(m.getId(), PersonalizationKind.NAME_PLAQUE, "Finn", 1, null, null);
        personalization.quote(late.getId(), new BigDecimal("20.00"), new BigDecimal("10.00"), 4, null);
        personalization.sendProof(late.getId(), "v1", "staff");
        personalization.approveProof(m.getId(), late.getId(), 1);
        personalization.payDeposit(m.getId(), late.getId(), "tok_ok");

        assertThatThrownBy(() -> personalization.cancelByMember(m.getId(), late.getId())).isInstanceOf(ApiException.class);
        personalization.cancelByStaff(late.getId(), "printer fault");
        assertThat(payments.findByPersonalizationId(late.getId())).extracting(Payment::getStatus).containsOnly(PaymentStatus.REFUNDED);
    }

    @Test
    void membersCannotSeeEachOthersRequests() {
        Member a = fixtures.member();
        Member b = fixtures.member();
        PersonalizationRequest r = personalization.submit(a.getId(), PersonalizationKind.NAME_PLAQUE, "Ann", 1, null, null);

        assertThatThrownBy(() -> personalization.getOwned(b.getId(), r.getId())).isInstanceOf(ApiException.class);
    }

    // ------------------------------------------------------------------------------- experiences

    private ExperienceSession session(int capacity, LocalDateTime startsAt) {
        Product workshop = fixtures.product(0, "30.00", ProductKind.EXPERIENCE);
        ExperienceSession s = new ExperienceSession();
        s.setProductId(workshop.getId());
        s.setStartsAt(startsAt);
        s.setCapacity(capacity);
        s.setPrice(new BigDecimal("30.00"));
        return sessions.save(s);
    }

    @Test
    void bookingPayingAndCancellingMoveSeatsCorrectly() {
        Member m = fixtures.member();
        ExperienceSession s = session(3, LocalDateTime.now().plusDays(10));

        ExperienceBooking b = experiences.book(m.getId(), s.getId(), 2);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(b.getAmount()).isEqualByComparingTo("60.00");
        assertThat(sessions.findById(s.getId()).orElseThrow().getBooked()).isEqualTo(2);

        assertThatThrownBy(() -> experiences.book(fixtures.member().getId(), s.getId(), 2))
                .isInstanceOf(ApiException.class).hasMessageContaining("Not enough seats");

        assertThat(experiences.pay(m.getId(), b.getId(), "tok_ok").getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        experiences.cancelByMember(m.getId(), b.getId());
        assertThat(sessions.findById(s.getId()).orElseThrow().getBooked()).isZero();
        assertThat(payments.findByBookingId(b.getId())).extracting(Payment::getStatus).containsOnly(PaymentStatus.REFUNDED);
    }

    @Test
    void concurrentBookingsNeverExceedCapacity() throws Exception {
        ExperienceSession s = session(3, LocalDateTime.now().plusDays(5));
        List<Member> people = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            people.add(fixtures.member());
        }
        ExecutorService pool = Executors.newFixedThreadPool(6);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (Member p : people) {
            futures.add(pool.submit(() -> {
                go.await();
                try {
                    experiences.book(p.getId(), s.getId(), 1);
                    ok.incrementAndGet();
                } catch (ApiException full) {
                    // no seat left
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(ok.get()).isEqualTo(3);
        assertThat(sessions.findById(s.getId()).orElseThrow().getBooked()).isEqualTo(3);
    }

    @Test
    void aDeclinedCardKeepsTheBookingPendingAndMembersCannotCancelLate() {
        Member m = fixtures.member();
        ExperienceSession soon = session(5, LocalDateTime.now().plusHours(5));
        ExperienceBooking b = experiences.book(m.getId(), soon.getId(), 1);

        assertThatThrownBy(() -> experiences.pay(m.getId(), b.getId(), MockPaymentGateway.DECLINE_TOKEN)).isInstanceOf(ApiException.class);
        assertThat(bookings.findById(b.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        experiences.pay(m.getId(), b.getId(), "tok_ok");
        assertThatThrownBy(() -> experiences.cancelByMember(m.getId(), b.getId()))
                .isInstanceOf(ApiException.class).hasMessageContaining("24 hours");
    }

    @Test
    void cancellingASessionRefundsEveryone() {
        Member a = fixtures.member();
        Member b = fixtures.member();
        ExperienceSession s = session(10, LocalDateTime.now().plusDays(3));
        ExperienceBooking paid = experiences.book(a.getId(), s.getId(), 2);
        experiences.pay(a.getId(), paid.getId(), "tok_ok");
        experiences.book(b.getId(), s.getId(), 1);

        int affected = experiences.cancelSession(s.getId());

        assertThat(affected).isEqualTo(2);
        assertThat(sessions.findById(s.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(sessions.findById(s.getId()).orElseThrow().getBooked()).isZero();
        assertThat(payments.findByBookingId(paid.getId())).extracting(Payment::getStatus).containsOnly(PaymentStatus.REFUNDED);
    }

    @Test
    void unpaidBookingsAreReleasedAfterThePaymentWindow() {
        Member m = fixtures.member();
        ExperienceSession s = session(4, LocalDateTime.now().plusDays(3));
        ExperienceBooking b = experiences.book(m.getId(), s.getId(), 3);
        jdbc.update("update oms_experience_booking set create_time = ? where id = ?", LocalDateTime.now().minusHours(2), b.getId());

        int released = experiences.expirePending();

        assertThat(released).isGreaterThanOrEqualTo(1);
        assertThat(bookings.findById(b.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(sessions.findById(s.getId()).orElseThrow().getBooked()).isZero();
    }
}
