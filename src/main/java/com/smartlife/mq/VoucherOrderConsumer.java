package com.smartlife.mq;

import com.smartlife.common.RedisConstants;
import com.smartlife.entity.VoucherOrder;
import com.smartlife.service.VoucherOrderService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 秒杀订单异步消费者（Redis Stream 消息队列）。
 *
 * <p>秒杀接口只做 Redis 内的资格判定即返回，真正的订单落库由本消费者
 * 通过消费者组（Consumer Group）异步完成，削峰填谷、保护数据库。
 * 处理失败的消息进入 Pending List，由补偿逻辑重试，保证订单不丢失。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherOrderConsumer implements ApplicationRunner {

    private static final String CONSUMER_NAME = "consumer-1";

    private final StringRedisTemplate stringRedisTemplate;
    private final VoucherOrderService voucherOrderService;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "voucher-order-consumer"));
    private volatile boolean running = true;

    @Override
    public void run(ApplicationArguments args) {
        initStreamGroup();
        executor.submit(this::consumeLoop);
    }

    /**
     * 初始化 Stream 与消费者组（幂等）。
     */
    private void initStreamGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(
                    RedisConstants.STREAM_ORDERS_KEY,
                    ReadOffset.from("0"),
                    RedisConstants.STREAM_ORDERS_GROUP);
            log.info("创建订单消息队列消费者组成功");
        } catch (Exception e) {
            // BUSYGROUP：组已存在，属正常情况
            log.info("订单消息队列消费者组已存在或 Redis 未就绪: {}", e.getMessage());
        }
    }

    private void consumeLoop() {
        while (running) {
            try {
                // 1. 读取新消息：XREADGROUP GROUP order-group consumer-1 COUNT 1 BLOCK 2000 STREAMS ... >
                List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                        Consumer.from(RedisConstants.STREAM_ORDERS_GROUP, CONSUMER_NAME),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                        StreamOffset.create(RedisConstants.STREAM_ORDERS_KEY, ReadOffset.lastConsumed()));
                if (records == null || records.isEmpty()) {
                    continue;
                }
                MapRecord<String, Object, Object> record = records.get(0);
                handleRecord(record);
                ack(record);
            } catch (Exception e) {
                log.error("处理订单消息异常，转入 Pending List 补偿", e);
                handlePendingList();
            }
        }
    }

    /**
     * Pending List 补偿：处理已投递但未 ACK 的消息，保证订单最终落库。
     */
    private void handlePendingList() {
        while (running) {
            try {
                List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                        Consumer.from(RedisConstants.STREAM_ORDERS_GROUP, CONSUMER_NAME),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(RedisConstants.STREAM_ORDERS_KEY, ReadOffset.from("0")));
                if (records == null || records.isEmpty()) {
                    // Pending List 已清空
                    break;
                }
                MapRecord<String, Object, Object> record = records.get(0);
                handleRecord(record);
                ack(record);
            } catch (Exception e) {
                log.error("处理 Pending List 消息异常，稍后重试", e);
                sleepQuietly();
            }
        }
    }

    private void handleRecord(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();
        VoucherOrder order = new VoucherOrder()
                .setId(Long.valueOf(value.get("orderId").toString()))
                .setUserId(Long.valueOf(value.get("userId").toString()))
                .setVoucherId(Long.valueOf(value.get("voucherId").toString()))
                .setPayType(1)
                .setStatus(1);
        voucherOrderService.createVoucherOrder(order);
    }

    private void ack(MapRecord<String, Object, Object> record) {
        stringRedisTemplate.opsForStream().acknowledge(
                RedisConstants.STREAM_ORDERS_KEY,
                RedisConstants.STREAM_ORDERS_GROUP,
                record.getId());
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        executor.shutdownNow();
    }
}
