package com.chocomall.search.engine;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.chocomall.model.entity.Product;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.search.model.SearchModels.PageResult;
import com.chocomall.search.model.SearchModels.ProductHit;
import com.chocomall.search.model.SearchModels.SearchRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Elasticsearch-backed search with relevance ranking (name, tags, flavors, category and brand are weighted) and typo
 * tolerance. Enable with {@code search.engine=elasticsearch} and {@code spring.elasticsearch.uris}. The index is
 * rebuilt with {@code POST /search/index/importAll} and kept current by the {@code SearchSyncListener}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.engine", havingValue = "elasticsearch")
public class ElasticsearchSearchEngine implements SearchEngine {

    private final ElasticsearchOperations operations;
    private final ProductRepository products;
    private final ProductCategoryRepository categories;
    private final ProductHitFactory hitFactory;
    private final ProductDocMapper mapper;

    @Override
    public String name() {
        return "elasticsearch";
    }

    @Override
    public PageResult<ProductHit> search(SearchRequest q) {
        ensureIndex();
        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.filter(Query.of(f -> f.term(t -> t.field("published").value(true))));
        if (q.keyword() != null && !q.keyword().isBlank()) {
            bool.must(Query.of(m -> m.multiMatch(mm -> mm.query(q.keyword().trim())
                    .fields(List.of("name^5", "keywords^3", "subTitle^2", "flavorNames^2", "categoryName", "brandName", "productSn"))
                    .fuzziness("AUTO"))));
        } else {
            bool.must(Query.of(m -> m.matchAll(a -> a)));
        }
        if (q.categoryId() != null) {
            List<FieldValue> ids = new ArrayList<>(List.of(FieldValue.of(q.categoryId())));
            categories.findByParentIdOrderBySortDesc(q.categoryId()).forEach(c -> ids.add(FieldValue.of(c.getId())));
            bool.filter(Query.of(f -> f.terms(t -> t.field("categoryId").terms(tf -> tf.value(ids)))));
        }
        if (q.brandId() != null) {
            bool.filter(Query.of(f -> f.term(t -> t.field("brandId").value(q.brandId()))));
        }
        if (q.kind() != null) {
            bool.filter(Query.of(f -> f.term(t -> t.field("kind").value(q.kind().name()))));
        }
        if (q.chocolateTypeId() != null) {
            bool.filter(Query.of(f -> f.term(t -> t.field("chocolateTypeId").value(q.chocolateTypeId()))));
        }
        if (q.flavorId() != null) {
            bool.filter(Query.of(f -> f.term(t -> t.field("flavorIds").value(q.flavorId()))));
        }
        if (q.claim() != null) {
            bool.filter(Query.of(f -> f.term(t -> t.field("claims").value(q.claim().name()))));
        }
        if (q.excludeAllergens() != null && !q.excludeAllergens().isEmpty()) {
            List<FieldValue> ids = q.excludeAllergens().stream().map(FieldValue::of).toList();
            bool.mustNot(Query.of(f -> f.terms(t -> t.field("allergenIds").terms(tf -> tf.value(ids)))));
        }
        if (q.minPrice() != null || q.maxPrice() != null) {
            bool.filter(Query.of(f -> f.range(r -> {
                r.field("price");
                if (q.minPrice() != null) {
                    r.gte(JsonData.of(q.minPrice().doubleValue()));
                }
                if (q.maxPrice() != null) {
                    r.lte(JsonData.of(q.maxPrice().doubleValue()));
                }
                return r;
            })));
        }
        boolean ranked = q.keyword() != null && !q.keyword().isBlank() && (q.sort() == null || q.sort() == 0);
        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(b -> b.bool(bool.build())))
                .withPageable(PageRequest.of(Math.max(q.page() - 1, 0), q.size(), ranked ? Sort.unsorted() : sort(q.sort())))
                .build();
        SearchHits<ProductDoc> found = operations.search(query, ProductDoc.class);
        List<ProductHit> list = found.getSearchHits().stream().map(SearchHit::getContent).map(mapper::toHit).toList();
        int pages = (int) Math.ceil(found.getTotalHits() / (double) q.size());
        return new PageResult<>(list, found.getTotalHits(), Math.max(q.page(), 1), q.size(), pages);
    }

    @Override
    public List<ProductHit> recommend(Long productId, int limit) {
        Product p = products.findById(productId).filter(Product::isPublished).orElse(null);
        if (p == null) {
            return List.of();
        }
        ensureIndex();
        BoolQuery bool = new BoolQuery.Builder()
                .filter(Query.of(f -> f.term(t -> t.field("published").value(true))))
                .filter(Query.of(f -> f.term(t -> t.field("categoryId").value(p.getCategoryId()))))
                .mustNot(Query.of(f -> f.term(t -> t.field("productId").value(productId)))).build();
        NativeQuery query = NativeQuery.builder().withQuery(Query.of(b -> b.bool(bool)))
                .withPageable(PageRequest.of(0, limit, Sort.by(Sort.Order.desc("sale")))).build();
        return operations.search(query, ProductDoc.class).getSearchHits().stream().map(SearchHit::getContent).map(mapper::toHit).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int reindexAll() {
        IndexOperations index = operations.indexOps(ProductDoc.class);
        if (index.exists()) {
            index.delete();
        }
        index.createWithMapping();
        List<Product> published = products.findAll().stream().filter(Product::isPublished).toList();
        List<ProductHit> hits = hitFactory.hits(published);
        List<ProductDoc> docs = new ArrayList<>();
        for (int i = 0; i < published.size(); i++) {
            docs.add(mapper.toDoc(published.get(i), hits.get(i)));
        }
        operations.save(docs);
        index.refresh();
        log.info("Indexed {} products into Elasticsearch", docs.size());
        return docs.size();
    }

    @Override
    @Transactional(readOnly = true)
    public void index(Long productId) {
        ensureIndex();
        Product p = products.findById(productId).orElse(null);
        if (p == null || !p.isPublished()) {
            remove(productId);
            return;
        }
        operations.save(mapper.toDoc(p, hitFactory.hits(List.of(p)).get(0)));
    }

    @Override
    public void remove(Long productId) {
        ensureIndex();
        operations.delete(String.valueOf(productId), ProductDoc.class);
    }

    private void ensureIndex() {
        IndexOperations index = operations.indexOps(ProductDoc.class);
        if (!index.exists()) {
            index.createWithMapping();
        }
    }

    private static Sort sort(Integer code) {
        return switch (code == null ? 0 : code) {
            case 1 -> Sort.by(Sort.Order.desc("productId"));
            case 2 -> Sort.by(Sort.Order.desc("sale"), Sort.Order.desc("productId"));
            case 3 -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("productId"));
            case 4 -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("productId"));
            default -> Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("productId"));
        };
    }
}
