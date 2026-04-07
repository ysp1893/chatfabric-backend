package com.chatfabric.chat.service;

import org.springframework.data.redis.core.RedisTemplate;

public class RedisOnlineUserTracker implements OnlineUserTracker {

    private static final String ONLINE_USERS_KEY = "chatfabric:online-users";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisOnlineUserTracker(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void markOnline(Long userId) {
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, String.valueOf(userId));
    }

    @Override
    public void markOffline(Long userId) {
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, String.valueOf(userId));
    }

    @Override
    public boolean isOnline(Long userId) {
        Boolean member = redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, String.valueOf(userId));
        return Boolean.TRUE.equals(member);
    }
}
