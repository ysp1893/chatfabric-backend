package com.chatfabric.chat.controller;

import com.chatfabric.chat.dto.user.UserRegistrationRequest;
import com.chatfabric.chat.dto.user.UserResponse;
import com.chatfabric.chat.security.SecurityUserPrincipal;
import com.chatfabric.chat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRegistrationRequest request) {
        return userService.register(request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id,
                                @AuthenticationPrincipal SecurityUserPrincipal principal) {
        return userService.getByIdForAuthenticatedUser(id, principal.getId());
    }

    @GetMapping
    public List<UserResponse> getAllUsers(@AuthenticationPrincipal SecurityUserPrincipal principal) {
        return userService.getAllUsers();
    }
}
