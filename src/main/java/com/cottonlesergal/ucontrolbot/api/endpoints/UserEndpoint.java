package com.cottonlesergal.ucontrolbot.api.endpoints;

import com.cottonlesergal.ucontrolbot.Bot;
import com.cottonlesergal.ucontrolbot.models.DiscordUser;
import com.google.gson.Gson;
import io.javalin.http.Context;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoint for user-related API requests.
 */
public class UserEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(UserEndpoint.class);
    private final Bot bot;
    private final Gson gson = new Gson();

    /**
     * Initializes the user endpoint with the specified bot instance.
     *
     * @param bot The bot instance
     */
    public UserEndpoint(Bot bot) {
        this.bot = bot;
    }

    /**
     * Gets users that the bot can see.
     * Note: This only returns users in the bot's cache due to Discord API limitations.
     *
     * @param ctx The HTTP context
     */
    public void getUsers(Context ctx) {
        try {
            JDA jda = bot.getJda();

            // Get query parameters for filtering
            String nameFilter = ctx.queryParam("name");
            String guildId = ctx.queryParam("guildId");

            List<User> users = new ArrayList<>();

            // If guild ID is provided, get members from that guild
            if (guildId != null && !guildId.isEmpty()) {
                Guild guild = jda.getGuildById(guildId);
                if (guild == null) {
                    ctx.status(404).json(Map.of(
                            "error", "Guild not found",
                            "message", "No guild found with ID: " + guildId
                    ));
                    return;
                }

                List<Member> members;
                if (guild.isLoaded()) {
                    members = guild.getMembers();
                } else {
                    // Load members if needed
                    members = guild.loadMembers().get();
                }

                users = members.stream()
                        .map(Member::getUser)
                        .collect(Collectors.toList());
            } else {
                // Otherwise, get all users from cache
                users = jda.getUserCache().asList();
            }

            // Apply name filter if provided
            if (nameFilter != null && !nameFilter.isEmpty()) {
                String finalNameFilter = nameFilter.toLowerCase();
                users = users.stream()
                        .filter(user -> user.getName().toLowerCase().contains(finalNameFilter))
                        .collect(Collectors.toList());
            }

            // Convert to response format
            List<Map<String, Object>> userData = users.stream()
                    .map(user -> {
                        DiscordUser discordUser = DiscordUser.fromJdaUser(user);
                        return discordUser.getUserData();
                    })
                    .collect(Collectors.toList());

            ctx.json(userData);
        } catch (Exception e) {
            logger.error("Error getting users", e);
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Gets information about a specific user.
     *
     * @param ctx The HTTP context
     */
    public void getUser(Context ctx) {
        String userId = ctx.pathParam("userId");

        try {
            JDA jda = bot.getJda();
            User user = jda.retrieveUserById(userId).complete();

            if (user == null) {
                ctx.status(404).json(Map.of(
                        "error", "User not found",
                        "message", "No user found with ID: " + userId
                ));
                return;
            }

            // Basic user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("name", user.getName());
            userData.put("discriminator", user.getDiscriminator());
            userData.put("avatarUrl", user.getEffectiveAvatarUrl());
            userData.put("bot", user.isBot());
            userData.put("globalName", user.getGlobalName());

            // Get guild-specific data if available
            String guildId = ctx.queryParam("guildId");
            if (guildId != null && !guildId.isEmpty()) {
                Guild guild = jda.getGuildById(guildId);
                if (guild != null) {
                    Member member = guild.retrieveMember(user).complete();
                    if (member != null) {
                        Map<String, Object> memberData = new HashMap<>();
                        memberData.put("nickname", member.getNickname());
                        memberData.put("joinedAt", member.getTimeJoined().toInstant().toEpochMilli());
                        memberData.put("boosting", member.isBoosting());
                        memberData.put("color", member.getColorRaw());

                        // Get roles
                        List<Map<String, Object>> roles = member.getRoles().stream()
                                .map(role -> {
                                    Map<String, Object> roleData = new HashMap<>();
                                    roleData.put("id", role.getId());
                                    roleData.put("name", role.getName());
                                    roleData.put("color", role.getColorRaw());
                                    roleData.put("position", role.getPosition());
                                    return roleData;
                                })
                                .collect(Collectors.toList());
                        memberData.put("roles", roles);

                        // Add permissions
                        memberData.put("permissions", member.getPermissions().stream()
                                .map(Enum::name)
                                .collect(Collectors.toList()));

                        userData.put("memberData", memberData);
                    }
                }
            }

            // Get mutual guilds
            List<Map<String, Object>> mutualGuilds = jda.getMutualGuilds(user).stream()
                    .map(guild -> {
                        Map<String, Object> guildData = new HashMap<>();
                        guildData.put("id", guild.getId());
                        guildData.put("name", guild.getName());
                        guildData.put("iconUrl", guild.getIconUrl());
                        return guildData;
                    })
                    .collect(Collectors.toList());
            userData.put("mutualGuilds", mutualGuilds);

            ctx.json(userData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error retrieving user: " + userId, e);
            ctx.status(e.getErrorCode()).json(Map.of(
                    "error", "Discord API error",
                    "code", e.getErrorCode(),
                    "message", e.getMeaning()
            ));
        } catch (Exception e) {
            logger.error("Error retrieving user: " + userId, e);
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Gets the DM channel for a specific user.
     *
     * @param ctx The HTTP context
     */
    public void getDmChannel(Context ctx) {
        String userId = ctx.pathParam("userId");

        try {
            JDA jda = bot.getJda();
            User user = jda.retrieveUserById(userId).complete();

            if (user == null) {
                ctx.status(404).json(Map.of(
                        "error", "User not found",
                        "message", "No user found with ID: " + userId
                ));
                return;
            }

            // Open or retrieve DM channel
            PrivateChannel channel = user.openPrivateChannel().complete();

            Map<String, Object> channelData = new HashMap<>();
            channelData.put("id", channel.getId());
            channelData.put("type", channel.getType().name());
            channelData.put("name", "@" + user.getName());
            channelData.put("userId", user.getId());

            ctx.json(channelData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error retrieving DM channel for user: " + userId, e);
            ctx.status(e.getErrorCode()).json(Map.of(
                    "error", "Discord API error",
                    "code", e.getErrorCode(),
                    "message", e.getMeaning()
            ));
        } catch (Exception e) {
            logger.error("Error retrieving DM channel for user: " + userId, e);
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Creates a DM channel with a specific user.
     * Note: This is essentially the same as getDmChannel since Discord
     * automatically creates the channel if it doesn't exist.
     *
     * @param ctx The HTTP context
     */
    public void createDmChannel(Context ctx) {
        String userId = ctx.pathParam("userId");

        try {
            JDA jda = bot.getJda();
            User user = jda.retrieveUserById(userId).complete();

            if (user == null) {
                ctx.status(404).json(Map.of(
                        "error", "User not found",
                        "message", "No user found with ID: " + userId
                ));
                return;
            }

            // Open or create DM channel
            PrivateChannel channel = user.openPrivateChannel().complete();

            Map<String, Object> channelData = new HashMap<>();
            channelData.put("id", channel.getId());
            channelData.put("type", channel.getType().name());
            channelData.put("name", "@" + user.getName());
            channelData.put("userId", user.getId());

            ctx.status(201).json(channelData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error creating DM channel for user: " + userId, e);
            ctx.status(e.getErrorCode()).json(Map.of(
                    "error", "Discord API error",
                    "code", e.getErrorCode(),
                    "message", e.getMeaning()
            ));
        } catch (Exception e) {
            logger.error("Error creating DM channel for user: " + userId, e);
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }
}