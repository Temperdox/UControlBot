package com.cottonlesergal.ucontrolbot.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

@Component
public class ApiUsageFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(ApiUsageFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();

        // Only check API requests
        if (uri.startsWith("/api/") && !uri.startsWith("/api/db/")) {
            logger.warn("⚠️ Direct API usage detected: {} {}", httpRequest.getMethod(), uri);
            logger.warn("⚠️ Consider using the database-backed endpoints at /api/db/ instead");
        }

        chain.doFilter(request, response);
    }
}
