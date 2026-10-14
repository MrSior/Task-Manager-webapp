package com.taskmanager.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Пишет access-лог в JSON (через MDC) для каждого HTTP-запроса.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        MDC.put("http.method", request.getMethod());
        MDC.put("http.path", request.getRequestURI());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.put("http.status_code", String.valueOf(response.getStatus()));
            MDC.put("http.duration_ms", String.valueOf(System.currentTimeMillis() - startedAt));
            if (!isActuator(request)) {
                log.info("HTTP {} {}", request.getMethod(), request.getRequestURI());
            }
            MDC.clear();
        }
    }

    private boolean isActuator(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/actuator");
    }
}
