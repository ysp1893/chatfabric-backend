package com.chatfabric.chat.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOnlineUserTracker implements OnlineUserTracker {

    private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

    @Override
    public void markOnline(Long userId) {
        onlineUsers.add(userId);
    }

    @Override
    public void markOffline(Long userId) {
        onlineUsers.remove(userId);
    }

    @Override
    public boolean isOnline(Long userId) {
        return onlineUsers.contains(userId);
    }
}
