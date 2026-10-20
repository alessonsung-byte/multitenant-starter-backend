package com.example.starter.business.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * <code>AuditedEntity</code> 每張資料表共有的「誰、什麼時候建立／修改」四個欄位。
 * <br>
 * 時間是毫秒數，使用者是 mt_user.user_id。新增時呼叫 {@link #markCreated(String)}，修改時呼叫 {@link #markUpdated(String)}。
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditedEntity {

    /**
     * 新建資料的時間，值為毫秒
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private Long createTime;

    /**
     * 新建資料的使用者編號，即 mt_user.user_id
     */
    @Column(name = "create_user", nullable = false, updatable = false)
    private String createUser;

    /**
     * 更新資料的時間，值為毫秒
     */
    @Column(name = "update_time")
    private Long updateTime;

    /**
     * 更新資料的使用者編號，即 mt_user.user_id
     */
    @Column(name = "update_user")
    private String updateUser;

    /**
     * 記錄由誰在現在建立
     */
    public void markCreated(String operatorId) {
        this.createTime = System.currentTimeMillis();
        this.createUser = operatorId;
    }

    /**
     * 記錄由誰在現在修改
     */
    public void markUpdated(String operatorId) {
        this.updateTime = System.currentTimeMillis();
        this.updateUser = operatorId;
    }
}
