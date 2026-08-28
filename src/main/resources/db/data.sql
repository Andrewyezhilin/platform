-- 演示数据
USE smartlife;

INSERT INTO tb_shop_type (id, name, icon, sort) VALUES
(1, '美食', '/types/ms.png', 1),
(2, 'KTV', '/types/ktv.png', 2),
(3, '丽人·美发', '/types/lrmf.png', 3),
(4, '健身运动', '/types/jsyd.png', 4),
(5, '按摩·足疗', '/types/amzl.png', 5)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO tb_shop (id, name, type_id, images, area, address, x, y, avg_price, sold, comments, score, open_hours) VALUES
(1, '海底捞火锅（望京店）', 1, '/shops/1.jpg', '望京', '望京西路 88 号', 116.47, 39.99, 12800, 4215, 3035, 47, '10:00-22:00'),
(2, '很久以前羊肉串', 1, '/shops/2.jpg', '国贸', '建国路 66 号', 116.46, 39.91, 9800, 3521, 2160, 46, '11:00-23:00'),
(3, '纯 K KTV（三里屯店）', 2, '/shops/3.jpg', '三里屯', '工体北路 4 号', 116.45, 39.93, 15900, 1204, 806, 44, '12:00-02:00'),
(4, '超级猩猩健身', 4, '/shops/4.jpg', '中关村', '海淀大街 27 号', 116.31, 39.98, 6900, 2210, 1523, 48, '07:00-22:00')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO tb_voucher (id, shop_id, title, sub_title, rules, pay_value, actual_value, type, status) VALUES
(1, 1, '100 元代金券', '周一至周五均可使用', '全场通用\n无需预约', 8000, 10000, 0, 1),
(2, 1, '50 元秒杀券', '周末专享', '仅堂食\n每人限购一张', 3900, 5000, 1, 1)
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO tb_seckill_voucher (voucher_id, stock, begin_time, end_time) VALUES
(2, 100, '2026-01-01 00:00:00', '2026-12-31 23:59:59')
ON DUPLICATE KEY UPDATE stock = VALUES(stock);
