package com.example.starter.user.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * <code>RolePermissionId</code> is the compound primary keys for entity {@link RolePermissionEntity}
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class RolePermissionId implements Serializable {

    @Serial
    private static final long serialVersionUID = 4183928916975663847L;
    
    private String roleId;
    
    private String permissionId;
}
