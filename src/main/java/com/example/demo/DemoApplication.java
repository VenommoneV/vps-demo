package com.example.demo;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class DemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);
    // 定义 Topic 名称
    private static final String TOPIC_NAME = "sys-log-topic";

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    // 1. 自动创建 Topic (生产环境通常由运维创建，这里为了演示自动创建)
    @Bean
    public NewTopic logTopic() {
        return TopicBuilder.name(TOPIC_NAME)
                .partitions(1)
                .replicas(1)
                .build();
    }

    // 2. 生产者接口：模拟产生日志
    @GetMapping("/log")
    public String sendLog(@RequestParam String msg) {
        String logContent = "Log[" + System.currentTimeMillis() + "]: " + msg;
        
        // 异步发送消息
        kafkaTemplate.send(TOPIC_NAME, logContent);
        
        return "<h2 style='color:green'>日志已发送至 Kafka</h2><p>" + logContent + "</p>";
    }

    // 3. 消费者监听：模拟日志落库或分析
    @KafkaListener(topics = TOPIC_NAME, groupId = "log-group")
    public void listen(String message) {
        // 模拟耗时处理
        logger.info("【Kafka消费】收到日志: {}", message);
    }

    @GetMapping("/")
    public String home() {
        return "<h1>Kafka Demo Ready (KRaft Mode)</h1>" + 
               "<p>Try: <a href='/log?msg=TestLog'>/log?msg=TestLog</a></p>";
    }
}