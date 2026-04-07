package com.chatfabric.chat.dto.auth;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username may contain only letters, numbers, underscore, dot, and hyphen")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
