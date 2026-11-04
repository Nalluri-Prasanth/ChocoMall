package com.chocomall.admin.service;

import com.chocomall.admin.dto.CatalogDtos.ClaimParam;
import com.chocomall.admin.dto.CatalogDtos.ClaimView;
import com.chocomall.admin.dto.CatalogDtos.ContributionView;
import com.chocomall.admin.dto.CatalogDtos.NamedRef;
import com.chocomall.admin.dto.CatalogDtos.ProductDetail;
import com.chocomall.admin.dto.CatalogDtos.ProductParam;
import com.chocomall.admin.dto.CatalogDtos.ProductQuery;
import com.chocomall.admin.dto.CatalogDtos.SkuParam;
import com.chocomall.admin.security.CurrentAdmin;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.ResultCode;
import com.chocomall.common.exception.ApiException;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.messaging.SearchSyncPublisher;
import com.chocomall.core.stock.StockService;
import com.chocomall.core.tx.AfterCommit;
import com.chocomall.model.entity.Allergen;
import com.chocomall.model.entity.Flavor;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductClaim;
import com.chocomall.model.entity.ProductSku;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.enums.StockReason;
import com.chocomall.model.repository.AllergenRepository;
import com.chocomall.model.repository.BrandRepository;
import com.chocomall.model.repository.ChocolateTypeRepository;
import com.chocomall.model.repository.FlavorRepository;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductClaimRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ProductSkuRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product management: creation and edits with their business rules, publishing gates (allergens reviewed,
 * at least one active SKU), claim verification and the contribution-per-unit view.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductAdminService {

    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final ProductCategoryRepository categories;
    private final BrandRepository brands;
    private final ChocolateTypeRepository types;
    private final FlavorRepository flavors;
    private final AllergenRepository allergens;
    private final ProductClaimRepository claims;
    private final OrderItemRepository orderItems;
    private final StockService stock;
    private final SearchSyncPublisher searchSync;
    private final EntityManager em;

    // ---------------------------------------------------------------------------- create / update

    public ProductDetail create(ProductParam param) {
        Asserts.state(products.findByProductSn(param.productSn()).isEmpty(), "Product number " + param.productSn() + " is already used");
        Product p = new Product();
        apply(p, param);
        Product saved = products.save(p);
        syncSkus(saved, param.skus());
        if (param.initialStock() != null && param.initialStock() > 0 && StockService.tracksStock(saved)) {
            stock.adjust(saved.getId(), param.initialStock(), StockReason.ADJUSTMENT, "initial-stock", CurrentAdmin.username());
        }
        refreshCounts(saved.getCategoryId(), saved.getBrandId());
        notifySearch(saved.getId());
        return detail(saved.getId());
    }

    public ProductDetail update(Long id, ProductParam param) {
        Product p = load(id);
        if (!p.getProductSn().equals(param.productSn())) {
            Asserts.state(products.findByProductSn(param.productSn()).isEmpty(), "Product number " + param.productSn() + " is already used");
        }
        Long oldCategory = p.getCategoryId();
        Long oldBrand = p.getBrandId();
        apply(p, param);
        if (p.isPublished()) {
            ensurePublishable(p, param.skus().stream().anyMatch(s -> !Boolean.FALSE.equals(s.active())));
        }
        products.save(p);
        syncSkus(p, param.skus());
        refreshCounts(oldCategory, oldBrand);
        refreshCounts(p.getCategoryId(), p.getBrandId());
        notifySearch(id);
        return detail(id);
    }

    @Transactional(readOnly = true)
    public ProductDetail detail(Long id) {
        Product p = load(id);
        // stock counters change through atomic UPDATE statements that bypass the persistence context
        em.flush();
        em.refresh(p);
        String category = categories.findById(p.getCategoryId()).map(c -> c.getName()).orElse(null);
        String brand = p.getBrandId() == null ? null : brands.findById(p.getBrandId()).map(b -> b.getName()).orElse(null);
        String type = p.getChocolateTypeId() == null ? null : types.findById(p.getChocolateTypeId()).map(t -> t.getName()).orElse(null);
        List<NamedRef> flavorRefs = flavors.findAllById(p.getFlavorIds()).stream().map(f -> new NamedRef(f.getId(), f.getName())).toList();
        List<NamedRef> allergenRefs = allergens.findAllById(p.getAllergenIds()).stream().map(a -> new NamedRef(a.getId(), a.getName())).toList();
        return new ProductDetail(p, category, brand, type, p.getStock() - p.getLockStock(),
                skus.findByProductIdOrderBySortAscIdAsc(id), flavorRefs, allergenRefs,
                claims.findByProductId(id).stream().map(this::toView).toList());
    }

    @Transactional(readOnly = true)
    public CommonPage<Product> list(ProductQuery q) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            if (q.keyword() != null && !q.keyword().isBlank()) {
                String like = "%" + q.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                all.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("productSn")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("keywords"), "")), like)));
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
            if (q.launchTier() != null) {
                all.add(cb.equal(root.get("launchTier"), q.launchTier()));
            }
            if (q.published() != null) {
                all.add(cb.equal(root.get("published"), q.published()));
            }
            if (q.recommended() != null) {
                all.add(cb.equal(root.get("recommendStatus"), q.recommended()));
            }
            if (q.isNew() != null) {
                all.add(cb.equal(root.get("newStatus"), q.isNew()));
            }
            if (Boolean.TRUE.equals(q.lowStock())) {
                all.add(cb.le(cb.diff(root.<Integer>get("stock"), root.<Integer>get("lockStock")), root.<Integer>get("lowStock")));
            }
            return cb.and(all.toArray(Predicate[]::new));
        };
        Sort sort = Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id"));
        return CommonPage.of(products.findAll(spec, PageParams.of(q.pageNum(), q.pageSize(), sort)));
    }

    // ---------------------------------------------------------------------------- status flags

    public void setPublished(Set<Long> ids, boolean value) {
        List<Product> found = products.findAllById(ids);
        Asserts.isTrue(found.size() == ids.size(), "One of the products does not exist");
        if (value) {
            List<String> problems = new ArrayList<>();
            for (Product p : found) {
                try {
                    ensurePublishable(p, skus.findByProductIdOrderBySortAscIdAsc(p.getId()).stream().anyMatch(ProductSku::isActive));
                } catch (ApiException e) {
                    problems.add(p.getProductSn() + " " + p.getName() + ": " + e.getMessage());
                }
            }
            if (!problems.isEmpty()) {
                throw new ApiException(ResultCode.CONFLICT, "Cannot publish. " + String.join("; ", problems));
            }
        }
        found.forEach(p -> {
            p.setPublished(value);
            notifySearch(p.getId());
        });
    }

    public void setNew(Set<Long> ids, boolean value) {
        products.findAllById(ids).forEach(p -> {
            p.setNewStatus(value);
            notifySearch(p.getId());
        });
    }

    public void setRecommended(Set<Long> ids, boolean value) {
        products.findAllById(ids).forEach(p -> {
            p.setRecommendStatus(value);
            notifySearch(p.getId());
        });
    }

    /** Soft delete: the product disappears from the shop and back office but past orders keep their snapshot. */
    public void delete(Set<Long> ids) {
        for (Product p : products.findAllById(ids)) {
            Asserts.state(!orderItems.hasOpenOrder(p.getId()),
                    p.getName() + " is part of an open order. Finish or cancel those orders first.");
            p.setDeleted(true);
            p.setPublished(false);
            refreshCounts(p.getCategoryId(), p.getBrandId());
            AfterCommit.run(() -> searchSync.productRemoved(p.getId()));
        }
    }

    // ---------------------------------------------------------------------------- claims

    public ClaimView addClaim(Long productId, ClaimParam param) {
        load(productId);
        ProductClaim c = claims.findByProductIdAndClaimType(productId, param.claimType()).orElseGet(ProductClaim::new);
        c.setProductId(productId);
        c.setClaimType(param.claimType());
        c.setEvidence(param.evidence());
        c.setVerified(false);
        c.setVerifiedBy(null);
        c.setVerifiedTime(null);
        return toView(claims.save(c));
    }

    /** A claim becomes visible to shoppers only after staff confirm that supplier or process evidence exists. */
    public ClaimView verifyClaim(Long claimId) {
        ProductClaim c = Asserts.found(claims.findById(claimId), "Claim");
        Asserts.isTrue(c.getEvidence() != null && !c.getEvidence().isBlank(),
                "Record the supplier or process evidence before verifying this claim");
        c.setVerified(true);
        c.setVerifiedBy(CurrentAdmin.username());
        c.setVerifiedTime(LocalDateTime.now());
        notifySearch(c.getProductId());
        return toView(c);
    }

    public void removeClaim(Long claimId) {
        ProductClaim c = Asserts.found(claims.findById(claimId), "Claim");
        claims.delete(c);
        notifySearch(c.getProductId());
    }

    // ---------------------------------------------------------------------------- economics

    @Transactional(readOnly = true)
    public ContributionView contribution(Long productId) {
        return contributionOf(load(productId));
    }

    public static ContributionView contributionOf(Product p) {
        boolean complete = p.getIngredientCost() != null && p.getPackagingCost() != null && p.getLaborCost() != null;
        BigDecimal ingredient = zero(p.getIngredientCost());
        BigDecimal packaging = zero(p.getPackagingCost());
        BigDecimal labor = zero(p.getLaborCost());
        BigDecimal fee = p.getPrice().multiply(p.getSellingFeeRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal contribution = p.getPrice().subtract(ingredient).subtract(packaging).subtract(labor).subtract(fee);
        BigDecimal margin = contribution.multiply(BigDecimal.valueOf(100)).divide(p.getPrice(), 1, RoundingMode.HALF_UP);
        return new ContributionView(p.getId(), p.getName(), p.getPrice(), ingredient, packaging, labor, fee, contribution, margin, complete);
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ---------------------------------------------------------------------------- internals

    private Product load(Long id) {
        return Asserts.found(products.findById(id), "Product");
    }

    private void apply(Product p, ProductParam in) {
        Asserts.found(categories.findById(in.categoryId()), "Category");
        if (in.brandId() != null) {
            Asserts.found(brands.findById(in.brandId()), "Brand");
        }
        if (in.chocolateTypeId() != null) {
            Asserts.found(types.findById(in.chocolateTypeId()), "Chocolate type");
        }
        Set<Long> flavorIds = in.flavorIds() == null ? Set.of() : in.flavorIds();
        Set<Long> allergenIds = in.allergenIds() == null ? Set.of() : in.allergenIds();
        Asserts.isTrue(flavors.findAllById(flavorIds).stream().map(Flavor::getId).collect(Collectors.toSet()).size() == flavorIds.size(),
                "One of the flavors does not exist");
        Asserts.isTrue(allergens.findAllById(allergenIds).stream().map(Allergen::getId).collect(Collectors.toSet()).size() == allergenIds.size(),
                "One of the allergens does not exist");

        boolean buildABox = Boolean.TRUE.equals(in.buildABox());
        boolean preorder = Boolean.TRUE.equals(in.preorderOnly());
        Asserts.isTrue(!buildABox || in.kind() == ProductKind.MADE_IN_SHOP, "A build-a-box product must be made in the shop");
        Asserts.isTrue(!(buildABox && preorder), "A build-a-box product cannot be preorder-only");
        Asserts.isTrue(!(preorder && (in.kind() == ProductKind.EXPERIENCE || in.kind() == ProductKind.PERSONALIZED)),
                "Experiences and personalised items are booked or requested, not preordered");
        if (in.originalPrice() != null) {
            Asserts.isTrue(in.originalPrice().compareTo(in.price()) >= 0, "The original price cannot be lower than the price");
        }

        p.setBrandId(in.brandId());
        p.setCategoryId(in.categoryId());
        p.setChocolateTypeId(in.chocolateTypeId());
        p.setProductSn(in.productSn());
        p.setName(in.name());
        p.setPic(in.pic());
        p.setSubTitle(in.subTitle());
        p.setDescription(in.description());
        p.setPrice(in.price());
        p.setOriginalPrice(in.originalPrice());
        p.setLowStock(in.lowStock() == null ? 10 : in.lowStock());
        p.setUnit(in.unit() == null || in.unit().isBlank() ? "piece" : in.unit());
        p.setWeightGrams(in.weightGrams());
        p.setKind(in.kind());
        p.setLaunchTier(in.launchTier());
        p.setSellingFormat(in.sellingFormat());
        p.setSalesPurpose(in.salesPurpose());
        p.setPreorderOnly(preorder);
        p.setBuildABox(buildABox);
        p.setRequiresColdChain(Boolean.TRUE.equals(in.requiresColdChain()));
        p.setAllergensReviewed(Boolean.TRUE.equals(in.allergensReviewed()));
        p.setShelfLifeDays(in.shelfLifeDays());
        p.setIngredientCost(in.ingredientCost());
        p.setPackagingCost(in.packagingCost());
        p.setLaborCost(in.laborCost());
        p.setSellingFeeRate(in.sellingFeeRate() == null ? BigDecimal.ZERO : in.sellingFeeRate());
        p.setKeywords(in.keywords());
        p.setSort(in.sort() == null ? 0 : in.sort());
        p.getFlavorIds().clear();
        p.getFlavorIds().addAll(flavorIds);
        p.getAllergenIds().clear();
        p.getAllergenIds().addAll(allergenIds);
    }

    private void ensurePublishable(Product p, boolean hasActiveSku) {
        Asserts.state(p.isAllergensReviewed(), "allergens have not been reviewed");
        Asserts.state(hasActiveSku, "it has no active SKU");
        Asserts.state(p.getPrice() != null && p.getPrice().signum() > 0, "it has no price");
    }

    /** Aligns the SKUs with the request: update by code, add new ones, drop (or deactivate when ordered) the rest. */
    private void syncSkus(Product p, List<SkuParam> wanted) {
        Map<String, ProductSku> existing = new HashMap<>();
        skus.findByProductIdOrderBySortAscIdAsc(p.getId()).forEach(s -> existing.put(s.getSkuCode(), s));
        Set<String> seen = new java.util.HashSet<>();
        int order = 0;
        for (SkuParam in : wanted) {
            Asserts.isTrue(seen.add(in.skuCode()), "Duplicate SKU code " + in.skuCode());
            ProductSku s = existing.get(in.skuCode());
            if (s == null) {
                skus.findBySkuCode(in.skuCode()).ifPresent(other ->
                        Asserts.state(false, "SKU code " + in.skuCode() + " belongs to another product"));
                s = new ProductSku();
                s.setProductId(p.getId());
                s.setSkuCode(in.skuCode());
            }
            s.setLabel(in.label());
            s.setUnits(in.units());
            s.setPrice(in.price());
            s.setPic(in.pic());
            s.setSort(in.sort() == null ? order : in.sort());
            s.setActive(!Boolean.FALSE.equals(in.active()));
            skus.save(s);
            order++;
        }
        existing.values().stream().filter(s -> !seen.contains(s.getSkuCode())).forEach(s -> {
            if (orderItems.existsByProductSkuId(s.getId())) {
                s.setActive(false);
            } else {
                skus.delete(s);
            }
        });
    }

    private void refreshCounts(Long categoryId, Long brandId) {
        if (categoryId != null) {
            categories.findById(categoryId).ifPresent(c -> c.setProductCount((int) products.countByCategoryId(categoryId)));
        }
        if (brandId != null) {
            brands.findById(brandId).ifPresent(b -> b.setProductCount((int) products.countByBrandId(brandId)));
        }
    }

    private void notifySearch(Long productId) {
        AfterCommit.run(() -> searchSync.productChanged(productId));
    }

    private ClaimView toView(ProductClaim c) {
        return new ClaimView(c.getId(), c.getClaimType(), c.getEvidence(), c.isVerified(), c.getVerifiedBy(), c.getVerifiedTime());
    }
}
