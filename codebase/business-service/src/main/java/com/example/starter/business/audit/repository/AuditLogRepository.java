package com.example.starter.business.audit.repository;

import com.example.starter.business.audit.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String>, JpaSpecificationExecutor<AuditLogEntity> {
}
