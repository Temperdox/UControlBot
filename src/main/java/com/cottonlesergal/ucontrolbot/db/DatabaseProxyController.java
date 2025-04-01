package com.cottonlesergal.ucontrolbot.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db")
public class DatabaseProxyController {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseProxyController.class);

    @Autowired
    private DatabaseManager dbManager;

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        logger.info("Direct database request for users");
        try {
            List<Map<String, Object>> users = dbManager.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching users from database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/guilds")
    public ResponseEntity<?> getGuilds() {
        logger.info("Direct database request for guilds");
        try {
            List<Map<String, Object>> guilds = dbManager.getAllGuilds();
            return ResponseEntity.ok(guilds);
        } catch (Exception e) {
            logger.error("Error fetching guilds from database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/channels/{guildId}")
    public ResponseEntity<?> getChannels(@PathVariable String guildId) {
        logger.info("Direct database request for channels in guild {}", guildId);
        try {
            List<Map<String, Object>> channels = dbManager.getChannelsByGuildId(guildId);
            return ResponseEntity.ok(channels);
        } catch (Exception e) {
            logger.error("Error fetching channels from database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/messages/{channelId}")
    public ResponseEntity<?> getMessages(@PathVariable String channelId) {
        logger.info("Direct database request for messages in channel {}", channelId);
        try {
            List<Map<String, Object>> messages = dbManager.getMessagesByChannelId(channelId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("Error fetching messages from database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
