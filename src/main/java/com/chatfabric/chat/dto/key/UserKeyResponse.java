package com.chatfabric.chat.dto.key;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserKeyResponse {

    private final Long id;
    private final Long userId;
    private final String username;
    private final String publicEncryptionKey;
    private final String publicSigningKey;
    private final Integer keyVersion;
    private final boolean active;
    private final LocalDateTime createdAt;
}
