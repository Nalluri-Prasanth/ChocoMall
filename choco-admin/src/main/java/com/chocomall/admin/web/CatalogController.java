package com.chocomall.admin.web;

import com.chocomall.admin.dto.CatalogDtos.AllergenParam;
import com.chocomall.admin.dto.CatalogDtos.BrandParam;
import com.chocomall.admin.dto.CatalogDtos.CategoryNode;
import com.chocomall.admin.dto.CatalogDtos.CategoryParam;
import com.chocomall.admin.dto.CatalogDtos.ChocolateTypeParam;
import com.chocomall.admin.dto.CatalogDtos.FlavorParam;
import com.chocomall.admin.dto.CatalogDtos.StockingParam;
import com.chocomall.admin.service.CatalogRefService;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.CommonResult;
import com.chocomall.model.entity.Allergen;
import com.chocomall.model.entity.Brand;
import com.chocomall.model.entity.ChocolateType;
import com.chocomall.model.entity.Flavor;
import com.chocomall.model.entity.ProductCategory;
import com.chocomall.model.enums.BrandStockingStatus;
import com.chocomall.model.enums.Priority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Brand directory, category tree and the flavor / chocolate-type / allergen reference lists. */
@Tag(name = "Catalog references")
@RestController
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogRefService catalog;

    // ---- brands
    @Operation(summary = "Search the global brand directory by name, region, stocking stage and visibility")
    @GetMapping("/brand/list")
    public CommonResult<CommonPage<Brand>> brands(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String region,
                                                  @RequestParam(required = false) BrandStockingStatus stockingStatus,
                                                  @RequestParam(required = false) Boolean showStatus,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(catalog.searchBrands(keyword, region, stockingStatus, showStatus, pageNum, pageSize));
    }

    @GetMapping("/brand/regions")
    public CommonResult<List<String>> brandRegions() {
        return CommonResult.success(catalog.brandRegions());
    }

    @Operation(summary = "Brands per stocking stage and the guide's recommended starting range")
    @GetMapping("/brand/stocking/summary")
    public CommonResult<Map<String, Object>> stockingSummary() {
        return CommonResult.success(catalog.stockingSummary());
    }

    @GetMapping("/brand/{id}")
    public CommonResult<Brand> brand(@PathVariable Long id) {
        return CommonResult.success(catalog.brand(id));
    }

    @PostMapping("/brand/create")
    public CommonResult<Brand> createBrand(@Valid @RequestBody BrandParam p) {
        return CommonResult.success(catalog.createBrand(p));
    }

    @PostMapping("/brand/update/{id}")
    public CommonResult<Brand> updateBrand(@PathVariable Long id, @Valid @RequestBody BrandParam p) {
        return CommonResult.success(catalog.updateBrand(id, p));
    }

    @PostMapping("/brand/delete/{id}")
    public CommonResult<Void> deleteBrand(@PathVariable Long id) {
        catalog.deleteBrand(id);
        return CommonResult.success();
    }

    @PostMapping("/brand/update/showStatus/{id}")
    public CommonResult<Void> brandVisibility(@PathVariable Long id, @RequestParam boolean show) {
        catalog.setBrandVisibility(id, show);
        return CommonResult.success();
    }

    @Operation(summary = "Move a brand through sampling, authorization, stocking and rotation")
    @PostMapping("/brand/update/stocking/{id}")
    public CommonResult<Brand> moveBrand(@PathVariable Long id, @Valid @RequestBody StockingParam p) {
        return CommonResult.success(catalog.moveBrand(id, p));
    }

    // ---- categories
    @GetMapping("/productCategory/list/withChildren")
    public CommonResult<List<CategoryNode>> categoryTree() {
        return CommonResult.success(catalog.categoryTree());
    }

    @GetMapping("/productCategory/list/{parentId}")
    public CommonResult<CommonPage<ProductCategory>> categories(@PathVariable Long parentId,
                                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(catalog.categories(parentId, pageNum, pageSize));
    }

    @GetMapping("/productCategory/{id}")
    public CommonResult<ProductCategory> category(@PathVariable Long id) {
        return CommonResult.success(catalog.category(id));
    }

    @PostMapping("/productCategory/create")
    public CommonResult<ProductCategory> createCategory(@Valid @RequestBody CategoryParam p) {
        return CommonResult.success(catalog.createCategory(p));
    }

    @PostMapping("/productCategory/update/{id}")
    public CommonResult<ProductCategory> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryParam p) {
        return CommonResult.success(catalog.updateCategory(id, p));
    }

    @PostMapping("/productCategory/delete/{id}")
    public CommonResult<Void> deleteCategory(@PathVariable Long id) {
        catalog.deleteCategory(id);
        return CommonResult.success();
    }

    @PostMapping("/productCategory/update/flags/{id}")
    public CommonResult<Void> categoryFlags(@PathVariable Long id, @RequestParam(required = false) Boolean showStatus,
                                            @RequestParam(required = false) Boolean navStatus) {
        catalog.setCategoryFlags(id, showStatus, navStatus);
        return CommonResult.success();
    }

    // ---- flavors
    @GetMapping("/flavor/list")
    public CommonResult<List<Flavor>> flavors(@RequestParam(required = false) String group,
                                              @RequestParam(required = false) Priority priority) {
        return CommonResult.success(catalog.flavors(group, priority));
    }

    @PostMapping("/flavor/create")
    public CommonResult<Flavor> createFlavor(@Valid @RequestBody FlavorParam p) {
        return CommonResult.success(catalog.createFlavor(p));
    }

    @PostMapping("/flavor/update/{id}")
    public CommonResult<Flavor> updateFlavor(@PathVariable Long id, @Valid @RequestBody FlavorParam p) {
        return CommonResult.success(catalog.updateFlavor(id, p));
    }

    @PostMapping("/flavor/delete/{id}")
    public CommonResult<Void> deleteFlavor(@PathVariable Long id) {
        catalog.deleteFlavor(id);
        return CommonResult.success();
    }

    // ---- chocolate types
    @GetMapping("/chocolateType/list")
    public CommonResult<List<ChocolateType>> types(@RequestParam(required = false) String classification) {
        return CommonResult.success(catalog.chocolateTypes(classification));
    }

    @PostMapping("/chocolateType/create")
    public CommonResult<ChocolateType> createType(@Valid @RequestBody ChocolateTypeParam p) {
        return CommonResult.success(catalog.createType(p));
    }

    @PostMapping("/chocolateType/update/{id}")
    public CommonResult<ChocolateType> updateType(@PathVariable Long id, @Valid @RequestBody ChocolateTypeParam p) {
        return CommonResult.success(catalog.updateType(id, p));
    }

    @PostMapping("/chocolateType/delete/{id}")
    public CommonResult<Void> deleteType(@PathVariable Long id) {
        catalog.deleteType(id);
        return CommonResult.success();
    }

    // ---- allergens
    @GetMapping("/allergen/list")
    public CommonResult<List<Allergen>> allergens() {
        return CommonResult.success(catalog.allergens());
    }

    @PostMapping("/allergen/create")
    public CommonResult<Allergen> createAllergen(@Valid @RequestBody AllergenParam p) {
        return CommonResult.success(catalog.createAllergen(p));
    }

    @PostMapping("/allergen/update/{id}")
    public CommonResult<Allergen> updateAllergen(@PathVariable Long id, @Valid @RequestBody AllergenParam p) {
        return CommonResult.success(catalog.updateAllergen(id, p));
    }

    @PostMapping("/allergen/delete/{id}")
    public CommonResult<Void> deleteAllergen(@PathVariable Long id) {
        catalog.deleteAllergen(id);
        return CommonResult.success();
    }
}
