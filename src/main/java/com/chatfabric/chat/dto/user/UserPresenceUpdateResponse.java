package com.chatfabric.chat.dto.user;

import com.chatfabric.chat.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserPresenceUpdateResponse {

    private final Long userId;
    private final String username;
    private final UserStatus status;
}
