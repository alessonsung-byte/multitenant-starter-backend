package com.example.starter.user.repository;

import com.example.starter.user.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <code>TenantRepository</code> 存取租戶資料（mt_tenant）
 */
@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, String> {

    List<TenantEntity> findAllByOrderByCreateTimeAsc();

    boolean existsByTenantName(String tenantName);

    boolean existsByTenantNameAndTenantIdNot(String tenantName, String tenantId);
}
