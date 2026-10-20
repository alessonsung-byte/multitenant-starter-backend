package com.example.starter.user.repository;

import com.example.starter.user.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

/**
 * <code>RoleRepository</code> 存取角色資料（mt_role）
 */
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, String> {

    @Query("SELECT r FROM RoleEntity r, RoleUserEntity ru WHERE r.roleId = ru.id.roleId AND ru.id.userId = :userId")
    Set<RoleEntity> findRolesByUserId(@Param("userId") String userId);

    Optional<RoleEntity> findByRoleName(String roleName);
}
