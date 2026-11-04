package com.chocomall.admin.service;

import com.chocomall.admin.dto.CatalogDtos.AllergenParam;
import com.chocomall.admin.dto.CatalogDtos.BrandParam;
import com.chocomall.admin.dto.CatalogDtos.CategoryNode;
import com.chocomall.admin.dto.CatalogDtos.CategoryParam;
import com.chocomall.admin.dto.CatalogDtos.ChocolateTypeParam;
import com.chocomall.admin.dto.CatalogDtos.FlavorParam;
import com.chocomall.admin.dto.CatalogDtos.StockingParam;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.model.entity.Allergen;
import com.chocomall.model.entity.Brand;
import com.chocomall.model.entity.ChocolateType;
import com.chocomall.model.entity.Flavor;
import com.chocomall.model.entity.ProductCategory;
import com.chocomall.model.enums.BrandStockingStatus;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.repository.AllergenRepository;
import com.chocomall.model.repository.BrandRepository;
import com.chocomall.model.repository.ChocolateTypeRepository;
import com.chocomall.model.repository.FlavorRepository;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Brands (with the stocking workflow), the category tree and the flavor / chocolate-type / allergen lists. */
@Service
@RequiredArgsConstructor
@Transactional
public class CatalogRefService {

    /** Allowed moves of the brand stocking workflow: sample, confirm supply and cost, stock, rotate out. */
    private static final Map<BrandStockingStatus, Set<BrandStockingStatus>> FLOW = new EnumMap<>(BrandStockingStatus.class);

    static {
        FLOW.put(BrandStockingStatus.CANDIDATE, EnumSet.of(BrandStockingStatus.SAMPLING, BrandStockingStatus.ROTATED_OUT));
        FLOW.put(BrandStockingStatus.SAMPLING, EnumSet.of(BrandStockingStatus.AUTHORIZED, BrandStockingStatus.CANDIDATE, BrandStockingStatus.ROTATED_OUT));
        FLOW.put(BrandStockingStatus.AUTHORIZED, EnumSet.of(BrandStockingStatus.STOCKED, BrandStockingStatus.ROTATED_OUT));
        FLOW.put(BrandStockingStatus.STOCKED, EnumSet.of(BrandStockingStatus.ROTATED_OUT));
        FLOW.put(BrandStockingStatus.ROTATED_OUT, EnumSet.of(BrandStockingStatus.CANDIDATE, BrandStockingStatus.SAMPLING));
    }

    private final BrandRepository brands;
    private final ProductCategoryRepository categories;
    private final FlavorRepository flavors;
    private final ChocolateTypeRepository types;
    private final AllergenRepository allergens;
    private final ProductRepository products;

    // ---------------------------------------------------------------------------- brands

