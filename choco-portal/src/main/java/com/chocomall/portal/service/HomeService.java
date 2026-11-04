package com.chocomall.portal.service;

import com.chocomall.common.exception.Asserts;
import com.chocomall.model.entity.Brand;
import com.chocomall.model.entity.HomeSlot;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.Subject;
import com.chocomall.model.enums.HomeSlotType;
import com.chocomall.model.repository.BrandRepository;
import com.chocomall.model.repository.HomeSlotRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.SubjectRepository;
import com.chocomall.portal.dto.PortalDtos.HomeContent;
import com.chocomall.portal.dto.PortalDtos.NamedItem;
import com.chocomall.portal.dto.PortalDtos.NamedItemWithPic;
import com.chocomall.portal.dto.PortalDtos.ProductCard;
import com.chocomall.portal.dto.PortalDtos.SubjectView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Everything the home page needs in one cached call, plus the editorial subjects. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final HomeSlotRepository slots;
    private final ProductRepository products;
    private final BrandRepository brands;
    private final SubjectRepository subjects;
    private final CatalogViewService catalog;

    /** Cached for a few minutes; staff changes to the home page appear after the cache expires. */
    @Cacheable(value = "homeContent", key = "'home'")
    public HomeContent content() {
        List<NamedItemWithPic> banners = live(HomeSlotType.BANNER).stream()
                .map(s -> new NamedItemWithPic(s.getId(), s.getName(), s.getPic(), s.getUrl())).toList();
        List<NamedItem> brandItems = live(HomeSlotType.BRAND).stream()
                .map(s -> s.getRefId() == null ? null : brands.findById(s.getRefId()).filter(Brand::isShowStatus).orElse(null))
                .filter(java.util.Objects::nonNull).map(b -> new NamedItem(b.getId(), b.getName())).toList();
        return new HomeContent(banners, brandItems, productsOf(HomeSlotType.NEW_PRODUCT), productsOf(HomeSlotType.RECOMMEND_PRODUCT),
                recommendedSubjects(), catalog.categoryTree(), catalog.openWindows(), catalog.activeBundles());
    }

    public List<ProductCard> newProducts(int limit) {
        return catalog.cards(products.findByPublishedTrueAndNewStatusTrueOrderBySortDesc(PageRequest.of(0, limit)));
    }

    public List<ProductCard> recommended(int limit) {
        return catalog.cards(products.findByPublishedTrueAndRecommendStatusTrueOrderBySortDesc(PageRequest.of(0, limit)));
    }

    public List<ProductCard> bestSellers(int limit) {
        return catalog.cards(products.findByPublishedTrueOrderBySaleDesc(PageRequest.of(0, limit)));
    }

    public List<SubjectView> recommendedSubjects() {
        return subjects.findByShowStatusTrueAndRecommendStatusTrueOrderBySortDesc().stream().map(this::view).toList();
    }

    public com.chocomall.common.api.CommonPage<SubjectView> subjects(Integer pageNum, Integer pageSize) {
        return com.chocomall.common.api.CommonPage.of(subjects.findByShowStatusTrue(
                com.chocomall.common.util.PageParams.of(pageNum, pageSize, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("sort"), org.springframework.data.domain.Sort.Order.desc("id")))), this::view);
    }

    /** Reads a subject and counts the view. */
    @Transactional
    public Map<String, Object> subject(Long id) {
        Subject s = Asserts.found(subjects.findById(id).filter(Subject::isShowStatus), "Subject");
        s.setReadCount(s.getReadCount() + 1);
        List<Product> linked = products.findByIdIn(s.getProductIds()).stream().filter(Product::isPublished).toList();
        return Map.of("subject", view(s), "content", s.getContent() == null ? "" : s.getContent(), "products", catalog.cards(linked));
    }

    private SubjectView view(Subject s) {
        return new SubjectView(s.getId(), s.getTitle(), s.getPic(), s.getDescription(), s.getCategory(), s.getReadCount());
    }

    private List<HomeSlot> live(HomeSlotType type) {
        LocalDateTime now = LocalDateTime.now();
        return slots.findBySlotTypeAndStatusTrueOrderBySortDesc(type).stream()
                .filter(s -> (s.getStartTime() == null || !s.getStartTime().isAfter(now)) && (s.getEndTime() == null || s.getEndTime().isAfter(now)))
                .toList();
    }

    private List<ProductCard> productsOf(HomeSlotType type) {
        List<Long> ids = live(type).stream().map(HomeSlot::getRefId).filter(java.util.Objects::nonNull).toList();
        Map<Long, Product> byId = products.findByIdIn(ids).stream().filter(Product::isPublished)
                .collect(Collectors.toMap(Product::getId, p -> p));
        return catalog.cards(ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList());
    }
}
