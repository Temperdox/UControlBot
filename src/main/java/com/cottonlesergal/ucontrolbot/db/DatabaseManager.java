package com.cottonlesergal.ucontrolbot.db;

import com.cottonlesergal.ucontrolbot.config.Config;
import com.cottonlesergal.ucontrolbot.config.JdaProvider;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Database manager for the Discord bot.
 * Handles database operations and event processing.
 */
@Component
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    @Autowired
    private Config config;

    private JdbcTemplate jdbcTemplate;
    private final Gson gson = new Gson();
    private String botUserId;
    @Autowired
    private JdaProvider jdaProvider;

    /**
     * Initializes the database manager.
     */
    @PostConstruct
    public void initialize() {
        try {
            // Create data source
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
            dataSource.setUrl("jdbc:mariadb://localhost:3306/ucontrolbot_discord");
            dataSource.setUsername("root");
            dataSource.setPassword("");

            // Create JDBC template
            jdbcTemplate = new JdbcTemplate(dataSource);

            // Initialize database tables
            initializeDatabase();

            logger.info("Database manager initialized");
        } catch (Exception e) {
            logger.error("Error initializing database manager", e);
            throw new RuntimeException("Failed to initialize database manager", e);
        }
        try (Connection testConnection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
            logger.info("Database connection successful: {}", testConnection.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            logger.error("Database connection failed", e);
            throw new RuntimeException("Database connection failed", e);
        }
    }

    /**
     * Initializes the database tables.
     */
    private void initializeDatabase() {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {

            // Create users table
            statement.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "discriminator VARCHAR(10), " +
                    "global_name VARCHAR(255), " +
                    "display_name VARCHAR(255), " +
                    "avatar_url VARCHAR(512), " +
                    "is_bot BOOLEAN DEFAULT FALSE, " +
                    "is_owner BOOLEAN DEFAULT FALSE, " +
                    "status VARCHAR(50) DEFAULT 'offline', " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");

            // Create guilds table
            statement.execute("CREATE TABLE IF NOT EXISTS guilds (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "icon_url VARCHAR(512), " +
                    "owner_id VARCHAR(255), " +
                    "member_count INT DEFAULT 0, " +
                    "description TEXT, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL" +
                    ")");

            // Create channels table
            statement.execute("CREATE TABLE IF NOT EXISTS channels (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "guild_id VARCHAR(255), " +
                    "parent_id VARCHAR(255), " +
                    "name VARCHAR(255) NOT NULL, " +
                    "type VARCHAR(50) NOT NULL, " +
                    "topic TEXT, " +
                    "position INT DEFAULT 0, " +
                    "is_nsfw BOOLEAN DEFAULT FALSE, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (parent_id) REFERENCES channels(id) ON DELETE SET NULL" +
                    ")");

            // Create messages table
            statement.execute("CREATE TABLE IF NOT EXISTS messages (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "channel_id VARCHAR(255) NOT NULL, " +
                    "author_id VARCHAR(255) NOT NULL, " +
                    "content TEXT, " +
                    "timestamp BIGINT NOT NULL, " +
                    "edited_timestamp BIGINT, " +
                    "referenced_message_id VARCHAR(255), " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (channel_id) REFERENCES channels(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (referenced_message_id) REFERENCES messages(id) ON DELETE SET NULL" +
                    ")");

            // Create attachments table
            statement.execute("CREATE TABLE IF NOT EXISTS attachments (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "message_id VARCHAR(255) NOT NULL, " +
                    "filename VARCHAR(255) NOT NULL, " +
                    "url VARCHAR(512) NOT NULL, " +
                    "content_type VARCHAR(255), " +
                    "size BIGINT, " +
                    "FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE" +
                    ")");

            // Create embeds table
            statement.execute("CREATE TABLE IF NOT EXISTS embeds (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "message_id VARCHAR(255) NOT NULL, " +
                    "title VARCHAR(255), " +
                    "description TEXT, " +
                    "url VARCHAR(512), " +
                    "color INT, " +
                    "timestamp BIGINT, " +
                    "FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE" +
                    ")");

            // Create embed fields table
            statement.execute("CREATE TABLE IF NOT EXISTS embed_fields (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "embed_id INT NOT NULL, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "value TEXT NOT NULL, " +
                    "is_inline BOOLEAN DEFAULT FALSE, " +
                    "position INT DEFAULT 0, " +
                    "FOREIGN KEY (embed_id) REFERENCES embeds(id) ON DELETE CASCADE" +
                    ")");

            // Create roles table
            statement.execute("CREATE TABLE IF NOT EXISTS roles (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "guild_id VARCHAR(255) NOT NULL, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "color INT DEFAULT 0, " +
                    "position INT DEFAULT 0, " +
                    "permissions BIGINT DEFAULT 0, " +
                    "is_mentionable BOOLEAN DEFAULT FALSE, " +
                    "is_hoisted BOOLEAN DEFAULT FALSE, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");

            // Create user roles table
            statement.execute("CREATE TABLE IF NOT EXISTS user_roles (" +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "role_id VARCHAR(255) NOT NULL, " +
                    "guild_id VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY (user_id, role_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");

            // Create guild members table
            statement.execute("CREATE TABLE IF NOT EXISTS guild_members (" +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "guild_id VARCHAR(255) NOT NULL, " +
                    "nickname VARCHAR(255), " +
                    "joined_at BIGINT, " +
                    "PRIMARY KEY (user_id, guild_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");

            // Create DM channels table
            statement.execute("CREATE TABLE IF NOT EXISTS dm_channels (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "last_message_id VARCHAR(255), " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ")");

            // Create reactions table
            statement.execute("CREATE TABLE IF NOT EXISTS reactions (" +
                    "message_id VARCHAR(255) NOT NULL, " +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "emoji VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY (message_id, user_id, emoji), " +
                    "FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ")");

            // Create typing indicators table
            statement.execute("CREATE TABLE IF NOT EXISTS typing_indicators (" +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "channel_id VARCHAR(255) NOT NULL, " +
                    "timestamp BIGINT NOT NULL, " +
                    "PRIMARY KEY (user_id, channel_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (channel_id) REFERENCES channels(id) ON DELETE CASCADE" +
                    ")");

            // Create indexes for faster queries
            statement.execute("CREATE INDEX IF NOT EXISTS idx_users_status ON users(status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_channels_guild_id ON channels(guild_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_channels_parent_id ON channels(parent_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_messages_channel_id ON messages(channel_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_messages_author_id ON messages(author_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_guild_members_guild_id ON guild_members(guild_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_user_roles_guild_id ON user_roles(guild_id)");

            logger.info("Database tables initialized");
        } catch (SQLException e) {
            logger.error("Error initializing database tables", e);
            throw new RuntimeException("Failed to initialize database tables", e);
        }
    }

    // Add this to your DatabaseManager class
    public void checkDatabaseConnection() {
        fetchDBInfo();
    }

    private static void displayDatabaseMetrics(int userCount, int guildCount, int channelCount, int messageCount) {
        logger.info("Database status: Users={}, Guilds={}, Channels={}, Messages={}",
                userCount, guildCount, channelCount, messageCount);
    }

    // Call this method periodically
    @Scheduled(fixedRate = 60000) // Every minute
    public void logDatabaseStatus() {
        checkDatabaseConnection();
    }

    /**
     * Processes a Discord event and saves it to the database.
     *
     * @param eventType The event type
     * @param eventData The event data
     * @return True if the event was processed successfully
     */
    public boolean processEvent(String eventType, Object eventData) {
        try {
            logger.debug("Processing event: {}", eventType);

            // Map of event type to handler method
            Map<String, EventProcessor> eventHandlers = new HashMap<>();

            if ("MESSAGE_RECEIVED".equals(eventType)) {
                logger.info("📝 Received MESSAGE_RECEIVED event: {}", gson.toJson(eventData));
            }

            // Register event handlers
            eventHandlers.put("USER_UPDATE", this::processUserUpdate);
            eventHandlers.put("USER_UPDATE_STATUS", this::processUserStatusUpdate);
            eventHandlers.put("GUILD_JOIN", this::processGuildJoin);
            eventHandlers.put("GUILD_UPDATE", this::processGuildUpdate);
            eventHandlers.put("GUILD_MEMBER_JOIN", this::processGuildMemberJoin);
            eventHandlers.put("GUILD_MEMBER_LEAVE", this::processGuildMemberLeave);
            eventHandlers.put("CHANNEL_CREATE", this::processChannelCreate);
            eventHandlers.put("CHANNEL_UPDATE", this::processChannelUpdate);
            eventHandlers.put("MESSAGE_RECEIVED", this::processMessageReceived);
            eventHandlers.put("MESSAGE_UPDATE", this::processMessageUpdate);
            eventHandlers.put("MESSAGE_DELETE", this::processMessageDelete);
            eventHandlers.put("MESSAGE_REACTION_ADD", this::processReactionAdd);
            eventHandlers.put("MESSAGE_REACTION_REMOVE", this::processReactionRemove);
            eventHandlers.put("TYPING_START", this::processTypingStart);
            eventHandlers.put("ROLE_CREATE", this::processRoleCreate);
            eventHandlers.put("ROLE_UPDATE", this::processRoleUpdate);
            eventHandlers.put("REFRESH_DM_LIST", this::processRefreshDmList);

            // Get handler for event type
            EventProcessor processor = eventHandlers.get(eventType);
            if (processor != null) {
                // Convert object to map for easier processing
                @SuppressWarnings("unchecked")
                Map<String, Object> data = gson.fromJson(gson.toJson(eventData), Map.class);

                // Process event
                return processor.process(data);
            } else {
                logger.warn("No handler for event type: {}", eventType);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error processing event: {}", eventType, e);
            return false;
        }
    }

    /**
     * Gets the bot user ID.
     *
     * @return The bot user ID
     */
    public String getBotUserId() {
        return this.botUserId;
    }

    public List<Map<String, Object>> getAllUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM users");
            for (Map<String, Object> row : rows) {
                // Convert database column names to camelCase for frontend
                Map<String, Object> user = new HashMap<>();
                user.put("id", row.get("id"));
                user.put("username", row.get("username"));
                user.put("discriminator", row.get("discriminator"));
                user.put("globalName", row.get("global_name"));
                user.put("displayName", row.get("display_name"));
                user.put("avatarUrl", row.get("avatar_url"));
                user.put("isBot", row.get("is_bot"));
                user.put("isOwner", row.get("is_owner"));
                user.put("status", row.get("status"));
                users.add(user);
            }
        } catch (Exception e) {
            logger.error("Error getting all users", e);
            throw e;
        }
        return users;
    }

    public List<Map<String, Object>> getAllGuilds() {
        List<Map<String, Object>> guilds = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM guilds");
            for (Map<String, Object> row : rows) {
                Map<String, Object> guild = new HashMap<>();
                guild.put("id", row.get("id"));
                guild.put("name", row.get("name"));
                guild.put("iconUrl", row.get("icon_url"));
                guild.put("ownerId", row.get("owner_id"));
                guild.put("memberCount", row.get("member_count"));
                guild.put("description", row.get("description"));
                guilds.add(guild);
            }
        } catch (Exception e) {
            logger.error("Error getting all guilds", e);
            throw e;
        }
        return guilds;
    }

    public List<Map<String, Object>> getChannelsByGuildId(String guildId) {
        List<Map<String, Object>> channels = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT * FROM channels WHERE guild_id = ?", guildId);
            for (Map<String, Object> row : rows) {
                Map<String, Object> channel = new HashMap<>();
                channel.put("id", row.get("id"));
                channel.put("guildId", row.get("guild_id"));
                channel.put("parentId", row.get("parent_id"));
                channel.put("name", row.get("name"));
                channel.put("type", row.get("type"));
                channel.put("topic", row.get("topic"));
                channel.put("position", row.get("position"));
                channel.put("nsfw", row.get("is_nsfw"));
                channels.add(channel);
            }
        } catch (Exception e) {
            logger.error("Error getting channels for guild " + guildId, e);
            throw e;
        }
        return channels;
    }

    public List<Map<String, Object>> getMessagesByChannelId(String channelId) {
        List<Map<String, Object>> messages = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT m.*, u.username, u.avatar_url, u.is_bot " +
                            "FROM messages m " +
                            "LEFT JOIN users u ON m.author_id = u.id " +
                            "WHERE m.channel_id = ? " +
                            "ORDER BY m.timestamp DESC LIMIT 50", channelId);

            for (Map<String, Object> row : rows) {
                Map<String, Object> message = new HashMap<>();
                message.put("id", row.get("id"));
                message.put("channelId", row.get("channel_id"));
                message.put("content", row.get("content"));
                message.put("timestamp", row.get("timestamp"));
                message.put("editedTimestamp", row.get("edited_timestamp"));

                // Add author data as a nested object
                Map<String, Object> author = new HashMap<>();
                author.put("id", row.get("author_id"));
                author.put("username", row.get("username"));
                author.put("avatarUrl", row.get("avatar_url"));
                author.put("isBot", row.get("is_bot"));
                message.put("author", author);

                // Get attachments
                List<Map<String, Object>> attachments = jdbcTemplate.queryForList(
                        "SELECT * FROM attachments WHERE message_id = ?",
                        row.get("id"));
                message.put("attachments", attachments);

                messages.add(message);
            }
        } catch (Exception e) {
            logger.error("Error getting messages for channel " + channelId, e);
            throw e;
        }
        return messages;
    }

    public void checkDatabaseStatus() {
        fetchDBInfo();
    }

    private void fetchDBInfo() {
        try {
            int userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            int guildCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM guilds", Integer.class);
            int channelCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM channels", Integer.class);
            int messageCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class);

            displayDatabaseMetrics(userCount, guildCount, channelCount, messageCount);
        } catch (Exception e) {
            logger.error("Database check failed", e);
        }
    }

    /**
     * Sets the bot user ID.
     *
     * @param botUserId The bot user ID
     */
    public void setBotUserId(String botUserId) {
        this.botUserId = botUserId;

        // Create or update bot user in database
        jdbcTemplate.update(
                "INSERT INTO users (id, username, is_bot, status) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE username = VALUES(username), is_bot = VALUES(is_bot), status = VALUES(status)",
                botUserId, "Bot", true, "online"
        );
    }

    private boolean processRefreshDmList(Map<String, Object> data) {
        try {
            // This is an instruction to refresh the UI's DM list,
            // so we don't need to do anything in the database
            logger.info("Processing REFRESH_DM_LIST event");
            return true;
        } catch (Exception e) {
            logger.error("Error processing REFRESH_DM_LIST", e);
            return false;
        }
    }

    /**
     * Gets the ID of a DM channel for a user.
     *
     * @param userId The user ID
     * @return The DM channel ID, or null if not found
     */
    public String getDmChannelIdByUserId(String userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM dm_channels WHERE user_id = ?",
                    String.class,
                    userId
            );
        } catch (Exception e) {
            logger.debug("No DM channel found for user ID: {}", userId);
            return null;
        }
    }

    /**
     * Saves a typing indicator.
     *
     * @param userId The user ID
     * @param channelId The channel ID
     * @return True if saved successfully
     */
    public boolean saveTypingIndicator(String userId, String channelId) {
        try {
            // Ensure channel exists
            ensureChannelExists(channelId, null);

            // Ensure user exists
            ensureUserExists(userId);

            // Insert or update typing indicator
            jdbcTemplate.update(
                    "INSERT INTO typing_indicators (user_id, channel_id, timestamp) VALUES (?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE timestamp = ?",
                    userId, channelId, System.currentTimeMillis(), System.currentTimeMillis()
            );

            return true;
        } catch (Exception e) {
            logger.error("Error saving typing indicator", e);
            return false;
        }
    }

    /**
     * Cleans up typing indicators that are older than 10 seconds.
     *
     * @return True if cleaned up successfully
     */
    public boolean cleanupTypingIndicators() {
        try {
            long tenSecondsAgo = System.currentTimeMillis() - 10000;

            jdbcTemplate.update(
                    "DELETE FROM typing_indicators WHERE timestamp < ?",
                    tenSecondsAgo
            );

            return true;
        } catch (Exception e) {
            logger.error("Error cleaning up typing indicators", e);
            return false;
        }
    }

    /**
     * Ensures that a user exists in the database.
     *
     * @param userId The user ID
     * @return True if the user exists or was created
     */
    private boolean ensureUserExists(String userId) {
        try {
            // Check if user exists
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE id = ?",
                    Integer.class,
                    userId
            );

            if (count == 0) {
                // Create minimal user entry
                jdbcTemplate.update(
                        "INSERT INTO users (id, username) VALUES (?, ?)",
                        userId, jdaProvider.getJda().getUserById(userId).getGlobalName() != null ? jdaProvider.getJda().getUserById(userId).getGlobalName() : "Unknown User"
                );
            }

            return true;
        } catch (Exception e) {
            logger.error("Error ensuring user exists: {}", userId, e);
            return false;
        }
    }

    /**
     * Ensures that a guild exists in the database.
     *
     * @param guildId The guild ID
     * @param name The guild name (optional)
     * @return True if the guild exists or was created
     */
    private boolean ensureGuildExists(String guildId, String name) {
        try {
            // Check if guild exists
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM guilds WHERE id = ?",
                    Integer.class,
                    guildId
            );

            if (count == 0) {
                // Create minimal guild entry
                jdbcTemplate.update(
                        "INSERT INTO guilds (id, name) VALUES (?, ?)",
                        guildId, name != null ? name : "Unknown Guild"
                );
            }

            return true;
        } catch (Exception e) {
            logger.error("Error ensuring guild exists: {}", guildId, e);
            return false;
        }
    }

    /**
     * Ensures that a channel exists in the database.
     *
     * @param channelId The channel ID
     * @param name The channel name (optional)
     * @return True if the channel exists or was created
     */
    private boolean ensureChannelExists(String channelId, String name) {
        try {
            // Check if channel exists
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM channels WHERE id = ?",
                    Integer.class,
                    channelId
            );

            if (count == 0) {
                // Create minimal channel entry
                jdbcTemplate.update(
                        "INSERT INTO channels (id, name, type) VALUES (?, ?, ?)",
                        channelId, name != null ? name : "Unknown Channel", "UNKNOWN"
                );
            }

            return true;
        } catch (Exception e) {
            logger.error("Error ensuring channel exists: {}", channelId, e);
            return false;
        }
    }

    // Event processors

    /**
     * Processes a user update event.
     */
    private boolean processUserUpdate(Map<String, Object> data) {
        try {
            // Extract user data
            String userId = (String) data.get("userId");
            if (userId == null) {
                userId = (String) data.get("id");
            }

            String userName = (String) data.get("userName");
            if (userName == null) {
                userName = (String) data.get("username");
            }
            if (userName == null) {
                userName = (String) data.get("name");
            }

            String discriminator = (String) data.get("discriminator");
            String globalName = (String) data.get("globalName");

            String avatarUrl = (String) data.get("newAvatarUrl");
            if (avatarUrl == null) {
                avatarUrl = (String) data.get("avatarUrl");
            }

            boolean isBot = false;
            if (data.containsKey("bot")) {
                isBot = (Boolean) data.get("bot");
            } else if (data.containsKey("isBot")) {
                isBot = (Boolean) data.get("isBot");
            }

            // If we have a userId, update the user
            if (userId != null) {
                jdbcTemplate.update(
                        "INSERT INTO users (id, username, discriminator, global_name, avatar_url, is_bot) " +
                                "VALUES (?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE username = VALUES(username), discriminator = VALUES(discriminator), " +
                                "global_name = VALUES(global_name), avatar_url = VALUES(avatar_url), is_bot = VALUES(is_bot)",
                        userId, userName, discriminator, globalName, avatarUrl, isBot
                );

                return true;
            } else {
                logger.warn("User update event missing user ID");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error processing user update", e);
            return false;
        }
    }

    /**
     * Processes a user status update event.
     */
    private boolean processUserStatusUpdate(Map<String, Object> data) {
        try {
            String userId = (String) data.get("userId");
            String newStatus = (String) data.get("newStatus");

            if (userId == null || newStatus == null) {
                logger.warn("User status update event missing user ID or status");
                return false;
            }

            // Ensure user exists
            ensureUserExists(userId);

            // Update user status
            jdbcTemplate.update(
                    "UPDATE users SET status = ? WHERE id = ?",
                    newStatus.toLowerCase(), userId
            );

            // Check if this is the owner
            Boolean isOwner = (Boolean) data.get("isOwner");
            if (isOwner != null && isOwner) {
                jdbcTemplate.update(
                        "UPDATE users SET is_owner = true WHERE id = ?",
                        userId
                );
            }

            return true;
        } catch (Exception e) {
            logger.error("Error processing user status update", e);
            return false;
        }
    }

    /**
     * Processes a guild join event.
     */
    private boolean processGuildJoin(Map<String, Object> data) {
        try {
            String guildId = (String) data.get("id");
            String name = (String) data.get("name");
            String iconUrl = (String) data.get("iconUrl");
            String ownerId = (String) data.get("ownerId");
            Integer memberCount = null;

            if (data.containsKey("memberCount")) {
                memberCount = ((Number) data.get("memberCount")).intValue();
            }

            String description = (String) data.get("description");

            if (guildId == null || name == null) {
                logger.warn("Guild join event missing guild ID or name");
                return false;
            }

            // If we have an owner ID, ensure the owner exists
            if (ownerId != null) {
                ensureUserExists(ownerId);
            }

            // Insert or update guild
            jdbcTemplate.update(
                    "INSERT INTO guilds (id, name, icon_url, owner_id, member_count, description) " +
                            "VALUES (?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE name = VALUES(name), icon_url = VALUES(icon_url), " +
                            "owner_id = VALUES(owner_id), member_count = VALUES(member_count), description = VALUES(description)",
                    guildId, name, iconUrl, ownerId, memberCount, description
            );

            return true;
        } catch (Exception e) {
            logger.error("Error processing guild join", e);
            return false;
        }
    }

    /**
     * Processes a guild update event.
     */
    private boolean processGuildUpdate(Map<String, Object> data) {
        // Same as guild join
        return processGuildJoin(data);
    }

    /**
     * Processes a guild member join event.
     */
    private boolean processGuildMemberJoin(Map<String, Object> data) {
        try {
            String guildId = (String) data.get("guildId");
            String userId = null;
            String nickname = null;
            Long joinedAt = null;

            // Extract member data
            @SuppressWarnings("unchecked")
            Map<String, Object> memberData = (Map<String, Object>) data.get("member");

            if (memberData != null) {
                userId = (String) memberData.get("id");
                nickname = (String) memberData.get("nickname");

                if (memberData.containsKey("joinedAt")) {
                    joinedAt = ((Number) memberData.get("joinedAt")).longValue();
                }
            } else {
                // Try to get user data directly
                userId = (String) data.get("userId");

                if (data.containsKey("joinTime")) {
                    joinedAt = ((Number) data.get("joinTime")).longValue();
                }
            }

            if (guildId == null || userId == null) {
                logger.warn("Guild member join event missing guild ID or user ID");
                return false;
            }

            // Ensure guild exists
            ensureGuildExists(guildId, null);

            // Ensure user exists
            ensureUserExists(userId);

            // Insert or update guild member
            jdbcTemplate.update(
                    "INSERT INTO guild_members (user_id, guild_id, nickname, joined_at) " +
                            "VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), joined_at = VALUES(joined_at)",
                    userId, guildId, nickname, joinedAt
            );

            return true;
        } catch (Exception e) {
            logger.error("Error processing guild member join", e);
            return false;
        }
    }

    /**
     * Processes a guild member leave event.
     */
    private boolean processGuildMemberLeave(Map<String, Object> data) {
        try {
            String guildId = (String) data.get("guildId");
            String userId = (String) data.get("userId");

            if (guildId == null || userId == null) {
                logger.warn("Guild member leave event missing guild ID or user ID");
                return false;
            }

            // Delete guild member
            jdbcTemplate.update(
                    "DELETE FROM guild_members WHERE user_id = ? AND guild_id = ?",
                    userId, guildId
            );

            // Delete user roles for this guild
            jdbcTemplate.update(
                    "DELETE FROM user_roles WHERE user_id = ? AND guild_id = ?",
                    userId, guildId
            );

            return true;
        } catch (Exception e) {
            logger.error("Error processing guild member leave", e);
            return false;
        }
    }

    /**
     * Processes a channel create event.
     */
    private boolean processChannelCreate(Map<String, Object> data) {
        try {
            String channelId = (String) data.get("channelId");
            String guildId = (String) data.get("guildId");
            String name = (String) data.get("channelName");
            String type = (String) data.get("channelType");
            String topic = (String) data.get("topic");
            String parentId = (String) data.get("parentId");

            Integer position = null;
            if (data.containsKey("position")) {
                position = ((Number) data.get("position")).intValue();
            }

            Boolean nsfw = null;
            if (data.containsKey("nsfw")) {
                nsfw = (Boolean) data.get("nsfw");
            }

            if (channelId == null || type == null) {
                logger.warn("Channel create event missing channel ID or type");
                return false;
            }

            // If we have a guild ID, ensure it exists
            if (guildId != null) {
                ensureGuildExists(guildId, null);
            }

            // If we have a parent ID, ensure it exists
            if (parentId != null) {
                ensureChannelExists(parentId, null);
            }

            // Insert or update channel
            jdbcTemplate.update(
                    "INSERT INTO channels (id, guild_id, parent_id, name, type, topic, position, is_nsfw) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE guild_id = VALUES(guild_id), parent_id = VALUES(parent_id), " +
                            "name = VALUES(name), type = VALUES(type), topic = VALUES(topic), " +
                            "position = VALUES(position), is_nsfw = VALUES(is_nsfw)",
                    channelId, guildId, parentId, name, type, topic, position, nsfw
            );

            return true;
        } catch (Exception e) {
            logger.error("Error processing channel create", e);
            return false;
        }
    }

    /**
     * Processes a channel update event.
     */
    private boolean processChannelUpdate(Map<String, Object> data) {
        // Same as channel create
        return processChannelCreate(data);
    }

    /**
     * Processes a message received event.
     */
    private boolean processMessageReceived(Map<String, Object> data) {
        try {
            // Extract message data
            String messageId = (String) data.get("id");
            String channelId = (String) data.get("channelId");
            String content = (String) data.get("content");

            // Parse timestamp
            Long timestamp = null;
            if (data.containsKey("timestamp")) {
                timestamp = ((Number) data.get("timestamp")).longValue();
            } else {
                timestamp = System.currentTimeMillis();
            }

            // Parse edited timestamp
            Long editedTimestamp = null;
            if (data.containsKey("editedTimestamp")) {
                editedTimestamp = ((Number) data.get("editedTimestamp")).longValue();
            }

            // Get referenced message ID
            String referencedMessageId = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> referencedMessage = (Map<String, Object>) data.get("referencedMessage");
            if (referencedMessage != null) {
                referencedMessageId = (String) referencedMessage.get("messageId");
            }

            // Get author information
            String authorId = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> author = (Map<String, Object>) data.get("author");
            if (author != null) {
                authorId = (String) author.get("id");

                // Save author
                processUserUpdate(author);
            }

            if (messageId == null || channelId == null || authorId == null) {
                logger.warn("Message received event missing required data");
                return false;
            }

            // Ensure channel exists
            ensureChannelExists(channelId, null);

            // Insert or update message
            jdbcTemplate.update(
                    "INSERT INTO messages (id, channel_id, author_id, content, timestamp, edited_timestamp, referenced_message_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE content = VALUES(content), edited_timestamp = VALUES(edited_timestamp), " +
                            "referenced_message_id = VALUES(referenced_message_id)",
                    messageId, channelId, authorId, content, timestamp, editedTimestamp, referencedMessageId
            );

            // Process attachments
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attachments = (List<Map<String, Object>>) data.get("attachments");
            if (attachments != null) {
                for (Map<String, Object> attachment : attachments) {
                    String attachmentId = (String) attachment.get("id");
                    String filename = (String) attachment.get("filename");
                    String url = (String) attachment.get("url");
                    String contentType = (String) attachment.get("contentType");

                    Long size = null;
                    if (attachment.containsKey("size")) {
                        size = ((Number) attachment.get("size")).longValue();
                    }

                    if (attachmentId != null && filename != null && url != null) {
                        jdbcTemplate.update(
                                "INSERT INTO attachments (id, message_id, filename, url, content_type, size) " +
                                        "VALUES (?, ?, ?, ?, ?, ?) " +
                                        "ON DUPLICATE KEY UPDATE filename = VALUES(filename), url = VALUES(url), " +
                                        "content_type = VALUES(content_type), size = VALUES(size)",
                                attachmentId, messageId, filename, url, contentType, size
                        );
                    }
                }
            }

            // Process embeds
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> embeds = (List<Map<String, Object>>) data.get("embeds");
            if (embeds != null) {
                for (Map<String, Object> embed : embeds) {
                    String title = (String) embed.get("title");
                    String description = (String) embed.get("description");
                    String url = (String) embed.get("url");

                    Integer color;
                    if (embed.containsKey("color")) {
                        color = ((Number) embed.get("color")).intValue();
                    } else {
                        color = null;
                    }

                    Long embedTimestamp;
                    if (embed.containsKey("timestamp")) {
                        embedTimestamp = ((Number) embed.get("timestamp")).longValue();
                    } else {
                        embedTimestamp = null;
                    }

                    // Use KeyHolder to get generated keys
                    KeyHolder keyHolder = new GeneratedKeyHolder();

                    jdbcTemplate.update(connection -> {
                        PreparedStatement ps = connection.prepareStatement(
                                "INSERT INTO embeds (message_id, title, description, url, color, timestamp) " +
                                        "VALUES (?, ?, ?, ?, ?, ?)",
                                Statement.RETURN_GENERATED_KEYS
                        );
                        ps.setString(1, messageId);
                        ps.setString(2, title);
                        ps.setString(3, description);
                        ps.setString(4, url);
                        if (color != null) ps.setInt(5, color); else ps.setNull(5, java.sql.Types.INTEGER);
                        if (embedTimestamp != null) ps.setLong(6, embedTimestamp); else ps.setNull(6, java.sql.Types.BIGINT);
                        return ps;
                    }, keyHolder);

                    Number embedId = keyHolder.getKey();
                    if (embedId != null) {
                        // Process embed fields
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> fields = (List<Map<String, Object>>) embed.get("fields");
                        if (fields != null) {
                            int position = 0;
                            for (Map<String, Object> field : fields) {
                                String name = (String) field.get("name");
                                String value = (String) field.get("value");
                                Boolean inline = (Boolean) field.get("inline");

                                if (name != null && value != null) {
                                    jdbcTemplate.update(
                                            "INSERT INTO embed_fields (embed_id, name, value, is_inline, position) " +
                                                    "VALUES (?, ?, ?, ?, ?)",
                                            embedId.intValue(), name, value, inline != null && inline, position++
                                    );
                                }
                            }
                        }
                    }
                }
            }

            // If this is a DM and we have an author, update the DM channel
            if (!data.containsKey("guildId") && authorId != null) {
                boolean isDmFromBot = botUserId != null && authorId.equals(botUserId);
                String dmUserId;

                if (isDmFromBot) {
                    // If the message is from the bot, try to determine the recipient
                    @SuppressWarnings("unchecked")
                    Map<String, Object> recipient = (Map<String, Object>) data.get("recipient");
                    if (recipient != null) {
                        dmUserId = (String) recipient.get("id");
                    } else {
                        // Try to find the recipient from the database
                        try {
                            dmUserId = jdbcTemplate.queryForObject(
                                    "SELECT user_id FROM dm_channels WHERE id = ?",
                                    String.class,
                                    channelId
                            );
                        } catch (Exception e) {
                            logger.warn("Could not determine DM recipient for message: {}", messageId);
                            dmUserId = null;
                        }
                    }
                } else {
                    // If the message is from someone else, they are the DM user
                    dmUserId = authorId;
                }

                if (dmUserId != null) {
                    // Ensure user exists
                    ensureUserExists(dmUserId);

                    // Update or insert DM channel
                    jdbcTemplate.update(
                            "INSERT INTO dm_channels (id, user_id, last_message_id) " +
                                    "VALUES (?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), last_message_id = VALUES(last_message_id)",
                            channelId, dmUserId, messageId
                    );
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Error processing message received", e);
            return false;
        }
    }

    /**
     * Processes a message update event.
     */
    private boolean processMessageUpdate(Map<String, Object> data) {
        // Same as message received, with edited timestamp
        if (!data.containsKey("editedTimestamp")) {
            data.put("editedTimestamp", System.currentTimeMillis());
        }
        return processMessageReceived(data);
    }

    /**
     * Processes a message delete event.
     */
    private boolean processMessageDelete(Map<String, Object> data) {
        try {
            String messageId = (String) data.get("messageId");

            if (messageId == null) {
                logger.warn("Message delete event missing message ID");
                return false;
            }

            // Delete message (cascades to attachments, embeds, reactions)
            jdbcTemplate.update(
                    "DELETE FROM messages WHERE id = ?",
                    messageId
            );

            return true;
        } catch (Exception e) {
            logger.error("Error processing message delete", e);
            return false;
        }
    }

    /**
     * Processes a reaction add event.
     */
    private boolean processReactionAdd(Map<String, Object> data) {
        try {
            String messageId = (String) data.get("messageId");
            String userId = (String) data.get("userId");
            String emoji = (String) data.get("emoji");

            if (messageId == null || userId == null || emoji == null) {
                logger.warn("Reaction add event missing required data");
                return false;
            }

            // Ensure user exists
            ensureUserExists(userId);

            // Insert or ignore reaction
            jdbcTemplate.update(
                    "INSERT IGNORE INTO reactions (message_id, user_id, emoji) VALUES (?, ?, ?)",
                    messageId, userId, emoji
            );

            return true;
        } catch (Exception e) {
            logger.error("Error processing reaction add", e);
            return false;
        }
    }

    /**
     * Processes a reaction remove event.
     */
    private boolean processReactionRemove(Map<String, Object> data) {
        try {
            String messageId = (String) data.get("messageId");
            String userId = (String) data.get("userId");
            String emoji = (String) data.get("emoji");

            if (messageId == null || userId == null || emoji == null) {
                logger.warn("Reaction remove event missing required data");
                return false;
            }

            // Delete reaction
            jdbcTemplate.update(
                    "DELETE FROM reactions WHERE message_id = ? AND user_id = ? AND emoji = ?",
                    messageId, userId, emoji
            );

            return true;
        } catch (Exception e) {
            logger.error("Error processing reaction remove", e);
            return false;
        }
    }

    /**
     * Processes a typing start event.
     */
    private boolean processTypingStart(Map<String, Object> data) {
        try {
            String userId = (String) data.get("userId");
            String channelId = (String) data.get("channelId");

            if (userId == null || channelId == null) {
                logger.warn("Typing start event missing user ID or channel ID");
                return false;
            }

            // Insert or update typing indicator
            return saveTypingIndicator(userId, channelId);
        } catch (Exception e) {
            logger.error("Error processing typing start", e);
            return false;
        }
    }

    /**
     * Processes a role create event.
     */
    private boolean processRoleCreate(Map<String, Object> data) {
        try {
            String roleId = (String) data.get("roleId");
            String guildId = (String) data.get("guildId");
            String name = (String) data.get("roleName");

            Integer color = null;
            if (data.containsKey("roleColor")) {
                color = ((Number) data.get("roleColor")).intValue();
            }

            Integer position = null;
            if (data.containsKey("rolePosition")) {
                position = ((Number) data.get("rolePosition")).intValue();
            }

            Long permissions = null;
            if (data.containsKey("permissions")) {
                permissions = ((Number) data.get("permissions")).longValue();
            }

            Boolean mentionable = null;
            if (data.containsKey("mentionable")) {
                mentionable = (Boolean) data.get("mentionable");
            }

            Boolean hoisted = null;
            if (data.containsKey("hoisted")) {
                hoisted = (Boolean) data.get("hoisted");
            }

            if (roleId == null || guildId == null || name == null) {
                logger.warn("Role create event missing required data");
                return false;
            }

            // Ensure guild exists
            ensureGuildExists(guildId, null);

            // Insert or update role
            jdbcTemplate.update(
                    "INSERT INTO roles (id, guild_id, name, color, position, permissions, is_mentionable, is_hoisted) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE name = VALUES(name), color = VALUES(color), position = VALUES(position), " +
                            "permissions = VALUES(permissions), is_mentionable = VALUES(is_mentionable), is_hoisted = VALUES(is_hoisted)",
                    roleId, guildId, name, color, position, permissions, mentionable, hoisted
            );

            return true;
        } catch (Exception e) {
            logger.error("Error processing role create", e);
            return false;
        }
    }

    /**
     * Processes a role update event.
     */
    private boolean processRoleUpdate(Map<String, Object> data) {
        // Same as role create
        return processRoleCreate(data);
    }

    /**
     * Cleanup resources.
     */
    @PreDestroy
    public void cleanup() {
        // Cleanup method if needed
    }

    /**
     * Functional interface for event processors.
     */
    @FunctionalInterface
    private interface EventProcessor {
        boolean process(Map<String, Object> data);
    }
}