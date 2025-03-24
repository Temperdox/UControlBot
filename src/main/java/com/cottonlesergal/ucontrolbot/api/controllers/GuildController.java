package com.cottonlesergal.ucontrolbot.api.controllers;

import com.cottonlesergal.ucontrolbot.Bot;
import com.cottonlesergal.ucontrolbot.models.DiscordUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for guild-related API endpoints.
 */
@RestController
@RequestMapping("/api/guilds")
public class GuildController {
    private static final Logger logger = LoggerFactory.getLogger(GuildController.class);

    private final Bot bot;

    /**
     * Initializes the guild controller with the specified bot instance.
     *
     * @param bot The bot instance
     */
    @Autowired
    public GuildController(Bot bot) {
        this.bot = bot;
    }

    /**
     * Gets all guilds the bot has access to.
     *
     * @return List of guilds
     */
    @GetMapping
    public ResponseEntity<?> getGuilds() {
        try {
            List<Guild> guilds = bot.getJda().getGuilds();

            List<Map<String, Object>> guildDataList = guilds.stream()
                    .map(guild -> {
                        Map<String, Object> guildData = new HashMap<>();
                        guildData.put("id", guild.getId());
                        guildData.put("name", guild.getName());
                        guildData.put("iconUrl", guild.getIconUrl());
                        guildData.put("memberCount", guild.getMemberCount());
                        guildData.put("ownerId", guild.getOwnerId());
                        guildData.put("features", guild.getFeatures());
                        return guildData;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(guildDataList);
        } catch (Exception e) {
            logger.error("Error getting guilds", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Gets information about a specific guild.
     *
     * @param guildId Guild ID
     * @return Guild information
     */
    @GetMapping("/{guildId}")
    public ResponseEntity<?> getGuild(@PathVariable String guildId) {
        try {
            Guild guild = bot.getJda().getGuildById(guildId);
            if (guild == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Guild not found",
                                "message", "No guild found with ID: " + guildId
                        ));
            }

            Map<String, Object> guildData = new HashMap<>();
            guildData.put("id", guild.getId());
            guildData.put("name", guild.getName());
            guildData.put("iconUrl", guild.getIconUrl());
            guildData.put("memberCount", guild.getMemberCount());
            guildData.put("ownerId", guild.getOwnerId());
            guildData.put("description", guild.getDescription());
            guildData.put("vanityCode", guild.getVanityCode());
            guildData.put("locale", guild.getLocale().getLocale());
            guildData.put("nsfwLevel", guild.getNSFWLevel().name());
            guildData.put("boostTier", guild.getBoostTier().name());
            guildData.put("boostCount", guild.getBoostCount());
            guildData.put("verificationLevel", guild.getVerificationLevel().name());
            guildData.put("defaultNotificationLevel", guild.getDefaultNotificationLevel().name());
            guildData.put("explicitContentLevel", guild.getExplicitContentLevel().name());
            guildData.put("features", guild.getFeatures());

            if (guild.getAfkChannel() != null) {
                guildData.put("afkChannelId", guild.getAfkChannel().getId());
                guildData.put("afkTimeout", guild.getAfkTimeout().getSeconds());
            }

            if (guild.getSystemChannel() != null) {
                guildData.put("systemChannelId", guild.getSystemChannel().getId());
            }

            if (guild.getRulesChannel() != null) {
                guildData.put("rulesChannelId", guild.getRulesChannel().getId());
            }

            if (guild.getCommunityUpdatesChannel() != null) {
                guildData.put("communityUpdatesChannelId", guild.getCommunityUpdatesChannel().getId());
            }

            return ResponseEntity.ok(guildData);
        } catch (Exception e) {
            logger.error("Error getting guild: " + guildId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Gets all channels in a specific guild.
     *
     * @param guildId Guild ID
     * @return List of channels
     */
    @GetMapping("/{guildId}/channels")
    public ResponseEntity<?> getChannels(@PathVariable String guildId) {
        try {
            Guild guild = bot.getJda().getGuildById(guildId);
            if (guild == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Guild not found",
                                "message", "No guild found with ID: " + guildId
                        ));
            }

            List<Map<String, Object>> channels = guild.getChannels().stream()
                    .map(this::mapChannel)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(channels);
        } catch (Exception e) {
            logger.error("Error getting channels for guild: " + guildId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Gets all members of a specific guild.
     *
     * @param guildId Guild ID
     * @param limit Maximum number of members to return
     * @param after Return members after this user ID
     * @return List of members
     */
    @GetMapping("/{guildId}/members")
    public ResponseEntity<?> getMembers(
            @PathVariable String guildId,
            @RequestParam(required = false, defaultValue = "50") int limit,
            @RequestParam(required = false) String after) {
        try {
            Guild guild = bot.getJda().getGuildById(guildId);
            if (guild == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Guild not found",
                                "message", "No guild found with ID: " + guildId
                        ));
            }

            List<Member> members;

            // Check if guild is loaded already
            if (guild.isLoaded()) {
                if (after != null && !after.isEmpty()) {
                    // Filter members after a certain ID
                    long afterId = Long.parseLong(after);
                    members = guild.getMembers().stream()
                            .filter(member -> member.getIdLong() > afterId)
                            .limit(limit)
                            .collect(Collectors.toList());
                } else {
                    // Just get first batch of members
                    members = guild.getMembers().stream()
                            .limit(limit)
                            .collect(Collectors.toList());
                }
            } else {
                // Load members using Task API
                try {
                    members = guild.loadMembers()
                            .get()
                            .stream()
                            .limit(limit)
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    // If loading all members fails, use chunking
                    members = guild.findMembers(member -> true)
                            .get()
                            .stream()
                            .limit(limit)
                            .collect(Collectors.toList());
                }
            }

            List<Map<String, Object>> memberDataList = members.stream()
                    .map(member -> {
                        DiscordUser user = DiscordUser.fromJdaMember(member);
                        return user.getUserData();
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(memberDataList);
        } catch (Exception e) {
            logger.error("Error getting members for guild: " + guildId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Gets all roles in a specific guild.
     *
     * @param guildId Guild ID
     * @return List of roles
     */
    @GetMapping("/{guildId}/roles")
    public ResponseEntity<?> getRoles(@PathVariable String guildId) {
        try {
            Guild guild = bot.getJda().getGuildById(guildId);
            if (guild == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Guild not found",
                                "message", "No guild found with ID: " + guildId
                        ));
            }

            List<Map<String, Object>> roles = guild.getRoles().stream()
                    .map(role -> {
                        Map<String, Object> roleData = new HashMap<>();
                        roleData.put("id", role.getId());
                        roleData.put("name", role.getName());
                        roleData.put("color", role.getColorRaw());
                        roleData.put("position", role.getPosition());
                        roleData.put("permissions", role.getPermissionsRaw());
                        roleData.put("mentionable", role.isMentionable());
                        roleData.put("hoisted", role.isHoisted());
                        roleData.put("managed", role.isManaged());

                        // Role tags if available
                        if (role.getTags() != null) {
                            Map<String, Object> tags = new HashMap<>();

                            if (role.getTags().isBoost()) {
                                tags.put("isBoost", true);
                            }

                            if (role.getTags().getBotIdLong() != 0) {
                                tags.put("botId", role.getTags().getBotIdLong());
                            }

                            if (role.getTags().getIntegrationIdLong() != 0) {
                                tags.put("integrationId", role.getTags().getIntegrationIdLong());
                            }

                            // Only add tags if we have any
                            if (!tags.isEmpty()) {
                                roleData.put("tags", tags);
                            }
                        }

                        return roleData;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            logger.error("Error getting roles for guild: " + guildId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Gets all emojis in a specific guild.
     *
     * @param guildId Guild ID
     * @return List of emojis
     */
    @GetMapping("/{guildId}/emojis")
    public ResponseEntity<?> getEmojis(@PathVariable String guildId) {
        try {
            Guild guild = bot.getJda().getGuildById(guildId);
            if (guild == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Guild not found",
                                "message", "No guild found with ID: " + guildId
                        ));
            }

            List<Map<String, Object>> emojis = guild.getEmojis().stream()
                    .map(emoji -> {
                        Map<String, Object> emojiData = new HashMap<>();
                        emojiData.put("id", emoji.getId());
                        emojiData.put("name", emoji.getName());
                        emojiData.put("animated", emoji.isAnimated());
                        emojiData.put("managed", emoji.isManaged());
                        emojiData.put("available", emoji.isAvailable());
                        emojiData.put("url", emoji.getImageUrl());

                        // Get roles that can use this emoji
                        List<String> roleIds = emoji.getRoles().stream()
                                .map(Role::getId)
                                .collect(Collectors.toList());

                        emojiData.put("roles", roleIds);

                        return emojiData;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(emojis);
        } catch (Exception e) {
            logger.error("Error getting emojis for guild: " + guildId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Maps a JDA Channel to a Map.
     *
     * @param channel The JDA Channel
     * @return Map containing channel data
     */
    private Map<String, Object> mapChannel(Channel channel) {
        Map<String, Object> channelData = new HashMap<>();
        channelData.put("id", channel.getId());
        channelData.put("name", channel.getName());
        channelData.put("type", channel.getType().name());

        // Add flags if any
        if (!channel.getFlags().isEmpty()) {
            channelData.put("flags", channel.getFlags().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList()));
        }

        try {
            // Add type-specific data based on channel type
            ChannelType type = channel.getType();

            // Handle different channel types
            if (type == ChannelType.TEXT) {
                TextChannel textChannel = (TextChannel) channel;
                channelData.put("topic", textChannel.getTopic());
                channelData.put("nsfw", textChannel.isNSFW());
                channelData.put("slowmode", textChannel.getSlowmode());
                if (textChannel.getParentCategory() != null) {
                    channelData.put("parentId", textChannel.getParentCategory().getId());
                }
            }
            else if (type == ChannelType.VOICE) {
                VoiceChannel voiceChannel = (VoiceChannel) channel;
                channelData.put("bitrate", voiceChannel.getBitrate());
                channelData.put("userLimit", voiceChannel.getUserLimit());
                channelData.put("region", voiceChannel.getRegion().getName());
                if (voiceChannel.getParentCategory() != null) {
                    channelData.put("parentId", voiceChannel.getParentCategory().getId());
                }
            }
            else if (type == ChannelType.STAGE) {
                StageChannel stageChannel = (StageChannel) channel;
                channelData.put("bitrate", stageChannel.getBitrate());
                channelData.put("region", stageChannel.getRegion().getName());
                if (stageChannel.getParentCategory() != null) {
                    channelData.put("parentId", stageChannel.getParentCategory().getId());
                }
            }
            else if (type == ChannelType.FORUM) {
                ForumChannel forumChannel = (ForumChannel) channel;
                channelData.put("topic", forumChannel.getTopic());
                if (forumChannel.getParentCategory() != null) {
                    channelData.put("parentId", forumChannel.getParentCategory().getId());
                }
            }
            else if (type == ChannelType.CATEGORY) {
                Category categoryChannel = (Category) channel;
                // Get child channels
                List<String> childIds = categoryChannel.getChannels().stream()
                        .map(Channel::getId)
                        .collect(Collectors.toList());
                channelData.put("childChannels", childIds);
            }
            else if (type == ChannelType.NEWS) {
                NewsChannel newsChannel = (NewsChannel) channel;
                channelData.put("topic", newsChannel.getTopic());
                channelData.put("nsfw", newsChannel.isNSFW());
                if (newsChannel.getParentCategory() != null) {
                    channelData.put("parentId", newsChannel.getParentCategory().getId());
                }
            }
            else if (type == ChannelType.MEDIA) {
                MediaChannel mediaChannel = (MediaChannel) channel;
                channelData.put("topic", mediaChannel.getTopic());
                if (mediaChannel.getParentCategory() != null) {
                    channelData.put("parentId", mediaChannel.getParentCategory().getId());
                }
            }
            else if (type == ChannelType.GUILD_NEWS_THREAD
                    || type == ChannelType.GUILD_PUBLIC_THREAD
                    || type == ChannelType.GUILD_PRIVATE_THREAD) {
                ThreadChannel threadChannel = (ThreadChannel) channel;
                channelData.put("ownerId", threadChannel.getOwnerId());
                channelData.put("messageCount", threadChannel.getMessageCount());
                channelData.put("memberCount", threadChannel.getMemberCount());
                channelData.put("archived", threadChannel.isArchived());
                channelData.put("locked", threadChannel.isLocked());
                if (threadChannel.getParentChannel() != null) {
                    channelData.put("parentId", threadChannel.getParentChannel().getId());
                }
            }

            // Add position for guild channels
            if (channel instanceof TextChannel) {
                channelData.put("position", ((TextChannel) channel).getPosition());
            } else if (channel instanceof VoiceChannel) {
                channelData.put("position", ((VoiceChannel) channel).getPosition());
            } else if (channel instanceof Category) {
                channelData.put("position", ((Category) channel).getPosition());
            } else if (channel instanceof NewsChannel) {
                channelData.put("position", ((NewsChannel) channel).getPosition());
            } else if (channel instanceof StageChannel) {
                channelData.put("position", ((StageChannel) channel).getPosition());
            } else if (channel instanceof ForumChannel) {
                channelData.put("position", ((ForumChannel) channel).getPosition());
            } else if (channel instanceof MediaChannel) {
                channelData.put("position", ((MediaChannel) channel).getPosition());
            }

        } catch (Exception e) {
            // If we can't get type-specific data, just ignore it
            logger.warn("Could not get type-specific data for channel: " + channel.getId(), e);
        }

        return channelData;
    }
}