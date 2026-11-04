package com.chocomall.admin.dto;

import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductSku;
import com.chocomall.model.enums.BrandStockingStatus;
import com.chocomall.model.enums.ClaimType;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.enums.ProductKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Request and response shapes for the catalog: brands, categories, reference lists and products. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record BrandParam(
            @NotBlank @Size(max = 20) String code, @NotBlank @Size(max = 200) String name,
            @Size(max = 50) String region, @Size(max = 100) String associatedMarket, @Size(max = 100) String segment,
            @Size(max = 300) String typicalFormats, @Size(max = 200) String shopRole,
            @Size(max = 200) String evidenceStatus, @Size(max = 500) String referenceUrl, @Size(max = 500) String logo,
            Boolean showStatus, Integer sort) {
    }

    public record StockingParam(@NotNull BrandStockingStatus status, @Size(max = 200) String authorizedSupplier,
                                @Size(max = 300) String landedCostNote) {
    }

    public record CategoryParam(Long parentId, @NotBlank @Size(max = 100) String name, Boolean navStatus,
                                Boolean showStatus, Integer sort, @Size(max = 500) String icon,
                                @Size(max = 200) String keywords, @Size(max = 500) String description) {
    }

    public record CategoryNode(Long id, Long parentId, String name, Integer level, Integer productCount, boolean navStatus,
                               boolean showStatus, Integer sort, String icon, List<CategoryNode> children) {
    }

    public record FlavorParam(@NotBlank @Size(max = 10) String code, @Size(max = 100) String flavorGroup,
                              @NotBlank @Size(max = 100) String name, @Size(max = 100) String chocolateBase,
                              @Size(max = 300) String application, @NotNull Priority priority) {
    }

    public record ChocolateTypeParam(@NotBlank @Size(max = 10) String code, @NotBlank @Size(max = 200) String name,
                                     @Size(max = 100) String classification, @Size(max = 500) String distinguishing,
                                     @Size(max = 300) String bestShopUse, @NotNull Priority priority) {
    }

    public record AllergenParam(@NotBlank @Size(max = 30) String code, @NotBlank @Size(max = 100) String name) {
    }

    public record SkuParam(@NotBlank @Size(max = 50) String skuCode, @NotBlank @Size(max = 100) String label,
                           @NotNull @Min(1) Integer units, @NotNull @DecimalMin("0.01") BigDecimal price,
                           @Size(max = 500) String pic, Integer sort, Boolean active) {
    }

    public record ProductParam(
            Long brandId, @NotNull Long categoryId, Long chocolateTypeId,
            @NotBlank @Size(max = 20) String productSn, @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String pic, @Size(max = 300) String subTitle, @Size(max = 2000) String description,
            @NotNull @DecimalMin("0.01") BigDecimal price, BigDecimal originalPrice, @Min(0) Integer lowStock,
            @Size(max = 30) String unit, Integer weightGrams, @NotNull ProductKind kind, @NotNull Priority launchTier,
            @Size(max = 200) String sellingFormat, @Size(max = 300) String salesPurpose,
            Boolean preorderOnly, Boolean buildABox, Boolean requiresColdChain, Boolean allergensReviewed,
            @Min(1) Integer shelfLifeDays, @DecimalMin("0") BigDecimal ingredientCost, @DecimalMin("0") BigDecimal packagingCost,
            @DecimalMin("0") BigDecimal laborCost, @DecimalMin("0") BigDecimal sellingFeeRate,
            @Size(max = 200) String keywords, Integer sort, Set<Long> flavorIds, Set<Long> allergenIds,
            @Min(0) Integer initialStock, @NotEmpty @Valid List<SkuParam> skus) {
    }

    public record NamedRef(Long id, String name) {
    }

    public record ClaimView(Long id, ClaimType claimType, String evidence, boolean verified, String verifiedBy,
                            LocalDateTime verifiedTime) {
    }

    public record ClaimParam(@NotNull ClaimType claimType, @Size(max = 500) String evidence) {
    }

    public record ProductDetail(Product product, String categoryName, String brandName, String chocolateTypeName,
                                int available, List<ProductSku> skus, List<NamedRef> flavors, List<NamedRef> allergens,
                                List<ClaimView> claims) {
    }

    public record PublishParam(@NotEmpty Set<Long> ids, boolean value) {
    }

    public record ContributionView(Long productId, String name, BigDecimal price, BigDecimal ingredientCost,
                                   BigDecimal packagingCost, BigDecimal laborCost, BigDecimal sellingFee,
                                   BigDecimal contributionPerUnit, BigDecimal marginPercent, boolean costsComplete) {
    }

    public record ProductQuery(String keyword, Long categoryId, Long brandId, ProductKind kind, Priority launchTier,
                               Boolean published, Boolean recommended, Boolean isNew, Boolean lowStock,
                               Integer pageNum, Integer pageSize) {
    }
}
