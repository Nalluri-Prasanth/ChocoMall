package com.chocomall.admin.service;

import com.chocomall.admin.dto.SalesDtos.CouponParam;
import com.chocomall.admin.dto.SalesDtos.SlotParam;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.model.entity.Coupon;
import com.chocomall.model.entity.CouponHistory;
import com.chocomall.model.entity.HomeSlot;
import com.chocomall.model.enums.CouponGetType;
import com.chocomall.model.enums.CouponType;
import com.chocomall.model.enums.CouponUseStatus;
import com.chocomall.model.enums.HomeSlotType;
import com.chocomall.model.repository.CouponHistoryRepository;
import com.chocomall.model.repository.CouponRepository;
import com.chocomall.model.repository.HomeSlotRepository;
import com.chocomall.model.repository.MemberRepository;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coupons, coupon grants and the home page slots (banners, brands, new and recommended items). */
@Service
@RequiredArgsConstructor
@Transactional
public class MarketingAdminService {

    private final CouponRepository coupons;
    private final CouponHistoryRepository histories;
    private final HomeSlotRepository slots;
    private final MemberRepository members;
    private final ProductRepository products;
    private final ProductCategoryRepository categories;

    // ---------------------------------------------------------------------------- coupons

    @Transactional(readOnly = true)
    public CommonPage<Coupon> list(String keyword, CouponType type, Integer pageNum, Integer pageSize) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return CommonPage.of(coupons.search(kw, type, PageParams.of(pageNum, pageSize, Sort.by(Sort.Order.desc("id")))));
    }

    @Transactional(readOnly = true)
    public Coupon get(Long id) {
        return Asserts.found(coupons.findById(id), "Coupon");
    }

    public Coupon create(CouponParam p) {
        Coupon c = new Coupon();
        apply(c, p);
        return coupons.save(c);
    }

    public Coupon update(Long id, CouponParam p) {
        Coupon c = get(id);
        Asserts.state(p.totalCount() >= c.getReceivedCount(), "The total cannot drop below the " + c.getReceivedCount() + " coupons already claimed");
        apply(c, p);
        return c;
    }

    public void delete(Long id) {
        Coupon c = get(id);
        Asserts.state(c.getReceivedCount() == 0, "Members already hold this coupon; disable it instead");
        coupons.delete(c);
    }

    /** Hands a coupon to a member (compensation, campaign). Respects the per-member limit and the total. */
    public CouponHistory grant(Long couponId, Long memberId) {
        Coupon c = get(couponId);
        Asserts.found(members.findById(memberId), "Member");
        Asserts.state(histories.countByCouponIdAndMemberId(couponId, memberId) < c.getPerLimit(),
                "The member already holds the maximum number of this coupon");
        Asserts.state(coupons.incrementReceived(couponId) == 1, "No coupons left to give out");
        CouponHistory h = new CouponHistory();
        h.setCouponId(couponId);
        h.setMemberId(memberId);
        h.setCouponCode(c.getCode());
        h.setGetType(CouponGetType.ADMIN_GRANT);
        return histories.save(h);
    }

    @Transactional(readOnly = true)
    public CommonPage<CouponHistory> history(Long couponId, CouponUseStatus status, String orderSn, Integer pageNum, Integer pageSize) {
        return CommonPage.of(histories.search(couponId, status, orderSn == null || orderSn.isBlank() ? null : orderSn.trim(),
                PageParams.of(pageNum, pageSize, Sort.by(Sort.Order.desc("id")))));
    }

    private void apply(Coupon c, CouponParam p) {
        Asserts.isTrue(p.endTime().isAfter(p.startTime()), "The end must be after the start");
        Set<Long> productIds = p.productIds() == null ? Set.of() : p.productIds();
        Set<Long> categoryIds = p.categoryIds() == null ? Set.of() : p.categoryIds();
        if (p.couponType() == CouponType.PRODUCT) {
            Asserts.isTrue(!productIds.isEmpty(), "Choose the products this coupon applies to");
            Asserts.isTrue(products.findAllById(productIds).size() == productIds.size(), "One of the products does not exist");
        }
        if (p.couponType() == CouponType.CATEGORY) {
            Asserts.isTrue(!categoryIds.isEmpty(), "Choose the categories this coupon applies to");
            Asserts.isTrue(categories.findAllById(categoryIds).size() == categoryIds.size(), "One of the categories does not exist");
        }
        if (p.code() != null && !p.code().isBlank()) {
            Asserts.state(coupons.findAll().stream().noneMatch(o -> p.code().equalsIgnoreCase(o.getCode()) && !o.getId().equals(c.getId())),
                    "Coupon code " + p.code() + " is already used");
        }
        c.setCouponType(p.couponType());
        c.setName(p.name());
        c.setTotalCount(p.totalCount());
        c.setAmount(p.amount());
        c.setPerLimit(p.perLimit());
        c.setMinSpend(p.minSpend());
        c.setStartTime(p.startTime());
        c.setEndTime(p.endTime());
        c.setNote(p.note());
        c.setCode(p.code() == null || p.code().isBlank() ? null : p.code().trim().toUpperCase());
        c.setMemberLevelId(p.memberLevelId());
        c.setStatus(p.status() == null || p.status());
        c.getProductIds().clear();
        c.getCategoryIds().clear();
        if (p.couponType() == CouponType.PRODUCT) {
            c.getProductIds().addAll(productIds);
        } else if (p.couponType() == CouponType.CATEGORY) {
            c.getCategoryIds().addAll(categoryIds);
        }
    }

    // ---------------------------------------------------------------------------- home slots

    @Transactional(readOnly = true)
    public CommonPage<HomeSlot> slots(HomeSlotType type, Integer pageNum, Integer pageSize) {
        return CommonPage.of(slots.findBySlotType(type, PageParams.of(pageNum, pageSize, Sort.by(Sort.Order.desc("sort")))));
    }

    public HomeSlot createSlot(SlotParam p) {
        HomeSlot s = new HomeSlot();
        applySlot(s, p);
        return slots.save(s);
    }

    public HomeSlot updateSlot(Long id, SlotParam p) {
        HomeSlot s = Asserts.found(slots.findById(id), "Slot");
        applySlot(s, p);
        return s;
    }

    public void deleteSlots(Set<Long> ids) {
        slots.deleteAllById(ids);
    }

    public void setSlotStatus(Long id, boolean status) {
        Asserts.found(slots.findById(id), "Slot").setStatus(status);
    }

    private void applySlot(HomeSlot s, SlotParam p) {
        boolean needsRef = p.slotType() != HomeSlotType.BANNER;
        Asserts.isTrue(!needsRef || p.refId() != null, p.slotType() + " slots must reference a product, brand or subject");
        s.setSlotType(p.slotType());
        s.setRefId(p.refId());
        s.setName(p.name());
        s.setPic(p.pic());
        s.setUrl(p.url());
        s.setSort(p.sort() == null ? 0 : p.sort());
        s.setStatus(p.status() == null || p.status());
        s.setStartTime(p.startTime());
        s.setEndTime(p.endTime());
        s.setNote(p.note());
    }
}
