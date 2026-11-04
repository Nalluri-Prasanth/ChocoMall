package com.chocomall.admin.service;

import com.chocomall.admin.dto.OpsDtos.BundleItemParam;
import com.chocomall.admin.dto.OpsDtos.BundleParam;
import com.chocomall.admin.dto.OpsDtos.SessionParam;
import com.chocomall.admin.dto.OpsDtos.WindowParam;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.experience.ExperienceBookingService;
import com.chocomall.model.entity.Bundle;
import com.chocomall.model.entity.BundleItem;
import com.chocomall.model.entity.ExperienceBooking;
import com.chocomall.model.entity.ExperienceSession;
import com.chocomall.model.entity.PreorderWindow;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductSku;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.enums.SessionStatus;
import com.chocomall.model.enums.WindowStatus;
import com.chocomall.model.repository.BundleItemRepository;
import com.chocomall.model.repository.BundleRepository;
import com.chocomall.model.repository.ExperienceBookingRepository;
import com.chocomall.model.repository.ExperienceSessionRepository;
import com.chocomall.model.repository.PreorderWindowRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ProductSkuRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Capacity planning: preorder windows, experience sessions and bundles. */
@Service
@RequiredArgsConstructor
@Transactional
public class PlanningService {

    private final PreorderWindowRepository windows;
    private final ExperienceSessionRepository sessions;
    private final ExperienceBookingRepository bookings;
    private final ExperienceBookingService bookingService;
    private final BundleRepository bundles;
    private final BundleItemRepository bundleItems;
    private final ProductRepository products;
    private final ProductSkuRepository skus;

    // ---------------------------------------------------------------------------- preorder windows

    @Transactional(readOnly = true)
    public List<PreorderWindow> windows() {
        return windows.findAll(Sort.by(Sort.Order.desc("orderCutoff")));
    }

    @Transactional(readOnly = true)
    public PreorderWindow window(Long id) {
        return Asserts.found(windows.findById(id), "Preorder window");
    }

    public PreorderWindow createWindow(WindowParam p) {
        PreorderWindow w = new PreorderWindow();
        applyWindow(w, p);
        return windows.save(w);
    }

    public PreorderWindow updateWindow(Long id, WindowParam p) {
        PreorderWindow w = window(id);
        Asserts.state(p.capacityUnits() >= w.getReservedUnits(),
                "Capacity cannot drop below the " + w.getReservedUnits() + " units already reserved");
        applyWindow(w, p);
        return w;
    }

    public PreorderWindow setWindowStatus(Long id, WindowStatus status) {
        PreorderWindow w = window(id);
        if (status == WindowStatus.OPEN) {
            Asserts.state(w.getOrderCutoff().isAfter(LocalDateTime.now()), "The order cut-off has passed; move it first");
        }
        w.setStatus(status);
        return w;
    }

    public void deleteWindow(Long id) {
        PreorderWindow w = window(id);
        Asserts.state(w.getReservedUnits() == 0, "Orders already reserve capacity in this window");
        windows.delete(w);
    }

    private void applyWindow(PreorderWindow w, WindowParam p) {
        Asserts.isTrue(p.orderCutoff().isAfter(LocalDateTime.now()) || w.getId() != null, "The cut-off must be in the future");
        Asserts.isTrue(!p.fulfilFrom().isBefore(p.orderCutoff().toLocalDate()), "Fulfilment cannot start before the cut-off");
        Asserts.isTrue(!p.fulfilTo().isBefore(p.fulfilFrom()), "Fulfilment end cannot be before its start");
        w.setName(p.name());
        w.setOccasion(p.occasion());
        w.setOrderCutoff(p.orderCutoff());
        w.setFulfilFrom(p.fulfilFrom());
        w.setFulfilTo(p.fulfilTo());
        w.setCapacityUnits(p.capacityUnits());
        w.setNote(p.note());
    }

    // ---------------------------------------------------------------------------- experience sessions

    @Transactional(readOnly = true)
    public List<ExperienceSession> upcomingSessions(Long productId) {
        LocalDateTime now = LocalDateTime.now();
        return productId == null
                ? sessions.findByStatusAndStartsAtAfterOrderByStartsAt(SessionStatus.OPEN, now)
                : sessions.findByProductIdAndStatusAndStartsAtAfterOrderByStartsAt(productId, SessionStatus.OPEN, now);
    }

