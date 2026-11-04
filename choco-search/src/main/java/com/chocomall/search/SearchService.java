package com.chocomall.search;

import com.chocomall.search.engine.SearchEngine;
import com.chocomall.search.model.SearchModels.Facets;
import com.chocomall.search.model.SearchModels.PageResult;
import com.chocomall.search.model.SearchModels.ProductHit;
import com.chocomall.search.model.SearchModels.SearchRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int FACET_SAMPLE = 200;

    private final SearchEngine engine;

    public String engineName() {
        return engine.name();
    }

    public PageResult<ProductHit> search(SearchRequest request) {
        return engine.search(request);
    }

    public List<ProductHit> recommend(Long productId, int limit) {
        return engine.recommend(productId, limit);
    }

    /** "Related search": how the results of this query spread over categories, brands, flavors and kinds. */
    public Facets facets(SearchRequest request) {
        List<ProductHit> sample = engine.search(request.withPaging(1, FACET_SAMPLE)).list();
        BigDecimal min = sample.stream().map(ProductHit::price).min(BigDecimal::compareTo).orElse(null);
        BigDecimal max = sample.stream().map(ProductHit::price).max(BigDecimal::compareTo).orElse(null);
        return new Facets(
                count(sample, ProductHit::categoryName),
                count(sample, ProductHit::brandName),
                countMany(sample, ProductHit::flavors),
                count(sample, h -> h.kind().name()),
                min, max);
    }

    public int reindexAll() {
        return engine.reindexAll();
    }

    public void index(Long productId) {
        engine.index(productId);
    }

    public void remove(Long productId) {
        engine.remove(productId);
    }

    private static Map<String, Long> count(List<ProductHit> hits, Function<ProductHit, String> key) {
        return sorted(hits.stream().map(key).filter(java.util.Objects::nonNull).collect(Collectors.groupingBy(k -> k, Collectors.counting())));
    }

    private static Map<String, Long> countMany(List<ProductHit> hits, Function<ProductHit, List<String>> keys) {
        return sorted(hits.stream().flatMap(h -> keys.apply(h).stream()).collect(Collectors.groupingBy(k -> k, Collectors.counting())));
    }

    private static Map<String, Long> sorted(Map<String, Long> counts) {
        Map<String, Long> out = new LinkedHashMap<>();
        counts.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }
}
