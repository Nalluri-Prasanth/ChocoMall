package com.chocomall.core.messaging;

/** Tells the search service that a product changed so its index can be refreshed. */
public interface SearchSyncPublisher {

    void productChanged(Long productId);

    void productRemoved(Long productId);
}
