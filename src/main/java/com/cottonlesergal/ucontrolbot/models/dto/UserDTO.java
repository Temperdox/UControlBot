package com.cottonlesergal.ucontrolbot.models.dto;

import net.dv8tion.jda.api.entities.User;

public class UserDTO extends BaseDTO {
    // Constants for user properties
    public static final String ID = "id";
    public static final String USERNAME = "username";
    public static final String DISPLAY_NAME = "displayName";
    public static final String AVATAR = "avatarUrl";
    public static final String DISCRIMINATOR = "discriminator";
    public static final String IS_BOT = "isBot";
    public static final String STATUS = "status";

    public UserDTO() {
        // Default constructor
    }

    public UserDTO(User jdaUser) {
        if (jdaUser != null) {
            set(ID, jdaUser.getId());
            set(USERNAME, jdaUser.getName());
            set(AVATAR, jdaUser.getEffectiveAvatarUrl());
            set(DISCRIMINATOR, jdaUser.getDiscriminator());
            set(IS_BOT, jdaUser.isBot());
        }
    }

    // Add user-specific methods
    public String getId() {
        return (String) get(ID);
    }

    public String getUsername() {
        return (String) get(USERNAME);
    }

    public String getDisplayName() {
        return (String) get(DISPLAY_NAME);
    }

    public String getAvatarUrl() {
        return (String) get(AVATAR);
    }

    public UserDTO fetchInfo(String userId) {
        // Implement fetch logic
        return this;
    }
}