package com.chatfabric.chat.websocket;

import com.chatfabric.chat.dto.user.UserPresenceUpdateResponse;
import com.chatfabric.chat.security.SecurityUserPrincipal;
import com.chatfabric.chat.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class WebSocketEventListener {

    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<String, Long>();
    private final Map<Long, AtomicInteger> userSessionCounts = new ConcurrentHashMap<Long, AtomicInteger>();
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(UserService userService,
                                  SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!(accessor.getUser() instanceof SecurityUserPrincipal)) {
            log.debug("WebSocket connect received without authenticated principal");
            return;
        }
        SecurityUserPrincipal principal = (SecurityUserPrincipal) accessor.getUser();
        Long userId = principal.getId();
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
            broadcastPresence(userId);
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
            broadcastPresence(userId);
        }
        log.info("WebSocket session disconnected sessionId={} userId={}", sessionId, userId);
    }

    private void broadcastPresence(Long userId) {
        UserPresenceUpdateResponse update = userService.getPresenceUpdate(userId);
        messagingTemplate.convertAndSend("/topic/presence", update);
        log.debug("Broadcasted presence update userId={} status={}", update.getUserId(), update.getStatus());
    }
}
