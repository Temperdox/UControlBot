package com.cottonlesergal.ucontrolbot.api.controllers;

import com.cottonlesergal.ucontrolbot.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@RestController
public class FaviconController {

    private final Bot bot;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public FaviconController(Bot bot) {
        this.bot = bot;
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<byte[]> getFavicon() {
        try {
            // Get the bot's avatar URL from JDA
            String avatarUrl = bot.getJda().getSelfUser().getEffectiveAvatarUrl();

            // For Discord avatars, we can request a specific size and format
            // Make sure it's the right size for a favicon (typically 16x16, 32x32, or 64x64)
            if (!avatarUrl.contains("?")) {
                avatarUrl += "?size=32";
            }

            // Use RestTemplate to download the image
            byte[] imageBytes = restTemplate.getForObject(avatarUrl, byte[].class);

            if (imageBytes != null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG) // Adjust based on actual format
                        .body(imageBytes);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}