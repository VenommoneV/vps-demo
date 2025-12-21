package com.example.demo;  // 声明包名，定义项目的基础路径

import org.apache.kafka.clients.admin.NewTopic;  // 导入 Kafka 用于管理 Topic 的类
import org.slf4j.Logger;                        // 导入日志接口
import org.slf4j.LoggerFactory;                 // 导入日志工厂，用于创建日志对象
import org.springframework.beans.factory.annotation.Autowired;  // 导入自动注入注解
import org.springframework.boot.SpringApplication;              // 导入 Spring Boot 启动类
import org.springframework.boot.autoconfigure.SpringBootApplication;  // 导入 Spring Boot 自动配置注解
import org.springframework.context.annotation.Bean;             // 导入 Bean 声明注解
import org.springframework.kafka.annotation.KafkaListener;      // 导入 Kafka 监听器注解
import org.springframework.data.redis.core.StringRedisTemplate;  // 导入 Redis 操作模板（专门操作字符串）
import org.springframework.kafka.config.TopicBuilder;           // 导入 Kafka Topic 构建器工具
import org.springframework.kafka.core.KafkaTemplate;            // 导入 Kafka 发送消息的模板类
import org.springframework.web.bind.annotation.GetMapping;      // 导入 HTTP GET 请求映射注解
import org.springframework.web.bind.annotation.RequestParam;   // 导入请求参数绑定注解
import org.springframework.web.bind.annotation.RestController;  // 导入 REST 控制器注解

import java.time.Duration;  // 导入时间长度类，用于设置 Redis 过期时间
import java.util.UUID;      // 导入 UUID 工具类，用于生成唯一消息 ID

@SpringBootApplication      // 标记这是一个 Spring Boot 应用，开启自动配置
@RestController             // 标记这是一个控制器，且所有方法的返回值都将作为 HTTP 响应体直接写入
public class DemoApplication {  // 定义应用的主类

    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);  // 初始化当前类的日志记录器对象
    // 定义 Topic 名称
    private static final String TOPIC_NAME = "sys-log-topic";  // 定义 Kafka 消息主题的固定名称

    public static void main(String[] args) {  // Java 应用的入口方法
        SpringApplication.run(DemoApplication.class, args);  // 启动 Spring 应用上下文
    }  // main 方法结束

    @Autowired  // Spring 自动从容器中注入 KafkaTemplate 实例
    private KafkaTemplate<String, String> kafkaTemplate;  // 定义 Kafka 消息发送模板变量

    // 注入 Redis 操作工具
    @Autowired  // Spring 自动从容器中注入 StringRedisTemplate 实例
    private StringRedisTemplate redisTemplate;  // 定义 Redis 操作模板变量

    // 1. 自动创建 Topic (生产环境通常由运维创建，这里为了演示自动创建)
    @Bean  // 声明此方法返回一个 Spring 管理的 Bean
    public NewTopic logTopic() {  // 创建 Kafka Topic 的配置方法
        return TopicBuilder.name(TOPIC_NAME)  // 指定 Topic 名称
                .partitions(1)                // 设置分区数量为 1
                .replicas(1)                  // 设置副本数量为 1
                .build();                     // 构建并返回 NewTopic 对象
    }  // 方法结束

    // 2. 生产者接口：模拟产生日志
    @GetMapping("/log")  // 映射 GET 方式的 /log 请求
    public String sendLog(@RequestParam String msg) {  // 接收名为 msg 的请求参数
        String msgId = UUID.randomUUID().toString();  // 生成一个唯一的随机字符串作为消息 ID

        // 模拟消息格式： "UserLogin|uuid-1234-5678"
        String payload = msg + "|" + msgId;  // 将原始内容和 ID 使用管道符拼接成传输报文

        
        kafkaTemplate.send(TOPIC_NAME, payload);  // 将拼接好的报文发送到指定的 Kafka 主题中
        return "<h3>Log Sent!</h3> Msg: " + msg + "<br/>ID: " + msgId;  // 返回发送成功的 HTML 信息到浏览器
    }  // 方法结束

    // // 3. 消费者监听：模拟日志落库或分析
    // @KafkaListener(topics = TOPIC_NAME, groupId = "log-group")
    // public void listen(String message) {
    //     // 模拟耗时处理
    //     logger.info("【Kafka消费】收到日志: {}", message);
    // }

     // 消费者：利用 Redis 锁进行去重
     @KafkaListener(topics = TOPIC_NAME, groupId = "log-group")  // 开启 Kafka 监听，订阅主题并指定消费者组
     public void listen(String message) {  // 接收 Kafka 消息的方法
         // 1. 解析消息，拿到唯一 ID
         // 假设消息结构是 "Content|ID"
         String[] parts = message.split("\\|");  // 使用管道符对收到的字符串进行分割
         if (parts.length < 2) return;   // 如果分割后的长度小于 2，说明消息格式不正确，直接跳过
         
         String content = parts[0];  // 获取消息的主体内容
         String msgId = parts[1];    // 获取消息的唯一标识 ID
 
         // 2. 幂等性检查 (核心代码)
         // SETNX key value EX 600 (如果 key 不存在则设置，并 10 分钟过期)
         String lockKey = "processed_msg:" + msgId;  // 构造在 Redis 中存储的唯一键名
         Boolean isFirstTime = redisTemplate.opsForValue()  // 获取 Redis 的字符串操作对象
                 .setIfAbsent(lockKey, "1", Duration.ofMinutes(10));  // 尝试设置键值，若不存在则成功并设置 10 分钟过期
 
         if (Boolean.TRUE.equals(isFirstTime)) {  // 判断是否为第一次设置（即该消息之前未处理过）
             // 返回 true，说明 Redis 里没这个 ID，是第一次处理
             logger.info(" [成功消费] Content: {}, ID: {}", content, msgId);  // 打印消费成功的日志
             
             // TODO: 这里写具体的业务逻辑，比如入库 MySQL
             
         } else {  // 如果返回 false，说明键已存在
             // 返回 false，说明 Redis 里已经有这个 ID 了，是重复消息
             logger.warn(" [重复拦截] ID: {} 已经被处理过，跳过！", msgId);  // 打印拦截重复消息的警告日志
         }  // 判断结束
     }  // listen 方法结束

     
    @GetMapping("/")  // 映射根路径的 GET 请求
    public String home() {  // 首页展示方法
        return "<h1>Kafka Demo Ready (KRaft Mode)</h1>" +   // 返回包含 HTML 标签的欢迎词
               "<p>Try: <a href='/log?msg=TestLog'>/log?msg=TestLog</a></p>";  // 返回包含测试链接的段落
    }  // home 方法结束
}  // 类结束
