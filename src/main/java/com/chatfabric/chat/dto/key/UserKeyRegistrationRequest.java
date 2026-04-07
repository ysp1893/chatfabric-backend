package com.chatfabric.chat.dto.key;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UserKeyRegistrationRequest {

    @NotBlank(message = "Public encryption key is required")
    @Size(max = 16384, message = "Public encryption key is too large")
    private String publicEncryptionKey;

    @NotBlank(message = "Public signing key is required")
    @Size(max = 16384, message = "Public signing key is too large")
    private String publicSigningKey;

    @NotNull(message = "Key version is required")
    @Min(value = 1, message = "Key version must be at least 1")
    private Integer keyVersion;
}
