package com.chatfabric.chat.service;

import com.chatfabric.chat.dto.message.MessageResponse;
import com.chatfabric.chat.dto.message.SendMessageRequest;
import com.chatfabric.chat.entity.Chat;
import com.chatfabric.chat.entity.Message;
import com.chatfabric.chat.entity.MessageFormat;
import com.chatfabric.chat.entity.MessageStatus;
import com.chatfabric.chat.entity.User;
import com.chatfabric.chat.exception.BadRequestException;
import com.chatfabric.chat.repository.MessageRepository;
import com.chatfabric.chat.util.EntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatService chatService;
    private final UserService userService;
    private final AuditService auditService;

    public MessageService(MessageRepository messageRepository,
                          ChatService chatService,
                          UserService userService,
                          AuditService auditService) {
        this.messageRepository = messageRepository;
        this.chatService = chatService;
        this.userService = userService;
        this.auditService = auditService;
    }

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request, Long authenticatedUserId) {
        Chat chat = chatService.getChatEntityById(request.getChatId());
        User sender = userService.getEntityById(authenticatedUserId);

        chatService.validateUserInChat(chat.getId(), sender.getId());

        MessagePayload payload = resolvePayload(request);
        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(payload.content)
                .ciphertext(payload.ciphertext)
                .nonce(payload.nonce)
                .algorithm(payload.algorithm)
                .encryptedMessageKey(payload.encryptedMessageKey)
                .signature(payload.signature)
                .keyVersion(payload.keyVersion)
                .messageFormat(payload.messageFormat)
                .status(MessageStatus.SENT)
                .build();

        Message savedMessage = messageRepository.save(message);
        log.info("Stored message id={} chatId={} senderId={}",
                savedMessage.getId(),
                savedMessage.getChat().getId(),
                savedMessage.getSender().getId());
        auditService.logMessageSent(authenticatedUserId, savedMessage.getChat().getId(), savedMessage.getId());
        return EntityMapper.toMessageResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesByChatId(Long chatId, Long authenticatedUserId) {
        chatService.getChatEntityById(chatId);
        chatService.validateUserInChat(chatId, authenticatedUserId);
        List<Message> messages = messageRepository.findByChatIdOrderByTimestampAsc(chatId);
        List<MessageResponse> responses = new ArrayList<MessageResponse>();
        for (Message message : messages) {
            responses.add(EntityMapper.toMessageResponse(message));
        }
        return responses;
    }

    private MessagePayload resolvePayload(SendMessageRequest request) {
        String content = request.getContent() == null ? null : request.getContent().trim();
        String ciphertext = request.getCiphertext() == null ? null : request.getCiphertext().trim();
        MessageFormat requestedFormat = request.getMessageFormat();

        boolean hasCiphertext = ciphertext != null && !ciphertext.isEmpty();
        boolean hasContent = content != null && !content.isEmpty();

        if (requestedFormat == null) {
            requestedFormat = MessageFormat.E2EE_V1;
        }

        if (requestedFormat == MessageFormat.PLAINTEXT_V1 || hasContent) {
            throw new BadRequestException("Plaintext message creation is disabled. Encrypt in the client and send an E2EE_V1 payload.");
        }

        if (requestedFormat != MessageFormat.E2EE_V1) {
            throw new BadRequestException("Unsupported message format: " + requestedFormat);
        }

        if (!hasCiphertext) {
            throw new BadRequestException("Encrypted messages require ciphertext");
        }
        if (isBlank(request.getNonce())) {
            throw new BadRequestException("Encrypted messages require nonce");
        }
        if (isBlank(request.getAlgorithm())) {
            throw new BadRequestException("Encrypted messages require algorithm");
        }
        if (isBlank(request.getEncryptedMessageKey())) {
            throw new BadRequestException("Encrypted messages require encryptedMessageKey");
        }
        if (isBlank(request.getSignature())) {
            throw new BadRequestException("Encrypted messages require signature");
        }
        if (request.getKeyVersion() == null || request.getKeyVersion() < 1) {
            throw new BadRequestException("Encrypted messages require a valid keyVersion");
        }

        return new MessagePayload(
                MessageFormat.E2EE_V1,
                null,
                ciphertext,
                request.getNonce().trim(),
                request.getAlgorithm().trim(),
                request.getEncryptedMessageKey().trim(),
                request.getSignature().trim(),
                request.getKeyVersion()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class MessagePayload {

        private final MessageFormat messageFormat;
        private final String content;
        private final String ciphertext;
        private final String nonce;
        private final String algorithm;
        private final String encryptedMessageKey;
        private final String signature;
        private final Integer keyVersion;

        private MessagePayload(MessageFormat messageFormat,
                               String content,
                               String ciphertext,
                               String nonce,
                               String algorithm,
                               String encryptedMessageKey,
                               String signature) {
            this(messageFormat, content, ciphertext, nonce, algorithm, encryptedMessageKey, signature, null);
        }

        private MessagePayload(MessageFormat messageFormat,
                               String content,
                               String ciphertext,
                               String nonce,
                               String algorithm,
                               String encryptedMessageKey,
                               String signature,
                               Integer keyVersion) {
            this.messageFormat = messageFormat;
            this.content = content;
            this.ciphertext = ciphertext;
            this.nonce = nonce;
            this.algorithm = algorithm;
            this.encryptedMessageKey = encryptedMessageKey;
            this.signature = signature;
            this.keyVersion = keyVersion;
        }
    }
}
