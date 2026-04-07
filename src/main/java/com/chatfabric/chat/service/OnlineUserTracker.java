package com.chatfabric.chat.service;

public interface OnlineUserTracker {

    void markOnline(Long userId);

    void markOffline(Long userId);

    boolean isOnline(Long userId);
}
