package com.cottonlesergal.ucontrolbot.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DatabaseApiInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseApiInterceptor.class);

    @Autowired
    private DatabaseManager dbManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only intercept API requests
        if (request.getRequestURI().startsWith("/api/")) {
            logger.info("API request intercepted: {} {}", request.getMethod(), request.getRequestURI());

            // For message endpoints, ensure we process them for database storage
            if (request.getMethod().equals("POST") && request.getRequestURI().contains("/messages")) {
                try {
                    // Read request body for message content
                    String content = request.getReader().lines().collect(Collectors.joining());

                    // Parse JSON
                    Map<String, Object> messageData = new ObjectMapper().readValue(content, Map.class);

                    // Extract channel ID from URI
                    String uri = request.getRequestURI();
                    String channelId = uri.substring(uri.lastIndexOf("/channels/") + 10, uri.lastIndexOf("/messages"));

                    // Create message object
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", "msg-" + System.currentTimeMillis());
                    data.put("channelId", channelId);
                    data.put("content", messageData.get("content"));
                    data.put("timestamp", System.currentTimeMillis());

                    // Add author info
                    if (dbManager.getBotUserId() != null) {
                        Map<String, Object> author = new HashMap<>();
                        author.put("id", dbManager.getBotUserId());
                        author.put("isBot", true);
                        data.put("author", author);
                    }

                    // Process the message event
                    dbManager.processEvent("MESSAGE_RECEIVED", data);
                } catch (Exception e) {
                    logger.error("Error processing intercepted message", e);
                }
            }

            // Log database status before processing the request
            dbManager.checkDatabaseStatus();
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // Run after controller processing but before view rendering
        if (request.getRequestURI().startsWith("/api/")) {
            logger.info("API request completed: {} {} - Status: {}",
                    request.getMethod(), request.getRequestURI(), response.getStatus());
        }
    }
}
