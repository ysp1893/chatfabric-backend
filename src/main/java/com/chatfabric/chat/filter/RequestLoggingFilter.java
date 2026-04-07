package com.chatfabric.chat.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        filterChain.doFilter(request, response);
        long durationMs = System.currentTimeMillis() - start;

        String principalName = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
        log.info("HTTP request method={} path={} status={} durationMs={} principal={} remote={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMs,
                principalName,
                getClientAddress(request));
    }

    private String getClientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            int separator = forwarded.indexOf(',');
            return separator > -1 ? forwarded.substring(0, separator).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
