package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMQConfig {
    @Bean
    public String transferTopic(@Value("${app.mq.topic}") String topic) {
        return topic;
    }
}
