package com.cottonlesergal.ucontrolbot.listeners;

import com.cottonlesergal.ucontrolbot.Bot;
import com.cottonlesergal.ucontrolbot.api.WebServer;
import com.cottonlesergal.ucontrolbot.config.Config;
import com.cottonlesergal.ucontrolbot.models.DiscordUser;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for Discord message-related events.
 */
@Component
public class MessageListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);

    @Autowired
    private JDA jda;

    @Autowired
    private WebServer webServer;

    @Autowired
    private Config config;

    private final long startTime = System.currentTimeMillis();

    public MessageListener() {
        logger.info("MessageListener created");
    }

    @PostConstruct
    public void registerListener() {
        jda.addEventListener(this);
        logger.info("MessageListener registered with JDA");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore messages from other bots (including ourself)
        if (event.getAuthor().isBot()) {
            return;
        }

        try {
            // Log message
            if (event.isFromType(ChannelType.PRIVATE)) {
                logger.info("DM from {}: {}", event.getAuthor().getName(), event.getMessage().getContentDisplay());
            } else {
                logger.info("Message from {}#{} in {}/{}: {}",
                        event.getAuthor().getName(),
                        event.getAuthor().getDiscriminator(),
                        event.getGuild().getName(),
                        event.getChannel().getName(),
                        event.getMessage().getContentDisplay()
                );
            }

            // Process commands
            processCommand(event);

            // Broadcast event to WebSocket clients
            broadcastMessageEvent("MESSAGE_RECEIVED", event.getMessage());
        } catch (Exception e) {
            logger.error("Error processing message", e);
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        // Ignore messages from other bots (including ourself)
        /*if (event.getAuthor().isBot()) {
            return;
        }*/

        try {
            // Log message update
            if (event.isFromType(ChannelType.PRIVATE)) {
                logger.info("DM edit from {}: {}", event.getAuthor().getName(), event.getMessage().getContentDisplay());
            } else {
                logger.info("Message edit from {}#{} in {}/{}: {}",
                        event.getAuthor().getName(),
                        event.getAuthor().getDiscriminator(),
                        event.getGuild().getName(),
                        event.getChannel().getName(),
                        event.getMessage().getContentDisplay()
                );
            }

            // Broadcast event to WebSocket clients
            broadcastMessageEvent("MESSAGE_UPDATE", event.getMessage());
        } catch (Exception e) {
            logger.error("Error processing message update", e);
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        try {
            // Create data for the deleted message
            Map<String, Object> data = new HashMap<>();
            data.put("messageId", event.getMessageId());
            data.put("channelId", event.getChannel().getId());

            if (event.isFromGuild()) {
                data.put("guildId", event.getGuild().getId());
                data.put("guildName", event.getGuild().getName());
            }

            // Broadcast event to WebSocket clients
            broadcastEvent("MESSAGE_DELETE", data);
        } catch (Exception e) {
            logger.error("Error processing message delete", e);
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        try {
            // Create data for the reaction
            Map<String, Object> data = new HashMap<>();
            data.put("messageId", event.getMessageId());
            data.put("channelId", event.getChannel().getId());
            data.put("userId", event.getUserId());
            data.put("emoji", event.getEmoji().getAsReactionCode());

            // Add user information if possible
            User user = event.getUser();
            if (user != null) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("name", user.getName());
                userData.put("discriminator", user.getDiscriminator());
                userData.put("avatarUrl", user.getEffectiveAvatarUrl());
                userData.put("bot", user.isBot());
                data.put("user", userData);
            }

            if (event.isFromGuild()) {
                data.put("guildId", event.getGuild().getId());
                data.put("guildName", event.getGuild().getName());
            }

            // Broadcast event to WebSocket clients
            broadcastEvent("MESSAGE_REACTION_ADD", data);
        } catch (Exception e) {
            logger.error("Error processing reaction add", e);
        }
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        try {
            // Create data for the reaction
            Map<String, Object> data = new HashMap<>();
            data.put("messageId", event.getMessageId());
            data.put("channelId", event.getChannel().getId());
            data.put("userId", event.getUserId());
            data.put("emoji", event.getEmoji().getAsReactionCode());

            // Add user information if possible
            User user = event.getUser();
            if (user != null) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("name", user.getName());
                userData.put("discriminator", user.getDiscriminator());
                userData.put("avatarUrl", user.getEffectiveAvatarUrl());
                userData.put("bot", user.isBot());
                data.put("user", userData);
            }

            if (event.isFromGuild()) {
                data.put("guildId", event.getGuild().getId());
                data.put("guildName", event.getGuild().getName());
            }

            // Broadcast event to WebSocket clients
            broadcastEvent("MESSAGE_REACTION_REMOVE", data);
        } catch (Exception e) {
            logger.error("Error processing reaction remove", e);
        }
    }

    /**
     * Processes a command message.
     *
     * @param event The message event
     */
    private void processCommand(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw();
        String prefix = config.getCommandPrefix();

        // Check if the message starts with the command prefix
        if (content.startsWith(prefix)) {
            String[] args = content.substring(prefix.length()).trim().split("\\s+");
            String commandName = args[0].toLowerCase();

            // Handle commands
            switch (commandName) {
                case "ping":
                    handlePingCommand(event);
                    break;
                case "help":
                    handleHelpCommand(event);
                    break;
                case "info":
                    handleInfoCommand(event);
                    break;
                // Add more commands as needed
            }
        }
    }

    /**
     * Handles the ping command.
     *
     * @param event The message event
     */
    private void handlePingCommand(MessageReceivedEvent event) {
        long gatewayPing = jda.getGatewayPing();
        event.getChannel().sendMessage("Pong! Gateway ping: " + gatewayPing + "ms").queue(message -> {
            long apiPing = System.currentTimeMillis() - event.getMessage().getTimeCreated().toInstant().toEpochMilli();
            message.editMessage("Pong! Gateway ping: " + gatewayPing + "ms | API ping: " + apiPing + "ms").queue();
        });
    }

    /**
     * Handles the help command.
     *
     * @param event The message event
     */
    private void handleHelpCommand(MessageReceivedEvent event) {
        String prefix = config.getCommandPrefix();
        StringBuilder help = new StringBuilder("**Available Commands:**\n");
        help.append("`").append(prefix).append("ping` - Check bot response time\n");
        help.append("`").append(prefix).append("help` - Show this help message\n");
        help.append("`").append(prefix).append("info` - Show bot information\n");
        // Add more commands to the help message as needed

        event.getChannel().sendMessage(help.toString()).queue();
    }

    /**
     * Handles the info command.
     *
     * @param event The message event
     */
    private void handleInfoCommand(MessageReceivedEvent event) {
        StringBuilder info = new StringBuilder("**Bot Information:**\n");
        info.append("Name: ").append(jda.getSelfUser().getName()).append("\n");
        info.append("Guilds: ").append(jda.getGuilds().size()).append("\n");
        info.append("Users: ").append(jda.getUserCache().size()).append("\n");
        info.append("Web Interface: ").append(config.getApiBaseUrl()).append("\n");
        info.append("Uptime: ").append(getUptime()).append("\n");

        event.getChannel().sendMessage(info.toString()).queue();
    }

    /**
     * Gets the bot uptime as a formatted string.
     *
     * @return The formatted uptime
     */
    private String getUptime() {
        long uptime = System.currentTimeMillis() - startTime;
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        return String.format("%d days, %d hours, %d minutes, %d seconds",
                days, hours % 24, minutes % 60, seconds % 60);
    }

    /**
     * Broadcasts a message event to WebSocket clients.
     *
     * @param eventType The event type
     * @param message The message
     */
    private void broadcastMessageEvent(String eventType, Message message) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", message.getId());
        messageData.put("content", message.getContentRaw());
        messageData.put("channelId", message.getChannel().getId());
        messageData.put("timestamp", message.getTimeCreated().toInstant().toEpochMilli());

        // Add author information
        User author = message.getAuthor();
        DiscordUser user = DiscordUser.fromJdaUser(author);
        messageData.put("author", user.getUserData());

        // Add guild information if applicable
        if (message.isFromGuild()) {
            messageData.put("guildId", message.getGuild().getId());
            messageData.put("guildName", message.getGuild().getName());
        }

        // Broadcast event
        broadcastEvent(eventType, messageData);
    }

    /**
     * Broadcasts an event to WebSocket clients.
     *
     * @param eventType The event type
     * @param data The event data
     */
    private void broadcastEvent(String eventType, Map<String, Object> data) {
        if (webServer != null) {
            webServer.broadcastEvent(eventType, data);
        }
    }
}