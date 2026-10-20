package com.example.starter.business.order.entity;

/**
 * <code>OrderStatus</code> 訂單狀態：只能從正常變成作廢，不能改回
 */
public enum OrderStatus {

    /**
     * 正常
     */
    NORMAL,

    /**
     * 作廢：不計入任何金額與筆數，畫面上以刪除線顯示並可看到原因
     */
    VOIDED
}
