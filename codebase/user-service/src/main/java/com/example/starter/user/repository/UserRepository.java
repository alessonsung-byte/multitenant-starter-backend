package com.example.starter.user.repository;

import com.example.starter.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <code>UserRepository</code> 存取使用者基本資料（mt_user）
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByUserAccount(String userAccount);

    boolean existsByUserAccount(String userAccount);

    List<UserEntity> findAllByOrderByCreateTimeAsc();

    List<UserEntity> findAllByTenantIdOrderByCreateTimeAsc(String tenantId);
}
