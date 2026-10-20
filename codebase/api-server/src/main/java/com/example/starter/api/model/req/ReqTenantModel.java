package com.example.starter.api.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * <code>ReqTenantModel</code> 新增租戶、修改租戶名稱的 request
 */
@Data
public class ReqTenantModel {

    @NotBlank(message = "{tenant.name.empty}")
    @Size(max = 128, message = "{tenant.name.too.long}")
    private String tenantName;
}
