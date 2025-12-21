# 项目演进记录

本文档记录 `vps-demo` 项目的技术演进历程，包括每次重大变更的**原因**、**经过**和**结果**。

---

## 版本历史概览

| 版本 | 日期 | 主要变更 | 状态 |
|------|------|----------|------|
| v0.1 | 初始 | Spring Boot 基础项目 | ✅ 完成 |
| v0.2 | - | 集成 Kafka 消息队列 | ✅ 完成 |
| v0.3 | 2024-12 | 集成 Redis 缓存练习 | ✅ 完成 |
| v0.4 | 2024-12 | Redis + Kafka 消息去重 (幂等性控制) | ✅ 完成 |

---

## v0.1 - Spring Boot 基础项目

### 原因 (Why)
- 需要一个简单的 Spring Boot 演示项目
- 用于学习和实践 Java Web 开发
- 作为后续技术集成的基础骨架

### 经过 (What)
1. 使用 Spring Boot 2.7.5 版本创建项目
2. 添加 `spring-boot-starter-web` 依赖
3. 创建基础的 `DemoApplication` 启动类
4. 配置 Dockerfile 支持容器化部署

### 结果 (Result)
- ✅ 项目可正常启动
- ✅ 提供基础的 HTTP 服务能力
- ✅ 支持 Docker 容器化部署

---

## v0.2 - 集成 Kafka 消息队列

### 原因 (Why)
- 学习消息队列的核心概念：生产者、消费者、Topic
- 掌握 Spring Kafka 的使用方法
- 理解异步消息处理的应用场景（日志收集、事件驱动等）

### 经过 (What)
1. **添加依赖**：在 `pom.xml` 中引入 `spring-kafka`
2. **配置 Kafka**：在 `application.yml` 中配置 Kafka 连接信息
   - 配置 bootstrap-servers 连接地址
   - 配置消费者组、序列化器
3. **实现生产者**：通过 `KafkaTemplate` 发送消息
   - 提供 `/log` 接口模拟日志发送
4. **实现消费者**：使用 `@KafkaListener` 注解监听消息
   - 监听 `sys-log-topic` 主题
5. **自动创建 Topic**：使用 `TopicBuilder` 自动创建主题

### 结果 (Result)
- ✅ 可通过 HTTP 接口发送消息到 Kafka
- ✅ 消费者可自动接收并处理消息
- ✅ 理解了 Kafka 的生产-消费模型
- 📝 学习要点：
  - `KafkaTemplate` 用于发送消息
  - `@KafkaListener` 用于监听消息
  - 消息的序列化与反序列化

---

## v0.3 - 集成 Redis 缓存练习

### 原因 (Why)
- **学习目的**：掌握 Redis 作为缓存和数据存储的基本操作
- **技术储备**：Redis 是互联网项目中最常用的 NoSQL 数据库
- **应用场景**：
  - 缓存热点数据，提升系统性能
  - 实现分布式锁
  - 排行榜、计数器等实时数据
  - Session 共享

### 经过 (What)

#### 1. 添加依赖
在 `pom.xml` 中添加 Spring Data Redis 依赖：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

#### 2. 配置 Redis 连接
在 `application.yml` 中添加 Redis 配置：
```yaml
spring:
  redis:
    host: localhost          # Redis 服务器地址
    port: 6379               # Redis 端口
    password:                # 密码（可选）
    database: 0              # 数据库索引
    timeout: 3000ms          # 连接超时
    lettuce:                 # 连接池配置
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
```

#### 3. 基础集成结果
- ✅ 成功引入 Redis 依赖并完成连接配置。
- ✅ 验证了 `StringRedisTemplate` 的自动注入。
- ✅ 为后续分布式锁和幂等性逻辑打下了基础。

---

## v0.4 - Redis + Kafka 消息去重 (幂等性控制)

### 原因 (Why)
- **解决重复消费**：分布式场景下，Kafka 可能会因为网络抖动等原因导致消息被多次推送。
- **业务安全性**：确保核心业务逻辑（如入库、支付等）不会因为重复消息而产生脏数据。
- **实战演练**：结合 Redis 的 `SETNX` (setIfAbsent) 特性实现一个生产级别的去重方案。

### 经过 (What)

#### 1. 消息协议升级
- 修改生产者接口 `/log`，为每条消息生成唯一的 `UUID` 作为 `msgId`。
- 将消息体重新定义为 `内容|ID` 的格式进行传输。

#### 2. 消费者幂等逻辑实现
- 在 `DemoApplication` 的 `listen` 方法中实现去重核心逻辑。
- **解析**：通过管道符 `|` 分解出消息内容和唯一 ID。
- **校验**：使用 `redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(10))`。
- **过期策略**：设置 10 分钟过期时间，在保证去重时效性的同时，避免 Redis 内存溢出。

#### 3. 代码注解完善
- 为 `DemoApplication.java` 全文补充了逐行注解，清晰展示每一行代码在 Spring Boot、Kafka 和 Redis 中的作用。

### 结果 (Result)
- ✅ 实现了可靠的消息去重机制。
- ✅ 即使消息被重复投递，控制台也只会输出一次“成功消费”日志，后续重复消息会被“拦截”。
- ✅ 深入理解了分布式环境下的幂等性设计原则。

---

## 下一步计划

- [ ] 集成 MySQL 数据库（JPA/MyBatis）
- [ ] 添加 Swagger API 文档
- [ ] 集成 Spring Security 安全框架
- [ ] 添加单元测试
- [ ] 集成 Docker Compose 编排服务

---

## 技术栈总览

```
┌─────────────────────────────────────────────────────────┐
│                    VPS Demo 项目                         │
├─────────────────────────────────────────────────────────┤
│  应用层    │  Spring Boot 2.7.5 + Spring MVC            │
├─────────────────────────────────────────────────────────┤
│  消息队列  │  Apache Kafka (spring-kafka)               │
├─────────────────────────────────────────────────────────┤
│  缓存层    │  Redis (spring-data-redis + Lettuce)       │
├─────────────────────────────────────────────────────────┤
│  部署      │  Docker                                     │
└─────────────────────────────────────────────────────────┘
```

---

*最后更新：2024年12月*



