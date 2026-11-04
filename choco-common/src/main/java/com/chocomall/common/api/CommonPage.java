package com.chocomall.common.api;

import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

/** Paged payload. {@code pageNum} is 1-based, as in the storefront and admin UIs. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommonPage<T> {

    private int pageNum;
    private int pageSize;
    private int totalPage;
    private long total;
    private List<T> list;

    public static <T> CommonPage<T> of(Page<T> page) {
        return new CommonPage<>(page.getNumber() + 1, page.getSize(), page.getTotalPages(),
                page.getTotalElements(), page.getContent());
    }

    public static <E, T> CommonPage<T> of(Page<E> page, Function<E, T> mapper) {
        return new CommonPage<>(page.getNumber() + 1, page.getSize(), page.getTotalPages(),
                page.getTotalElements(), page.getContent().stream().map(mapper).toList());
    }

    public static <T> CommonPage<T> of(List<T> all, int pageNum, int pageSize) {
        int from = Math.min((pageNum - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        int pages = pageSize == 0 ? 0 : (int) Math.ceil(all.size() / (double) pageSize);
        return new CommonPage<>(pageNum, pageSize, pages, all.size(), all.subList(from, to));
    }
}
