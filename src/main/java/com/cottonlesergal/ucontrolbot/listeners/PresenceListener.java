package com.cottonlesergal.ucontrolbot.listeners;

import com.cottonlesergal.ucontrolbot.api.WebServer;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.events.user.UserActivityEndEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for Discord presence-related events.
 * Handles user status changes, activities, and scheduled status polling.
 */
@Component
public class PresenceListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PresenceListener.class);

    @Autowired
    private JDA jda;

    @Autowired
    private WebServer webServer;

    // Cache of important users to track (can be updated dynamically)
    private final Map<String, User> trackedUsers = new ConcurrentHashMap<>();

    // Cache of last known statuses to prevent duplicate broadcasts
    private final Map<String, String> lastKnownStatuses = new ConcurrentHashMap<>();

    public PresenceListener() {
        logger.info("PresenceListener created");
    }

    @PostConstruct
    public void registerListener() {
        jda.addEventListener(this);
        logger.info("PresenceListener registered with JDA");
    }

    @Override
    public void onReady(ReadyEvent event) {
        logger.info("JDA Ready event received - initializing status tracking");
        initializeTrackedUsers();
    }

    @Override
    public void onSessionResume(SessionResumeEvent event) {
        logger.info("JDA Session resumed - refreshing status tracking");
        initializeTrackedUsers();
    }

    /**
     * Initialize the list of tracked users from guilds and important users
     */
    private void initializeTrackedUsers() {
        trackedUsers.clear();
        lastKnownStatuses.clear();

        // Add all guild members to tracked users
        for (Guild guild : jda.getGuilds()) {
            for (Member member : guild.getMembers()) {
                User user = member.getUser();
                trackedUsers.put(user.getId(), user);

                // Store initial status
                String status = member.getOnlineStatus().name().toLowerCase();
                lastKnownStatuses.put(user.getId(), status);

                logger.debug("Adding tracked user {} ({}): initial status {}",
                        user.getName(), user.getId(), status);
            }
        }

        logger.info("Initialized tracking for {} users", trackedUsers.size());
    }

    @Override
    public void onUserUpdateOnlineStatus(UserUpdateOnlineStatusEvent event) {
        User user = event.getUser();
        Member member = event.getMember();

        // Skip if not in a guild
        if (member == null) {
            logger.debug("Received status update for user not in guild: {}", user.getName());
            return;
        }

        String oldStatus = event.getOldOnlineStatus().name().toLowerCase();
        String newStatus = event.getNewOnlineStatus().name().toLowerCase();

        // Skip if no actual change (sometimes Discord sends duplicates)
        if (oldStatus.equals(newStatus)) {
            logger.debug("Ignoring duplicate status update for {}: {} -> {}",
                    user.getName(), oldStatus, newStatus);
            return;
        }

        logger.info("PRESENCE UPDATE DETECTED: User {} status changed from {} to {} in guild {}",
                user.getName(), oldStatus, newStatus, member.getGuild().getName());

        // Update cached status
        lastKnownStatuses.put(user.getId(), newStatus);

        // Add to tracked users if not already tracked
        if (!trackedUsers.containsKey(user.getId())) {
            trackedUsers.put(user.getId(), user);
            logger.debug("Added new user to tracking: {}", user.getName());
        }

        // Broadcast user status update event
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userName", user.getName());
        data.put("oldStatus", oldStatus);
        data.put("newStatus", newStatus);
        data.put("guildId", member.getGuild().getId());
        data.put("guildName", member.getGuild().getName());

        broadcastEvent("USER_UPDATE_STATUS", data);
    }

    @Override
    public void onUserUpdateActivities(UserUpdateActivitiesEvent event) {
        User user = event.getUser();
        Member member = event.getMember();

        // Skip if not in a guild
        if (member == null) {
            return;
        }

        logger.debug("Activity update for user {}: {}",
                user.getName(), member.getActivities());

        // We could broadcast activity updates here if needed
    }

    @Override
    public void onUserActivityStart(UserActivityStartEvent event) {
        User user = event.getUser();
        Member member = event.getMember();

        // Skip if not in a guild
        if (member == null) {
            return;
        }

        logger.debug("Activity started for user {}: {}",
                user.getName(), event.getNewActivity().getName());

        // Add to tracked users if not already tracked
        if (!trackedUsers.containsKey(user.getId())) {
            trackedUsers.put(user.getId(), user);
        }
    }

    @Override
    public void onUserActivityEnd(UserActivityEndEvent event) {
        User user = event.getUser();
        Member member = event.getMember();

        // Skip if not in a guild
        if (member == null) {
            return;
        }

        logger.debug("Activity ended for user {}: {}",
                user.getName(), event.getOldActivity().getName());
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        User user = event.getUser();
        Member member = event.getMember();

        // Add new member to tracked users
        trackedUsers.put(user.getId(), user);
        lastKnownStatuses.put(user.getId(), member.getOnlineStatus().name().toLowerCase());

        logger.debug("Added new guild member to tracking: {}", user.getName());
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        // Update tracked user if nickname changes
        trackedUsers.put(event.getUser().getId(), event.getUser());
    }

    /**
     * Scheduled task to poll and broadcast status updates for tracked users.
     * Runs every minute to ensure status data stays fresh even if Discord
     * doesn't send events for all status changes.
     */
    @Scheduled(fixedRate = 20000) // Every minute
    public void pollUserStatuses() {
        logger.debug("Polling statuses for {} tracked users", trackedUsers.size());
        List<String> updatedUsers = new ArrayList<>();

        // Check status for each guild member
        for (Guild guild : jda.getGuilds()) {
            for (Member member : guild.getMembers()) {
                User user = member.getUser();
                String userId = user.getId();
                String currentStatus = member.getOnlineStatus().name().toLowerCase();
                String previousStatus = lastKnownStatuses.get(userId);

                // Skip if status hasn't changed
                if (currentStatus.equals(previousStatus)) {
                    continue;
                }

                // Status has changed - update cache and broadcast
                lastKnownStatuses.put(userId, currentStatus);
                updatedUsers.add(user.getName());

                logger.info("Polling detected status change for {}: {} -> {}",
                        user.getName(), previousStatus, currentStatus);

                // Broadcast status update
                Map<String, Object> data = new HashMap<>();
                data.put("userId", userId);
                data.put("userName", user.getName());
                data.put("oldStatus", previousStatus != null ? previousStatus : "unknown");
                data.put("newStatus", currentStatus);
                data.put("guildId", guild.getId());
                data.put("guildName", guild.getName());

                broadcastEvent("USER_UPDATE_STATUS", data);
            }
        }

        if (!updatedUsers.isEmpty()) {
            logger.info("Status polling updated {} users: {}",
                    updatedUsers.size(), String.join(", ", updatedUsers));
        }
    }

    /**
     * Broadcasts an event to WebSocket clients.
     *
     * @param eventType The event type
     * @param data The event data
     */
    private void broadcastEvent(String eventType, Map<String, Object> data) {
        if (webServer != null) {
            logger.debug("Broadcasting {} event: {}", eventType, data);
            webServer.broadcastEvent(eventType, data);
        } else {
            logger.warn("Cannot broadcast event - WebServer is null");
        }
    }
}