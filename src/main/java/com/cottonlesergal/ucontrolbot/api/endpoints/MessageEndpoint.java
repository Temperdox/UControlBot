package com.cottonlesergal.ucontrolbot.api.endpoints;

import com.cottonlesergal.ucontrolbot.Bot;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
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

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoint for message-related API requests.
 */
public class MessageEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(MessageEndpoint.class);
    private final Bot bot;
    private final Gson gson = new Gson();

    /**
     * Initializes the message endpoint with the specified bot instance.
     *
     * @param bot The bot instance
     */
    public MessageEndpoint(Bot bot) {
        this.bot = bot;
    }

    /**
     * Gets messages from a specific channel.
     *
     * @param ctx The HTTP context
     */
    public void getMessages(Context ctx) {
        String channelId = ctx.pathParam("channelId");

        // Pagination parameters
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(50);
        String before = ctx.queryParam("before");
        String after = ctx.queryParam("after");

        try {
            MessageChannel channel = getChannelById(channelId);
            if (channel == null) {
                ctx.status(404).json(Map.of(
                        "error", "Channel not found",
                        "message", "No channel found with ID: " + channelId
                ));
                return;
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

            ctx.json(messageDataList);
        } catch (ErrorResponseException e) {
            logger.error("Discord error getting messages for channel: " + channelId, e);
            ctx.status(e.getErrorCode()).json(Map.of(
                    "error", "Discord API error",
                    "code", e.getErrorCode(),
                    "message", e.getMeaning()
            ));
        } catch (Exception e) {
            logger.error("Error getting messages for channel: " + channelId, e);
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Gets a specific message from a channel.
     *
     * @param ctx The HTTP context
     */
    public void getMessage(Context ctx) {
        String channelId = ctx.pathParam("channelId");
        String messageId = ctx.pathParam("messageId");

        try {
            MessageChannel channel = getChannelById(channelId);
            if (channel == null) {
                ctx.status(404).json(Map.of(
                        "error", "Channel not found",
                        "message", "No channel found with ID: " + channelId
                ));
                return;
            }

            Message message = channel.retrieveMessageById(messageId).complete();
            Map<String, Object> messageData = mapMessage(message);

            ctx.json(messageData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error getting message: " + messageId, e);
            ctx.status(e.getErrorCode()).json(Map.of(
                    "error", "Discord API error",
                    "code", e.getErrorCode(),
                    "message", e.getMeaning()
            ));
        } catch (Exception e) {
            logger.error("Error getting message: " + messageId, e);
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Sends a message to a specific channel.
     *
     * @param ctx The HTTP context
     */
    public void sendMessage(Context ctx) {
        String channelId = ctx.pathParam("channelId");

        try {
            MessageChannel channel = getChannelById(channelId);
            if (channel == null) {
                ctx.status(404).json(Map.of(
                        "error", "Channel not found",
                        "message", "No channel found with ID: " + channelId
                ));
                return;
            }

            // Parse request body
            JsonObject requestBody = gson.fromJson(ctx.body(), JsonObject.class);

            if (!requestBody.has("content") && !requestBody.has("embed")) {
                ctx.status(400).json(Map.of(
                        "error", "Bad request",
                        "message", "Message must contain content or embed"
                ));
                return;
            }

            // Build message
            MessageCreateBuilder messageBuilder = new MessageCreateBuilder();

            // Add content if present
            if (requestBody.has("content")) {
                messageBuilder.setContent(requestBody.get("content").getAsString());
            }

            // Add embed if present
            if (requestBody.has("embed")) {
                JsonObject embedJson = requestBody.getAsJsonObject("embed");
                MessageEmbed embed = buildEmbed(embedJson);
                messageBuilder.addEmbeds(embed);
            }

            // Set message flags if needed
            if (requestBody.has("suppressEmbeds") && requestBody.get("suppressEmbeds").getAsBoolean()) {
                messageBuilder.setSuppressEmbeds(true);
            }

            // Send message
            MessageCreateData messageData = messageBuilder.build();
            Message message = channel.sendMessage(messageData).complete();

            // Return created message
            Map<String, Object> responseData = mapMessage(message);
            ctx.status(201).json(responseData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error sending message to channel: " + channelId, e);
            ctx.status(e.getErrorCode()).json(Map.of(
                    "error", "Discord API error",
                    "code", e.getErrorCode(),
                    "message", e.getMeaning()
            ));
        } catch (Exception e) {
            logger.error("Error sending message to channel: " + channelId, e);
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Edits a message in a specific channel.
     *
     * @param ctx The HTTP context
     */
    public void editMessage(Context ctx) {
        String channelId = ctx.pathParam("channelId");
        String messageId = ctx.pathParam("messageId");

        try {
            MessageChannel channel = getChannelById(channelId);
            if (channel == null) {
                ctx.status(404).json(Map.of(
                        "error", "Channel not found",
                        "message", "No channel found with ID: " + channelId
                ));
                return;
            }

            // Parse request body
            JsonObject requestBody = gson.fromJson(ctx.body(), JsonObject.class);

            // Check if we're trying to edit our own message
            Message message = channel.retrieveMessageById(messageId).complete();
            if (!message.getAuthor().getId().equals(bot.getJda().getSelfUser().getId())) {
                ctx.status(403).json(Map.of(
                        "error", "Forbidden",
                        "message", "Cannot edit messages from other users"
                ));
                return;
            }

            // Build edited message
            if (requestBody.has("content")) {
                message = message.editMessage(requestBody.get("content").getAsString()).complete();
            }

            // Return updated message
            Map<String, Object> responseData = mapMessage(message);
            ctx.json(responseData);
        } catch (ErrorResponseException e) {
            logger.error("Discord error editing message: " + messageId, e);
            ctx.status(e.getErrorCode()).json(Map.of(
                    "error", "Discord API error",
                    "code", e.getErrorCode(),
                    "message", e.getMeaning()
            ));
        } catch (Exception e) {
            logger.error("Error editing message: " + messageId, e);
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Deletes a message from a specific channel.
     *
     * @param ctx The HTTP context
     */
    public void deleteMessage(Context ctx) {
        String channelId = ctx.pathParam("channelId");
        String messageId = ctx.pathParam("messageId");

        try {
            MessageChannel channel = getChannelById(channelId);
            if (channel == null) {
                ctx.status(404).json(Map.of(
                        "error", "Channel not found",
                        "message", "No channel found with ID: " + channelId
                ));
                return;
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
                ctx.status(403).json(Map.of(
                        "error", "Forbidden",
                        "message", "Cannot delete this message due to permissions"
                ));
                return;
            }

            // Delete message
            message.delete().complete();

            // Return success
            ctx.status(204);
        } catch (ErrorResponseException e) {
            logger.error("Discord error deleting message: " + messageId, e);
            ctx.status(e.getErrorCode()).json(Map.of(
                    "error", "Discord API error",
                    "code", e.getErrorCode(),
                    "message", e.getMeaning()
            ));
        } catch (Exception e) {
            logger.error("Error deleting message: " + messageId, e);
            ctx.status(500).json(Map.of(
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
                .map(attachment -> Map.of(
                        "id", attachment.getId(),
                        "filename", attachment.getFileName(),
                        "size", attachment.getSize(),
                        "url", attachment.getUrl(),
                        "contentType", attachment.getContentType() != null ? attachment.getContentType() : "unknown"
                ))
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
     * Builds a MessageEmbed from JSON data.
     *
     * @param embedJson The embed JSON data
     * @return The built MessageEmbed
     */
    private MessageEmbed buildEmbed(JsonObject embedJson) {
        net.dv8tion.jda.api.EmbedBuilder builder = new net.dv8tion.jda.api.EmbedBuilder();

        // Set basic properties
        if (embedJson.has("title")) builder.setTitle(embedJson.get("title").getAsString());
        if (embedJson.has("description")) builder.setDescription(embedJson.get("description").getAsString());
        if (embedJson.has("url")) builder.setUrl(embedJson.get("url").getAsString());
        if (embedJson.has("color")) builder.setColor(embedJson.get("color").getAsInt());
        if (embedJson.has("timestamp")) {
            long timestamp = embedJson.get("timestamp").getAsLong();
            builder.setTimestamp(OffsetDateTime.now().withNano(0)); // Set to current time as fallback
            try {
                builder.setTimestamp(OffsetDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(timestamp),
                        java.time.ZoneId.systemDefault()
                ));
            } catch (Exception e) {
                logger.warn("Invalid timestamp format, using current time", e);
            }
        }

        // Set author
        if (embedJson.has("author")) {
            JsonObject authorJson = embedJson.getAsJsonObject("author");
            String name = authorJson.has("name") ? authorJson.get("name").getAsString() : "";
            String url = authorJson.has("url") ? authorJson.get("url").getAsString() : null;
            String iconUrl = authorJson.has("iconUrl") ? authorJson.get("iconUrl").getAsString() : null;
            builder.setAuthor(name, url, iconUrl);
        }

        // Set footer
        if (embedJson.has("footer")) {
            JsonObject footerJson = embedJson.getAsJsonObject("footer");
            String text = footerJson.has("text") ? footerJson.get("text").getAsString() : "";
            String iconUrl = footerJson.has("iconUrl") ? footerJson.get("iconUrl").getAsString() : null;
            builder.setFooter(text, iconUrl);
        }

        // Set image
        if (embedJson.has("image")) {
            JsonObject imageJson = embedJson.getAsJsonObject("image");
            if (imageJson.has("url")) builder.setImage(imageJson.get("url").getAsString());
        }

        // Set thumbnail
        if (embedJson.has("thumbnail")) {
            JsonObject thumbnailJson = embedJson.getAsJsonObject("thumbnail");
            if (thumbnailJson.has("url")) builder.setThumbnail(thumbnailJson.get("url").getAsString());
        }

        // Add fields
        if (embedJson.has("fields") && embedJson.get("fields").isJsonArray()) {
            embedJson.getAsJsonArray("fields").forEach(fieldElement -> {
                if (fieldElement.isJsonObject()) {
                    JsonObject fieldJson = fieldElement.getAsJsonObject();
                    String name = fieldJson.has("name") ? fieldJson.get("name").getAsString() : "";
                    String value = fieldJson.has("value") ? fieldJson.get("value").getAsString() : "";
                    boolean inline = fieldJson.has("inline") && fieldJson.get("inline").getAsBoolean();
                    builder.addField(name, value, inline);
                }
            });
        }

        return builder.build();
    }
}