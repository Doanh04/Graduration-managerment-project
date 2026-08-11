package com.graduration.Configuration;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationSupport {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 100;

    private PaginationSupport() {}

    public static Pageable pageRequest(Integer page, Integer size) {
        return pageRequest(page, size, Sort.unsorted());
    }

    public static Pageable pageRequest(Integer page, Integer size, Sort sort) {
        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeSize = size == null ? DEFAULT_SIZE : Math.max(1, Math.min(size, MAX_SIZE));
        return PageRequest.of(safePage, safeSize, sort);
    }
}
