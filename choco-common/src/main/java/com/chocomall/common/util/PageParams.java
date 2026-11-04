package com.chocomall.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Translates the 1-based {@code pageNum}/{@code pageSize} query parameters into a {@link Pageable}. */
public final class PageParams {

    public static final int MAX_PAGE_SIZE = 100;

    private PageParams() {
    }

    public static Pageable of(Integer pageNum, Integer pageSize) {
        return of(pageNum, pageSize, Sort.unsorted());
    }

    public static Pageable of(Integer pageNum, Integer pageSize, Sort sort) {
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        return PageRequest.of(page - 1, size, sort);
    }
}
