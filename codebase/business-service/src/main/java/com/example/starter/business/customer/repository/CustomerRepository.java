package com.example.starter.business.customer.repository;

import com.example.starter.business.customer.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 屬於租戶的資料表：Hibernate 會自動加上「租戶 = 目前租戶」（總管理員除外）
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, String>, JpaSpecificationExecutor<CustomerEntity> {

    /**
     * 目前租戶內是否已有這個客戶代碼（Hibernate 自動加上租戶條件，所以不同租戶可以重複）
     */
    boolean existsByCustomerCode(String customerCode);

    boolean existsByCustomerCodeAndCustomerIdNot(String customerCode, String customerId);
}
