package com.chatfabric.chat.websocket;

import com.chatfabric.chat.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class WebSocketEventListener {

    private static final String USER_ID_HEADER = "userId";

    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<String, Long>();
    private final Map<Long, AtomicInteger> userSessionCounts = new ConcurrentHashMap<Long, AtomicInteger>();
    private final UserService userService;

    public WebSocketEventListener(UserService userService) {
        this.userService = userService;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        List<String> userIdHeaders = accessor.getNativeHeader(USER_ID_HEADER);

        if (userIdHeaders == null || userIdHeaders.isEmpty()) {
            log.debug("WebSocket connect received without userId header");
            return;
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdHeaders.get(0));
        } catch (NumberFormatException exception) {
            log.warn("Ignoring WebSocket connect with invalid userId header={}", userIdHeaders.get(0));
            return;
        }
        String sessionId = accessor.getSessionId();
        sessionUserMap.put(sessionId, userId);
        AtomicInteger count = userSessionCounts.computeIfAbsent(userId, new java.util.function.Function<Long, AtomicInteger>() {
            @Override
            public AtomicInteger apply(Long key) {
                return new AtomicInteger(0);
            }
        });
        if (count.incrementAndGet() == 1) {
            userService.markOnline(userId);
        }
        log.info("WebSocket session connected sessionId={} userId={}", sessionId, userId);
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long userId = sessionUserMap.remove(sessionId);
        if (userId == null) {
            return;
        }
        AtomicInteger count = userSessionCounts.get(userId);
        if (count == null) {
            return;
        }
        if (count.decrementAndGet() <= 0) {
            userSessionCounts.remove(userId);
            userService.markOffline(userId);
        }
        log.info("WebSocket session disconnected sessionId={} userId={}", sessionId, userId);
    }
}
