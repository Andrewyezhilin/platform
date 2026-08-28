-- 秒杀资格校验 + 库存预扣 + 消息入队（原子执行，解决超卖与一人一单的并发判定）
-- KEYS[1] 库存 key            smartlife:seckill:stock:{voucherId}
-- KEYS[2] 已下单用户集合 key   smartlife:seckill:order:{voucherId}
-- KEYS[3] 订单消息队列 key     smartlife:stream:orders
-- ARGV[1] voucherId
-- ARGV[2] userId
-- ARGV[3] orderId
-- 返回值：0-成功入队  1-库存不足  2-重复下单

local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
if stock <= 0 then
    return 1
end

if redis.call('SISMEMBER', KEYS[2], ARGV[2]) == 1 then
    return 2
end

-- 扣库存 + 记录用户（同一脚本内原子完成）
redis.call('INCRBY', KEYS[1], -1)
redis.call('SADD', KEYS[2], ARGV[2])

-- 下单消息写入 Redis Stream，由后台消费者异步落库
redis.call('XADD', KEYS[3], '*',
        'voucherId', ARGV[1],
        'userId', ARGV[2],
        'orderId', ARGV[3])
return 0
