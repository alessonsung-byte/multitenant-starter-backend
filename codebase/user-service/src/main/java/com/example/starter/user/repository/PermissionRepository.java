package com.example.starter.user.repository;

import com.example.starter.user.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * <code>PermissionRepository</code> is the repository for entity {@link PermissionEntity}
 */
@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, String> {

    @Query("SELECT p FROM PermissionEntity p, RolePermissionEntity rp WHERE p.permissionId = rp.id.permissionId AND rp.id.roleId = :roleId")
    Set<PermissionEntity> findPermissionsByRoleId(@Param("roleId") String roleId);
}
