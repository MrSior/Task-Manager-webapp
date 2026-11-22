package com.taskmanager.web;

import com.taskmanager.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds {@code X-App-Version} so rollout strategies (canary / A/B / blue-green)
 * are visible in curl and screencasts.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class VersionHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-App-Version";

    private final AppProperties appProperties;

    public VersionHeaderFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        response.setHeader(HEADER, appProperties.getVersion());
        filterChain.doFilter(request, response);
    }
}
