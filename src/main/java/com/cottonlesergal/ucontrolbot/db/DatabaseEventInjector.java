package com.cottonlesergal.ucontrolbot.db;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DatabaseEventInjector {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseEventInjector.class);

    @Autowired
    private DatabaseManager dbManager;

    @Autowired
    private JDA jda;

    @Scheduled(fixedRate = 60000) // Every minute
    public void injectGuildsAndChannels() {
        try {
            logger.info("Forcing database injection of guilds and channels");

            // Inject all guilds
            for (Guild guild : jda.getGuilds()) {
                Map<String, Object> guildData = new HashMap<>();
                guildData.put("id", guild.getId());
                guildData.put("name", guild.getName());
                guildData.put("iconUrl", guild.getIconUrl());
                guildData.put("ownerId", guild.getOwnerId());
                guildData.put("memberCount", guild.getMemberCount());

                // Inject guild
                dbManager.processEvent("GUILD_JOIN", guildData);

                // Inject all channels in this guild
                for (Channel channel : guild.getChannels()) {
                    Map<String, Object> channelData = new HashMap<>();
                    channelData.put("channelId", channel.getId());
                    channelData.put("guildId", guild.getId());
                    channelData.put("channelName", channel.getName());
                    channelData.put("channelType", channel.getType().name());

                    // Inject channel
                    dbManager.processEvent("CHANNEL_CREATE", channelData);
                }
            }
        } catch (Exception e) {
            logger.error("Error injecting guild and channel data", e);
        }
    }
}