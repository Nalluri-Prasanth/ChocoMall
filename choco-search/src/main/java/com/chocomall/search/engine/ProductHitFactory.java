package com.chocomall.search.engine;

import com.chocomall.core.stock.StockService;
import com.chocomall.model.entity.Allergen;
import com.chocomall.model.entity.Brand;
import com.chocomall.model.entity.Flavor;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductCategory;
import com.chocomall.model.entity.ProductClaim;
import com.chocomall.model.repository.AllergenRepository;
import com.chocomall.model.repository.BrandRepository;
import com.chocomall.model.repository.FlavorRepository;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductClaimRepository;
import com.chocomall.search.model.SearchModels.ProductHit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Turns products into search hits with the names of their category, brand, flavors, allergens and verified claims. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductHitFactory {

    private final ProductCategoryRepository categories;
    private final BrandRepository brands;
    private final FlavorRepository flavors;
    private final AllergenRepository allergens;
    private final ProductClaimRepository claims;

    public List<ProductHit> hits(Collection<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, String> categoryNames = categories.findAll().stream().collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));
        Map<Long, String> brandNames = brands.findAll().stream().collect(Collectors.toMap(Brand::getId, Brand::getName));
        Map<Long, String> flavorNames = flavors.findAll().stream().collect(Collectors.toMap(Flavor::getId, Flavor::getName));
        Map<Long, String> allergenNames = allergens.findAll().stream().collect(Collectors.toMap(Allergen::getId, Allergen::getName));
        Map<Long, List<ProductClaim>> verified = claims.findByProductIdInAndVerifiedTrue(products.stream().map(Product::getId).toList())
                .stream().collect(Collectors.groupingBy(ProductClaim::getProductId));
        return products.stream().map(p -> {
            boolean tracked = StockService.tracksStock(p);
            return new ProductHit(p.getId(), p.getProductSn(), p.getName(), p.getSubTitle(), p.getPic(), p.getPrice(), p.getOriginalPrice(),
                    p.getKind(), p.getSale(), p.getCategoryId(), categoryNames.get(p.getCategoryId()), p.getBrandId(),
                    p.getBrandId() == null ? null : brandNames.get(p.getBrandId()),
                    p.getFlavorIds().stream().map(flavorNames::get).filter(java.util.Objects::nonNull).sorted().toList(),
                    p.getAllergenIds().stream().map(allergenNames::get).filter(java.util.Objects::nonNull).sorted().toList(),
                    verified.getOrDefault(p.getId(), List.of()).stream().map(c -> c.getClaimType().name()).toList(),
                    p.isPreorderOnly(), p.isBuildABox(), p.isBuildABox() || !tracked || p.getStock() - p.getLockStock() > 0);
        }).toList();
    }
}
