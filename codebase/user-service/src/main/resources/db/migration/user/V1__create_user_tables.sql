-- ----------------------------------------------------
-- 帳號、登入、角色、權限相關資料表
-- ----------------------------------------------------
-- 資料庫本身（starter）需先建立，Flyway 只負責建立資料表。
-- docker-compose-local.yml 的 postgres 容器會自動建立資料庫 starter。

-- 產生 UUID 用
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- TABLE: mt_user 使用者基本資料
CREATE TABLE mt_user
(
    user_id      VARCHAR(128) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    user_account VARCHAR(128) NOT NULL,
    user_email   VARCHAR(128) NULL,
    user_enabled BOOLEAN      NOT NULL,
    create_time  BIGINT       NOT NULL,
    create_user  VARCHAR(128) NOT NULL,
    update_time  BIGINT,
    update_user  VARCHAR(128)
);
CREATE UNIQUE INDEX ix_user_account ON mt_user (user_account);
COMMENT ON TABLE mt_user IS '使用者基本資料';
COMMENT ON COLUMN mt_user.user_id IS '使用者編號，資料庫自動產生 UUID';
COMMENT ON COLUMN mt_user.user_account IS '使用者帳號（顯示用），與 mt_signin.signin_account 相同，不可重複';
COMMENT ON COLUMN mt_user.user_email IS '使用者信箱，可為空';
COMMENT ON COLUMN mt_user.user_enabled IS '是否啟用。停用的帳號無法登入（停用而非刪除）';
COMMENT ON COLUMN mt_user.create_time IS '新建資料的時間，值為毫秒';
COMMENT ON COLUMN mt_user.create_user IS '新建資料的使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_user.update_time IS '更新資料的時間，值為毫秒';
COMMENT ON COLUMN mt_user.update_user IS '更新資料的使用者編號，即 mt_user.user_id';

