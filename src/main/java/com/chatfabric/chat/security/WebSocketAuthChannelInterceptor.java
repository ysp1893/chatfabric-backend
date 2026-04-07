package com.chatfabric.chat.security;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public WebSocketAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider,
                                           CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor.getNativeHeader(SecurityConstants.AUTHORIZATION_HEADER));
            if (!StringUtils.hasText(token)) {
                throw new BadCredentialsException("Missing Authorization header for WebSocket connection");
            }
            try {
                if (!jwtTokenProvider.validateToken(token)) {
                    throw new BadCredentialsException("Invalid JWT token");
                }
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                SecurityUserPrincipal principal = customUserDetailsService.loadPrincipalByUserId(userId);
                accessor.setUser(principal);
                log.debug("Authenticated WebSocket connect for userId={}", userId);
            } catch (JwtException exception) {
                throw new BadCredentialsException("Invalid JWT token", exception);
            }
        }

        return message;
    }

    private String resolveToken(List<String> authorizationHeaders) {
        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            return null;
        }
        String value = authorizationHeaders.get(0);
        if (!StringUtils.hasText(value) || !value.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return null;
        }
        return value.substring(SecurityConstants.TOKEN_PREFIX.length());
    }
}