    @Transactional(readOnly = true)
    public ExperienceSession session(Long id) {
        return Asserts.found(sessions.findById(id), "Session");
    }

    public ExperienceSession createSession(SessionParam p) {
        ExperienceSession s = new ExperienceSession();
        applySession(s, p);
        return sessions.save(s);
    }

    public ExperienceSession updateSession(Long id, SessionParam p) {
        ExperienceSession s = session(id);
        Asserts.state(s.getStatus() != SessionStatus.CANCELLED, "The session is cancelled");
        Asserts.state(p.capacity() >= s.getBooked(), "Capacity cannot drop below the " + s.getBooked() + " seats already booked");
        applySession(s, p);
        return s;
    }

    /** Cancels the session and every booking on it, refunding confirmed payments. Returns affected bookings. */
    public int cancelSession(Long id) {
        session(id);
        return bookingService.cancelSession(id);
    }

    @Transactional(readOnly = true)
    public CommonPage<ExperienceBooking> bookings(Long sessionId, Integer pageNum, Integer pageSize) {
        return CommonPage.of(bookings.findBySessionId(sessionId, PageParams.of(pageNum, pageSize)));
    }

    private void applySession(ExperienceSession s, SessionParam p) {
        Product product = Asserts.found(products.findById(p.productId()), "Product");
        Asserts.isTrue(product.getKind() == ProductKind.EXPERIENCE, "Sessions can only be created for experience products");
        Asserts.isTrue(p.startsAt().isAfter(LocalDateTime.now()) || s.getId() != null, "The session must start in the future");
        s.setProductId(p.productId());
        s.setStartsAt(p.startsAt());
        s.setDurationMinutes(p.durationMinutes() == null ? 60 : p.durationMinutes());
        s.setCapacity(p.capacity());
        s.setPrice(p.price());
        s.setLocation(p.location());
    }

    // ---------------------------------------------------------------------------- bundles

    @Transactional(readOnly = true)
    public List<Map<String, Object>> bundles() {
        return bundles.findAll(Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id"))).stream()
                .map(b -> Map.<String, Object>of("bundle", b, "items", bundleItems.findByBundleId(b.getId()))).toList();
    }

    public Bundle createBundle(BundleParam p) {
        Bundle b = new Bundle();
        applyBundle(b, p);
        Bundle saved = bundles.save(b);
        saveItems(saved.getId(), p.items());
        return saved;
    }

    public Bundle updateBundle(Long id, BundleParam p) {
        Bundle b = Asserts.found(bundles.findById(id), "Bundle");
        applyBundle(b, p);
        bundleItems.deleteByBundleId(id);
        bundleItems.flush();
        saveItems(id, p.items());
        return b;
    }

    public void deleteBundle(Long id) {
        bundleItems.deleteByBundleId(id);
        bundles.deleteById(id);
    }

    private void applyBundle(Bundle b, BundleParam p) {
        Asserts.isTrue(p.price().signum() > 0, "The bundle price must be positive");
        Asserts.isTrue(p.startTime() == null || p.endTime() == null || p.endTime().isAfter(p.startTime()), "The end must be after the start");
        b.setName(p.name());
        b.setDescription(p.description());
        b.setPic(p.pic());
        b.setPrice(p.price());
        b.setOriginalPrice(p.originalPrice());
        b.setStartTime(p.startTime());
        b.setEndTime(p.endTime());
        b.setStatus(p.status() == null || p.status());
        b.setSort(p.sort() == null ? 0 : p.sort());
    }

    private void saveItems(Long bundleId, List<BundleItemParam> items) {
        for (BundleItemParam in : items) {
            Product product = Asserts.found(products.findById(in.productId()), "Product " + in.productId());
            if (in.skuId() != null) {
                ProductSku sku = Asserts.found(skus.findById(in.skuId()), "SKU " + in.skuId());
                Asserts.isTrue(sku.getProductId().equals(product.getId()), "SKU " + sku.getSkuCode() + " does not belong to " + product.getName());
            }
            BundleItem item = new BundleItem();
            item.setBundleId(bundleId);
            item.setProductId(in.productId());
            item.setSkuId(in.skuId());
            item.setQuantity(in.quantity());
            bundleItems.save(item);
        }
    }
}
