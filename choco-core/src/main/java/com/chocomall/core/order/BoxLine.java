package com.chocomall.core.order;

/** One chosen product inside a build-a-box selection: {@code quantity} pieces of {@code productId} per box. */
public record BoxLine(Long productId, int quantity) {
}
