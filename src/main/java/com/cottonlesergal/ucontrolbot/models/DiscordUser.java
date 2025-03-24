package com.cottonlesergal.ucontrolbot.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A unified model for Discord User data that can be used
 * across both the bot and web client applications.
 */
public class DiscordUser {
    // Constants for accessing user properties
    public static final String ID = "id";
    public static final String NAME = "username";
    public static final String DISPLAY_NAME = "displayName";
    public static final String AVATAR = "avatarUrl";
    public static final String DISCRIMINATOR = "discriminator";
    public static final String IS_BOT = "isBot";
    public static final String STATUS = "status";
    public static final String GUILD_ID = "guildId";
    public static final String ROLES = "roles";
    public static final String NICKNAME = "nickname";
    public static final String JOIN_DATE = "joinDate";
    public static final String BANNER = "bannerUrl";
    public static final String ACCENT_COLOR = "accentColor";
    public static final String GLOBAL_NAME = "globalName";
    public static final String IS_OWNER = "isOwner";

    // The actual data storage
    private Map<String, Object> userData;

    // For JSON serialization/deserialization
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    // JDA reference for fetching data
    private transient JDA jda;

    /**
     * Default constructor
     */
    public DiscordUser() {
        this.userData = new HashMap<>();
    }

    /**
     * Constructor with initial data
     *
     * @param userData Initial user data
     */
    public DiscordUser(Map<String, Object> userData) {
        this.userData = userData != null ? userData : new HashMap<>();
    }

    /**
     * Constructor with JDA instance
     *
     * @param jda JDA instance for API access
     */
    public DiscordUser(JDA jda) {
        this.userData = new HashMap<>();
        this.jda = jda;
    }

    /**
     * Gets a user property by key
     *
     * @param property Property key
     * @return Property value
     */
    public Object get(String property) {
        return userData.get(property);
    }

    /**
     * Sets a user property
     *
     * @param property Property key
     * @param value Property value
     * @return This instance for chaining
     */
    public DiscordUser set(String property, Object value) {
        userData.put(property, value);
        return this;
    }

    /**
     * Gets the user's ID
     *
     * @return User ID
     */
    public String getId() {
        return (String) get(ID);
    }

    /**
     * Gets the user's name
     *
     * @return Username
     */
    public String getName() {
        return (String) get(NAME);
    }

    /**
     * Gets the user's display name (nickname if available, otherwise username)
     *
     * @return Display name
     */
    public String getDisplayName() {
        return (String) get(DISPLAY_NAME);
    }

    /**
     * Gets the user's avatar URL
     *
     * @return Avatar URL
     */
    public String getAvatarUrl() {
        return (String) get(AVATAR);
    }

    /**
     * Checks if user is a bot
     *
     * @return True if bot
     */
    public boolean isBot() {
        Boolean isBot = (Boolean) get(IS_BOT);
        return isBot != null && isBot;
    }

    /**
     * Fetches user information from Discord API
     *
     * @param userId Discord user ID
     * @return This instance with updated data
     */
    public DiscordUser fetchInfo(String userId) {
        if (jda == null) {
            throw new IllegalStateException("JDA instance not set. Cannot fetch user info.");
        }

        User jdaUser = jda.getUserById(userId);
        if (jdaUser == null) {
            // Try retrieving from REST API if not cached
            try {
                jdaUser = jda.retrieveUserById(userId).complete();
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch user with ID: " + userId, e);
            }
        }

        if (jdaUser != null) {
            populateFromJdaUser(jdaUser);
        }

        return this;
    }

    /**
     * Fetches user information asynchronously
     *
     * @param userId Discord user ID
     * @return CompletableFuture that resolves to this instance with updated data
     */
    public CompletableFuture<DiscordUser> fetchInfoAsync(String userId) {
        if (jda == null) {
            CompletableFuture<DiscordUser> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("JDA instance not set. Cannot fetch user info."));
            return future;
        }

