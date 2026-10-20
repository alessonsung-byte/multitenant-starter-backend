package com.example.starter.api.model.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * <code>ReqCreateAccountModel</code> 總管理員新增帳號的 request。密碼由後端產生臨時密碼，不由前端指定
 */
@Data
public class ReqCreateAccountModel {

    /**
     * 登入帳號：英文字母、數字與 . _ -，3～64 個字元
     */
    @NotBlank(message = "{account.empty}")
    private String account;

    /**
     * 角色：SYS_ADMIN（總管理員）或 TENANT_USER（租戶帳號）
     */
    @NotBlank(message = "{account.role.empty}")
    private String roleName;

    /**
     * 所屬租戶：租戶帳號必填；總管理員不可填
     */
    private String tenantId;

    @Email(message = "{account.email.invalid}")
    @Size(max = 128, message = "{account.email.invalid}")
    private String email;
}
