package com.chatfabric.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditService {

    public void logAuthenticationSuccess(Long userId, String username) {
        log.info("AUDIT authentication_success userId={} username={}", userId, username);
    }

    public void logAuthenticationFailure(String username) {
        log.warn("AUDIT authentication_failure username={}", username);
    }

    public void logChatAccess(Long userId, Long chatId, String action) {
        log.info("AUDIT chat_action userId={} chatId={} action={}", userId, chatId, action);
    }

    public void logMessageSent(Long userId, Long chatId, Long messageId) {
        log.info("AUDIT message_sent userId={} chatId={} messageId={}", userId, chatId, messageId);
    }
}
