package com.example.starter.business.order.repository;

import com.example.starter.business.order.entity.SalesOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrderEntity, String>, JpaSpecificationExecutor<SalesOrderEntity> {

    /**
     * 客戶是否有任何訂單（包含已作廢的）
     */
    boolean existsByCustomerId(String customerId);
}