        return jda.retrieveUserById(userId).submit()
                .thenApply(jdaUser -> {
                    populateFromJdaUser(jdaUser);
                    return this;
                });
    }

    /**
     * Fetches guild member information
     *
     * @param userId Discord user ID
     * @param guildId Discord guild ID
     * @return This instance with updated data
     */
    public DiscordUser fetchMemberInfo(String userId, String guildId) {
        if (jda == null) {
            throw new IllegalStateException("JDA instance not set. Cannot fetch member info.");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Guild not found with ID: " + guildId);
        }

        // First fetch basic user info
        fetchInfo(userId);

        // Then fetch guild-specific info
        Member member = guild.getMemberById(userId);
        if (member == null) {
            try {
                member = guild.retrieveMemberById(userId).complete();
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch member with ID: " + userId + " in guild: " + guildId, e);
            }
        }

        if (member != null) {
            populateFromGuildMember(member);
        }

        return this;
    }

    /**
     * Sets the JDA instance for API access
     *
     * @param jda JDA instance
     * @return This instance for chaining
     */
    public DiscordUser setJda(JDA jda) {
        this.jda = jda;
        return this;
    }

    /**
     * Populates user data from JDA User object
     *
     * @param jdaUser JDA User
     */
    private void populateFromJdaUser(User jdaUser) {
        set(ID, jdaUser.getId());
        set(NAME, jdaUser.getName());
        set(GLOBAL_NAME, jdaUser.getGlobalName());
        set(DISPLAY_NAME, jdaUser.getGlobalName() != null ? jdaUser.getGlobalName() : jdaUser.getName());
        set(AVATAR, jdaUser.getEffectiveAvatarUrl());
        set(DISCRIMINATOR, jdaUser.getDiscriminator());
        set(IS_BOT, jdaUser.isBot());

        // These require privileged gateway intents, so may be null
        try {
            set(BANNER, jdaUser.retrieveProfile().complete().getBannerUrl());
            set(ACCENT_COLOR, jdaUser.retrieveProfile().complete().getAccentColorRaw());
        } catch (Exception e) {
            // Silently ignore if we can't retrieve extended profile data
        }
    }

    /**
     * Populates user data from JDA Member object
     *
     * @param member JDA Member
     */
    private void populateFromGuildMember(Member member) {
        set(GUILD_ID, member.getGuild().getId());
        set(NICKNAME, member.getNickname());
        set(DISPLAY_NAME, member.getEffectiveName());
        set(JOIN_DATE, member.getTimeJoined().toEpochSecond());
        set(IS_OWNER, member.isOwner());

        // Get roles
        if (member.getRoles() != null) {
            set(ROLES, member.getRoles().stream()
                    .map(role -> Map.of(
                            "id", role.getId(),
                            "name", role.getName(),
                            "color", role.getColorRaw(),
                            "position", role.getPosition()
                    ))
                    .toArray());
        }

        // Get online status if available
        try {
            set(STATUS, member.getOnlineStatus().name());
        } catch (Exception e) {
            // Presence intent may not be enabled
        }
    }

    /**
     * Converts the user data to JSON
     *
     * @return JSON string
     */
    public String toJson() {
        return gson.toJson(userData);
    }

    /**
     * Creates a DiscordUser from JSON
     *
     * @param json JSON string
     * @return New DiscordUser instance
     */
    public static DiscordUser fromJson(String json) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = gson.fromJson(json, Map.class);
        return new DiscordUser(data);
    }

    /**
     * Creates a DiscordUser from a JDA User object
     *
     * @param jdaUser JDA User
     * @return New DiscordUser instance
     */
    public static DiscordUser fromJdaUser(User jdaUser) {
        DiscordUser user = new DiscordUser();
        user.populateFromJdaUser(jdaUser);
        return user;
    }

    /**
     * Creates a DiscordUser from a JDA Member object
     *
     * @param member JDA Member
     * @return New DiscordUser instance
     */
    public static DiscordUser fromJdaMember(Member member) {
        DiscordUser user = fromJdaUser(member.getUser());
        user.populateFromGuildMember(member);
        return user;
    }

    /**
     * Gets the raw user data map
     *
     * @return User data map
     */
    public Map<String, Object> getUserData() {
        return new HashMap<>(userData);
    }

    @Override
    public String toString() {
        return "DiscordUser{" + toJson() + "}";
    }
}