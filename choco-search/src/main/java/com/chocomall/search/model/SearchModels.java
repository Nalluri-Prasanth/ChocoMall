package com.chocomall.search.model;

import com.chocomall.model.enums.ClaimType;
import com.chocomall.model.enums.ProductKind;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Request and response shapes of the search API. */
public final class SearchModels {

    private SearchModels() {
    }

    public record SearchRequest(String keyword, Long categoryId, Long brandId, ProductKind kind, Long flavorId,
                                Long chocolateTypeId, Set<Long> excludeAllergens, ClaimType claim, BigDecimal minPrice,
                                BigDecimal maxPrice, Integer sort, int page, int size) {

        public SearchRequest withPaging(int newPage, int newSize) {
            return new SearchRequest(keyword, categoryId, brandId, kind, flavorId, chocolateTypeId, excludeAllergens, claim,
                    minPrice, maxPrice, sort, newPage, newSize);
        }
    }

    public record ProductHit(Long id, String productSn, String name, String subTitle, String pic, BigDecimal price,
                             BigDecimal originalPrice, ProductKind kind, Integer sale, Long categoryId, String categoryName,
                             Long brandId, String brandName, List<String> flavors, List<String> allergens, List<String> claims,
                             boolean preorderOnly, boolean buildABox, boolean inStock) {
    }

    public record PageResult<T>(List<T> list, long total, int pageNum, int pageSize, int totalPage) {
    }

    /** Counts by category, brand, flavor and kind for the current search: the "related search" filters. */
    public record Facets(Map<String, Long> categories, Map<String, Long> brands, Map<String, Long> flavors,
                         Map<String, Long> kinds, BigDecimal minPrice, BigDecimal maxPrice) {
    }
}
