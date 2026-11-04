package com.chocomall.portal.service;

import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.ResultCode;
import com.chocomall.common.exception.ApiException;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.model.entity.BrandAttention;
import com.chocomall.model.entity.Coupon;
import com.chocomall.model.entity.CouponHistory;
import com.chocomall.model.entity.IntegrationHistory;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.MemberAddress;
import com.chocomall.model.entity.MemberLevel;
import com.chocomall.model.entity.MemberPreference;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductCollection;
import com.chocomall.model.entity.ProductComment;
import com.chocomall.model.entity.ReadHistory;
import com.chocomall.model.enums.CouponGetType;
import com.chocomall.model.enums.CouponUseStatus;
import com.chocomall.model.repository.BrandAttentionRepository;
import com.chocomall.model.repository.BrandRepository;
import com.chocomall.model.repository.CouponHistoryRepository;
import com.chocomall.model.repository.CouponRepository;
import com.chocomall.model.repository.FlavorRepository;
import com.chocomall.model.repository.IntegrationHistoryRepository;
import com.chocomall.model.repository.MemberAddressRepository;
import com.chocomall.model.repository.MemberLevelRepository;
import com.chocomall.model.repository.MemberPreferenceRepository;
import com.chocomall.model.repository.MemberRepository;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.ProductCollectionRepository;
import com.chocomall.model.repository.ProductCommentRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ReadHistoryRepository;
import com.chocomall.portal.dto.PortalDtos.AddressParam;
import com.chocomall.portal.dto.PortalDtos.CollectionView;
import com.chocomall.portal.dto.PortalDtos.CommentParam;
import com.chocomall.portal.dto.PortalDtos.CouponView;
import com.chocomall.portal.dto.PortalDtos.ProductCard;
import com.chocomall.portal.dto.PortalDtos.ReadHistoryView;
import com.chocomall.portal.service.CatalogViewService.SearchQuery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything in "my account": addresses, coupons, browsing history, favourites, followed brands, flavor
 * preferences (stored only with explicit consent and deletable at any time), loyalty points and product reviews.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberCenterService {

    private final MemberRepository members;
    private final MemberLevelRepository levels;
    private final MemberAddressRepository addresses;
    private final CouponRepository coupons;
    private final CouponHistoryRepository couponHistories;
    private final ReadHistoryRepository readHistory;
    private final ProductCollectionRepository collections;
    private final BrandAttentionRepository attentions;
    private final BrandRepository brands;
    private final MemberPreferenceRepository preferences;
    private final FlavorRepository flavors;
    private final IntegrationHistoryRepository pointsHistory;
    private final ProductRepository products;
    private final ProductCommentRepository comments;
    private final OrderItemRepository orderItems;
    private final CatalogViewService catalog;

    // ---------------------------------------------------------------------------- addresses

    @Transactional(readOnly = true)
    public List<MemberAddress> addresses(Long memberId) {
        return addresses.findByMemberId(memberId);
    }

    public MemberAddress addAddress(Long memberId, AddressParam p) {
        MemberAddress a = new MemberAddress();
        a.setMemberId(memberId);
        applyAddress(a, p, memberId, addresses.findByMemberId(memberId).isEmpty());
        return addresses.save(a);
    }

    public MemberAddress updateAddress(Long memberId, Long id, AddressParam p) {
        MemberAddress a = Asserts.found(addresses.findByIdAndMemberId(id, memberId), "Address");
        applyAddress(a, p, memberId, a.isDefaultStatus());
        return a;
    }

    public void deleteAddress(Long memberId, Long id) {
        addresses.delete(Asserts.found(addresses.findByIdAndMemberId(id, memberId), "Address"));
    }

    private void applyAddress(MemberAddress a, AddressParam p, Long memberId, boolean makeDefault) {
        boolean isDefault = Boolean.TRUE.equals(p.defaultStatus()) || makeDefault;
        if (isDefault) {
            addresses.clearDefault(memberId);
        }
        a.setName(p.name());
        a.setPhone(p.phone());
        a.setPostCode(p.postCode());
        a.setProvince(p.province());
        a.setCity(p.city());
        a.setRegion(p.region());
        a.setDetailAddress(p.detailAddress());
        a.setDefaultStatus(isDefault);
    }

    // ---------------------------------------------------------------------------- coupons

    @Transactional(readOnly = true)
    public List<CouponView> myCoupons(Long memberId, CouponUseStatus status) {
        List<CouponHistory> list = status == null ? couponHistories.findByMemberId(memberId) : couponHistories.findByMemberIdAndUseStatus(memberId, status);
        Map<Long, Coupon> byId = coupons.findAllById(list.stream().map(CouponHistory::getCouponId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c));
        LocalDateTime now = LocalDateTime.now();
        return list.stream().map(h -> {
            Coupon c = byId.get(h.getCouponId());
            if (c == null) {
                return null;
            }
            String state = h.getUseStatus() == CouponUseStatus.UNUSED && c.getEndTime().isBefore(now) ? "EXPIRED" : h.getUseStatus().name();
            return new CouponView(h.getId(), c.getId(), c.getName(), c.getAmount(), c.getMinSpend(), c.getEndTime(), state, c.getCouponType().name(), c.getNote());
        }).filter(java.util.Objects::nonNull).toList();
    }

    /** Coupons the member can still claim: active, in date, not exhausted, below the per-member limit. */
    @Transactional(readOnly = true)
    public List<CouponView> claimable(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        Member m = Asserts.found(members.findById(memberId), "Member");
        return coupons.findByStatusTrueAndStartTimeBeforeAndEndTimeAfter(now, now).stream()
                .filter(c -> c.getReceivedCount() < c.getTotalCount())
                .filter(c -> couponHistories.countByCouponIdAndMemberId(c.getId(), memberId) < c.getPerLimit())
                .filter(c -> levelAllows(c, m))
                .map(c -> new CouponView(null, c.getId(), c.getName(), c.getAmount(), c.getMinSpend(), c.getEndTime(), "AVAILABLE",
                        c.getCouponType().name(), c.getNote())).toList();
    }

    public CouponHistory claim(Long memberId, Long couponId) {
        Coupon c = Asserts.found(coupons.findById(couponId), "Coupon");
        Member m = Asserts.found(members.findById(memberId), "Member");
        LocalDateTime now = LocalDateTime.now();
        Asserts.state(c.isStatus() && !c.getStartTime().isAfter(now) && c.getEndTime().isAfter(now), "This coupon is not available right now");
        Asserts.state(levelAllows(c, m), "This coupon is reserved for a higher membership level");
        Asserts.state(couponHistories.countByCouponIdAndMemberId(couponId, memberId) < c.getPerLimit(), "You already hold the maximum number of this coupon");
        if (coupons.incrementReceived(couponId) == 0) {
            throw new ApiException(ResultCode.CONFLICT, "Sorry, this coupon has been fully claimed");
        }
        CouponHistory h = new CouponHistory();
        h.setCouponId(couponId);
        h.setMemberId(memberId);
        h.setCouponCode(c.getCode());
        h.setGetType(CouponGetType.MEMBER_CLAIM);
        return couponHistories.save(h);
    }

    private boolean levelAllows(Coupon c, Member m) {
        if (c.getMemberLevelId() == null) {
            return true;
        }
        MemberLevel required = levels.findById(c.getMemberLevelId()).orElse(null);
        MemberLevel own = m.getLevelId() == null ? null : levels.findById(m.getLevelId()).orElse(null);
        return required == null || (own != null && own.getGrowthPoint() >= required.getGrowthPoint());
    }

    // ---------------------------------------------------------------------------- browsing history, favourites, brands

    /** Records a product view (one row per product, refreshed on every view). */
    public void recordView(Long memberId, Long productId) {
        ReadHistory h = readHistory.findByMemberIdAndProductId(memberId, productId).orElseGet(() -> {
            ReadHistory fresh = new ReadHistory();
            fresh.setMemberId(memberId);
            fresh.setProductId(productId);
            return fresh;
        });
        h.setViewTime(LocalDateTime.now());
        readHistory.save(h);
    }

    @Transactional(readOnly = true)
    public CommonPage<ReadHistoryView> history(Long memberId, Integer pageNum, Integer pageSize) {
        var page = readHistory.findByMemberIdOrderByViewTimeDesc(memberId, PageParams.of(pageNum, pageSize));
        Map<Long, Product> byId = products.findByIdIn(page.getContent().stream().map(ReadHistory::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        return CommonPage.of(page, h -> {
            Product p = byId.get(h.getProductId());
            return new ReadHistoryView(h.getProductId(), p == null ? "(removed)" : p.getName(), p == null ? null : p.getPic(),
                    p == null ? null : p.getPrice(), h.getViewTime());
        });
    }

    public void clearHistory(Long memberId) {
        readHistory.deleteByMemberId(memberId);
    }

    public void addFavourite(Long memberId, Long productId) {
        Asserts.found(products.findById(productId), "Product");
        if (collections.findByMemberIdAndProductId(memberId, productId).isEmpty()) {
            ProductCollection c = new ProductCollection();
            c.setMemberId(memberId);
            c.setProductId(productId);
            collections.save(c);
        }
    }

    public void removeFavourite(Long memberId, Long productId) {
        collections.deleteByMemberIdAndProductId(memberId, productId);
    }

    public void clearFavourites(Long memberId) {
        collections.deleteByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public CommonPage<CollectionView> favourites(Long memberId, Integer pageNum, Integer pageSize) {
        var page = collections.findByMemberIdOrderByCreateTimeDesc(memberId, PageParams.of(pageNum, pageSize));
        Map<Long, Product> byId = products.findByIdIn(page.getContent().stream().map(ProductCollection::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        return CommonPage.of(page, c -> {
            Product p = byId.get(c.getProductId());
            return new CollectionView(c.getProductId(), p == null ? "(removed)" : p.getName(), p == null ? null : p.getPic(),
                    p == null ? null : p.getPrice(), c.getCreateTime());
        });
    }

    public void followBrand(Long memberId, Long brandId) {
        Asserts.found(brands.findById(brandId), "Brand");
        if (attentions.findByMemberIdAndBrandId(memberId, brandId).isEmpty()) {
            BrandAttention a = new BrandAttention();
            a.setMemberId(memberId);
            a.setBrandId(brandId);
            attentions.save(a);
        }
    }

    public void unfollowBrand(Long memberId, Long brandId) {
        attentions.deleteByMemberIdAndBrandId(memberId, brandId);
    }

    @Transactional(readOnly = true)
    public CommonPage<Map<String, Object>> followedBrands(Long memberId, Integer pageNum, Integer pageSize) {
        var page = attentions.findByMemberIdOrderByCreateTimeDesc(memberId, PageParams.of(pageNum, pageSize));
        Map<Long, String> names = brands.findAllById(page.getContent().stream().map(BrandAttention::getBrandId).toList()).stream()
                .collect(Collectors.toMap(b -> b.getId(), b -> b.getName()));
        return CommonPage.of(page, a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("brandId", a.getBrandId());
            m.put("name", names.get(a.getBrandId()));
            m.put("followedAt", a.getCreateTime());
            return m;
        });
    }

    // ---------------------------------------------------------------------------- flavor preferences (with consent)

    @Transactional(readOnly = true)
    public Map<String, Object> preferences(Long memberId) {
        List<MemberPreference> list = preferences.findByMemberId(memberId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("flavorIds", list.stream().map(MemberPreference::getFlavorId).toList());
        out.put("consentGivenAt", list.stream().map(MemberPreference::getConsentTime).findFirst().orElse(null));
        return out;
    }

    /** Saves favourite flavors for better recommendations. Requires explicit consent; keeps nothing else. */
    public void savePreferences(Long memberId, Set<Long> flavorIds, boolean consent) {
        Asserts.isTrue(consent, "Saving flavor preferences needs your consent");
        Asserts.isTrue(flavorIds.size() <= 20, "Choose up to 20 flavors");
        Asserts.isTrue(flavors.findAllById(flavorIds).size() == flavorIds.size(), "One of the flavors does not exist");
        preferences.deleteByMemberId(memberId);
        preferences.flush();
        LocalDateTime now = LocalDateTime.now();
        for (Long id : flavorIds) {
            MemberPreference p = new MemberPreference();
            p.setMemberId(memberId);
            p.setFlavorId(id);
            p.setConsentTime(now);
            preferences.save(p);
        }
    }

    public void deletePreferences(Long memberId) {
        preferences.deleteByMemberId(memberId);
    }

    /** Products matching the saved flavors; falls back to best sellers when the member has none. */
    @Transactional(readOnly = true)
    public List<ProductCard> forYou(Long memberId, int limit) {
        List<MemberPreference> saved = preferences.findByMemberId(memberId);
        Map<Long, ProductCard> picks = new LinkedHashMap<>();
        for (MemberPreference pref : saved) {
            catalog.search(new SearchQuery(null, null, null, null, pref.getFlavorId(), null, null, null, null, null, 2, 1, limit))
                    .getList().forEach(card -> picks.putIfAbsent(card.id(), card));
        }
        if (picks.isEmpty()) {
            catalog.cards(products.findByPublishedTrueOrderBySaleDesc(PageRequest.of(0, limit))).forEach(c -> picks.put(c.id(), c));
        }
        return new ArrayList<>(picks.values()).subList(0, Math.min(limit, picks.size()));
    }

    // ---------------------------------------------------------------------------- points & reviews

    @Transactional(readOnly = true)
    public CommonPage<IntegrationHistory> points(Long memberId, Integer pageNum, Integer pageSize) {
        return CommonPage.of(pointsHistory.findByMemberIdOrderByIdDesc(memberId, PageParams.of(pageNum, pageSize)));
    }

    /** Reviews come from customers who collected the product; one review per product. */
    public ProductComment review(Long memberId, String nickname, CommentParam p) {
        Asserts.isTrue(p.star() >= 1 && p.star() <= 5, "Rate between 1 and 5 stars");
        Asserts.state(orderItems.hasCompletedPurchase(memberId, p.productId()), "You can review a product after your order is completed");
        Asserts.state(!comments.existsByProductIdAndMemberId(p.productId(), memberId), "You already reviewed this product");
        ProductComment c = new ProductComment();
        c.setProductId(p.productId());
        c.setMemberId(memberId);
        c.setMemberNickName(nickname);
        c.setStar(p.star());
        c.setContent(p.content());
        return comments.save(c);
    }

    @Transactional(readOnly = true)
    public CommonPage<ProductComment> reviews(Long productId, Integer pageNum, Integer pageSize) {
        return CommonPage.of(comments.findByProductIdAndShowStatusTrue(productId,
                PageParams.of(pageNum, pageSize, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("id")))));
    }
}
