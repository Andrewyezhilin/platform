# 智慧生活服务平台（Smart Life Platform）

智能化生活服务平台，聚焦**用户评论分析**与**商户信息服务**，包含商户查询、优惠券秒杀、消息推送等模块，并通过 **MindBridge 智能客服**（Spring AI + MCP）实现个性化服务。

## 技术栈

SpringBoot 3.5 · Spring AI 1.0 · Caffeine · Spring MVC · Redis · MySQL · Nginx · JWT · MyBatis · MCP · Lua · Redisson

## 系统架构

```
                        ┌─────────────┐
   客户端 ──────────────▶│    Nginx    │ 负载均衡 + 限流
                        └──────┬──────┘
                     ┌─────────┴─────────┐
              ┌──────▼──────┐     ┌──────▼──────┐
              │  应用实例 1  │     │  应用实例 2  │  ← Pub/Sub 广播同步 L1 失效
              │ Caffeine L1 │     │ Caffeine L1 │
              └──────┬──────┘     └──────┬──────┘
                     └─────────┬─────────┘
              ┌────────────────┼────────────────┐
       ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
       │  Redis (L2) │  │    MySQL    │  │  大模型 API  │
       │ 缓存/秒杀/MQ │  │   MyBatis   │  │  (MCP 工具)  │
       └─────────────┘  └─────────────┘  └─────────────┘
```

## 核心设计

### 1. 商户查询：多级缓存，毫秒级响应

- **L1 Caffeine 本地缓存**（30s 短周期，W-TinyLFU 淘汰）+ **L2 Redis**，实测缓存命中响应约 3ms；
- **布隆过滤器**（Redisson，100 万容量 / 1% 误判率）前置拦截不存在的商户 ID，防**缓存穿透**，空值短 TTL 缓存兜底；
- **动态 TTL**：写入 Redis 时在基础 TTL 上叠加最多 20% 随机抖动，防**缓存雪崩**；
- 热点 Key 重建使用 **Redis 互斥锁 + Double Check**，防**缓存击穿**；
- **Cache Aside 一致性**：先更新数据库、再删除缓存，并通过 Redis Pub/Sub 广播使集群所有节点的 L1 同步失效。

核心实现：`utils/CacheClient.java`

### 2. 优惠券秒杀：防超卖 + 一人一单 + 异步下单

```
请求 ─▶ Lua 脚本（原子）：库存校验 + 预扣 + 一人一单判定 + XADD 入队 ─▶ 立即返回订单号
                                     │
        后台消费者组 ◀── Redis Stream ┘
             │
             ▶ Redisson 分布式锁（一人一单兜底）─▶ 乐观锁扣 DB 库存 ─▶ 订单落库
             ▶ 失败消息进入 Pending List 补偿重试，保证订单不丢
```

三层防超卖防线：Lua 原子判定（主）→ DB 乐观锁 `stock > 0`（兜底）→ 订单表 `(user_id, voucher_id)` 唯一索引（最终一致）。

**压测结果**：30 用户 × 2 并发共 60 请求抢 10 张券 → 恰好 10 单成功、Redis 与 DB 库存同步归零、零超卖、重复请求全部拦截。

核心实现：`resources/lua/seckill.lua`、`service/VoucherOrderService.java`、`mq/VoucherOrderConsumer.java`

### 3. 消息推送：Feed 流（推模式）

- 发布评论时写扩散到所有粉丝的收件箱（Redis ZSet，score 为时间戳）；
- 滚动分页（`lastId` + `offset`）解决传统分页在动态插入下的重复读问题；
- 点赞使用 ZSet 记录用户与时间，天然支持点赞排行。

### 4. MindBridge 智能客服（Spring AI + MCP）

- **MCP Server**（SSE 传输，`/sse` 端点）：把商户搜索、券查询、订单查询、评论获取等平台能力以标准化 MCP Tool 暴露，任意 MCP 客户端（Claude / Cursor 等）可直接接入；
- **多轮对话与上下文记忆**：`ChatMemory` 滑动窗口（20 条）+ 自研 Redis 仓储（24h 过期），服务无状态、可水平扩展、重启不丢会话（高可用）；
- **个性化服务**：登录用户 ID 经 `ToolContext` 传递到工具层，支持"查我的订单"等个性化指令；
- **评论智能分析**：`/ai/review-analysis/{shopId}` 汇总用户评论，输出情感倾向、优缺点与改进建议；
- 模型侧使用 OpenAI 兼容协议，DeepSeek / 通义千问 / OpenAI 可通过环境变量随意切换。

核心实现：`ai/AiConfig.java`、`ai/RedisChatMemoryRepository.java`、`ai/mcp/SmartLifeTools.java`

### 5. 登录认证：JWT + Redis 双重会话

JWT 无状态校验（jjwt，HS256）+ Redis 服务端会话（滑动续期 30min），兼顾横向扩展与主动踢下线能力。

## 快速开始

```bash
# 1. 启动中间件
cd deploy && docker compose up -d mysql redis
# （或使用本机 MySQL/Redis，执行 src/main/resources/db/schema.sql 与 data.sql 初始化）

# 2. 配置大模型（可选，不配置则智能客服不可用，其余模块不受影响）
export AI_BASE_URL=https://api.deepseek.com
export AI_API_KEY=sk-xxx
export AI_MODEL=deepseek-chat

# 3. 启动应用
mvn spring-boot:run

# 4.（可选）完整集群：双应用实例 + Nginx 负载均衡
cd deploy && docker compose --profile full up -d
```

## API 速览

| 模块 | 接口 | 说明 |
|---|---|---|
| 用户 | `POST /user/code`、`POST /user/login` | 验证码登录，返回 JWT |
| 商户 | `GET /shop/{id}` | 多级缓存查询 |
| 商户 | `PUT /shop` | Cache Aside 更新 |
| 秒杀 | `POST /voucher/seckill` | 上架秒杀券（库存预热） |
| 秒杀 | `POST /voucher-order/seckill/{id}` | 秒杀下单（异步落库） |
| 评论 | `POST /blog`、`PUT /blog/like/{id}` | 发布（Feed 推送）/ 点赞 |
| Feed | `GET /blog/of-follow?lastId=&offset=` | 关注流滚动分页 |
| 客服 | `POST /ai/chat` | 多轮对话（携带 conversationId） |
| 客服 | `GET /ai/review-analysis/{shopId}` | 评论智能分析 |
| MCP | `GET /sse` | MCP Server SSE 端点 |

需要登录的接口携带请求头：`Authorization: Bearer <token>`。

## 目录结构

```
src/main/java/com/smartlife/
├── ai/            # MindBridge 智能客服（ChatClient、Redis 会话记忆、MCP 工具）
├── common/        # 统一响应、常量、用户上下文、全局异常
├── config/        # Caffeine、布隆过滤器、L1 失效订阅、MVC 拦截器注册
├── controller/    # REST 接口层
├── interceptor/   # JWT 解析 + 登录校验拦截器
├── mapper/        # MyBatis Mapper 接口（XML 位于 resources/mapper）
├── mq/            # Redis Stream 秒杀订单消费者
├── service/       # 业务层
└── utils/         # 多级缓存客户端、JWT、全局 ID 生成器
src/main/resources/
├── lua/seckill.lua    # 秒杀原子脚本
├── db/                # 建表与演示数据 SQL
└── mapper/            # MyBatis XML
deploy/
├── nginx/nginx.conf   # 负载均衡 + 限流 + SSE 长连接
└── docker-compose.yml # 一键启动
```
