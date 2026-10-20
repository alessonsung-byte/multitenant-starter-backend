package com.example.starter.user.repository;

import com.example.starter.user.entity.RoleUserEntity;
import com.example.starter.user.entity.RoleUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <code>RoleUserRepository</code> 存取使用者擁有的角色（mt_role_user）
 */
@Repository
public interface RoleUserRepository extends JpaRepository<RoleUserEntity, RoleUserId> {
}
