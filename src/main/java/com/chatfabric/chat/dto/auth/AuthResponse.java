package com.chatfabric.chat.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final String tokenType;
    private final String accessToken;
    private final long expiresInSeconds;
    private final Long userId;
    private final String username;
}
