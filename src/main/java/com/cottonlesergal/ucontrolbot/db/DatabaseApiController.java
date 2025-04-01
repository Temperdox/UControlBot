package com.cottonlesergal.ucontrolbot.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for database API endpoints.
 * This controller provides direct database access endpoints.
 */
@RestController
@RequestMapping("/api/db")
public class DatabaseApiController {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseApiController.class);

    @Autowired
    private DatabaseManager dbManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Gets all users from the database.
     */
    /*@GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        logger.info("Database request for users, limit={}, name={}", limit, name);
        try {
            List<Map<String, Object>> users = dbManager.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching users from database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }*/

    /**
     * Gets a specific user from the database.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        logger.info("Database request for user {}", userId);
        try {
            String query = "SELECT * FROM users WHERE id = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(query, userId);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            return ResponseEntity.ok(result.get(0));
        } catch (Exception e) {
            logger.error("Error fetching user from database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Creates or updates a user in the database.
     */
    @PostMapping("/users")
    public ResponseEntity<?> saveUser(@RequestBody Map<String, Object> userData) {
        logger.info("Database request to save user: {}", userData);
        try {
            String userId = (String) userData.get("id");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "User ID is required"));
            }

            String username = (String) userData.get("username");
            if (username == null) {
                username = "Unknown User";
            }

            String discriminator = (String) userData.get("discriminator");
            String avatarUrl = (String) userData.get("avatarUrl");
            Boolean isBot = (Boolean) userData.get("isBot");
            Boolean isOwner = (Boolean) userData.get("isOwner");
            String status = (String) userData.get("status");
            if (status == null) {
                status = "offline";
            }

            // Check if user exists
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE id = ?",
                    Integer.class,
                    userId
            );

            if (count > 0) {
                // Update user
                jdbcTemplate.update(
                        "UPDATE users SET username = ?, discriminator = ?, avatar_url = ?, " +
                                "is_bot = ?, is_owner = ?, status = ? WHERE id = ?",
                        username, discriminator, avatarUrl,
                        isBot != null ? isBot : false,
                        isOwner != null ? isOwner : false,
                        status, userId
                );
            } else {
                // Insert user
                jdbcTemplate.update(
                        "INSERT INTO users (id, username, discriminator, avatar_url, is_bot, is_owner, status) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        userId, username, discriminator, avatarUrl,
                        isBot != null ? isBot : false,
                        isOwner != null ? isOwner : false,
                        status
                );
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("id", userId, "status", "success"));
        } catch (Exception e) {
            logger.error("Error saving user to database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Updates a user's status in the database.
     */
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable String userId,
            @RequestBody Map<String, Object> statusData) {
        logger.info("Database request to update status for user {}: {}", userId, statusData);
        try {
            String status = (String) statusData.get("status");
            if (status == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Status is required"));
            }

            // Check if user exists
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE id = ?",
                    Integer.class,
                    userId
            );

            if (count == 0) {
                // Create minimalistic user entry
                jdbcTemplate.update(
                        "INSERT INTO users (id, username, status) VALUES (?, ?, ?)",
                        userId, "Unknown User", status
                );
            } else {
                // Update user status
                jdbcTemplate.update(
                        "UPDATE users SET status = ? WHERE id = ?",
                        status, userId
                );
            }

            return ResponseEntity.ok(Map.of("id", userId, "status", status));
        } catch (Exception e) {
            logger.error("Error updating user status in database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Gets all guilds from the database.
     */
    /*@GetMapping("/guilds")
    public ResponseEntity<?> getGuilds() {
        logger.info("Database request for guilds");
        try {
            List<Map<String, Object>> guilds = dbManager.getAllGuilds();
            return ResponseEntity.ok(guilds);
        } catch (Exception e) {
            logger.error("Error fetching guilds from database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }*/

    /**
     * Creates or updates a guild in the database.
     */
    @PostMapping("/guilds")
    public ResponseEntity<?> saveGuild(@RequestBody Map<String, Object> guildData) {
        logger.info("Database request to save guild: {}", guildData);
        try {
            String guildId = (String) guildData.get("id");
            if (guildId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Guild ID is required"));
            }

            String name = (String) guildData.get("name");
            if (name == null) {
                name = "Unknown Guild";
            }

            String iconUrl = (String) guildData.get("iconUrl");
            String ownerId = (String) guildData.get("ownerId");
            Integer memberCount = null;

            if (guildData.containsKey("memberCount")) {
                if (guildData.get("memberCount") instanceof Integer) {
                    memberCount = (Integer) guildData.get("memberCount");
                } else if (guildData.get("memberCount") instanceof Double) {
                    memberCount = ((Double) guildData.get("memberCount")).intValue();
                }
            }

            String description = (String) guildData.get("description");

            // Check if guild exists
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM guilds WHERE id = ?",
                    Integer.class,
                    guildId
            );

            if (count > 0) {
                // Update guild
                jdbcTemplate.update(
                        "UPDATE guilds SET name = ?, icon_url = ?, owner_id = ?, " +
                                "member_count = ?, description = ? WHERE id = ?",
                        name, iconUrl, ownerId, memberCount, description, guildId
                );
            } else {
                // Insert guild
                jdbcTemplate.update(
                        "INSERT INTO guilds (id, name, icon_url, owner_id, member_count, description) " +
                                "VALUES (?, ?, ?, ?, ?, ?)",
                        guildId, name, iconUrl, ownerId, memberCount, description
                );
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("id", guildId, "status", "success"));
        } catch (Exception e) {
            logger.error("Error saving guild to database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Creates or updates a channel in the database.
     */
    @PostMapping("/channels")
    public ResponseEntity<?> saveChannel(@RequestBody Map<String, Object> channelData) {
        logger.info("Database request to save channel: {}", channelData);
        try {
            String channelId = (String) channelData.get("id");
            if (channelId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Channel ID is required"));
            }

            String name = (String) channelData.get("name");
            if (name == null) {
                name = "Unknown Channel";
            }

            String type = (String) channelData.get("type");
            if (type == null) {
                type = "UNKNOWN";
            }

            String guildId = (String) channelData.get("guildId");
            String parentId = (String) channelData.get("parentId");
            String topic = (String) channelData.get("topic");

            Integer position = null;
            if (channelData.containsKey("position")) {
                if (channelData.get("position") instanceof Integer) {
                    position = (Integer) channelData.get("position");
                } else if (channelData.get("position") instanceof Double) {
                    position = ((Double) channelData.get("position")).intValue();
                }
            }

            Boolean nsfw = (Boolean) channelData.get("nsfw");

            // Check if channel exists
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM channels WHERE id = ?",
                    Integer.class,
                    channelId
            );

            if (count > 0) {
                // Update channel
                jdbcTemplate.update(
                        "UPDATE channels SET guild_id = ?, parent_id = ?, name = ?, " +
                                "type = ?, topic = ?, position = ?, is_nsfw = ? WHERE id = ?",
                        guildId, parentId, name, type, topic,
                        position != null ? position : 0,
                        nsfw != null ? nsfw : false,
                        channelId
                );
            } else {
                // Insert channel
                jdbcTemplate.update(
                        "INSERT INTO channels (id, guild_id, parent_id, name, type, topic, position, is_nsfw) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        channelId, guildId, parentId, name, type, topic,
                        position != null ? position : 0,
                        nsfw != null ? nsfw : false
                );
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("id", channelId, "status", "success"));
        } catch (Exception e) {
            logger.error("Error saving channel to database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Gets messages from a channel from the database.
     */
    /*@GetMapping("/messages/{channelId}")
    public ResponseEntity<?> getMessages(
            @PathVariable String channelId,
            @RequestParam(required = false, defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after) {
        logger.info("Database request for messages in channel {}, limit={}", channelId, limit);
        try {
            List<Map<String, Object>> messages = dbManager.getMessagesByChannelId(channelId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("Error fetching messages from database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }*/

    /**
     * Saves a message to the database.
     * This is a key endpoint for ensuring messages are saved properly.
     */
    @PostMapping("/messages")
    public ResponseEntity<?> saveMessage(@RequestBody Map<String, Object> messageData) {
        logger.info("Database request to save message: {}", messageData);
        try {
            // Extract message ID
            String messageId = (String) messageData.get("id");
            if (messageId == null) {
                // Generate ID if not provided
                messageId = "msg-" + System.currentTimeMillis();
                messageData.put("id", messageId);
            }

            // Extract required fields
            String channelId = (String) messageData.get("channelId");
            if (channelId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Channel ID is required"));
            }

            String content = (String) messageData.get("content");
            if (content == null) {
                content = "";
            }

            // Get author information
            String authorId = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> author = (Map<String, Object>) messageData.get("author");
            if (author != null) {
                authorId = (String) author.get("id");

                // Save author to users table if needed
                if (authorId != null) {
                    saveUser(author);
                }
            }

            if (authorId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Author ID is required"));
            }

            // Parse timestamp
            Long timestamp = null;
            if (messageData.containsKey("timestamp")) {
                if (messageData.get("timestamp") instanceof Long) {
                    timestamp = (Long) messageData.get("timestamp");
                } else if (messageData.get("timestamp") instanceof Double) {
                    timestamp = ((Double) messageData.get("timestamp")).longValue();
                } else if (messageData.get("timestamp") instanceof String) {
                    timestamp = Long.parseLong((String) messageData.get("timestamp"));
                }
            }

            if (timestamp == null) {
                timestamp = System.currentTimeMillis();
            }

            // Parse edited timestamp
            Long editedTimestamp = null;
            if (messageData.containsKey("editedTimestamp")) {
                if (messageData.get("editedTimestamp") instanceof Long) {
                    editedTimestamp = (Long) messageData.get("editedTimestamp");
                } else if (messageData.get("editedTimestamp") instanceof Double) {
                    editedTimestamp = ((Double) messageData.get("editedTimestamp")).longValue();
                } else if (messageData.get("editedTimestamp") instanceof String) {
                    editedTimestamp = Long.parseLong((String) messageData.get("editedTimestamp"));
                }
            }

            // Get referenced message ID
            String referencedMessageId = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> referencedMessage = (Map<String, Object>) messageData.get("referencedMessage");
            if (referencedMessage != null) {
                referencedMessageId = (String) referencedMessage.get("messageId");
            }

            // Check if message exists
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM messages WHERE id = ?",
                    Integer.class,
                    messageId
            );

            // Start transaction
            jdbcTemplate.execute("BEGIN");

            try {
                if (count > 0) {
                    // Update existing message
                    jdbcTemplate.update(
                            "UPDATE messages SET content = ?, edited_timestamp = ?, referenced_message_id = ? WHERE id = ?",
                            content, editedTimestamp, referencedMessageId, messageId
                    );

                    // Delete existing attachments and embeds to replace them
                    jdbcTemplate.update("DELETE FROM attachments WHERE message_id = ?", messageId);

                    // Get embed IDs to delete fields
                    List<Integer> embedIds = jdbcTemplate.queryForList(
                            "SELECT id FROM embeds WHERE message_id = ?",
                            Integer.class, messageId
                    );

                    // Delete embed fields
                    for (Integer embedId : embedIds) {
                        jdbcTemplate.update("DELETE FROM embed_fields WHERE embed_id = ?", embedId);
                    }

                    // Delete embeds
                    jdbcTemplate.update("DELETE FROM embeds WHERE message_id = ?", messageId);
                } else {
                    // Insert new message
                    jdbcTemplate.update(
                            "INSERT INTO messages (id, channel_id, author_id, content, timestamp, edited_timestamp, referenced_message_id) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                            messageId, channelId, authorId, content, timestamp, editedTimestamp, referencedMessageId
                    );
                }

                // Process attachments
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> attachments = (List<Map<String, Object>>) messageData.get("attachments");
                if (attachments != null && !attachments.isEmpty()) {
                    for (Map<String, Object> attachment : attachments) {
                        String attachmentId = (String) attachment.get("id");
                        if (attachmentId == null) {
                            attachmentId = "att-" + System.currentTimeMillis() + "-" + Math.random();
                        }

                        String filename = (String) attachment.get("filename");
                        String url = (String) attachment.get("url");
                        String contentType = (String) attachment.get("contentType");

                        Long size = null;
                        if (attachment.containsKey("size")) {
                            if (attachment.get("size") instanceof Long) {
                                size = (Long) attachment.get("size");
                            } else if (attachment.get("size") instanceof Double) {
                                size = ((Double) attachment.get("size")).longValue();
                            } else if (attachment.get("size") instanceof String) {
                                size = Long.parseLong((String) attachment.get("size"));
                            }
                        }

                        jdbcTemplate.update(
                                "INSERT INTO attachments (id, message_id, filename, url, content_type, size) " +
                                        "VALUES (?, ?, ?, ?, ?, ?)",
                                attachmentId, messageId, filename, url, contentType, size
                        );
                    }
                }

                // Process embeds
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> embeds = (List<Map<String, Object>>) messageData.get("embeds");
                if (embeds != null && !embeds.isEmpty()) {
                    for (Map<String, Object> embed : embeds) {
                        String title = (String) embed.get("title");
                        String description = (String) embed.get("description");
                        String url = (String) embed.get("url");

                        Integer color = null;
                        if (embed.containsKey("color")) {
                            if (embed.get("color") instanceof Integer) {
                                color = (Integer) embed.get("color");
                            } else if (embed.get("color") instanceof Double) {
                                color = ((Double) embed.get("color")).intValue();
                            } else if (embed.get("color") instanceof String) {
                                color = Integer.parseInt((String) embed.get("color"));
                            }
                        }

                        Long embedTimestamp = null;
                        if (embed.containsKey("timestamp")) {
                            if (embed.get("timestamp") instanceof Long) {
                                embedTimestamp = (Long) embed.get("timestamp");
                            } else if (embed.get("timestamp") instanceof Double) {
                                embedTimestamp = ((Double) embed.get("timestamp")).longValue();
                            } else if (embed.get("timestamp") instanceof String) {
                                embedTimestamp = Long.parseLong((String) embed.get("timestamp"));
                            }
                        }

                        // Insert embed and get its ID
                        jdbcTemplate.update(
                                "INSERT INTO embeds (message_id, title, description, url, color, timestamp) " +
                                        "VALUES (?, ?, ?, ?, ?, ?)",
                                messageId, title, description, url, color, embedTimestamp
                        );

                        // Get the ID of the newly inserted embed
                        Integer embedId = jdbcTemplate.queryForObject(
                                "SELECT LAST_INSERT_ID()", Integer.class
                        );

                        // Process embed fields
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> fields = (List<Map<String, Object>>) embed.get("fields");
                        if (fields != null && !fields.isEmpty()) {
                            for (int i = 0; i < fields.size(); i++) {
                                Map<String, Object> field = fields.get(i);
                                String name = (String) field.get("name");
                                String value = (String) field.get("value");
                                Boolean inline = (Boolean) field.get("inline");

                                jdbcTemplate.update(
                                        "INSERT INTO embed_fields (embed_id, name, value, is_inline, position) " +
                                                "VALUES (?, ?, ?, ?, ?)",
                                        embedId, name, value, inline != null ? inline : false, i
                                );
                            }
                        }
                    }
                }

                // If this is a DM, update the DM channel record
                if (messageData.containsKey("isDm") && (Boolean) messageData.get("isDm")) {
                    String dmUserId = authorId;
                    // If the message is from the bot, use the recipient ID as DM user
                    if (messageData.containsKey("fromBot") && (Boolean) messageData.get("fromBot")) {
                        String recipientId = (String) messageData.get("recipientId");
                        if (recipientId != null) {
                            dmUserId = recipientId;
                        }
                    }

                    int dmExists = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM dm_channels WHERE id = ?",
                            Integer.class,
                            channelId
                    );

                    if (dmExists > 0) {
                        jdbcTemplate.update(
                                "UPDATE dm_channels SET user_id = ?, last_message_id = ? WHERE id = ?",
                                dmUserId, messageId, channelId
                        );
                    } else {
                        jdbcTemplate.update(
                                "INSERT INTO dm_channels (id, user_id, last_message_id) VALUES (?, ?, ?)",
                                channelId, dmUserId, messageId
                        );
                    }
                }

                // Commit transaction
                jdbcTemplate.execute("COMMIT");

                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("id", messageId, "status", "success"));
            } catch (Exception e) {
                // Rollback transaction on error
                jdbcTemplate.execute("ROLLBACK");
                throw e;
            }
        } catch (Exception e) {
            logger.error("Error saving message to database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}