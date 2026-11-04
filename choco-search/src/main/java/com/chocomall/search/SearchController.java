package com.chocomall.search;

import com.chocomall.common.api.CommonResult;
import com.chocomall.model.enums.ClaimType;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.search.model.SearchModels.Facets;
import com.chocomall.search.model.SearchModels.PageResult;
import com.chocomall.search.model.SearchModels.ProductHit;
import com.chocomall.search.model.SearchModels.SearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService search;

    @Operation(summary = "Keyword search")
    @GetMapping("/simple")
    public CommonResult<PageResult<ProductHit>> simple(@RequestParam(required = false) String keyword,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "12") int pageSize) {
        return CommonResult.success(search.search(new SearchRequest(keyword, null, null, null, null, null, null, null, null, null, 0,
                pageNum, clamp(pageSize))));
    }

    @Operation(summary = "Search with filters: category, brand, kind, flavor, chocolate type, allergens to avoid, verified claim, price range, sort")
    @GetMapping
    public CommonResult<PageResult<ProductHit>> complex(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Long categoryId,
                                                        @RequestParam(required = false) Long brandId,
                                                        @RequestParam(required = false) ProductKind kind,
                                                        @RequestParam(required = false) Long flavorId,
                                                        @RequestParam(required = false) Long chocolateTypeId,
                                                        @RequestParam(required = false) Set<Long> excludeAllergens,
                                                        @RequestParam(required = false) ClaimType claim,
                                                        @RequestParam(required = false) BigDecimal minPrice,
                                                        @RequestParam(required = false) BigDecimal maxPrice,
                                                        @RequestParam(defaultValue = "0") Integer sort,
                                                        @RequestParam(defaultValue = "1") int pageNum,
                                                        @RequestParam(defaultValue = "12") int pageSize) {
        return CommonResult.success(search.search(new SearchRequest(keyword, categoryId, brandId, kind, flavorId, chocolateTypeId,
                excludeAllergens, claim, minPrice, maxPrice, sort, pageNum, clamp(pageSize))));
    }

    @Operation(summary = "Related search: how the results spread over categories, brands, flavors and kinds")
    @GetMapping("/related")
    public CommonResult<Facets> related(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Set<Long> excludeAllergens) {
        return CommonResult.success(search.facets(new SearchRequest(keyword, null, null, null, null, null, excludeAllergens, null,
                null, null, 0, 1, 1)));
    }

    @Operation(summary = "Products similar to the given one")
    @GetMapping("/recommend/{productId}")
    public CommonResult<List<ProductHit>> recommend(@PathVariable Long productId, @RequestParam(defaultValue = "6") int limit) {
        return CommonResult.success(search.recommend(productId, Math.min(Math.max(limit, 1), 24)));
    }

    @GetMapping("/engine")
    public CommonResult<Map<String, String>> engine() {
        return CommonResult.success(Map.of("engine", search.engineName()));
    }

    // ---- index management (requires the X-Search-Admin-Key header)

    @Operation(summary = "Rebuild the whole index from the product catalog")
    @PostMapping("/index/importAll")
    public CommonResult<Map<String, Integer>> importAll() {
        return CommonResult.success(Map.of("indexed", search.reindexAll()));
    }

    @PostMapping("/index/{productId}")
    public CommonResult<Void> index(@PathVariable Long productId) {
        search.index(productId);
        return CommonResult.success();
    }

    @PostMapping("/index/delete/{productId}")
    public CommonResult<Void> remove(@PathVariable Long productId) {
        search.remove(productId);
        return CommonResult.success();
    }

    private static int clamp(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
