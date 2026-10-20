package com.example.starter.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * <code>RoleUserEntity</code> is the entity object for table mt_role_user
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mt_role_user")
public class RoleUserEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -5625323644249121466L;
    
    @EmbeddedId
    private RoleUserId id;

    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "create_user")
    private String createUser;

    @Column(name = "update_time")
    private Long updateTime;

    @Column(name = "update_user")
    private String updateUser;
}
