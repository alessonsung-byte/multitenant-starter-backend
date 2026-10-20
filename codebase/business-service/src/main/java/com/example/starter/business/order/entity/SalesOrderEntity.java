package com.example.starter.business.order.entity;

import com.example.starter.business.common.AuditedEntity;
import com.example.starter.common.exception.ConflictException;
import com.example.starter.common.exception.InvalidArgumentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <code>SalesOrderEntity</code> 對應資料表 mt_sales_order：訂單（屬於租戶）。
 * <br>
 * 訂單內容只能在建立時決定（{@link #create}），之後唯一能做的改變是作廢（{@link #voidOrder}），
 * 所以這個類別沒有任何 setter（資料庫的 trigger 也會擋下其他修改）。
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mt_sales_order")
public class SalesOrderEntity extends AuditedEntity {

    /**
     * 作廢原因最多幾個字（與資料表欄位長度相同）
     */
    public static final int VOID_REASON_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private String orderId;

    /**
     * 所屬租戶。由 Hibernate 依登入者自動填入與過濾
     */
    @TenantId
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "quantity")
    private int quantity;

    /**
     * 小計＝單價×數量
     */
    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus = OrderStatus.NORMAL;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "void_time")
    private Long voidTime;

    @Column(name = "void_user")
    private String voidUser;

    /**
     * 建立一筆訂單：小計由這裡計算（不採用前端送來的金額）
     *
     * @param unitPrice 單價，已由 Service 檢查為 0 以上、最多兩位小數
     */
    public static SalesOrderEntity create(String customerId, LocalDate orderDate, String itemName, BigDecimal unitPrice, int quantity,
                                          String operatorId) {
        SalesOrderEntity entity = new SalesOrderEntity();
        entity.customerId = customerId;
        entity.orderDate = orderDate;
        entity.itemName = itemName;
        entity.unitPrice = unitPrice.setScale(2);
        entity.quantity = quantity;
        entity.subtotal = entity.unitPrice.multiply(BigDecimal.valueOf(quantity));
        entity.orderStatus = OrderStatus.NORMAL;
        entity.markCreated(operatorId);
        return entity;
    }

    /**
     * 作廢：原因必填；已作廢的不能再作廢
     */
    public void voidOrder(String reason, String operatorId) {
        if (orderStatus != OrderStatus.NORMAL) {
            throw new ConflictException("這筆訂單已經作廢");
        }
        String trimmed = StringUtils.trimToNull(reason);
        if (trimmed == null) {
            throw new InvalidArgumentException("請輸入作廢原因");
        }
        if (trimmed.length() > VOID_REASON_MAX_LENGTH) {
            throw new InvalidArgumentException("作廢原因最多 " + VOID_REASON_MAX_LENGTH + " 個字");
        }
        this.orderStatus = OrderStatus.VOIDED;
        this.voidReason = trimmed;
        this.voidTime = System.currentTimeMillis();
        this.voidUser = operatorId;
        markUpdated(operatorId);
    }
}
