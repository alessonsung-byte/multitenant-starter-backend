package com.example.starter.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * <code>RolePermissionEntity</code> is the entity object for table mt_role_permission
 */
@Getter
@Setter
@Entity
@Table(name = "mt_role_permission")
public class RolePermissionEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -8053866354341714217L;

    @EmbeddedId
    private RolePermissionId id;

    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "create_user")
    private String createUser;

    @Column(name = "update_time")
    private Long updateTime;

    @Column(name = "update_user")
    private String updateUser;
}
