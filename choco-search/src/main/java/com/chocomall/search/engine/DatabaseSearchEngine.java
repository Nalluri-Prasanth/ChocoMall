package com.chocomall.search.engine;

import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductClaim;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.search.model.SearchModels.PageResult;
import com.chocomall.search.model.SearchModels.ProductHit;
import com.chocomall.search.model.SearchModels.SearchRequest;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Searches the shop database directly. Always up to date and needs no extra infrastructure, so it is the default
 * for development and small shops; switch to Elasticsearch ({@code search.engine=elasticsearch}) for relevance
 * ranking, typo tolerance and scale.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.engine", havingValue = "database", matchIfMissing = true)
public class DatabaseSearchEngine implements SearchEngine {

    private final ProductRepository products;
    private final ProductCategoryRepository categories;
    private final ProductHitFactory hits;

    @Override
    public String name() {
        return "database";
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductHit> search(SearchRequest q) {
        var page = products.findAll(specification(q), PageRequest.of(Math.max(q.page() - 1, 0), q.size(), sort(q.sort())));
        return new PageResult<>(hits.hits(page.getContent()), page.getTotalElements(), page.getNumber() + 1, page.getSize(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductHit> recommend(Long productId, int limit) {
        Product p = products.findById(productId).filter(Product::isPublished).orElse(null);
        if (p == null) {
            return List.of();
        }
        Specification<Product> spec = (root, query, cb) -> cb.and(cb.isTrue(root.get("published")),
                cb.notEqual(root.get("id"), productId), cb.equal(root.get("categoryId"), p.getCategoryId()));
        return hits.hits(products.findAll(spec, PageRequest.of(0, limit, Sort.by(Sort.Order.desc("sale"), Sort.Order.desc("id")))).getContent());
    }

    @Override
    public int reindexAll() {
        return 0; // nothing to rebuild: searches read the live tables
    }

    @Override
    public void index(Long productId) {
        // nothing to do
    }

    @Override
    public void remove(Long productId) {
        // nothing to do
    }

    private Specification<Product> specification(SearchRequest q) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.isTrue(root.get("published")));
            if (q.keyword() != null && !q.keyword().isBlank()) {
                String like = "%" + q.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                all.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(cb.coalesce(root.<String>get("subTitle"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("keywords"), "")), like), cb.like(cb.lower(root.get("productSn")), like)));
            }
            if (q.categoryId() != null) {
                List<Long> ids = new ArrayList<>(List.of(q.categoryId()));
                categories.findByParentIdOrderBySortDesc(q.categoryId()).forEach(c -> ids.add(c.getId()));
                all.add(root.get("categoryId").in(ids));
            }
            if (q.brandId() != null) {
                all.add(cb.equal(root.get("brandId"), q.brandId()));
            }
            if (q.kind() != null) {
                all.add(cb.equal(root.get("kind"), q.kind()));
            }
            if (q.chocolateTypeId() != null) {
                all.add(cb.equal(root.get("chocolateTypeId"), q.chocolateTypeId()));
            }
            if (q.minPrice() != null) {
                all.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), q.minPrice()));
            }
            if (q.maxPrice() != null) {
                all.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), q.maxPrice()));
            }
            if (q.flavorId() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Product> p2 = sub.from(Product.class);
                sub.select(p2.<Long>get("id")).where(cb.equal(p2.get("id"), root.get("id")), p2.join("flavorIds").in(q.flavorId()));
                all.add(cb.exists(sub));
            }
            if (q.excludeAllergens() != null && !q.excludeAllergens().isEmpty()) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Product> p2 = sub.from(Product.class);
                sub.select(p2.<Long>get("id")).where(cb.equal(p2.get("id"), root.get("id")), p2.join("allergenIds").in(q.excludeAllergens()));
                all.add(cb.not(cb.exists(sub)));
            }
            if (q.claim() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<ProductClaim> c = sub.from(ProductClaim.class);
                sub.select(c.<Long>get("id")).where(cb.equal(c.get("productId"), root.get("id")),
                        cb.equal(c.get("claimType"), q.claim()), cb.isTrue(c.get("verified")));
                all.add(cb.exists(sub));
            }
            return cb.and(all.toArray(Predicate[]::new));
        };
    }

    static Sort sort(Integer code) {
        return switch (code == null ? 0 : code) {
            case 1 -> Sort.by(Sort.Order.desc("id"));
            case 2 -> Sort.by(Sort.Order.desc("sale"), Sort.Order.desc("id"));
            case 3 -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("id"));
            case 4 -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
            default -> Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id"));
        };
    }
}
