package com.chatfabric.chat.dto.user;

import com.chatfabric.chat.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private final Long id;
    private final String username;
    private final UserStatus status;
    private final LocalDateTime createdAt;
}
