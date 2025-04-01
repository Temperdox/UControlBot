package com.cottonlesergal.ucontrolbot.api;

import com.cottonlesergal.ucontrolbot.config.Config;
import com.cottonlesergal.ucontrolbot.db.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Web server for the Discord bot API and web interface.
 * Handles WebSocket connections for real-time updates.
 * With Spring Boot, REST API endpoints are handled by separate controller classes.
 */
@Component
public class WebServer extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

    private final Gson gson = new Gson();

    @Autowired
    private Config config;

    @Autowired
    private DatabaseManager dbManager;

    // WebSocket sessions
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // Session subscriptions for DMs and channels
    private final ConcurrentMap<String, String> dmSubscriptions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> channelSubscriptions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> guildSubscriptions = new ConcurrentHashMap<>();

    // Scheduled executor for background tasks
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Initializes the web server.
     */
    public WebServer() {
        logger.info("Web server initialized");
    }

    /**
     * Called when the server starts.
     */
    @PostConstruct
    public void start() {
        logger.info("Web server started");

        // Schedule periodic tasks
        scheduler.scheduleAtFixedRate(this::cleanupTypingIndicators, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Called when the server stops.
     * Closes all WebSocket sessions.
     */
    @PreDestroy
    public void stop() {
        // Close all WebSocket sessions
        sessions.values().forEach(this::closeSession);
        sessions.clear();
        dmSubscriptions.clear();
        channelSubscriptions.clear();
        guildSubscriptions.clear();

        // Shutdown scheduler
        scheduler.shutdown();

        logger.info("Web server stopped");
    }

    /**
     * Safely closes a WebSocket session.
     *
     * @param session The session to close
     */
    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            logger.error("Error closing WebSocket session", e);
        }
    }

    /**
     * Broadcasts an event to all WebSocket clients.
     *
     * @param eventType Event type
     * @param data Event data
     */
    public void broadcastEvent(String eventType, Object data) {
        String message = String.format("{\"type\":\"%s\",\"data\":%s}", eventType, gson.toJson(data));

        // Save event to database first
        dbManager.processEvent(eventType, data);

        // Then broadcast to WebSocket clients
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                logger.error("Error sending WebSocket message", e);
            }
        });
    }

    /**
     * Broadcasts an event to clients subscribed to a specific channel.
     *
     * @param eventType Event type
     * @param channelId Channel ID
     * @param data Event data
     */
    public void broadcastChannelEvent(String eventType, String channelId, Object data) {
        String message = String.format("{\"type\":\"%s\",\"data\":%s}", eventType, gson.toJson(data));

        // Save event to database first
        dbManager.processEvent(eventType, data);

        // Send to all sessions subscribed to this channel
        channelSubscriptions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(channelId))
                .map(Map.Entry::getKey)
                .map(sessions::get)
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        logger.error("Error sending channel WebSocket message", e);
                    }
                });
    }

    /**
     * Broadcasts an event to clients subscribed to a specific DM conversation.
     *
     * @param eventType Event type
     * @param userId User ID
     * @param data Event data
     */
    public void broadcastDmEvent(String eventType, String userId, Object data) {
        String message = String.format("{\"type\":\"%s\",\"data\":%s}", eventType, gson.toJson(data));

        // Save event to database first
        dbManager.processEvent(eventType, data);

        // Send to all sessions subscribed to this user's DMs
        dmSubscriptions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(userId))
                .map(Map.Entry::getKey)
                .map(sessions::get)
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        logger.error("Error sending DM WebSocket message", e);
                    }
                });
    }

    /**
     * Broadcasts an event to clients subscribed to a specific guild.
     *
     * @param eventType Event type
     * @param guildId Guild ID
     * @param data Event data
     */
    public void broadcastGuildEvent(String eventType, String guildId, Object data) {
        String message = String.format("{\"type\":\"%s\",\"data\":%s}", eventType, gson.toJson(data));

        // Save event to database first
        dbManager.processEvent(eventType, data);

        // Send to all sessions subscribed to this guild
        guildSubscriptions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(guildId))
                .map(Map.Entry::getKey)
                .map(sessions::get)
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        logger.error("Error sending guild WebSocket message", e);
                    }
                });
    }

    /**
     * Handles WebSocket connection established events.
     *
     * @param session The WebSocket session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        logger.info("WebSocket connected: {}", sessionId);

        // Send a welcome message to confirm connection
        try {
            JsonObject welcomeData = new JsonObject();
            welcomeData.addProperty("message", "Connection established");
            welcomeData.addProperty("sessionId", sessionId);

            String welcomeMessage = String.format("{\"type\":\"WELCOME\",\"data\":%s}", gson.toJson(welcomeData));
            session.sendMessage(new TextMessage(welcomeMessage));
        } catch (IOException e) {
            logger.error("Error sending welcome message", e);
        }
    }

    /**
     * Handles WebSocket messages.
     *
     * @param session The WebSocket session
     * @param message The message received
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        try {
            JsonObject json = JsonParser.parseString(message.getPayload()).getAsJsonObject();
            String type = json.get("type").getAsString();

            switch (type) {
                case "IDENTIFY":
                    // Handle bot/user identification
                    if (json.has("data") && json.getAsJsonObject("data").has("botId")) {
                        String botId = json.getAsJsonObject("data").get("botId").getAsString();
                        logger.info("Client {} identified as bot {}", sessionId, botId);

                        // Set bot user ID in database manager
                        dbManager.setBotUserId(botId);

                        // Acknowledge identity
                        sendAcknowledgement(session, "IDENTIFY", botId);
                    }
                    break;

                case "SUBSCRIBE_DM":
                    // Handle DM subscription
                    if (json.has("data") && json.getAsJsonObject("data").has("userId")) {
                        String userId = json.getAsJsonObject("data").get("userId").getAsString();
                        logger.info("Client {} subscribed to DMs for user {}", sessionId, userId);

                        // Store subscription
                        dmSubscriptions.put(sessionId, userId);

                        // Acknowledge subscription
                        sendAcknowledgement(session, "SUBSCRIBE_DM", userId);
                    }
                    break;

                case "SUBSCRIBE_GUILD":
                    // Handle guild subscription
                    if (json.has("data") && json.getAsJsonObject("data").has("guildId")) {
                        String guildId = json.getAsJsonObject("data").get("guildId").getAsString();
                        logger.info("Client {} subscribed to guild {}", sessionId, guildId);

                        // Store subscription
                        guildSubscriptions.put(sessionId, guildId);

                        // Acknowledge subscription
                        sendAcknowledgement(session, "SUBSCRIBE_GUILD", guildId);
                    } else if (json.has("guildId")) {
                        // Support for legacy format
                        String guildId = json.get("guildId").getAsString();
                        logger.info("Client {} subscribed to guild {}", sessionId, guildId);

                        // Store subscription
                        guildSubscriptions.put(sessionId, guildId);

                        // Acknowledge subscription
                        sendAcknowledgement(session, "SUBSCRIBE_GUILD", guildId);
                    }
                    break;

                case "SUBSCRIBE_CHANNEL":
                    // Handle channel subscription
                    if (json.has("data") && json.getAsJsonObject("data").has("channelId")) {
                        String channelId = json.getAsJsonObject("data").get("channelId").getAsString();
                        logger.info("Client {} subscribed to channel {}", sessionId, channelId);

                        // Store subscription
                        channelSubscriptions.put(sessionId, channelId);

                        // Acknowledge subscription
                        sendAcknowledgement(session, "SUBSCRIBE_CHANNEL", channelId);
                    } else if (json.has("channelId")) {
                        // Support for legacy format
                        String channelId = json.get("channelId").getAsString();
                        logger.info("Client {} subscribed to channel {}", sessionId, channelId);

                        // Store subscription
                        channelSubscriptions.put(sessionId, channelId);

                        // Acknowledge subscription
                        sendAcknowledgement(session, "SUBSCRIBE_CHANNEL", channelId);
                    }
                    break;

                case "UNSUBSCRIBE_DM":
                    if (json.has("data") && json.getAsJsonObject("data").has("userId")) {
                        String userId = json.getAsJsonObject("data").get("userId").getAsString();
                        logger.info("Client {} unsubscribed from DMs for user {}", sessionId, userId);
                        dmSubscriptions.remove(sessionId);
                    }
                    break;

                case "UNSUBSCRIBE_CHANNEL":
                    if (json.has("data") && json.getAsJsonObject("data").has("channelId")) {
                        String channelId = json.getAsJsonObject("data").get("channelId").getAsString();
                        logger.info("Client {} unsubscribed from channel {}", sessionId, channelId);
                        channelSubscriptions.remove(sessionId);
                    }
                    break;

                case "UNSUBSCRIBE_GUILD":
                    if (json.has("data") && json.getAsJsonObject("data").has("guildId")) {
                        String guildId = json.getAsJsonObject("data").get("guildId").getAsString();
                        logger.info("Client {} unsubscribed from guild {}", sessionId, guildId);
                        guildSubscriptions.remove(sessionId);
                    }
                    break;

                case "TYPING":
                    // Handle typing indicators
                    if (json.has("data")) {
                        JsonObject data = json.getAsJsonObject("data");
                        if (data.has("channelId") && !data.get("channelId").isJsonNull()) {
                            String channelId = data.get("channelId").getAsString();
                            logger.info("Client {} is typing in channel {}", sessionId, channelId);

                            // Save to database
                            if (data.has("userId") && !data.get("userId").isJsonNull()) {
                                String userId = data.get("userId").getAsString();
                                dbManager.saveTypingIndicator(userId, channelId);
                            }

                            // Relay typing indicator to other clients subscribed to this channel
                            broadcastChannelEvent("TYPING_START", channelId, data);
                        } else if (data.has("userId") && !data.get("userId").isJsonNull()) {
                            String userId = data.get("userId").getAsString();
                            logger.info("Client {} is typing in DM with user {}", sessionId, userId);

                            // Save to database - need to get DM channel ID
                            String dmChannelId = dbManager.getDmChannelIdByUserId(userId);
                            if (dmChannelId != null) {
                                dbManager.saveTypingIndicator(userId, dmChannelId);
                            }

                            // Relay typing indicator to other clients subscribed to this DM
                            broadcastDmEvent("TYPING_START", userId, data);
                        }
                    }
                    break;

                case "PING":
                    // Handle ping for keepalive
                    long timestamp = System.currentTimeMillis();
                    if (json.has("timestamp")) {
                        timestamp = json.get("timestamp").getAsLong();
                    }

                    // Send pong response
                    JsonObject pongData = new JsonObject();
                    pongData.addProperty("timestamp", timestamp);
                    pongData.addProperty("serverTime", System.currentTimeMillis());

                    String pongMessage = String.format("{\"type\":\"PONG\",\"data\":%s}", gson.toJson(pongData));
                    session.sendMessage(new TextMessage(pongMessage));
                    break;

                default:
                    logger.warn("Unknown WebSocket message type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Error processing WebSocket message", e);
        }
    }

    /**
     * Send acknowledgement for a subscription or action
     */
    private void sendAcknowledgement(WebSocketSession session, String action, String id) {
        try {
            JsonObject ackData = new JsonObject();
            ackData.addProperty("action", action);
            ackData.addProperty("id", id);
            ackData.addProperty("status", "success");
            ackData.addProperty("timestamp", System.currentTimeMillis());

            String ackMessage = String.format("{\"type\":\"ACK\",\"data\":%s}", gson.toJson(ackData));
            session.sendMessage(new TextMessage(ackMessage));
        } catch (IOException e) {
            logger.error("Error sending acknowledgement message", e);
        }
    }

    /**
     * Handles WebSocket connection closed events.
     *
     * @param session The WebSocket session
     * @param status The close status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        cleanupSession(sessionId);
        logger.info("WebSocket disconnected: {} with status {}", sessionId, status);
    }

    /**
     * Handles WebSocket transport errors.
     *
     * @param session The WebSocket session
     * @param exception The exception that occurred
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();
        cleanupSession(sessionId);
        logger.error("WebSocket error for session {}: {}", sessionId, exception.getMessage(), exception);
    }

    /**
     * Cleans up all session-related data when a session ends.
     *
     * @param sessionId The WebSocket session ID to clean up
     */
    private void cleanupSession(String sessionId) {
        sessions.remove(sessionId);
        dmSubscriptions.remove(sessionId);
        channelSubscriptions.remove(sessionId);
        guildSubscriptions.remove(sessionId);
    }


    /**
     * Cleanup typing indicators that are older than 10 seconds
     */
    private void cleanupTypingIndicators() {
        try {
            dbManager.cleanupTypingIndicators();
        } catch (Exception e) {
            logger.error("Error cleaning up typing indicators", e);
        }
    }
}