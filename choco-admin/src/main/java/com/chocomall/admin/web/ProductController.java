package com.chocomall.admin.web;

import com.chocomall.admin.dto.CatalogDtos.ClaimParam;
import com.chocomall.admin.dto.CatalogDtos.ClaimView;
import com.chocomall.admin.dto.CatalogDtos.ContributionView;
import com.chocomall.admin.dto.CatalogDtos.ProductDetail;
import com.chocomall.admin.dto.CatalogDtos.ProductParam;
import com.chocomall.admin.dto.CatalogDtos.ProductQuery;
import com.chocomall.admin.dto.CatalogDtos.PublishParam;
import com.chocomall.admin.service.ProductAdminService;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.CommonResult;
import com.chocomall.model.entity.Product;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.enums.ProductKind;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Products")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductAdminService products;

    @Operation(summary = "Search products by keyword, category, brand, kind, launch tier and flags")
    @GetMapping("/product/list")
    public CommonResult<CommonPage<Product>> list(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) Long brandId,
                                                  @RequestParam(required = false) ProductKind kind,
                                                  @RequestParam(required = false) Priority launchTier,
                                                  @RequestParam(required = false) Boolean published,
                                                  @RequestParam(required = false) Boolean recommended,
                                                  @RequestParam(required = false) Boolean isNew,
                                                  @RequestParam(required = false) Boolean lowStock,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(products.list(new ProductQuery(keyword, categoryId, brandId, kind, launchTier,
                published, recommended, isNew, lowStock, pageNum, pageSize)));
    }

    @Operation(summary = "Full product detail with SKUs, flavors, allergens and claims")
    @GetMapping("/product/{id}")
    public CommonResult<ProductDetail> detail(@PathVariable Long id) {
        return CommonResult.success(products.detail(id));
    }

    @PostMapping("/product/create")
    public CommonResult<ProductDetail> create(@Valid @RequestBody ProductParam param) {
        return CommonResult.success(products.create(param));
    }

    @PostMapping("/product/update/{id}")
    public CommonResult<ProductDetail> update(@PathVariable Long id, @Valid @RequestBody ProductParam param) {
        return CommonResult.success(products.update(id, param));
    }

    @Operation(summary = "Publish or unpublish. Publishing requires reviewed allergens and an active SKU.")
    @PostMapping("/product/update/publishStatus")
    public CommonResult<Void> publish(@Valid @RequestBody PublishParam p) {
        products.setPublished(p.ids(), p.value());
        return CommonResult.success();
    }

    @PostMapping("/product/update/newStatus")
    public CommonResult<Void> markNew(@Valid @RequestBody PublishParam p) {
        products.setNew(p.ids(), p.value());
        return CommonResult.success();
    }

    @PostMapping("/product/update/recommendStatus")
    public CommonResult<Void> recommend(@Valid @RequestBody PublishParam p) {
        products.setRecommended(p.ids(), p.value());
        return CommonResult.success();
    }

    @Operation(summary = "Soft-delete products (blocked while they are part of an open order)")
    @PostMapping("/product/update/deleteStatus")
    public CommonResult<Void> delete(@RequestParam Set<Long> ids) {
        products.delete(ids);
        return CommonResult.success();
    }

    @Operation(summary = "Price minus ingredient, packaging, labour and selling fees, per unit")
    @GetMapping("/product/{id}/contribution")
    public CommonResult<ContributionView> contribution(@PathVariable Long id) {
        return CommonResult.success(products.contribution(id));
    }

    @Operation(summary = "Record a dietary or sourcing claim (not shown to shoppers until verified)")
    @PostMapping("/product/{id}/claim")
    public CommonResult<ClaimView> addClaim(@PathVariable Long id, @Valid @RequestBody ClaimParam param) {
        return CommonResult.success(products.addClaim(id, param));
    }

    @Operation(summary = "Verify a claim once supplier or process evidence is on file")
    @PostMapping("/product/claim/verify/{claimId}")
    public CommonResult<ClaimView> verifyClaim(@PathVariable Long claimId) {
        return CommonResult.success(products.verifyClaim(claimId));
    }

    @PostMapping("/product/claim/delete/{claimId}")
    public CommonResult<Void> removeClaim(@PathVariable Long claimId) {
        products.removeClaim(claimId);
        return CommonResult.success();
    }
}
