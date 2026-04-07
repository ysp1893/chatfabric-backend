package com.chatfabric.chat.config;

import com.chatfabric.chat.service.InMemoryOnlineUserTracker;
import com.chatfabric.chat.service.OnlineUserTracker;
import com.chatfabric.chat.service.RedisOnlineUserTracker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(name = "app.presence.store", havingValue = "redis")
    public OnlineUserTracker redisOnlineUserTracker(RedisTemplate<String, String> redisTemplate) {
        return new RedisOnlineUserTracker(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(OnlineUserTracker.class)
    public OnlineUserTracker inMemoryOnlineUserTracker() {
        return new InMemoryOnlineUserTracker();
    }
}
