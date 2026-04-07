package com.chatfabric.chat.controller;

import com.chatfabric.chat.dto.auth.AuthResponse;
import com.chatfabric.chat.dto.auth.LoginRequest;
import com.chatfabric.chat.security.JwtTokenProvider;
import com.chatfabric.chat.security.SecurityUserPrincipal;
import com.chatfabric.chat.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername().trim(),
                            request.getPassword()
                    )
            );

            SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
            String token = jwtTokenProvider.generateToken(authentication);
            auditService.logAuthenticationSuccess(principal.getId(), principal.getUsername());

            return AuthResponse.builder()
                    .tokenType("Bearer")
                    .accessToken(token)
                    .expiresInSeconds(jwtTokenProvider.getExpirationSeconds())
                    .userId(principal.getId())
                    .username(principal.getUsername())
                    .build();
        } catch (AuthenticationException exception) {
            auditService.logAuthenticationFailure(request.getUsername().trim());
            throw new BadCredentialsException("Invalid username or password", exception);
        }
    }
}
