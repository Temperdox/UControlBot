package com.cottonlesergal.ucontrolbot.db;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DatabaseInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JDA jda;

    @Autowired
    private DatabaseManager dbManager;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Forcing database initialization...");

        try {
            // Verify database tables
            List<String> tables = jdbcTemplate.queryForList(
                    "SHOW TABLES", String.class);
            logger.info("Database tables: {}", tables);

            // Force inject the bot user
            User botUser = jda.getSelfUser();
            Map<String, Object> botData = new HashMap<>();
            botData.put("id", botUser.getId());
            botData.put("username", botUser.getName());
            botData.put("discriminator", botUser.getDiscriminator());
            botData.put("avatarUrl", botUser.getEffectiveAvatarUrl());
            botData.put("isBot", true);
            botData.put("status", "online");

            jdbcTemplate.update(
                    "INSERT INTO users (id, username, discriminator, avatar_url, is_bot, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE username = VALUES(username), discriminator = VALUES(discriminator), " +
                            "avatar_url = VALUES(avatar_url), is_bot = VALUES(is_bot), status = VALUES(status)",
                    botUser.getId(), botUser.getName(), botUser.getDiscriminator(),
                    botUser.getEffectiveAvatarUrl(), true, "online"
            );

            // Force inject owner
            String ownerId = "568631703053139974"; // From your logs
            try {
                User owner = jda.retrieveUserById(ownerId).complete();
                jdbcTemplate.update(
                        "INSERT INTO users (id, username, discriminator, avatar_url, is_bot, is_owner, status) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE username = VALUES(username), discriminator = VALUES(discriminator), " +
                                "avatar_url = VALUES(avatar_url), is_bot = VALUES(is_bot), is_owner = VALUES(is_owner), " +
                                "status = VALUES(status)",
                        owner.getId(), owner.getName(), owner.getDiscriminator(),
                        owner.getEffectiveAvatarUrl(), false, true, "online"
                );
            } catch (Exception e) {
                logger.warn("Could not inject owner user", e);
            }

            // Force inject all guilds and channels
            for (Guild guild : jda.getGuilds()) {
                logger.info("Injecting guild: {} ({})", guild.getName(), guild.getId());

                // Insert guild
                jdbcTemplate.update(
                        "INSERT INTO guilds (id, name, icon_url, owner_id, member_count) " +
                                "VALUES (?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE name = VALUES(name), icon_url = VALUES(icon_url), " +
                                "owner_id = VALUES(owner_id), member_count = VALUES(member_count)",
                        guild.getId(), guild.getName(), guild.getIconUrl(),
                        guild.getOwnerId(), guild.getMemberCount()
                );

                // Step 1: Insert all categories (parents first to avoid FK violation)
                guild.getCategories().forEach(category -> {
                    logger.info("Injecting category: {} ({})", category.getName(), category.getId());

                    jdbcTemplate.update(
                            "INSERT INTO channels (id, guild_id, parent_id, name, type, topic, position) " +
                                    "VALUES (?, ?, NULL, ?, ?, NULL, ?) " +
                                    "ON DUPLICATE KEY UPDATE guild_id = VALUES(guild_id), parent_id = VALUES(parent_id), " +
                                    "name = VALUES(name), type = VALUES(type), topic = VALUES(topic), position = VALUES(position)",
                            category.getId(), guild.getId(), category.getName(),
                            category.getType().name(), category.getPosition()
                    );
                });

                // Step 2: Insert all other channels with parent_id if applicable
                for (GuildChannel channel : guild.getChannels()) {
                    if (channel.getType() == ChannelType.CATEGORY) continue; // already handled

                    logger.info("Injecting channel: {} ({})", channel.getName(), channel.getId());

                    String parentId = null;
                    String topic = null;
                    int position = 0;

                    switch (channel.getType()) {
                        case TEXT -> {
                            TextChannel text = (TextChannel) channel;
                            parentId = text.getParentCategoryId();
                            topic = text.getTopic();
                            position = text.getPosition();
                        }
                        case VOICE -> {
                            var voice = (VoiceChannel) channel;
                            parentId = voice.getParentCategoryId();
                            position = voice.getPosition();
                        }
                        case STAGE -> {
                            var stage = (StageChannel) channel;
                            parentId = stage.getParentCategoryId();
                            position = stage.getPosition();
                        }
                        case NEWS -> {
                            var news = (NewsChannel) channel;
                            parentId = news.getParentCategoryId();
                            position = news.getPosition();
                        }
                        case FORUM -> {
                            var forum = (ForumChannel) channel;
                            parentId = forum.getParentCategoryId();
                            position = forum.getPosition();
                        }
                        default -> logger.warn("Unhandled channel type: {}", channel.getType());
                    }

                    jdbcTemplate.update(
                            "INSERT INTO channels (id, guild_id, parent_id, name, type, topic, position) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE guild_id = VALUES(guild_id), parent_id = VALUES(parent_id), " +
                                    "name = VALUES(name), type = VALUES(type), topic = VALUES(topic), position = VALUES(position)",
                            channel.getId(), guild.getId(), parentId, channel.getName(),
                            channel.getType().name(), topic, position
                    );
                }
            }

            logger.info("Database initialization complete");
            dbManager.checkDatabaseStatus();
        } catch (Exception e) {
            logger.error("Database initialization failed", e);
        }
    }
}