package com.cottonlesergal.ucontrolbot.models.dto;

public interface DiscordDTO {
    /**
     * Converts the DTO to a JSON string
     */
    String toJson();

    /**
     * Updates the DTO with data from a JSON string
     */
    void fromJson(String json);

    /**
     * Get a specific property
     */
    Object get(String property);

    /**
     * Set a specific property
     */
    void set(String property, Object value);
}