    @Transactional(readOnly = true)
    public CommonPage<Brand> searchBrands(String keyword, String region, BrandStockingStatus status, Boolean show,
                                          Integer pageNum, Integer pageSize) {
        return CommonPage.of(brands.search(blank(keyword), blank(region), status, show, PageParams.of(pageNum, pageSize,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("sort"), org.springframework.data.domain.Sort.Order.asc("name")))));
    }

    @Transactional(readOnly = true)
    public Brand brand(Long id) {
        return Asserts.found(brands.findById(id), "Brand");
    }

    @Transactional(readOnly = true)
    public List<String> brandRegions() {
        return brands.regions();
    }

    /** Counts per stage, plus the guide's recommendation of 8 to 12 outside brands at the start. */
    @Transactional(readOnly = true)
    public Map<String, Object> stockingSummary() {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (BrandStockingStatus s : BrandStockingStatus.values()) {
            byStatus.put(s.name(), brands.findAll().stream().filter(b -> b.getStockingStatus() == s).count());
        }
        out.put("byStatus", byStatus);
        out.put("recommendedStartingRange", "8-12 outside brands across familiar, premium and craft tiers");
        return out;
    }

    public Brand createBrand(BrandParam p) {
        Asserts.state(brands.findByCode(p.code()).isEmpty(), "Brand code " + p.code() + " already exists");
        Brand b = new Brand();
        applyBrand(b, p);
        return brands.save(b);
    }

    public Brand updateBrand(Long id, BrandParam p) {
        Brand b = brand(id);
        if (!b.getCode().equals(p.code())) {
            Asserts.state(brands.findByCode(p.code()).isEmpty(), "Brand code " + p.code() + " already exists");
        }
        applyBrand(b, p);
        return b;
    }

    public void deleteBrand(Long id) {
        Brand b = brand(id);
        Asserts.state(products.countByBrandId(id) == 0, "The brand still has products");
        brands.delete(b);
    }

    public void setBrandVisibility(Long id, boolean show) {
        brand(id).setShowStatus(show);
    }

    public Brand moveBrand(Long id, StockingParam p) {
        Brand b = brand(id);
        BrandStockingStatus from = b.getStockingStatus();
        if (from == p.status()) {
            return b;
        }
        Asserts.state(FLOW.get(from).contains(p.status()), "A brand that is " + from + " cannot become " + p.status());
        if (p.authorizedSupplier() != null && !p.authorizedSupplier().isBlank()) {
            b.setAuthorizedSupplier(p.authorizedSupplier());
        }
        if (p.landedCostNote() != null && !p.landedCostNote().isBlank()) {
            b.setLandedCostNote(p.landedCostNote());
        }
        if (p.status() == BrandStockingStatus.AUTHORIZED) {
            Asserts.isTrue(notBlank(b.getAuthorizedSupplier()), "Record the authorized supplier before authorizing a brand");
        }
        if (p.status() == BrandStockingStatus.STOCKED) {
            Asserts.isTrue(notBlank(b.getAuthorizedSupplier()) && notBlank(b.getLandedCostNote()),
                    "Record the authorized supplier and the landed cost before stocking a brand");
        }
        b.setStockingStatus(p.status());
        return b;
    }

    private void applyBrand(Brand b, BrandParam p) {
        b.setCode(p.code());
        b.setName(p.name());
        b.setRegion(p.region());
        b.setAssociatedMarket(p.associatedMarket());
        b.setSegment(p.segment());
        b.setTypicalFormats(p.typicalFormats());
        b.setShopRole(p.shopRole());
        b.setEvidenceStatus(p.evidenceStatus());
        b.setReferenceUrl(p.referenceUrl());
        b.setLogo(p.logo());
        b.setShowStatus(p.showStatus() == null || p.showStatus());
        b.setSort(p.sort() == null ? 0 : p.sort());
    }

    // ---------------------------------------------------------------------------- categories

    @Transactional(readOnly = true)
    public List<CategoryNode> categoryTree() {
        List<ProductCategory> all = categories.findAll();
        Map<Long, CategoryNode> nodes = new LinkedHashMap<>();
        all.stream().sorted((a, b) -> Integer.compare(b.getSort(), a.getSort())).forEach(c ->
                nodes.put(c.getId(), new CategoryNode(c.getId(), c.getParentId(), c.getName(), c.getLevel(), c.getProductCount(),
                        c.isNavStatus(), c.isShowStatus(), c.getSort(), c.getIcon(), new ArrayList<>())));
        List<CategoryNode> roots = new ArrayList<>();
        nodes.values().forEach(n -> {
            CategoryNode parent = nodes.get(n.parentId());
            (parent == null ? roots : parent.children()).add(n);
        });
        return roots;
    }

    @Transactional(readOnly = true)
    public CommonPage<ProductCategory> categories(Long parentId, Integer pageNum, Integer pageSize) {
        return CommonPage.of(categories.findByParentId(parentId, PageParams.of(pageNum, pageSize)));
    }

    @Transactional(readOnly = true)
    public ProductCategory category(Long id) {
        return Asserts.found(categories.findById(id), "Category");
    }

    public ProductCategory createCategory(CategoryParam p) {
        ProductCategory c = new ProductCategory();
        applyCategory(c, p);
        return categories.save(c);
    }

    public ProductCategory updateCategory(Long id, CategoryParam p) {
        ProductCategory c = category(id);
        Asserts.isTrue(!id.equals(p.parentId()), "A category cannot be its own parent");
        applyCategory(c, p);
        return c;
    }

    public void deleteCategory(Long id) {
        ProductCategory c = category(id);
        Asserts.state(categories.findByParentIdOrderBySortDesc(id).isEmpty(), "Delete the sub-categories first");
        Asserts.state(products.countByCategoryId(id) == 0, "The category still has products");
        categories.delete(c);
    }

    public void setCategoryFlags(Long id, Boolean show, Boolean nav) {
        ProductCategory c = category(id);
        if (show != null) {
            c.setShowStatus(show);
        }
        if (nav != null) {
            c.setNavStatus(nav);
        }
    }

    private void applyCategory(ProductCategory c, CategoryParam p) {
        long parent = p.parentId() == null ? 0L : p.parentId();
        c.setParentId(parent);
        c.setLevel(parent == 0 ? 0 : Asserts.found(categories.findById(parent), "Parent category").getLevel() + 1);
        Asserts.isTrue(c.getLevel() <= 1, "Categories have at most two levels");
        c.setName(p.name());
        c.setNavStatus(p.navStatus() == null || p.navStatus());
        c.setShowStatus(p.showStatus() == null || p.showStatus());
        c.setSort(p.sort() == null ? 0 : p.sort());
        c.setIcon(p.icon());
        c.setKeywords(p.keywords());
        c.setDescription(p.description());
    }

    // ---------------------------------------------------------------------------- flavors

    @Transactional(readOnly = true)
    public List<Flavor> flavors(String group, Priority priority) {
        return flavors.findAll().stream()
                .filter(f -> group == null || group.equals(f.getFlavorGroup()))
                .filter(f -> priority == null || priority == f.getPriority()).toList();
    }

    public Flavor createFlavor(FlavorParam p) {
        Asserts.state(flavors.findByCode(p.code()).isEmpty(), "Flavor code " + p.code() + " already exists");
        Flavor f = new Flavor();
        applyFlavor(f, p);
        return flavors.save(f);
    }

    public Flavor updateFlavor(Long id, FlavorParam p) {
        Flavor f = Asserts.found(flavors.findById(id), "Flavor");
        applyFlavor(f, p);
        return f;
    }

    public void deleteFlavor(Long id) {
        Flavor f = Asserts.found(flavors.findById(id), "Flavor");
        Asserts.state(products.countByFlavor(id) == 0, "The flavor is used by products");
        flavors.delete(f);
    }

    private void applyFlavor(Flavor f, FlavorParam p) {
        f.setCode(p.code());
        f.setFlavorGroup(p.flavorGroup());
        f.setName(p.name());
        f.setChocolateBase(p.chocolateBase());
        f.setApplication(p.application());
        f.setPriority(p.priority());
    }

    // ---------------------------------------------------------------------------- chocolate types

    @Transactional(readOnly = true)
    public List<ChocolateType> chocolateTypes(String classification) {
        return classification == null ? types.findAll() : types.findByClassification(classification);
    }

    public ChocolateType createType(ChocolateTypeParam p) {
        Asserts.state(types.findByCode(p.code()).isEmpty(), "Type code " + p.code() + " already exists");
        ChocolateType t = new ChocolateType();
        applyType(t, p);
        return types.save(t);
    }

    public ChocolateType updateType(Long id, ChocolateTypeParam p) {
        ChocolateType t = Asserts.found(types.findById(id), "Chocolate type");
        applyType(t, p);
        return t;
    }

    public void deleteType(Long id) {
        ChocolateType t = Asserts.found(types.findById(id), "Chocolate type");
        Asserts.state(products.countByChocolateTypeId(id) == 0, "The chocolate type is used by products");
        types.delete(t);
    }

    private void applyType(ChocolateType t, ChocolateTypeParam p) {
        t.setCode(p.code());
        t.setName(p.name());
        t.setClassification(p.classification());
        t.setDistinguishing(p.distinguishing());
        t.setBestShopUse(p.bestShopUse());
        t.setPriority(p.priority());
    }

    // ---------------------------------------------------------------------------- allergens

    @Transactional(readOnly = true)
    public List<Allergen> allergens() {
        return allergens.findAll();
    }

    public Allergen createAllergen(AllergenParam p) {
        Asserts.state(allergens.findByCode(p.code()).isEmpty(), "Allergen code " + p.code() + " already exists");
        Allergen a = new Allergen();
        a.setCode(p.code());
        a.setName(p.name());
        return allergens.save(a);
    }

    public Allergen updateAllergen(Long id, AllergenParam p) {
        Allergen a = Asserts.found(allergens.findById(id), "Allergen");
        a.setCode(p.code());
        a.setName(p.name());
        return a;
    }

    public void deleteAllergen(Long id) {
        Allergen a = Asserts.found(allergens.findById(id), "Allergen");
        Asserts.state(products.countByAllergen(id) == 0, "The allergen is used by products");
        allergens.delete(a);
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
