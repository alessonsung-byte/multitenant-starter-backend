package com.example.starter.user.repository;

import com.example.starter.user.entity.SigninEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <code>SigninRepository</code> 存取使用者登入資料（mt_signin）
 */
@Repository
public interface SigninRepository extends JpaRepository<SigninEntity, String> {

    Optional<SigninEntity> findBySigninAccount(String signinAccount);

    Optional<SigninEntity> findByUserId(String userId);

    boolean existsBySigninAccount(String signinAccount);
}
