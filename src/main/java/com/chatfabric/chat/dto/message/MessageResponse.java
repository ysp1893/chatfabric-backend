package com.chatfabric.chat.dto.message;

import com.chatfabric.chat.entity.MessageFormat;
import com.chatfabric.chat.entity.MessageStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponse {

    private final Long id;
    private final Long chatId;
    private final Long senderId;
    private final String senderUsername;
    private final String content;
    private final String ciphertext;
    private final String nonce;
    private final String algorithm;
    private final String encryptedMessageKey;
    private final String signature;
    private final Integer keyVersion;
    private final MessageFormat messageFormat;
    private final LocalDateTime timestamp;
    private final MessageStatus status;
}
