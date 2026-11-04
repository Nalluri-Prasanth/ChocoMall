package com.chocomall.search.engine;

import com.chocomall.model.entity.Product;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.search.model.SearchModels.ProductHit;
import java.math.BigDecimal;
import java.util.ArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Converts between database products, search hits and Elasticsearch documents. */
@Component
@ConditionalOnProperty(name = "search.engine", havingValue = "elasticsearch")
public class ProductDocMapper {

    public ProductDoc toDoc(Product p, ProductHit hit) {
        ProductDoc d = new ProductDoc();
        d.setId(String.valueOf(p.getId()));
        d.setProductId(p.getId());
        d.setProductSn(p.getProductSn());
        d.setName(p.getName());
        d.setSubTitle(p.getSubTitle());
        d.setKeywords(p.getKeywords());
        d.setPic(p.getPic());
        d.setPublished(p.isPublished());
        d.setCategoryId(p.getCategoryId());
        d.setCategoryName(hit.categoryName());
        d.setBrandId(p.getBrandId());
        d.setBrandName(hit.brandName());
        d.setChocolateTypeId(p.getChocolateTypeId());
        d.setKind(p.getKind().name());
        d.setPrice(p.getPrice().doubleValue());
        d.setOriginalPrice(p.getOriginalPrice() == null ? null : p.getOriginalPrice().doubleValue());
        d.setSale(p.getSale());
        d.setSort(p.getSort());
        d.setPreorderOnly(p.isPreorderOnly());
        d.setBuildABox(p.isBuildABox());
        d.setInStock(hit.inStock());
        d.setFlavorIds(new ArrayList<>(p.getFlavorIds()));
        d.setFlavorNames(hit.flavors());
        d.setAllergenIds(new ArrayList<>(p.getAllergenIds()));
        d.setAllergens(hit.allergens());
        d.setClaims(hit.claims());
        return d;
    }

    public ProductHit toHit(ProductDoc d) {
        return new ProductHit(d.getProductId(), d.getProductSn(), d.getName(), d.getSubTitle(), d.getPic(), BigDecimal.valueOf(d.getPrice()),
                d.getOriginalPrice() == null ? null : BigDecimal.valueOf(d.getOriginalPrice()), ProductKind.valueOf(d.getKind()), d.getSale(),
                d.getCategoryId(), d.getCategoryName(), d.getBrandId(), d.getBrandName(),
                d.getFlavorNames() == null ? java.util.List.of() : d.getFlavorNames(),
                d.getAllergens() == null ? java.util.List.of() : d.getAllergens(),
                d.getClaims() == null ? java.util.List.of() : d.getClaims(),
                d.isPreorderOnly(), d.isBuildABox(), d.isInStock());
    }
}
