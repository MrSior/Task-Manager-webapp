package com.taskmanager.web;

import com.taskmanager.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Demo-only: with probability {@code app.demo-fail-rate} returns HTTP 500 for /api/**
 * so that http_server_requests_* shows 5xx and SLI_http drops in VMUI.
 * Default rate is 0 (disabled).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class DemoFailFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;

    public DemoFailFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        double rate = appProperties.getDemoFailRate();
        if (rate <= 0) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        double rate = Math.min(1.0, appProperties.getDemoFailRate());
        if (ThreadLocalRandom.current().nextDouble() < rate) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "demo fail injection");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
