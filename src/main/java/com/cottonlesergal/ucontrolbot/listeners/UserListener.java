package com.cottonlesergal.ucontrolbot.listeners;

import com.cottonlesergal.ucontrolbot.Bot;
import com.cottonlesergal.ucontrolbot.api.WebServer;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.user.UserActivityEndEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for Discord user-related events.
 */
@Component
public class UserListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(UserListener.class);

    @Autowired
    private JDA jda;

    @Autowired
    private WebServer webServer;

    public UserListener() {
        logger.info("UserListener created");
    }

    @PostConstruct
    public void registerListener() {
        jda.addEventListener(this);
        logger.info("UserListener registered with JDA");
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        User user = event.getUser();
        String oldName = event.getOldName();
        String newName = event.getNewName();

        logger.info("User name updated: {} -> {} (ID: {})", oldName, newName, user.getId());

        // Broadcast user update event
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("oldName", oldName);
        data.put("newName", newName);

        broadcastEvent("USER_UPDATE_NAME", data);
    }

    @Override
    public void onUserUpdateDiscriminator(UserUpdateDiscriminatorEvent event) {
        User user = event.getUser();
        String oldDiscriminator = event.getOldDiscriminator();
        String newDiscriminator = event.getNewDiscriminator();

        logger.info("User discriminator updated: {} -> {} for user: {} (ID: {})",
                oldDiscriminator, newDiscriminator, user.getName(), user.getId());

        // Broadcast user update event
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userName", user.getName());
        data.put("oldDiscriminator", oldDiscriminator);
        data.put("newDiscriminator", newDiscriminator);

        broadcastEvent("USER_UPDATE_DISCRIMINATOR", data);
    }

    @Override
    public void onUserUpdateAvatar(UserUpdateAvatarEvent event) {
        User user = event.getUser();
        String oldAvatarId = event.getOldAvatarId();
        String newAvatarId = event.getNewAvatarId();

        logger.info("User avatar updated for user: {} (ID: {})", user.getName(), user.getId());

        // Broadcast user update event
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userName", user.getName());
        data.put("oldAvatarUrl", oldAvatarId != null ? String.format("https://cdn.discordapp.com/avatars/%s/%s.%s",
                user.getId(), oldAvatarId, oldAvatarId.startsWith("a_") ? "gif" : "png") : null);
        data.put("newAvatarUrl", user.getEffectiveAvatarUrl());

        broadcastEvent("USER_UPDATE_AVATAR", data);
    }

    @Override
    public void onUserUpdateOnlineStatus(UserUpdateOnlineStatusEvent event) {
        User user = event.getUser();
        Member member = event.getMember();

        String oldStatus = event.getOldOnlineStatus().name().toLowerCase();
        String newStatus = event.getNewOnlineStatus().name().toLowerCase();

        // Log status change
        if (member != null) {
            logger.info("User status updated: {} -> {} for user: {} (ID: {}) in guild: {} (ID: {})",
                    oldStatus, newStatus, user.getName(), user.getId(),
                    member.getGuild().getName(), member.getGuild().getId());
        } else {
            logger.info("User status updated: {} -> {} for user: {} (ID: {})",
                    oldStatus, newStatus, user.getName(), user.getId());
        }

        // Broadcast user status update event for all users, not just guild members
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userName", user.getName());
        data.put("oldStatus", oldStatus);
        data.put("newStatus", newStatus);

        // Include guild info if available
        if (member != null) {
            data.put("guildId", member.getGuild().getId());
            data.put("guildName", member.getGuild().getName());
        }

        // Broadcast to all clients
        broadcastEvent("USER_UPDATE_STATUS", data);
    }

    @Override
    public void onUserActivityStart(UserActivityStartEvent event) {
        User user = event.getUser();
        Member member = event.getMember();

        // Skip if not in a guild or not a tracked guild
        if (member == null) {
            return;
        }

        String activityName = event.getNewActivity().getName();
        String activityType = event.getNewActivity().getType().name();

        logger.info("User activity started: {} ({}) for user: {} (ID: {}) in guild: {} (ID: {})",
                activityName, activityType, user.getName(), user.getId(),
                member.getGuild().getName(), member.getGuild().getId());

        // Broadcast user activity start event
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userName", user.getName());
        data.put("guildId", member.getGuild().getId());
        data.put("guildName", member.getGuild().getName());
        data.put("activityName", activityName);
        data.put("activityType", activityType);

        if (event.getNewActivity().getUrl() != null) {
            data.put("activityUrl", event.getNewActivity().getUrl());
        }

        broadcastEvent("USER_ACTIVITY_START", data);
    }

    @Override
    public void onUserActivityEnd(UserActivityEndEvent event) {
        User user = event.getUser();
        Member member = event.getMember();

        // Skip if not in a guild or not a tracked guild
        if (member == null) {
            return;
        }

        String activityName = event.getOldActivity().getName();
        String activityType = event.getOldActivity().getType().name();

        logger.info("User activity ended: {} ({}) for user: {} (ID: {}) in guild: {} (ID: {})",
                activityName, activityType, user.getName(), user.getId(),
                member.getGuild().getName(), member.getGuild().getId());

        // Broadcast user activity end event
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userName", user.getName());
        data.put("guildId", member.getGuild().getId());
        data.put("guildName", member.getGuild().getName());
        data.put("activityName", activityName);
        data.put("activityType", activityType);

        broadcastEvent("USER_ACTIVITY_END", data);
    }

    @Override
    public void onUserTyping(UserTypingEvent event) {
        User user = event.getUser();

        // Skip if not in a guild
        if (event.getGuild() == null) {
            return;
        }

        logger.debug("User typing: {} (ID: {}) in channel: {} (ID: {}) in guild: {} (ID: {})",
                user.getName(), user.getId(),
                event.getChannel().getName(), event.getChannel().getId(),
                event.getGuild().getName(), event.getGuild().getId());

        // Broadcast user typing event
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userName", user.getName());
        data.put("channelId", event.getChannel().getId());
        data.put("channelName", event.getChannel().getName());
        data.put("guildId", event.getGuild().getId());
        data.put("guildName", event.getGuild().getName());
        data.put("timestamp", event.getTimestamp().toInstant().toEpochMilli());

        broadcastEvent("USER_TYPING", data);
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