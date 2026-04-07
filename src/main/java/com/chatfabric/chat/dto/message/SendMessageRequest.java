package com.chatfabric.chat.dto.message;

import lombok.Getter;
import lombok.Setter;

import com.chatfabric.chat.entity.MessageFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class SendMessageRequest {

    @NotNull(message = "Chat id is required")
    private Long chatId;

    @Size(max = 2000, message = "Message content must not exceed 2000 characters")
    private String content;

    @Size(max = 65535, message = "Ciphertext is too large")
    private String ciphertext;

    @Size(max = 1024, message = "Nonce is too large")
    private String nonce;

    @Size(max = 100, message = "Algorithm is too large")
    private String algorithm;

    @Size(max = 65535, message = "Encrypted message key is too large")
    private String encryptedMessageKey;

    @Size(max = 65535, message = "Signature is too large")
    private String signature;

    private Integer keyVersion;

    private MessageFormat messageFormat;
}
