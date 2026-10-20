package com.example.starter.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <code>PermissionEntity</code> is the entity object for table: mt_permission
 */
@Getter
@Setter
@Entity
@Table(name = "mt_permission")
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "permission_id")
    private String permissionId;

    @Column(name = "permission_name")
    private String permissionName;

    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "create_user")
    private String createUser;

    @Column(name = "update_time")
    private Long updateTime;

    @Column(name = "update_user")
    private String updateUser;
}
