package com.smartlife;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 智能化生活服务平台启动类。
 *
 * <p>核心模块：商户查询（多级缓存）、优惠券秒杀（Redis+Lua+消息队列）、
 * 消息推送（Feed 流）、MindBridge 智能客服（Spring AI + MCP）。</p>
 */
@EnableAsync
@MapperScan("com.smartlife.mapper")
@SpringBootApplication
public class SmartLifeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLifeApplication.class, args);
    }
}
