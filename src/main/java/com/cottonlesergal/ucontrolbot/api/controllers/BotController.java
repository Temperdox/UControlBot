package com.cottonlesergal.ucontrolbot.api.controllers;

import com.cottonlesergal.ucontrolbot.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for bot-related API endpoints.
 */
@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final JDA jda;
    private final Config config;

    /**
     * Initializes the bot controller with the JDA instance.
     *
     * @param jda The JDA instance
     */
    @Autowired
    public BotController(JDA jda, Config config) {
        this.jda = jda;
        this.config = config;
    }

    /**
     * Gets information about the bot.
     *
     * @return Bot information
     */
    @GetMapping("/info")
    public ResponseEntity<?> getBotInfo() {
        User selfUser = jda.getSelfUser();

        Map<String, Object> botInfo = new HashMap<>();
        botInfo.put("id", selfUser.getId());
        botInfo.put("username", selfUser.getName());
        botInfo.put("discriminator", selfUser.getDiscriminator());
        botInfo.put("avatarUrl", selfUser.getEffectiveAvatarUrl());

        return ResponseEntity.ok(botInfo);
    }

    @GetMapping("/info/owner")
    public ResponseEntity<?> getBotOwner() {
        try {
            String ownerId = config.getOwnerId(); // Now we can access config
            User owner = null;

            try {
                // Try to retrieve the owner user by ID
                owner = jda.retrieveUserById(ownerId).complete();
            } catch (Exception e) {
                /*logger.error("Could not retrieve owner user", e)*/;
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to retrieve owner user", "message", e.getMessage()));
            }

            if (owner == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Owner not found", "message", "Owner user could not be found"));
            }

            Map<String, Object> ownerInfo = new HashMap<>();
            ownerInfo.put("id", owner.getId());
            ownerInfo.put("username", owner.getName());
            ownerInfo.put("displayName", owner.getGlobalName() != null ? owner.getGlobalName() : owner.getName());
            ownerInfo.put("discriminator", owner.getDiscriminator());
            ownerInfo.put("avatarUrl", owner.getEffectiveAvatarUrl());
            ownerInfo.put("isOwner", true); // Explicitly mark as owner

            return ResponseEntity.ok(ownerInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get owner info", "message", e.getMessage()));
        }
    }
}