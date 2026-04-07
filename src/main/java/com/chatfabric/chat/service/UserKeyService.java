package com.chatfabric.chat.service;

import com.chatfabric.chat.dto.key.UserKeyRegistrationRequest;
import com.chatfabric.chat.dto.key.UserKeyResponse;
import com.chatfabric.chat.entity.User;
import com.chatfabric.chat.entity.UserKey;
import com.chatfabric.chat.exception.BadRequestException;
import com.chatfabric.chat.exception.ResourceNotFoundException;
import com.chatfabric.chat.repository.UserKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class UserKeyService {

    private final UserKeyRepository userKeyRepository;
    private final UserService userService;

    public UserKeyService(UserKeyRepository userKeyRepository,
                          UserService userService) {
        this.userKeyRepository = userKeyRepository;
        this.userService = userService;
    }

    @Transactional
    public UserKeyResponse registerActiveKey(Long authenticatedUserId, UserKeyRegistrationRequest request) {
        if (userKeyRepository.existsByUserIdAndKeyVersion(authenticatedUserId, request.getKeyVersion())) {
            throw new BadRequestException("A key with version=" + request.getKeyVersion() + " already exists for this user");
        }

        User user = userService.getEntityById(authenticatedUserId);
        List<UserKey> activeKeys = userKeyRepository.findAllByUserIdAndActiveTrue(authenticatedUserId);
        for (UserKey activeKey : activeKeys) {
            activeKey.setActive(false);
        }

        UserKey savedKey = userKeyRepository.save(UserKey.builder()
                .user(user)
                .publicEncryptionKey(request.getPublicEncryptionKey().trim())
                .publicSigningKey(request.getPublicSigningKey().trim())
                .keyVersion(request.getKeyVersion())
                .active(true)
                .build());

        log.info("Registered active public key userId={} keyVersion={}", authenticatedUserId, savedKey.getKeyVersion());
        return toResponse(savedKey);
    }

    @Transactional(readOnly = true)
    public UserKeyResponse getActiveKey(Long userId) {
        UserKey key = userKeyRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(new java.util.function.Supplier<ResourceNotFoundException>() {
                    @Override
                    public ResourceNotFoundException get() {
                        return new ResourceNotFoundException("Active public key not found for user id=" + userId);
                    }
                });
        return toResponse(key);
    }

    private UserKeyResponse toResponse(UserKey key) {
        return UserKeyResponse.builder()
                .id(key.getId())
                .userId(key.getUser().getId())
                .username(key.getUser().getUsername())
                .publicEncryptionKey(key.getPublicEncryptionKey())
                .publicSigningKey(key.getPublicSigningKey())
                .keyVersion(key.getKeyVersion())
                .active(key.isActive())
                .createdAt(key.getCreatedAt())
                .build();
    }
}
