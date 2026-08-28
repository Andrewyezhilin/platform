-- =====================================================
-- 智能化生活服务平台 数据库初始化脚本
-- =====================================================
CREATE DATABASE IF NOT EXISTS smartlife DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE smartlife;

-- 用户表
CREATE TABLE IF NOT EXISTS tb_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    phone       VARCHAR(11)     NOT NULL COMMENT '手机号',
    password    VARCHAR(128)    DEFAULT '' COMMENT '密码（预留）',
    nick_name   VARCHAR(32)     DEFAULT '' COMMENT '昵称',
    icon        VARCHAR(255)    DEFAULT '' COMMENT '头像',
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE = InnoDB COMMENT '用户表';

-- 商户类型表
CREATE TABLE IF NOT EXISTS tb_shop_type (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32)     DEFAULT '' COMMENT '类型名称',
    icon        VARCHAR(255)    DEFAULT '' COMMENT '图标',
    sort        INT UNSIGNED    DEFAULT 0 COMMENT '顺序',
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT '商户类型表';

-- 商户表
CREATE TABLE IF NOT EXISTS tb_shop (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(128)    NOT NULL COMMENT '商户名称',
    type_id     BIGINT UNSIGNED NOT NULL COMMENT '商户类型 ID',
    images      VARCHAR(1024)   DEFAULT '' COMMENT '商户图片，多个逗号分隔',
    area        VARCHAR(128)    DEFAULT '' COMMENT '商圈',
    address     VARCHAR(255)    DEFAULT '' COMMENT '地址',
    x           DOUBLE          DEFAULT 0 COMMENT '经度',
    y           DOUBLE          DEFAULT 0 COMMENT '纬度',
    avg_price   BIGINT UNSIGNED DEFAULT 0 COMMENT '均价（分）',
    sold        INT UNSIGNED    DEFAULT 0 COMMENT '销量',
    comments    INT UNSIGNED    DEFAULT 0 COMMENT '评论数',
    score       INT UNSIGNED    DEFAULT 0 COMMENT '评分 * 10',
    open_hours  VARCHAR(64)     DEFAULT '' COMMENT '营业时间',
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_type_id (type_id)
) ENGINE = InnoDB COMMENT '商户表';

-- 优惠券表
CREATE TABLE IF NOT EXISTS tb_voucher (
    id           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    shop_id      BIGINT UNSIGNED  NOT NULL COMMENT '商户 ID',
    title        VARCHAR(255)     NOT NULL COMMENT '券标题',
    sub_title    VARCHAR(255)     DEFAULT '' COMMENT '副标题',
    rules        VARCHAR(1024)    DEFAULT '' COMMENT '使用规则',
    pay_value    BIGINT UNSIGNED  NOT NULL COMMENT '支付金额（分）',
    actual_value BIGINT UNSIGNED  NOT NULL COMMENT '抵扣金额（分）',
    type         TINYINT UNSIGNED DEFAULT 0 COMMENT '0-普通券 1-秒杀券',
    status       TINYINT UNSIGNED DEFAULT 1 COMMENT '1-上架 2-下架 3-过期',
    create_time  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_shop_id (shop_id)
) ENGINE = InnoDB COMMENT '优惠券表';

-- 秒杀券库存表
CREATE TABLE IF NOT EXISTS tb_seckill_voucher (
    voucher_id  BIGINT UNSIGNED NOT NULL COMMENT '关联优惠券 ID',
    stock       INT             NOT NULL COMMENT '库存',
    begin_time  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开抢时间',
    end_time    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (voucher_id)
) ENGINE = InnoDB COMMENT '秒杀券库存表';

-- 优惠券订单表（ID 由 Redis 全局 ID 生成器生成）
CREATE TABLE IF NOT EXISTS tb_voucher_order (
    id          BIGINT           NOT NULL COMMENT '主键（全局唯一 ID）',
    user_id     BIGINT UNSIGNED  NOT NULL COMMENT '下单用户',
    voucher_id  BIGINT UNSIGNED  NOT NULL COMMENT '购买的券',
    pay_type    TINYINT UNSIGNED DEFAULT 1 COMMENT '1-余额 2-支付宝 3-微信',
    status      TINYINT UNSIGNED DEFAULT 1 COMMENT '1-未支付 2-已支付 3-已核销 4-已取消 5-退款中 6-已退款',
    create_time TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    pay_time    TIMESTAMP        NULL COMMENT '支付时间',
    use_time    TIMESTAMP        NULL COMMENT '核销时间',
    refund_time TIMESTAMP        NULL COMMENT '退款时间',
    update_time TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_voucher (user_id, voucher_id)
) ENGINE = InnoDB COMMENT '优惠券订单表';

-- 评论/探店笔记表
CREATE TABLE IF NOT EXISTS tb_blog (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    shop_id     BIGINT UNSIGNED NOT NULL COMMENT '商户 ID',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '发布用户',
    title       VARCHAR(255)    NOT NULL COMMENT '标题',
    images      VARCHAR(2048)   DEFAULT '' COMMENT '图片，多个逗号分隔',
    content     VARCHAR(2048)   NOT NULL COMMENT '评论内容',
    liked       INT UNSIGNED    DEFAULT 0 COMMENT '点赞数',
    comments    INT UNSIGNED    DEFAULT 0 COMMENT '回复数',
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_shop_id (shop_id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT '评论/探店笔记表';

-- 关注关系表
CREATE TABLE IF NOT EXISTS tb_follow (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '粉丝用户 ID',
    follow_user_id BIGINT UNSIGNED NOT NULL COMMENT '被关注用户 ID',
    create_time    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_follow (user_id, follow_user_id)
) ENGINE = InnoDB COMMENT '关注关系表';
