package com.example.starter.user.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * <code>RoleUserId</code> is the compound primary keys for entity {@link RoleUserEntity}
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class RoleUserId implements Serializable {

    @Serial
    private static final long serialVersionUID = -213561676832141307L;
    
    private String userId;

    private String roleId;
}
