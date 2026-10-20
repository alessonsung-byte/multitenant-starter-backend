package com.example.starter.business.order.service;

import com.example.starter.business.common.BusinessDate;
import com.example.starter.business.common.PageRequestRule;
import com.example.starter.business.common.TextRule;
import com.example.starter.business.customer.entity.CustomerEntity;
import com.example.starter.business.customer.repository.CustomerRepository;
import com.example.starter.business.order.entity.OrderStatus;
import com.example.starter.business.order.entity.SalesOrderEntity;
import com.example.starter.business.order.model.OrderInput;
import com.example.starter.business.order.model.OrderModel;
import com.example.starter.business.order.model.OrderPageModel;
import com.example.starter.business.order.repository.SalesOrderRepository;
import com.example.starter.common.exception.InvalidArgumentException;
import com.example.starter.common.exception.NotFoundException;
import com.example.starter.user.audit.AuditAction;
import com.example.starter.user.audit.AuditRecorder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <code>OrderService</code> 訂單（範例業務模組）。
 * <ul>
 *     <li>客戶必須是本租戶、往來中的客戶；小計＝單價×數量由後端計算</li>
 *     <li>不能修改，只能作廢（必填原因）；作廢的不計入任何金額與筆數</li>
 *     <li>查詢結果附上「符合條件的全部有效訂單」的筆數與金額合計（不只這一頁）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    public static final int MAX_QUANTITY = 999;

    public static final int ITEM_NAME_MAX_LENGTH = 128;

    /**
     * 單價上限（資料表欄位 NUMERIC(12, 2)，小計也不能超過）
     */
    public static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("1000000");

    private static final String TABLE = "mt_sales_order";

    private final SalesOrderRepository salesOrderRepository;

    private final CustomerRepository customerRepository;

    private final AuditRecorder auditRecorder;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public OrderModel create(OrderInput input, String operatorId) {
        String customerId = StringUtils.trimToNull(input.customerId());
        // 客戶屬於租戶：查詢時 Hibernate 自動加上目前租戶的條件，其他租戶的客戶查不到
        CustomerEntity customer = customerId == null ? null : customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            throw new NotFoundException("找不到這位客戶");
        }
        if (!customer.isCustomerActive()) {
            throw new InvalidArgumentException("客戶「" + customer.getCustomerName() + "」已停止往來，不能新增訂單");
        }
        LocalDate orderDate = validateDate(input.orderDate());
        String itemName = TextRule.required(input.itemName(), "品項名稱", ITEM_NAME_MAX_LENGTH);
        BigDecimal unitPrice = validateUnitPrice(input.unitPrice());
        int quantity = validateQuantity(input.quantity());

        SalesOrderEntity saved = salesOrderRepository.saveAndFlush(
                SalesOrderEntity.create(customer.getCustomerId(), orderDate, itemName, unitPrice, quantity, operatorId));
        auditRecorder.record(saved.getTenantId(), AuditAction.CREATE, TABLE, saved.getOrderId(), auditLabel(saved, customer), null,
                             snapshot(saved, customer));
        return toModels(List.of(saved)).getFirst();
    }

    /**
     * 作廢訂單（必填原因）
     */
    @Transactional
    public OrderModel voidOrder(String orderId, String reason, String operatorId) {
        SalesOrderEntity order = salesOrderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("找不到這筆訂單"));
        order.voidOrder(reason, operatorId);
        salesOrderRepository.saveAndFlush(order);
        CustomerEntity customer = customerRepository.findById(order.getCustomerId()).orElse(null);
        auditRecorder.record(order.getTenantId(), AuditAction.VOID, TABLE, orderId, auditLabel(order, customer),
                             AuditRecorder.snapshot("狀態", "正常"), AuditRecorder.snapshot("狀態", "作廢", "作廢原因", order.getVoidReason()));
        return toModels(List.of(order)).getFirst();
    }

    /**
     * 訂單查詢
     *
     * @param from          訂單日期起（含），選填
     * @param to            訂單日期迄（含），選填
     * @param customerId    只看某一位客戶，選填
     * @param includeVoided 是否列出作廢的（金額合計一律不含作廢的）
     * @param tenantId      總管理員檢視某一個租戶時使用；租戶帳號傳 null
     */
    @Transactional(readOnly = true)
    public OrderPageModel search(LocalDate from, LocalDate to, String customerId, boolean includeVoided, String tenantId,
                                 Integer page, Integer size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidArgumentException("開始日期不可晚於結束日期");
        }
        Specification<SalesOrderEntity> filters = filters(from, to, customerId, tenantId);
        Specification<SalesOrderEntity> listed = includeVoided ? filters : filters.and(normalOnly());
        Page<SalesOrderEntity> result = salesOrderRepository.findAll(listed, PageRequestRule.of(page, size,
                Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("createTime"), Sort.Order.asc("orderId"))));

        Object[] summary = summarize(filters.and(normalOnly()));
        return new OrderPageModel(toModels(result.getContent()), result.getTotalElements(), ((Number) summary[0]).longValue(),
                                  summary[1] == null ? BigDecimal.ZERO : (BigDecimal) summary[1]);
    }

    /**
     * 符合條件的有效訂單筆數與金額合計（一次 SQL 算出，不把資料全部讀進記憶體）
     */
    private Object[] summarize(Specification<SalesOrderEntity> specification) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);
        Root<SalesOrderEntity> root = query.from(SalesOrderEntity.class);
        query.multiselect(builder.count(root), builder.sum(root.<BigDecimal>get("subtotal")));
        query.where(specification.toPredicate(root, query, builder));
        return entityManager.createQuery(query).getSingleResult();
    }

    private static Specification<SalesOrderEntity> filters(LocalDate from, LocalDate to, String customerId, String tenantId) {
        Specification<SalesOrderEntity> specification = Specification.where(null);
        if (from != null) {
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("orderDate"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("orderDate"), to));
        }
        if (StringUtils.isNotBlank(customerId)) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("customerId"), customerId));
        }
        if (StringUtils.isNotBlank(tenantId)) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("tenantId"), tenantId));
        }
        return specification;
    }

    private static Specification<SalesOrderEntity> normalOnly() {
        return (root, query, builder) -> builder.equal(root.get("orderStatus"), OrderStatus.NORMAL);
    }

    /**
     * 操作紀錄的標題：客戶 品項 訂單日期
     */
    private static String auditLabel(SalesOrderEntity order, CustomerEntity customer) {
        String customerName = customer == null ? "" : customer.getCustomerName();
        return (customerName + " " + order.getItemName() + " " + order.getOrderDate()).trim();
    }

    private static Map<String, Object> snapshot(SalesOrderEntity order, CustomerEntity customer) {
        return AuditRecorder.snapshot("客戶", customer.getCustomerName(), "訂單日期", order.getOrderDate(), "品項", order.getItemName(),
                                      "單價", order.getUnitPrice(), "數量", order.getQuantity(), "小計", order.getSubtotal());
    }

    private static LocalDate validateDate(LocalDate orderDate) {
        if (orderDate == null) {
            throw new InvalidArgumentException("請輸入訂單日期");
        }
        if (orderDate.isAfter(BusinessDate.today())) {
            throw new InvalidArgumentException("訂單日期 " + orderDate + " 晚於今天");
        }
        return orderDate;
    }

    private static BigDecimal validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            throw new InvalidArgumentException("請輸入單價");
        }
        if (unitPrice.signum() < 0 || unitPrice.compareTo(MAX_UNIT_PRICE) > 0) {
            throw new InvalidArgumentException("單價必須是 0～" + MAX_UNIT_PRICE.toPlainString());
        }
        if (unitPrice.stripTrailingZeros().scale() > 2) {
            throw new InvalidArgumentException("單價最多兩位小數");
        }
        return unitPrice;
    }

    private static int validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > MAX_QUANTITY) {
            throw new InvalidArgumentException("數量必須是 1～" + MAX_QUANTITY);
        }
        return quantity;
    }

    private List<OrderModel> toModels(List<SalesOrderEntity> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<String, String> customerNames = customerRepository.findAllById(
                        orders.stream().map(SalesOrderEntity::getCustomerId).distinct().toList()).stream()
                .collect(Collectors.toMap(CustomerEntity::getCustomerId, CustomerEntity::getCustomerName));
        return orders.stream().map(order -> new OrderModel(order.getOrderId(), order.getTenantId(), order.getCustomerId(),
                                                           customerNames.get(order.getCustomerId()), order.getOrderDate(),
                                                           order.getItemName(), order.getUnitPrice(), order.getQuantity(),
                                                           order.getSubtotal(), order.getOrderStatus(), order.getVoidReason(),
                                                           order.getVoidTime(), order.getCreateTime())).toList();
    }
}
