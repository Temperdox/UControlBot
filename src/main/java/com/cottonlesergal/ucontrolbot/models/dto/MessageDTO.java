package com.cottonlesergal.ucontrolbot.models.dto;

import net.dv8tion.jda.api.entities.Message;

public class MessageDTO extends BaseDTO {
    public static final String ID = "id";
    public static final String CONTENT = "content";
    public static final String AUTHOR = "author";
    public static final String CHANNEL_ID = "channelId";
    public static final String GUILD_ID = "guildId";
    public static final String TIMESTAMP = "timestamp";

    public MessageDTO() {
        // Default constructor
    }

    public MessageDTO(Message jdaMessage) {
        if (jdaMessage != null) {
            set(ID, jdaMessage.getId());
            set(CONTENT, jdaMessage.getContentRaw());
            set(CHANNEL_ID, jdaMessage.getChannel().getId());
            set(TIMESTAMP, jdaMessage.getTimeCreated().toEpochSecond());

            if (jdaMessage.isFromGuild()) {
                set(GUILD_ID, jdaMessage.getGuild().getId());
            }

            // Include author as nested DTO
            UserDTO author = new UserDTO(jdaMessage.getAuthor());
            set(AUTHOR, author.toJson());
        }
    }

    // Add message-specific methods
    public String getId() {
        return (String) get(ID);
    }

    public String getContent() {
        return (String) get(CONTENT);
    }

    public UserDTO getAuthor() {
        String authorJson = (String) get(AUTHOR);
        if (authorJson != null) {
            UserDTO author = new UserDTO();
            author.fromJson(authorJson);
            return author;
        }
        return null;
    }
}