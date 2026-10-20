package com.example.starter.business.order.entity;

import com.example.starter.common.exception.ConflictException;
import com.example.starter.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <code>SalesOrderEntityTest</code> 訂單的業務規則：小計由後端計算、只能作廢一次、作廢原因必填
 */
class SalesOrderEntityTest {

    @Test
    void subtotalIsUnitPriceTimesQuantity() {
        SalesOrderEntity order = SalesOrderEntity.create("c-1", LocalDate.of(2026, 9, 1), "品項", new BigDecimal("12.5"), 3, "u-1");

        assertThat(order.getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(order.getSubtotal()).isEqualByComparingTo("37.50");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.NORMAL);
        assertThat(order.getCreateUser()).isEqualTo("u-1");
    }

    @Test
    void voidingRequiresAReasonAndHappensOnlyOnce() {
        SalesOrderEntity order = SalesOrderEntity.create("c-1", LocalDate.of(2026, 9, 1), "品項", BigDecimal.TEN, 1, "u-1");

        assertThatThrownBy(() -> order.voidOrder("  ", "u-2")).isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> order.voidOrder("x".repeat(SalesOrderEntity.VOID_REASON_MAX_LENGTH + 1), "u-2"))
                .isInstanceOf(InvalidArgumentException.class);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.NORMAL);

        order.voidOrder(" 登打錯誤 ", "u-2");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.VOIDED);
        assertThat(order.getVoidReason()).isEqualTo("登打錯誤");
        assertThat(order.getVoidUser()).isEqualTo("u-2");
        assertThat(order.getVoidTime()).isNotNull();

        assertThatThrownBy(() -> order.voidOrder("再一次", "u-2")).isInstanceOf(ConflictException.class);
    }
}
