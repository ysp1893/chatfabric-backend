package com.chatfabric.chat.service;

import com.chatfabric.chat.dto.user.UserRegistrationRequest;
import com.chatfabric.chat.dto.user.UserPresenceUpdateResponse;
import com.chatfabric.chat.dto.user.UserResponse;
import com.chatfabric.chat.entity.User;
import com.chatfabric.chat.entity.UserStatus;
import com.chatfabric.chat.exception.DuplicateResourceException;
import com.chatfabric.chat.exception.ResourceNotFoundException;
import com.chatfabric.chat.repository.UserRepository;
import com.chatfabric.chat.util.EntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OnlineUserTracker onlineUserTracker;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       OnlineUserTracker onlineUserTracker) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.onlineUserTracker = onlineUserTracker;
    }

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.OFFLINE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with id={}", savedUser.getId());
        return EntityMapper.toUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return EntityMapper.toUserResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getByIdForAuthenticatedUser(Long requestedUserId, Long authenticatedUserId) {
        validateSelfAccess(requestedUserId, authenticatedUserId);
        return getById(requestedUserId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAllByOrderByUsernameAsc();
        List<UserResponse> responses = new ArrayList<UserResponse>();
        for (User user : users) {
            responses.add(EntityMapper.toUserResponse(user));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(new java.util.function.Supplier<ResourceNotFoundException>() {
                    @Override
                    public ResourceNotFoundException get() {
                        return new ResourceNotFoundException("User not found with id=" + id);
                    }
                });
    }

    @Transactional
    public void markOnline(Long userId) {
        User user = getEntityById(userId);
        user.setStatus(UserStatus.ONLINE);
        onlineUserTracker.markOnline(userId);
        userRepository.save(user);
        log.info("User marked online userId={}", userId);
    }

    @Transactional
    public void markOffline(Long userId) {
        User user = getEntityById(userId);
        user.setStatus(UserStatus.OFFLINE);
        onlineUserTracker.markOffline(userId);
        userRepository.save(user);
        log.info("User marked offline userId={}", userId);
    }

    @Transactional(readOnly = true)
    public UserPresenceUpdateResponse getPresenceUpdate(Long userId) {
        User user = getEntityById(userId);
        return UserPresenceUpdateResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .build();
    }

    public void validateSelfAccess(Long requestedUserId, Long authenticatedUserId) {
        if (!requestedUserId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only access your own user profile");
        }
    }
}
