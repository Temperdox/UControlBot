package com.cottonlesergal.ucontrolbot.config;

import com.cottonlesergal.ucontrolbot.api.WebServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for the Spring Boot application.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebServer webServer;

    /**
     * Initializes the WebSocket configuration with the WebServer.
     *
     * @param webServer The WebServer instance to use as the WebSocket handler
     */
    @Autowired
    public WebSocketConfig(WebServer webServer) {
        this.webServer = webServer;
    }

    /**
     * Registers the WebSocket handlers.
     *
     * @param registry The WebSocket handler registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webServer, "/ws")
                .setAllowedOrigins("*"); // You might want to restrict this in production
    }
}