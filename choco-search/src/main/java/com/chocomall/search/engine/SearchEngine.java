package com.chocomall.search.engine;

import com.chocomall.search.model.SearchModels.PageResult;
import com.chocomall.search.model.SearchModels.ProductHit;
import com.chocomall.search.model.SearchModels.SearchRequest;
import java.util.List;

/** A product search backend. The API layer does not know which one is running. */
public interface SearchEngine {

    String name();

    PageResult<ProductHit> search(SearchRequest request);

    /** Products similar to the given one (same category or sharing flavors), best sellers first. */
    List<ProductHit> recommend(Long productId, int limit);

    /** Rebuilds the whole index and returns the number of documents. A no-op for the database engine. */
    int reindexAll();

    /** Adds or refreshes one product (removes it when it is no longer published). */
    void index(Long productId);

    void remove(Long productId);
}
