package com.cottonlesergal.ucontrolbot.models.dto;

import net.dv8tion.jda.api.entities.Guild;

public class GuildDTO extends BaseDTO {
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String ICON_URL = "iconUrl";
    public static final String OWNER_ID = "ownerId";
    public static final String MEMBER_COUNT = "memberCount";

    public GuildDTO() {
        // Default constructor
    }

    public GuildDTO(Guild jdaGuild) {
        if (jdaGuild != null) {
            set(ID, jdaGuild.getId());
            set(NAME, jdaGuild.getName());
            set(ICON_URL, jdaGuild.getIconUrl());
            set(OWNER_ID, jdaGuild.getOwnerId());
            set(MEMBER_COUNT, jdaGuild.getMemberCount());
        }
    }

    // Add guild-specific methods
    public String getId() {
        return (String) get(ID);
    }

    public String getName() {
        return (String) get(NAME);
    }
}