package com.example.starter.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <code>RoleEntity</code> is the entity object for table mt_role
 */
@Getter
@Setter
@Entity
@Table(name = "mt_role")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id")
    private String roleId;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "create_user")
    private String createUser;

    @Column(name = "update_time")
    private Long updateTime;

    @Column(name = "update_user")
    private String updateUser;
}
