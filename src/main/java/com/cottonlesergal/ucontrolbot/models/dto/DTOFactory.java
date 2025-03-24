package com.cottonlesergal.ucontrolbot.models.dto;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

public class DTOFactory {
    public static UserDTO createUserDTO(User jdaUser) {
        return new UserDTO(jdaUser);
    }

    public static GuildDTO createGuildDTO(Guild jdaGuild) {
        return new GuildDTO(jdaGuild);
    }

    public static MessageDTO createMessageDTO(Message jdaMessage) {
        return new MessageDTO(jdaMessage);
    }

    public static DiscordDTO fromJson(String json, Class<? extends DiscordDTO> dtoClass) {
        try {
            DiscordDTO dto = dtoClass.newInstance();
            dto.fromJson(json);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DTO from JSON", e);
        }
    }
}