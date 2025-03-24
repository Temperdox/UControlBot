package com.cottonlesergal.ucontrolbot.api.controllers;

import com.cottonlesergal.ucontrolbot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for message-related API endpoints.
 */
@RestController
@RequestMapping("/api/channels/{channelId}/messages")
public class MessageController {
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final Bot bot;

    /**
     * Initializes the message controller with the specified bot instance.
     *
     * @param bot The bot instance
     */
    @Autowired
    public MessageController(Bot bot) {
        this.bot = bot;
    }

    /**
     * Gets messages from a specific channel.
     *
     * @param channelId Channel ID
     * @param limit Maximum number of messages to return
     * @param before Return messages before this message ID
     * @param after Return messages after this message ID
     * @return List of messages
     */
    @GetMapping
    public ResponseEntity<?> getMessages(
            @PathVariable String channelId,
            @RequestParam(required = false, defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after) {
        try {
            MessageChannel channel = getChannelById(channelId);
            if (channel == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Channel not found",
                                "message", "No channel found with ID: " + channelId
                        ));
            }

            // Fetch messages with optional pagination
            List<Message> messages;
            if (before != null && !before.isEmpty()) {
                messages = channel.getHistoryBefore(before, limit).complete().getRetrievedHistory();
            } else if (after != null && !after.isEmpty()) {
                messages = channel.getHistoryAfter(after, limit).complete().getRetrievedHistory();
            } else {
                messages = channel.getHistory().retrievePast(limit).complete();
            }

            List<Map<String, Object>> messageDataList = messages.stream()
                    .map(this::mapMessage)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(messageDataList);
        } catch (ErrorResponseException e) {
            logger.error("Discord error getting messages for channel: " + channelId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Discord API error",
                            "code", e.getErrorCode(),
                            "message", e.getMeaning()
                    ));
        } catch (Exception e) {
            logger.error("Error getting messages for channel: " + channelId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Gets a specific message from a channel.
     *
     * @param channelId Channel ID
     * @param messageId Message ID
     * @return Message information
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMessage(
            @PathVariable String channelId,
            @PathVariable String messageId) {
        try {
            MessageChannel channel = getChannelById(channelId);
            if (channel == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Channel not found",
                                "message", "No channel found with ID: " + channelId
                        ));
            }

            Message message = channel.retrieveMessageById(messageId).complete();
            Map<String, Object> messageData = mapMessage(message);

            return ResponseEntity.ok(messageData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error getting message: " + messageId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Discord API error",
                            "code", e.getErrorCode(),
                            "message", e.getMeaning()
                    ));
        } catch (Exception e) {
            logger.error("Error getting message: " + messageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Sends a message to a specific channel.
     *
     * @param channelId Channel ID
     * @param request Message request body
     * @return Sent message information
     */
    @PostMapping
    public ResponseEntity<?> sendMessage(
            @PathVariable String channelId,
            @RequestBody MessageRequest request) {
        try {
            MessageChannel channel = getChannelById(channelId);
            if (channel == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Channel not found",
                                "message", "No channel found with ID: " + channelId
                        ));
            }

            if ((request.getContent() == null || request.getContent().isEmpty()) &&
                    (request.getEmbed() == null)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "error", "Bad request",
                                "message", "Message must contain content or embed"
                        ));
            }

            // Build message
            MessageCreateBuilder messageBuilder = new MessageCreateBuilder();

            // Add content if present
            if (request.getContent() != null && !request.getContent().isEmpty()) {
                messageBuilder.setContent(request.getContent());
            }

            // Add embed if present
            if (request.getEmbed() != null) {
                MessageEmbed embed = buildEmbed(request.getEmbed());
                messageBuilder.addEmbeds(embed);
            }

            // Set message flags if needed
            if (request.isSuppressEmbeds()) {
                messageBuilder.setSuppressEmbeds(true);
            }

            // Send message
            MessageCreateData messageData = messageBuilder.build();
            Message message = channel.sendMessage(messageData).complete();

            // Return created message
            Map<String, Object> responseData = mapMessage(message);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error sending message to channel: " + channelId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Discord API error",
                            "code", e.getErrorCode(),
                            "message", e.getMeaning()
                    ));
        } catch (Exception e) {
            logger.error("Error sending message to channel: " + channelId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Edits a message in a specific channel.
     *
     * @param channelId Channel ID
     * @param messageId Message ID
     * @param request Message edit request
     * @return Edited message information
     */
    @PatchMapping("/{messageId}")
    public ResponseEntity<?> editMessage(
            @PathVariable String channelId,
            @PathVariable String messageId,
            @RequestBody MessageRequest request) {
        try {
            MessageChannel channel = getChannelById(channelId);
            if (channel == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Channel not found",
                                "message", "No channel found with ID: " + channelId
                        ));
            }

            // Check if we're trying to edit our own message
            Message message = channel.retrieveMessageById(messageId).complete();
            if (!message.getAuthor().getId().equals(bot.getJda().getSelfUser().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "error", "Forbidden",
                                "message", "Cannot edit messages from other users"
                        ));
            }

            // Build edited message
            if (request.getContent() != null) {
                message = message.editMessage(request.getContent()).complete();
            }

            // Return updated message
            Map<String, Object> responseData = mapMessage(message);
            return ResponseEntity.ok(responseData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error editing message: " + messageId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Discord API error",
                            "code", e.getErrorCode(),
                            "message", e.getMeaning()
                    ));
        } catch (Exception e) {
            logger.error("Error editing message: " + messageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Deletes a message from a specific channel.
     *
     * @param channelId Channel ID
     * @param messageId Message ID
     * @return Success response
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String channelId,
            @PathVariable String messageId) {
        try {
            MessageChannel channel = getChannelById(channelId);
            if (channel == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Channel not found",
                                "message", "No channel found with ID: " + channelId
                        ));
            }

            // Try to get the message first to verify it exists
            Message message = channel.retrieveMessageById(messageId).complete();

            // Check if we can delete this message
            boolean canDelete = message.getAuthor().getId().equals(bot.getJda().getSelfUser().getId());
            if (!canDelete && channel instanceof GuildMessageChannel) {
                GuildMessageChannel guildChannel = (GuildMessageChannel) channel;
                Guild guild = guildChannel.getGuild();
                Member selfMember = guild.getSelfMember();
                canDelete = selfMember.hasPermission(net.dv8tion.jda.api.Permission.MESSAGE_MANAGE);
            }

            if (!canDelete) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "error", "Forbidden",
                                "message", "Cannot delete this message due to permissions"
                        ));
            }

            // Delete message
            message.delete().complete();

            // Return success with no content
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (ErrorResponseException e) {
            logger.error("Discord error deleting message: " + messageId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Discord API error",
                            "code", e.getErrorCode(),
                            "message", e.getMeaning()
                    ));
        } catch (Exception e) {
            logger.error("Error deleting message: " + messageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Gets a channel by ID, attempting to resolve it as any valid message channel type.
     *
     * @param channelId The channel ID
     * @return The message channel, or null if not found
     */
    private MessageChannel getChannelById(String channelId) {
        // Try as text channel
        TextChannel textChannel = bot.getJda().getTextChannelById(channelId);
        if (textChannel != null) {
            return textChannel;
        }

        // Try as thread channel
        ThreadChannel threadChannel = bot.getJda().getThreadChannelById(channelId);
        if (threadChannel != null) {
            return threadChannel;
        }

        // Try as private channel
        return bot.getJda().getPrivateChannelById(channelId);
    }

    /**
     * Maps a JDA Message to a Map.
     *
     * @param message The JDA Message
     * @return Map containing message data
     */
    private Map<String, Object> mapMessage(Message message) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", message.getId());
        messageData.put("content", message.getContentRaw());
        messageData.put("channelId", message.getChannel().getId());

        // Add timestamps
        messageData.put("timestamp", message.getTimeCreated().toInstant().toEpochMilli());
        if (message.getTimeEdited() != null) {
            messageData.put("editedTimestamp", message.getTimeEdited().toInstant().toEpochMilli());
        }

        // Add author information
        User author = message.getAuthor();
        Map<String, Object> authorData = new HashMap<>();
        authorData.put("id", author.getId());
        authorData.put("username", author.getName());
        authorData.put("discriminator", author.getDiscriminator());
        authorData.put("avatarUrl", author.getEffectiveAvatarUrl());
        authorData.put("bot", author.isBot());
        messageData.put("author", authorData);

        // Add member information if available
        if (message.getMember() != null) {
            Member member = message.getMember();
            Map<String, Object> memberData = new HashMap<>();
            memberData.put("nickname", member.getNickname());
            memberData.put("joinedAt", member.getTimeJoined().toInstant().toEpochMilli());
            memberData.put("roles", member.getRoles().stream()
                    .map(role -> Map.of(
                            "id", role.getId(),
                            "name", role.getName(),
                            "color", role.getColorRaw()
                    ))
                    .collect(Collectors.toList()));
            messageData.put("member", memberData);
        }

        // Add mentions
        messageData.put("mentions", message.getMentions().getUsers().stream()
                .map(user -> {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("id", user.getId());
                    userData.put("username", user.getName());
                    userData.put("discriminator", user.getDiscriminator());
                    userData.put("avatarUrl", user.getEffectiveAvatarUrl());
                    userData.put("bot", user.isBot());
                    return userData;
                })
                .collect(Collectors.toList()));

        // Add mentioned roles
        messageData.put("mentionedRoles", message.getMentions().getRoles().stream()
                .map(role -> {
                    Map<String, Object> roleData = new HashMap<>();
                    roleData.put("id", role.getId());
                    roleData.put("name", role.getName());
                    roleData.put("color", role.getColorRaw());
                    return roleData;
                })
                .collect(Collectors.toList()));

        // Add mentioned channels
        messageData.put("mentionedChannels", message.getMentions().getChannels().stream()
                .map(channel -> {
                    Map<String, Object> channelData = new HashMap<>();
                    channelData.put("id", channel.getId());
                    channelData.put("name", channel.getName());
                    channelData.put("type", channel.getType().name());
                    return channelData;
                })
                .collect(Collectors.toList()));

        // Add embeds
        messageData.put("embeds", message.getEmbeds().stream()
                .map(this::mapEmbed)
                .collect(Collectors.toList()));

        // Add attachments
        messageData.put("attachments", message.getAttachments().stream()
                .map(attachment -> {
                    Map<String, Object> attachmentData = new HashMap<>();
                    attachmentData.put("id", attachment.getId());
                    attachmentData.put("filename", attachment.getFileName());
                    attachmentData.put("size", attachment.getSize());
                    attachmentData.put("url", attachment.getUrl());
                    if (attachment.getContentType() != null) {
                        attachmentData.put("contentType", attachment.getContentType());
                    } else {
                        attachmentData.put("contentType", "unknown");
                    }
                    return attachmentData;
                })
                .collect(Collectors.toList()));

        // Add reactions
        messageData.put("reactions", message.getReactions().stream()
                .map(reaction -> {
                    Map<String, Object> reactionData = new HashMap<>();
                    reactionData.put("emoji", reaction.getEmoji().getAsReactionCode());
                    reactionData.put("count", reaction.getCount());
                    reactionData.put("me", reaction.isSelf());
                    return reactionData;
                })
                .collect(Collectors.toList()));

        // Add message flags
        messageData.put("flags", message.getFlags().stream()
                .map(Message.MessageFlag::name)
                .collect(Collectors.toList()));

        // Add reply data if this is a reply
        if (message.getReferencedMessage() != null) {
            Message referencedMessage = message.getReferencedMessage();
            Map<String, Object> reference = new HashMap<>();
            reference.put("messageId", referencedMessage.getId());
            reference.put("channelId", referencedMessage.getChannel().getId());
            reference.put("authorId", referencedMessage.getAuthor().getId());
            messageData.put("referencedMessage", reference);
        }

        return messageData;
    }

    /**
     * Maps a JDA MessageEmbed to a Map.
     *
     * @param embed The JDA MessageEmbed
     * @return Map containing embed data
     */
    private Map<String, Object> mapEmbed(MessageEmbed embed) {
        Map<String, Object> embedData = new HashMap<>();

        // Add basic properties
        if (embed.getTitle() != null) embedData.put("title", embed.getTitle());
        if (embed.getDescription() != null) embedData.put("description", embed.getDescription());
        if (embed.getUrl() != null) embedData.put("url", embed.getUrl());
        if (embed.getColor() != null) embedData.put("color", embed.getColor().getRGB());
        if (embed.getTimestamp() != null) embedData.put("timestamp", embed.getTimestamp().toInstant().toEpochMilli());

        // Add author
        if (embed.getAuthor() != null) {
            Map<String, Object> author = new HashMap<>();
            author.put("name", embed.getAuthor().getName());
            if (embed.getAuthor().getUrl() != null) author.put("url", embed.getAuthor().getUrl());
            if (embed.getAuthor().getIconUrl() != null) author.put("iconUrl", embed.getAuthor().getIconUrl());
            embedData.put("author", author);
        }

        // Add footer
        if (embed.getFooter() != null) {
            Map<String, Object> footer = new HashMap<>();
            footer.put("text", embed.getFooter().getText());
            if (embed.getFooter().getIconUrl() != null) footer.put("iconUrl", embed.getFooter().getIconUrl());
            embedData.put("footer", footer);
        }

        // Add image
        if (embed.getImage() != null) {
            Map<String, Object> image = new HashMap<>();
            image.put("url", embed.getImage().getUrl());
            embedData.put("image", image);
        }

        // Add thumbnail
        if (embed.getThumbnail() != null) {
            Map<String, Object> thumbnail = new HashMap<>();
            thumbnail.put("url", embed.getThumbnail().getUrl());
            embedData.put("thumbnail", thumbnail);
        }

        // Add fields
        if (!embed.getFields().isEmpty()) {
            List<Map<String, Object>> fields = embed.getFields().stream()
                    .map(field -> {
                        Map<String, Object> fieldData = new HashMap<>();
                        fieldData.put("name", field.getName());
                        fieldData.put("value", field.getValue());
                        fieldData.put("inline", field.isInline());
                        return fieldData;
                    })
                    .collect(Collectors.toList());
            embedData.put("fields", fields);
        }

        return embedData;
    }

    /**
     * Builds a MessageEmbed from a map of properties.
     *
     * @param embedJson The embed properties map
     * @return The built MessageEmbed
     */
    private MessageEmbed buildEmbed(Map<String, Object> embedJson) {
        net.dv8tion.jda.api.EmbedBuilder builder = new net.dv8tion.jda.api.EmbedBuilder();

        // Set basic properties
        if (embedJson.containsKey("title")) builder.setTitle((String) embedJson.get("title"));
        if (embedJson.containsKey("description")) builder.setDescription((String) embedJson.get("description"));
        if (embedJson.containsKey("url")) builder.setUrl((String) embedJson.get("url"));
        if (embedJson.containsKey("color")) builder.setColor((Integer) embedJson.get("color"));
        if (embedJson.containsKey("timestamp")) {
            try {
                long timestamp = (Long) embedJson.get("timestamp");
                builder.setTimestamp(OffsetDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(timestamp),
                        java.time.ZoneId.systemDefault()
                ));
            } catch (Exception e) {
                logger.warn("Invalid timestamp format, using current time", e);
                builder.setTimestamp(OffsetDateTime.now().withNano(0)); // Set to current time as fallback
            }
        }

        // Set author
        if (embedJson.containsKey("author") && embedJson.get("author") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> authorJson = (Map<String, Object>) embedJson.get("author");
            String name = authorJson.containsKey("name") ? (String) authorJson.get("name") : "";
            String url = authorJson.containsKey("url") ? (String) authorJson.get("url") : null;
            String iconUrl = authorJson.containsKey("iconUrl") ? (String) authorJson.get("iconUrl") : null;
            builder.setAuthor(name, url, iconUrl);
        }

        // Set footer
        if (embedJson.containsKey("footer") && embedJson.get("footer") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> footerJson = (Map<String, Object>) embedJson.get("footer");
            String text = footerJson.containsKey("text") ? (String) footerJson.get("text") : "";
            String iconUrl = footerJson.containsKey("iconUrl") ? (String) footerJson.get("iconUrl") : null;
            builder.setFooter(text, iconUrl);
        }

        // Set image
        if (embedJson.containsKey("image") && embedJson.get("image") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> imageJson = (Map<String, Object>) embedJson.get("image");
            if (imageJson.containsKey("url")) builder.setImage((String) imageJson.get("url"));
        }

        // Set thumbnail
        if (embedJson.containsKey("thumbnail") && embedJson.get("thumbnail") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> thumbnailJson = (Map<String, Object>) embedJson.get("thumbnail");
            if (thumbnailJson.containsKey("url")) builder.setThumbnail((String) thumbnailJson.get("url"));
        }

        // Add fields
        if (embedJson.containsKey("fields") && embedJson.get("fields") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = (List<Map<String, Object>>) embedJson.get("fields");
            for (Map<String, Object> fieldJson : fields) {
                String name = fieldJson.containsKey("name") ? (String) fieldJson.get("name") : "";
                String value = fieldJson.containsKey("value") ? (String) fieldJson.get("value") : "";
                boolean inline = fieldJson.containsKey("inline") && (Boolean) fieldJson.get("inline");
                builder.addField(name, value, inline);
            }
        }

        return builder.build();
    }

    /**
     * Message request class for sending and editing messages.
     */
    public static class MessageRequest {
        private String content;
        private Map<String, Object> embed;
        private boolean suppressEmbeds;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Map<String, Object> getEmbed() {
            return embed;
        }

        public void setEmbed(Map<String, Object> embed) {
            this.embed = embed;
        }

        public boolean isSuppressEmbeds() {
            return suppressEmbeds;
        }

        public void setSuppressEmbeds(boolean suppressEmbeds) {
            this.suppressEmbeds = suppressEmbeds;
        }
    }
}