-- TABLE: mt_signin 登入用帳號與密碼雜湊
CREATE TABLE mt_signin
(
    signin_id      VARCHAR(128) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    signin_account VARCHAR(128) NOT NULL,
    signin_pw      VARCHAR(256) NOT NULL,
    user_id        VARCHAR(128) NOT NULL,
    create_time    BIGINT       NOT NULL,
    create_user    VARCHAR(128) NOT NULL,
    update_time    BIGINT,
    update_user    VARCHAR(128),
    FOREIGN KEY (user_id) REFERENCES mt_user (user_id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ix_signin_account ON mt_signin (signin_account);
COMMENT ON TABLE mt_signin IS '登入用帳號與密碼雜湊';
COMMENT ON COLUMN mt_signin.signin_id IS '登入資料編號，資料庫自動產生 UUID';
COMMENT ON COLUMN mt_signin.signin_account IS '登入帳號，不可重複';
COMMENT ON COLUMN mt_signin.signin_pw IS '登入密碼的 BCrypt 雜湊值（不儲存明碼）';
COMMENT ON COLUMN mt_signin.user_id IS '對應的使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_signin.create_time IS '新建資料的時間，值為毫秒';
COMMENT ON COLUMN mt_signin.create_user IS '新建資料的使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_signin.update_time IS '更新資料的時間，值為毫秒';
COMMENT ON COLUMN mt_signin.update_user IS '更新資料的使用者編號，即 mt_user.user_id';

-- TABLE: mt_role 角色
CREATE TABLE mt_role
(
    role_id     VARCHAR(128) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    role_name   VARCHAR(128) NOT NULL,
    create_time BIGINT       NOT NULL,
    create_user VARCHAR(128) NOT NULL,
    update_time BIGINT,
    update_user VARCHAR(128)
);
CREATE UNIQUE INDEX ix_role_role_name ON mt_role (role_name);
COMMENT ON TABLE mt_role IS '角色';
COMMENT ON COLUMN mt_role.role_id IS '角色編號，資料庫自動產生 UUID';
COMMENT ON COLUMN mt_role.role_name IS '角色名稱，不可重複，也不可以 ROLE_ 開頭';
COMMENT ON COLUMN mt_role.create_time IS '新建資料的時間，值為毫秒';
COMMENT ON COLUMN mt_role.create_user IS '新建資料的使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_role.update_time IS '更新資料的時間，值為毫秒';
COMMENT ON COLUMN mt_role.update_user IS '更新資料的使用者編號，即 mt_user.user_id';

-- TABLE: mt_permission 權限
CREATE TABLE mt_permission
(
    permission_id   VARCHAR(128) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    permission_name VARCHAR(128) NOT NULL,
    create_time     BIGINT       NOT NULL,
    create_user     VARCHAR(128) NOT NULL,
    update_time     BIGINT,
    update_user     VARCHAR(128)
);
CREATE UNIQUE INDEX ix_perm_perm_name ON mt_permission (permission_name);
COMMENT ON TABLE mt_permission IS '權限';
COMMENT ON COLUMN mt_permission.permission_id IS '權限編號，資料庫自動產生 UUID';
COMMENT ON COLUMN mt_permission.permission_name IS '權限名稱，不可重複';
COMMENT ON COLUMN mt_permission.create_time IS '新建資料的時間，值為毫秒';
COMMENT ON COLUMN mt_permission.create_user IS '新建資料的使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_permission.update_time IS '更新資料的時間，值為毫秒';
COMMENT ON COLUMN mt_permission.update_user IS '更新資料的使用者編號，即 mt_user.user_id';

-- TABLE: mt_role_permission 角色擁有的權限（多對多）
CREATE TABLE mt_role_permission
(
    role_id       VARCHAR(128) NOT NULL,
    permission_id VARCHAR(128) NOT NULL,
    create_time   BIGINT       NOT NULL,
    create_user   VARCHAR(128) NOT NULL,
    update_time   BIGINT,
    update_user   VARCHAR(128),
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES mt_role (role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES mt_permission (permission_id) ON DELETE CASCADE
);
COMMENT ON TABLE mt_role_permission IS '角色擁有的權限（多對多）';
COMMENT ON COLUMN mt_role_permission.role_id IS '角色編號，即 mt_role.role_id';
COMMENT ON COLUMN mt_role_permission.permission_id IS '權限編號，即 mt_permission.permission_id';
COMMENT ON COLUMN mt_role_permission.create_time IS '新建資料的時間，值為毫秒';
COMMENT ON COLUMN mt_role_permission.create_user IS '新建資料的使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_role_permission.update_time IS '更新資料的時間，值為毫秒';
COMMENT ON COLUMN mt_role_permission.update_user IS '更新資料的使用者編號，即 mt_user.user_id';

-- TABLE: mt_role_user 使用者擁有的角色（多對多）
CREATE TABLE mt_role_user
(
    user_id     VARCHAR(128) NOT NULL,
    role_id     VARCHAR(128) NOT NULL,
    create_time BIGINT       NOT NULL,
    create_user VARCHAR(128) NOT NULL,
    update_time BIGINT,
    update_user VARCHAR(128),
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES mt_user (user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES mt_role (role_id) ON DELETE CASCADE
);
COMMENT ON TABLE mt_role_user IS '使用者擁有的角色（多對多）';
COMMENT ON COLUMN mt_role_user.user_id IS '使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_role_user.role_id IS '角色編號，即 mt_role.role_id';
COMMENT ON COLUMN mt_role_user.create_time IS '新建資料的時間，值為毫秒';
COMMENT ON COLUMN mt_role_user.create_user IS '新建資料的使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_role_user.update_time IS '更新資料的時間，值為毫秒';
COMMENT ON COLUMN mt_role_user.update_user IS '更新資料的使用者編號，即 mt_user.user_id';

-- ----------------------------------------------------
-- 預設資料：一個總管理員帳號
-- 帳號 admin，初始密碼 admin（BCrypt 強度 12 的雜湊值）。
-- 注意：初始密碼僅供第一次登入使用，正式上線前務必更換
-- ----------------------------------------------------
INSERT INTO mt_user (user_id, user_account, user_email, user_enabled, create_time, create_user)
VALUES ('d9d3d4e2-ec90-4cf0-b984-c131d901e38e', 'admin', NULL, TRUE,
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e');

INSERT INTO mt_signin (signin_account, signin_pw, user_id, create_time, create_user)
VALUES ('admin', '$2a$12$x7VuXwkETzpAq7Xad5jLUO3b1XfKDlBNcx70R52eVEf75cH/zP6Wy', 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e',
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e');

-- 角色 SYS_ADMIN：總管理員
INSERT INTO mt_role (role_id, role_name, create_time, create_user)
VALUES ('b6e7c25f-12af-4167-8b4b-124945e240fb', 'SYS_ADMIN',
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e');

INSERT INTO mt_role_user (user_id, role_id, create_time, create_user)
VALUES ('d9d3d4e2-ec90-4cf0-b984-c131d901e38e', 'b6e7c25f-12af-4167-8b4b-124945e240fb',
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e');
