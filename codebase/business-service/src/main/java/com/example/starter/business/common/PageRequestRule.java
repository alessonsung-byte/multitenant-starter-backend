package com.example.starter.business.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * <code>PageRequestRule</code> 分頁參數的共同規則：page 從 1 開始、size 預設 20、最多 100
 */
public final class PageRequestRule {

    public static final int DEFAULT_SIZE = 20;

    public static final int MAX_SIZE = 100;

    private PageRequestRule() {
    }

    /**
     * @param page 第幾頁（從 1 開始；小於 1 視為 1）
     * @param size 每頁幾筆（沒有給或小於 1 時為 20，超過 100 時為 100）
     */
    public static PageRequest of(Integer page, Integer size, Sort sort) {
        int pageNumber = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(pageNumber - 1, pageSize, sort);
    }
}
