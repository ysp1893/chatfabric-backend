package com.chatfabric.chat.filter;

import com.chatfabric.chat.config.properties.SecurityProperties;
import com.chatfabric.chat.dto.common.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<String, WindowCounter>();

    public RateLimitingFilter(SecurityProperties securityProperties,
                              ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!securityProperties.getRateLimit().isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        for (String excludedPath : securityProperties.getRateLimit().getExcludedPaths()) {
            if (path.startsWith(excludedPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = resolveKey(request);
        long currentWindow = System.currentTimeMillis() / 60000L;

        WindowCounter counter = counters.compute(key, new java.util.function.BiFunction<String, WindowCounter, WindowCounter>() {
            @Override
            public WindowCounter apply(String ignored, WindowCounter existing) {
                if (existing == null || existing.window != currentWindow) {
                    return new WindowCounter(currentWindow);
                }
                return existing;
            }
        });

        int currentCount = counter.count.incrementAndGet();
        if (currentCount > securityProperties.getRateLimit().getRequestsPerMinute()) {
            log.warn("Rate limit exceeded key={} path={}", key, request.getRequestURI());
            writeTooManyRequests(response, request);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            return "user:" + request.getUserPrincipal().getName();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            int separator = forwarded.indexOf(',');
            return "ip:" + (separator > -1 ? forwarded.substring(0, separator).trim() : forwarded.trim());
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response,
                                      HttpServletRequest request) throws IOException {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Rate limit exceeded. Please retry later.")
                .path(request.getRequestURI())
                .details(java.util.Collections.singletonList("Allowed requests per minute: " + securityProperties.getRateLimit().getRequestsPerMinute()))
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static final class WindowCounter {

        private final long window;
        private final AtomicInteger count = new AtomicInteger(0);

        private WindowCounter(long window) {
            this.window = window;
        }
    }
}
