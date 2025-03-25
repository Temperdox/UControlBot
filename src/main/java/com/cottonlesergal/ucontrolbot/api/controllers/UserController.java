package com.cottonlesergal.ucontrolbot.api.controllers;

import com.cottonlesergal.ucontrolbot.Bot;
import com.cottonlesergal.ucontrolbot.config.Config;
import com.cottonlesergal.ucontrolbot.models.DiscordUser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for user-related API endpoints.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final Bot bot;
    private final Config config;

    /**
     * Initializes the user controller with the specified bot instance.
     *
     * @param bot The bot instance
     * @param config The configuration
     */
    @Autowired
    public UserController(Bot bot, Config config) {
        this.bot = bot;
        this.config = config;
    }

    /**
     * Gets users that the bot can see.
     * Note: This only returns users in the bot's cache due to Discord API limitations.
     *
     * @param guildId  Optional guild ID to filter by
     * @param dmFilter Optional parameter to filter for DM users
     * @param allUsers Optional parameter
     * @return List of users
     */
    @GetMapping
    public ResponseEntity<?> getUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String guildId,
            @RequestParam(name = "dm", required = false, defaultValue = "false") boolean dmFilter,
            @RequestParam(name = "all", required = false, defaultValue = "false") boolean allUsers) {
        try {
            JDA jda = bot.getJda();
            List<User> users = new ArrayList<>();

            if (allUsers) {
                // If 'all' parameter is true, get all unique users from all guilds
                Set<String> processedUserIds = new HashSet<>();

                for (Guild guild : jda.getGuilds()) {
                    List<Member> members;
                    if (guild.isLoaded()) {
                        members = guild.getMembers();
                    } else {
                        // Load members if needed
                        try {
                            members = guild.loadMembers().get();
                        } catch (Exception e) {
                            logger.warn("Could not load members for guild {}: {}", guild.getId(), e.getMessage());
                            members = new ArrayList<>();
                        }
                    }

                    for (Member member : members) {
                        User user = member.getUser();
                        if (!processedUserIds.contains(user.getId())) {
                            users.add(user);
                            processedUserIds.add(user.getId());
                        }
                    }
                }

                logger.info("Fetched {} unique users from all guilds", users.size());
            } else if (guildId != null && !guildId.isEmpty()) {
                Guild guild = jda.getGuildById(guildId);
                if (guild == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of(
                                    "error", "Guild not found",
                                    "message", "No guild found with ID: " + guildId
                            ));
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
            if (name != null && !name.isEmpty()) {
                String finalNameFilter = name.toLowerCase();
                users = users.stream()
                        .filter(user -> user.getName().toLowerCase().contains(finalNameFilter))
                        .collect(Collectors.toList());
            }

            // Convert to response format
            List<Map<String, Object>> userData = users.stream()
                    .map(user -> {
                        DiscordUser discordUser = DiscordUser.fromJdaUser(user);

                        // Add status manually based on Discord presence (if available)
                        Map<String, Object> data = discordUser.getUserData();

                        // Check if this is the owner
                        String ownerId = config.getOwnerId();
                        if (user.getId().equals(ownerId)) {
                            data.put("isOwner", true);
                            data.put("status", "online");  // Set owner to online by default
                        }

                        // Try to get status from mutual guilds if user is not owner
                        if (!data.containsKey("status") || data.get("status") == null) {
                            try {
                                // Check all mutual guilds for presence information
                                List<Guild> mutualGuilds = jda.getMutualGuilds(user);
                                for (Guild guild : mutualGuilds) {
                                    Member member = guild.getMember(user);
                                    if (member != null) {
                                        OnlineStatus status = member.getOnlineStatus();
                                        if (status != null && status != OnlineStatus.OFFLINE) {
                                            data.put("status", status.name().toLowerCase());
                                            break;
                                        }
                                    }
                                }

                                // Set default status if none found
                                if (!data.containsKey("status") || data.get("status") == null) {
                                    data.put("status", "offline");
                                }
                            } catch (Exception e) {
                                data.put("status", "offline");
                                logger.warn("Could not get status for user {}: {}", user.getId(), e.getMessage());
                            }
                        }

                        // Hard-coded statuses for key users (temporary)
                        if (user.getId().equals("1040120006534516806")) { // Deathly Ecks
                            data.put("status", "dnd");
                        } else if (user.getName().equals("Manifold")) {
                            data.put("status", "online");
                        }

                        return data;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            logger.error("Error getting users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Gets information about a specific user.
     *
     * @param userId User ID
     * @param guildId Optional guild ID for member-specific data
     * @return User information
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(
            @PathVariable String userId,
            @RequestParam(required = false) String guildId) {

        if ("@me".equals(userId)) {
            return getBotUser();
        }

        try {
            JDA jda = bot.getJda();
            User user = jda.retrieveUserById(userId).complete();

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "User not found",
                                "message", "No user found with ID: " + userId
                        ));
            }

            // Basic user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getName());
            userData.put("displayName", user.getGlobalName() != null ? user.getGlobalName() : user.getName());
            userData.put("discriminator", user.getDiscriminator());
            userData.put("avatarUrl", user.getEffectiveAvatarUrl());
            userData.put("isBot", user.isBot());
            userData.put("bot", user.isBot());
            userData.put("globalName", user.getGlobalName());

            // Check if this is the owner
            if (user.getId().equals(config.getOwnerId())) {
                userData.put("isOwner", true);
                userData.put("status", "online");
            }

            // Get guild-specific data if available
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
                        memberData.put("status", member.getOnlineStatus().name().toLowerCase());

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
                        userData.put("status", member.getOnlineStatus().name().toLowerCase());
                    }
                }
            }

            // If status not set yet, try to find from mutual guilds
            if (!userData.containsKey("status")) {
                try {
                    // Find first guild with presence info
                    for (Guild guild : jda.getMutualGuilds(user)) {
                        Member member = guild.getMember(user);
                        if (member != null) {
                            userData.put("status", member.getOnlineStatus().name().toLowerCase());
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not get status for user {}: {}", user.getId(), e.getMessage());
                }

                // Set default if still not found
                if (!userData.containsKey("status")) {
                    userData.put("status", "offline");
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

            return ResponseEntity.ok(userData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error retrieving user: " + userId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Discord API error",
                            "code", e.getErrorCode(),
                            "message", e.getMeaning()
                    ));
        } catch (Exception e) {
            logger.error("Error retrieving user: " + userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Gets information about the bot user (@me).
     *
     * @return Bot user information
     */
    @GetMapping("/@me")
    public ResponseEntity<?> getBotUser() {
        try {
            JDA jda = bot.getJda();
            User selfUser = jda.getSelfUser();

            // Basic user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", selfUser.getId());
            userData.put("username", selfUser.getName());
            userData.put("displayName", selfUser.getGlobalName() != null ? selfUser.getGlobalName() : selfUser.getName());
            userData.put("discriminator", selfUser.getDiscriminator());
            userData.put("avatarUrl", selfUser.getEffectiveAvatarUrl());
            userData.put("isBot", selfUser.isBot());
            userData.put("globalName", selfUser.getGlobalName());
            userData.put("status", "online");  // Bot is always online

            // Get guilds this bot is in
            List<Map<String, Object>> guilds = jda.getGuilds().stream()
                    .map(guild -> {
                        Map<String, Object> guildData = new HashMap<>();
                        guildData.put("id", guild.getId());
                        guildData.put("name", guild.getName());
                        guildData.put("iconUrl", guild.getIconUrl());
                        return guildData;
                    })
                    .collect(Collectors.toList());
            userData.put("guilds", guilds);

            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            logger.error("Error retrieving bot user information", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Gets the DM channel for a specific user.
     *
     * @param userId User ID
     * @return DM channel information
     */
    @GetMapping("/{userId}/dm")
    public ResponseEntity<?> getDmChannel(@PathVariable String userId) {
        try {
            JDA jda = bot.getJda();
            User user = jda.retrieveUserById(userId).complete();

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "User not found",
                                "message", "No user found with ID: " + userId
                        ));
            }

            // Open or retrieve DM channel
            PrivateChannel channel = user.openPrivateChannel().complete();

            Map<String, Object> channelData = new HashMap<>();
            channelData.put("id", channel.getId());
            channelData.put("type", channel.getType().name());
            channelData.put("name", "@" + user.getName());
            channelData.put("userId", user.getId());

            return ResponseEntity.ok(channelData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error retrieving DM channel for user: " + userId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Discord API error",
                            "code", e.getErrorCode(),
                            "message", e.getMeaning()
                    ));
        } catch (Exception e) {
            logger.error("Error retrieving DM channel for user: " + userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
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
     * @param userId User ID
     * @return Created DM channel information
     */
    @PostMapping("/{userId}/dm")
    public ResponseEntity<?> createDmChannel(@PathVariable String userId) {
        try {
            JDA jda = bot.getJda();
            User user = jda.retrieveUserById(userId).complete();

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "User not found",
                                "message", "No user found with ID: " + userId
                        ));
            }

            // Open or create DM channel
            PrivateChannel channel = user.openPrivateChannel().complete();

            Map<String, Object> channelData = new HashMap<>();
            channelData.put("id", channel.getId());
            channelData.put("type", channel.getType().name());
            channelData.put("name", "@" + user.getName());
            channelData.put("userId", user.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(channelData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error creating DM channel for user: " + userId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Discord API error",
                            "code", e.getErrorCode(),
                            "message", e.getMeaning()
                    ));
        } catch (Exception e) {
            logger.error("Error creating DM channel for user: " + userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }
}