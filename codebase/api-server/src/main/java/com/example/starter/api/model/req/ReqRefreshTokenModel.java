package com.example.starter.api.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

/**
 * <code>ReqRefreshTokenModel</code> 換發 access token 或登出時送出的 refresh token
 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class ReqRefreshTokenModel {

    @NotBlank(message = "{refreshToken.empty}")
    private String refreshToken;
}
