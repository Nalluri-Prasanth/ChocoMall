package com.chocomall.portal.service;

import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.stock.StockService;
import com.chocomall.model.entity.Allergen;
import com.chocomall.model.entity.Brand;
import com.chocomall.model.entity.Bundle;
import com.chocomall.model.entity.BundleItem;
import com.chocomall.model.entity.ExperienceSession;
import com.chocomall.model.entity.Flavor;
import com.chocomall.model.entity.PreorderWindow;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductCategory;
import com.chocomall.model.entity.ProductClaim;
import com.chocomall.model.entity.ProductSku;
import com.chocomall.model.enums.ClaimType;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.enums.SessionStatus;
import com.chocomall.model.enums.WindowStatus;
import com.chocomall.model.repository.AllergenRepository;
import com.chocomall.model.repository.BrandAttentionRepository;
import com.chocomall.model.repository.BrandRepository;
import com.chocomall.model.repository.BundleItemRepository;
import com.chocomall.model.repository.BundleRepository;
import com.chocomall.model.repository.ChocolateTypeRepository;
import com.chocomall.model.repository.ExperienceSessionRepository;
import com.chocomall.model.repository.FlavorRepository;
import com.chocomall.model.repository.PreorderWindowRepository;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductClaimRepository;
import com.chocomall.model.repository.ProductCollectionRepository;
import com.chocomall.model.repository.ProductCommentRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ProductSkuRepository;
import com.chocomall.portal.dto.PortalDtos.BrandView;
import com.chocomall.portal.dto.PortalDtos.BundleItemView;
import com.chocomall.portal.dto.PortalDtos.BundleView;
import com.chocomall.portal.dto.PortalDtos.CategoryNode;
import com.chocomall.portal.dto.PortalDtos.ClaimBadge;
import com.chocomall.portal.dto.PortalDtos.NamedItem;
import com.chocomall.portal.dto.PortalDtos.ProductCard;
import com.chocomall.portal.dto.PortalDtos.ProductDetailView;
import com.chocomall.portal.dto.PortalDtos.SessionView;
import com.chocomall.portal.dto.PortalDtos.SkuView;
import com.chocomall.portal.dto.PortalDtos.WindowView;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read model of the shop. Only published products are visible, dietary claims are shown only when staff verified
 * their evidence, and allergens are always shown.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogViewService {

    public record SearchQuery(String keyword, Long categoryId, Long brandId, ProductKind kind, Long flavorId,
                              Long chocolateTypeId, Set<Long> excludeAllergens, ClaimType claim, BigDecimal minPrice,
                              BigDecimal maxPrice, Integer sort, Integer pageNum, Integer pageSize) {
    }

    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final ProductCategoryRepository categories;
    private final BrandRepository brands;
    private final ChocolateTypeRepository types;
    private final FlavorRepository flavors;
    private final AllergenRepository allergens;
    private final ProductClaimRepository claims;
    private final ProductCommentRepository comments;
    private final BundleRepository bundles;
    private final BundleItemRepository bundleItems;
    private final PreorderWindowRepository windows;
    private final ExperienceSessionRepository sessions;
    private final ProductCollectionRepository collections;
    private final BrandAttentionRepository attentions;

    // ---------------------------------------------------------------------------- cards & search

    public List<ProductCard> cards(Collection<Product> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, String> allergenNames = allergens.findAll().stream().collect(Collectors.toMap(Allergen::getId, Allergen::getName));
        Map<Long, List<ProductClaim>> verified = claims.findByProductIdInAndVerifiedTrue(list.stream().map(Product::getId).toList())
                .stream().collect(Collectors.groupingBy(ProductClaim::getProductId));
        Map<Long, String> categoryNames = categories.findAll().stream().collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));
        Map<Long, Long> skuCounts = skus.findByProductIdInAndActiveTrue(list.stream().map(Product::getId).toList()).stream()
                .collect(Collectors.groupingBy(ProductSku::getProductId, Collectors.counting()));
        List<ProductCard> out = new ArrayList<>();
        for (Product p : list) {
            boolean tracked = StockService.tracksStock(p);
            int available = p.getStock() - p.getLockStock();
            boolean inStock = p.isBuildABox() ? boxHasStock() : !tracked || available > 0;
            boolean low = tracked && available > 0 && available <= p.getLowStock();
            out.add(new ProductCard(p.getId(), p.getProductSn(), p.getName(), p.getSubTitle(), p.getPic(), p.getPrice(),
                    p.getOriginalPrice(), p.getKind(), p.isPreorderOnly(), p.isBuildABox(), p.isRequiresColdChain(),
                    inStock, low, p.getSale(),
                    p.getAllergenIds().stream().map(allergenNames::get).filter(java.util.Objects::nonNull).sorted().toList(),
                    verified.getOrDefault(p.getId(), List.of()).stream().map(c -> new ClaimBadge(c.getClaimType(), c.getEvidence())).toList(),
                    categoryNames.get(p.getCategoryId()), skuCounts.getOrDefault(p.getId(), 0L).intValue()));
        }
        return out;
    }

    /** A build-a-box needs at least one bonbon on the shelf. */
    private boolean boxHasStock() {
        return !boxCandidates().isEmpty();
    }

    /** Bonbons that can go into a build-a-box: published, stocked and in the bonbon category. */
    public List<Product> boxCandidates() {
        Long bonbonCategory = categories.findAll().stream().filter(c -> "Bonbons".equals(c.getName())).map(ProductCategory::getId)
                .findFirst().orElse(null);
        if (bonbonCategory == null) {
            return List.of();
        }
        return products.findByPublishedTrueAndCategoryIdInOrderBySortDesc(List.of(bonbonCategory), PageRequest.of(0, 200)).stream()
                .filter(p -> StockService.tracksStock(p) && p.getStock() - p.getLockStock() > 0).toList();
    }

    public CommonPage<ProductCard> search(SearchQuery q) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.isTrue(root.get("published")));
            if (q.keyword() != null && !q.keyword().isBlank()) {
                String like = "%" + q.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                all.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(cb.coalesce(root.<String>get("subTitle"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("keywords"), "")), like),
                        cb.like(cb.lower(root.get("productSn")), like)));
            }
            if (q.categoryId() != null) {
                List<Long> ids = new ArrayList<>(List.of(q.categoryId()));
                categories.findByParentIdOrderBySortDesc(q.categoryId()).forEach(c -> ids.add(c.getId()));
                all.add(root.get("categoryId").in(ids));
            }
            if (q.brandId() != null) {
                all.add(cb.equal(root.get("brandId"), q.brandId()));
            }
            if (q.kind() != null) {
                all.add(cb.equal(root.get("kind"), q.kind()));
            }
            if (q.chocolateTypeId() != null) {
                all.add(cb.equal(root.get("chocolateTypeId"), q.chocolateTypeId()));
            }
            if (q.minPrice() != null) {
                all.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), q.minPrice()));
            }
            if (q.maxPrice() != null) {
                all.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), q.maxPrice()));
            }
            if (q.flavorId() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Product> p2 = sub.from(Product.class);
                sub.select(p2.<Long>get("id")).where(cb.equal(p2.get("id"), root.get("id")), p2.join("flavorIds").in(q.flavorId()));
                all.add(cb.exists(sub));
            }
            if (q.excludeAllergens() != null && !q.excludeAllergens().isEmpty()) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Product> p2 = sub.from(Product.class);
                sub.select(p2.<Long>get("id")).where(cb.equal(p2.get("id"), root.get("id")), p2.join("allergenIds").in(q.excludeAllergens()));
                all.add(cb.not(cb.exists(sub)));
            }
            if (q.claim() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<ProductClaim> c = sub.from(ProductClaim.class);
                sub.select(c.<Long>get("id")).where(cb.equal(c.get("productId"), root.get("id")),
                        cb.equal(c.get("claimType"), q.claim()), cb.isTrue(c.get("verified")));
                all.add(cb.exists(sub));
            }
            return cb.and(all.toArray(Predicate[]::new));
        };
        var page = products.findAll(spec, PageParams.of(q.pageNum(), q.pageSize(), sortFor(q.sort())));
        return new CommonPage<>(page.getNumber() + 1, page.getSize(), page.getTotalPages(), page.getTotalElements(), cards(page.getContent()));
    }

    private static Sort sortFor(Integer code) {
        int c = code == null ? 0 : code;
        return switch (c) {
            case 1 -> Sort.by(Sort.Order.desc("id"));
            case 2 -> Sort.by(Sort.Order.desc("sale"), Sort.Order.desc("id"));
            case 3 -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("id"));
            case 4 -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
            default -> Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id"));
        };
    }

    // ---------------------------------------------------------------------------- detail

    public ProductDetailView detail(Long id, Long memberId) {
        Product p = Asserts.found(products.findById(id).filter(Product::isPublished), "Product");
        String category = categories.findById(p.getCategoryId()).map(ProductCategory::getName).orElse(null);
        String brand = p.getBrandId() == null ? null : brands.findById(p.getBrandId()).map(Brand::getName).orElse(null);
        String type = p.getChocolateTypeId() == null ? null : types.findById(p.getChocolateTypeId()).map(t -> t.getName()).orElse(null);
        List<NamedItem> flavorItems = flavors.findAllById(p.getFlavorIds()).stream().map((Flavor f) -> new NamedItem(f.getId(), f.getName())).toList();
        List<SkuView> skuViews = skus.findByProductIdOrderBySortAscIdAsc(id).stream().filter(ProductSku::isActive)
                .map(s -> new SkuView(s.getId(), s.getSkuCode(), s.getLabel(), s.getUnits(), s.getPrice())).toList();
        Double avg = comments.averageStar(id);
        long commentCount = comments.findByProductIdAndShowStatusTrue(id, PageRequest.of(0, 1)).getTotalElements();
        List<WindowView> windowViews = p.isPreorderOnly() ? openWindows() : List.of();
        List<SessionView> sessionViews = p.getKind() == ProductKind.EXPERIENCE ? upcomingSessions(p) : List.of();
        boolean favourite = memberId != null && collections.findByMemberIdAndProductId(memberId, id).isPresent();
        return new ProductDetailView(cards(List.of(p)).get(0), p.getDescription(), p.getCategoryId(), category, brand, type,
                flavorItems, skuViews, avg == null ? null : Math.round(avg * 10) / 10.0, commentCount, p.getShelfLifeDays(),
                bundlesContaining(id), windowViews, sessionViews, p.getWeightGrams(), favourite);
    }

    public List<ProductCard> related(Long productId, int limit) {
        Product p = Asserts.found(products.findById(productId).filter(Product::isPublished), "Product");
        return cards(products.findByPublishedTrueAndCategoryIdInOrderBySortDesc(List.of(p.getCategoryId()), PageRequest.of(0, limit + 1))
                .stream().filter(x -> !x.getId().equals(productId)).limit(limit).toList());
    }

    public List<NamedItem> allergenList() {
        return allergens.findAll().stream().map(a -> new NamedItem(a.getId(), a.getName())).toList();
    }

    public List<NamedItem> flavorList() {
        return flavors.findAll().stream().filter(f -> f.getPriority() != com.chocomall.model.enums.Priority.SPECIALIST)
                .map(f -> new NamedItem(f.getId(), f.getName())).toList();
    }

    public List<NamedItem> chocolateTypeList() {
        return types.findAll().stream().map(t -> new NamedItem(t.getId(), t.getName())).toList();
    }

    // ---------------------------------------------------------------------------- categories & brands

    @Cacheable("categoryTree")
    public List<CategoryNode> categoryTree() {
        List<ProductCategory> all = categories.findByShowStatusTrueOrderBySortDesc();
        Map<Long, CategoryNode> nodes = new LinkedHashMap<>();
        all.forEach(c -> nodes.put(c.getId(), new CategoryNode(c.getId(), c.getName(), c.getIcon(), new ArrayList<>())));
        List<CategoryNode> roots = new ArrayList<>();
        all.forEach(c -> {
            CategoryNode node = nodes.get(c.getId());
            CategoryNode parent = nodes.get(c.getParentId());
            (parent == null ? roots : parent.children()).add(node);
        });
        return roots;
    }

    public CommonPage<BrandView> brands(String keyword, String region, Integer pageNum, Integer pageSize, Long memberId) {
        var page = brands.search(blank(keyword), blank(region), null, true,
                PageParams.of(pageNum, pageSize, Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("name"))));
        return CommonPage.of(page, b -> view(b, memberId));
    }

    public BrandView brand(Long id, Long memberId) {
        return view(Asserts.found(brands.findById(id).filter(Brand::isShowStatus), "Brand"), memberId);
    }

    public List<ProductCard> brandProducts(Long brandId) {
        return cards(products.findByPublishedTrueAndBrandId(brandId));
    }

    private BrandView view(Brand b, Long memberId) {
        boolean followed = memberId != null && attentions.findByMemberIdAndBrandId(memberId, b.getId()).isPresent();
        return new BrandView(b.getId(), b.getName(), b.getRegion(), b.getAssociatedMarket(), b.getSegment(),
                b.getTypicalFormats(), b.getLogo(), b.getReferenceUrl(), followed);
    }

    // ---------------------------------------------------------------------------- bundles, windows, sessions

    public List<BundleView> activeBundles() {
        LocalDateTime now = LocalDateTime.now();
        return bundles.findByStatusTrueOrderBySortDesc().stream()
                .filter(b -> (b.getStartTime() == null || !b.getStartTime().isAfter(now)) && (b.getEndTime() == null || b.getEndTime().isAfter(now)))
                .map(this::view).toList();
    }

    private List<BundleView> bundlesContaining(Long productId) {
        Set<Long> bundleIds = new java.util.HashSet<>();
        bundleItems.findAll().stream().filter(i -> i.getProductId().equals(productId)).forEach(i -> bundleIds.add(i.getBundleId()));
        return activeBundles().stream().filter(b -> bundleIds.contains(b.id())).toList();
    }

    private BundleView view(Bundle b) {
        List<BundleItem> items = bundleItems.findByBundleId(b.getId());
        Map<Long, Product> byId = products.findByIdIn(items.stream().map(BundleItem::getProductId).toList())
                .stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, ProductSku> skuById = new HashMap<>();
        skus.findAllById(items.stream().map(BundleItem::getSkuId).filter(java.util.Objects::nonNull).toList()).forEach(s -> skuById.put(s.getId(), s));
        List<BundleItemView> itemViews = items.stream().filter(i -> byId.containsKey(i.getProductId()))
                .map(i -> new BundleItemView(i.getProductId(), byId.get(i.getProductId()).getName(),
                        i.getSkuId() == null ? null : skuById.get(i.getSkuId()) == null ? null : skuById.get(i.getSkuId()).getLabel(),
                        i.getQuantity())).toList();
        return new BundleView(b.getId(), b.getName(), b.getDescription(), b.getPic(), b.getPrice(), b.getOriginalPrice(), itemViews);
    }

    public List<WindowView> openWindows() {
        return windows.findByStatusAndOrderCutoffAfterOrderByOrderCutoff(WindowStatus.OPEN, LocalDateTime.now()).stream()
                .map(w -> new WindowView(w.getId(), w.getName(), w.getOccasion(), w.getOrderCutoff(), w.getFulfilFrom(), w.getFulfilTo(),
                        Math.max(0, w.getCapacityUnits() - w.getReservedUnits()))).toList();
    }

    public List<SessionView> upcomingSessions(Product experience) {
        return sessions.findByProductIdAndStatusAndStartsAtAfterOrderByStartsAt(experience.getId(), SessionStatus.OPEN, LocalDateTime.now())
                .stream().map(s -> sessionView(s, experience)).toList();
    }

    public List<SessionView> allUpcomingSessions() {
        Map<Long, Product> byId = new HashMap<>();
        return sessions.findByStatusAndStartsAtAfterOrderByStartsAt(SessionStatus.OPEN, LocalDateTime.now()).stream().map(s -> {
            Product p = byId.computeIfAbsent(s.getProductId(), id -> products.findById(id).orElse(null));
            return p == null || !p.isPublished() ? null : sessionView(s, p);
        }).filter(java.util.Objects::nonNull).toList();
    }

    private SessionView sessionView(ExperienceSession s, Product p) {
        return new SessionView(s.getId(), p.getId(), p.getName(), s.getStartsAt(), s.getDurationMinutes(), s.getLocation(),
                s.getPrice() != null ? s.getPrice() : p.getPrice(), Math.max(0, s.getCapacity() - s.getBooked()));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
