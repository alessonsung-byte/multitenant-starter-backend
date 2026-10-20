package com.example.starter.business.customer.service;

import com.example.starter.business.common.PageModel;
import com.example.starter.business.common.PageRequestRule;
import com.example.starter.business.common.TextRule;
import com.example.starter.business.customer.entity.CustomerEntity;
import com.example.starter.business.customer.model.CustomerInput;
import com.example.starter.business.customer.model.CustomerModel;
import com.example.starter.business.customer.repository.CustomerRepository;
import com.example.starter.business.order.repository.SalesOrderRepository;
import com.example.starter.common.exception.ConflictException;
import com.example.starter.common.exception.NotFoundException;
import com.example.starter.user.audit.AuditAction;
import com.example.starter.user.audit.AuditRecorder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * <code>CustomerService</code> 客戶：查詢、新增、修改、刪除（範例業務模組）。
 * <ul>
 *     <li>名稱必填；代碼選填，有填時同一租戶內不可重複（資料庫也有唯一索引）</li>
 *     <li>往來中／已停止往來：列表預設只列往來中的客戶</li>
 *     <li>沒有任何訂單（包含已作廢的）的客戶才可以刪除，有訂單回 409；有訂單的客戶請改為停止往來</li>
 * </ul>
 * 租戶的範圍由 Hibernate（{@code @TenantId}）與資料庫的 Row-Level Security 保證，這裡完全不需要寫「租戶 = ?」的條件；
 * 總管理員只能讀取（寫入由 Controller 限制，資料庫也會拒絕）。
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    public static final int NAME_MAX_LENGTH = 128;

    public static final int CODE_MAX_LENGTH = 64;

    public static final int EMAIL_MAX_LENGTH = 128;

    private static final String TABLE = "mt_customer";

    private final CustomerRepository customerRepository;

    private final SalesOrderRepository salesOrderRepository;

    private final AuditRecorder auditRecorder;

    /**
     * 依名稱或代碼搜尋（部分符合、不分大小寫），新建立的在前
     *
     * @param keyword         關鍵字；空白時列出全部
     * @param tenantId        總管理員檢視某一個租戶時使用；租戶帳號傳 null（本來就只看得到自己的租戶）
     * @param includeInactive true：也列出已停止往來的客戶
     * @param page            第幾頁（從 1 開始）
     * @param size            每頁幾筆
     */
    @Transactional(readOnly = true)
    public PageModel<CustomerModel> search(String keyword, String tenantId, boolean includeInactive, Integer page, Integer size) {
        Specification<CustomerEntity> specification = Specification.where(null);
        if (!includeInactive) {
            specification = specification.and((root, query, builder) -> builder.isTrue(root.get("customerActive")));
        }
        if (StringUtils.isNotBlank(tenantId)) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("tenantId"), tenantId));
        }
        String trimmed = StringUtils.trimToNull(keyword);
        if (trimmed != null) {
            String pattern = TextRule.likePattern(trimmed);
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("customerName")), pattern, '\\'),
                    builder.like(builder.lower(root.get("customerCode")), pattern, '\\')));
        }
        Page<CustomerEntity> result = customerRepository.findAll(specification,
                PageRequestRule.of(page, size, Sort.by(Sort.Order.desc("createTime"), Sort.Order.asc("customerId"))));
        return new PageModel<>(result.map(CustomerModel::from).getContent(), result.getTotalElements());
    }

    /**
     * 一位客戶。其他租戶的客戶一律回 404（Hibernate 自動加上租戶條件，所以「查不到」，不是「沒有權限」）
     */
    @Transactional(readOnly = true)
    public CustomerModel get(String customerId) {
        return CustomerModel.from(findEntity(customerId));
    }

    @Transactional
    public CustomerModel create(CustomerInput input, String operatorId) {
        CustomerEntity entity = new CustomerEntity();
        apply(entity, input, null);
        entity.markCreated(operatorId);
        CustomerModel created = CustomerModel.from(customerRepository.saveAndFlush(entity));
        auditRecorder.record(created.tenantId(), AuditAction.CREATE, TABLE, created.customerId(), created.customerName(), null,
                             snapshot(created));
        return created;
    }

    @Transactional
    public CustomerModel update(String customerId, CustomerInput input, String operatorId) {
        CustomerEntity entity = findEntity(customerId);
        CustomerModel before = CustomerModel.from(entity);
        apply(entity, input, customerId);
        if (input.active() != null) {
            entity.setCustomerActive(input.active());
        }
        entity.markUpdated(operatorId);
        CustomerModel after = CustomerModel.from(customerRepository.saveAndFlush(entity));
        auditRecorder.record(after.tenantId(), AuditAction.UPDATE, TABLE, customerId, after.customerName(), snapshot(before),
                             snapshot(after));
        return after;
    }

    /**
     * 刪除客戶：只限沒有任何訂單的客戶（打錯或重複建立時使用）。刪除前的內容寫入操作紀錄
     */
    @Transactional
    public void delete(String customerId) {
        CustomerEntity entity = findEntity(customerId);
        if (salesOrderRepository.existsByCustomerId(customerId)) {
            throw new ConflictException("此客戶已有訂單，無法刪除；請改為停止往來");
        }
        CustomerModel before = CustomerModel.from(entity);
        customerRepository.delete(entity);
        customerRepository.flush();
        auditRecorder.record(before.tenantId(), AuditAction.DELETE, TABLE, customerId, before.customerName(), snapshot(before), null);
    }

    /**
     * 名稱、代碼、信箱的共同檢查
     *
     * @param customerId 修改時為這位客戶（檢查代碼重複時排除自己）；新增時為 null
     */
    private void apply(CustomerEntity entity, CustomerInput input, String customerId) {
        String code = TextRule.optional(input.customerCode(), "客戶代碼", CODE_MAX_LENGTH);
        if (code != null) {
            boolean duplicated = customerId == null ? customerRepository.existsByCustomerCode(code)
                    : customerRepository.existsByCustomerCodeAndCustomerIdNot(code, customerId);
            if (duplicated) {
                throw new ConflictException("客戶代碼「" + code + "」已經存在");
            }
        }
        entity.setCustomerName(TextRule.required(input.customerName(), "客戶名稱", NAME_MAX_LENGTH));
        entity.setCustomerCode(code);
        entity.setCustomerEmail(TextRule.optional(input.customerEmail(), "聯絡信箱", EMAIL_MAX_LENGTH));
    }

    private CustomerEntity findEntity(String customerId) {
        return customerRepository.findById(customerId).orElseThrow(() -> new NotFoundException("找不到這位客戶"));
    }

    /**
     * 操作紀錄的內容：以總管理員看得懂的欄位名稱與值記錄（不放編號與 true/false）
     */
    private static Map<String, Object> snapshot(CustomerModel customer) {
        return AuditRecorder.snapshot("名稱", customer.customerName(), "代碼", customer.customerCode(), "信箱", customer.customerEmail(),
                                      "狀態", customer.active() ? "往來中" : "已停止往來");
    }
}
