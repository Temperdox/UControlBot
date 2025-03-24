package com.cottonlesergal.ucontrolbot.listeners;

import com.cottonlesergal.ucontrolbot.Bot;
import com.cottonlesergal.ucontrolbot.api.WebServer;
import com.cottonlesergal.ucontrolbot.config.Config;
import com.cottonlesergal.ucontrolbot.models.DiscordUser;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent;
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Listener for Discord guild-related events.
 */
@Component
public class GuildListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(GuildListener.class);

    @Autowired
    private JDA jda;

    @Autowired
    private WebServer webServer;

    @Autowired
    private Config config;

    public GuildListener() {
        logger.info("GuildListener created");
    }

    @PostConstruct
    public void registerListener() {
        jda.addEventListener(this);
        logger.info("GuildListener registered with JDA");
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        Guild guild = event.getGuild();
        logger.info("Guild ready: {} (ID: {})", guild.getName(), guild.getId());

        // Broadcast guild ready event
        broadcastGuildEvent("GUILD_READY", guild);
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();
        logger.info("Joined guild: {} (ID: {})", guild.getName(), guild.getId());

        // Broadcast guild join event
        broadcastGuildEvent("GUILD_JOIN", guild);
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        Guild guild = event.getGuild();
        logger.info("Left guild: {} (ID: {})", guild.getName(), guild.getId());

        // Broadcast guild leave event
        Map<String, Object> data = new HashMap<>();
        data.put("id", guild.getId());
        data.put("name", guild.getName());

        broadcastEvent("GUILD_LEAVE", data);
    }

    @Override
    public void onGuildUpdateName(GuildUpdateNameEvent event) {
        Guild guild = event.getGuild();
        String oldName = event.getOldName();
        String newName = event.getNewName();

        logger.info("Guild name updated: {} -> {} (ID: {})", oldName, newName, guild.getId());

        // Broadcast guild update event
        Map<String, Object> data = new HashMap<>();
        data.put("id", guild.getId());
        data.put("oldName", oldName);
        data.put("newName", newName);

        broadcastEvent("GUILD_UPDATE_NAME", data);
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        Guild guild = event.getGuild();
        String userId = event.getUser().getId();
        String userName = event.getUser().getName();

        logger.info("User banned: {} (ID: {}) from guild: {} (ID: {})",
                userName, userId, guild.getName(), guild.getId());

        // Broadcast guild ban event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("userId", userId);
        data.put("userName", userName);

        broadcastEvent("GUILD_BAN", data);
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        Guild guild = event.getGuild();
        String userId = event.getUser().getId();
        String userName = event.getUser().getName();

        logger.info("User unbanned: {} (ID: {}) from guild: {} (ID: {})",
                userName, userId, guild.getName(), guild.getId());

        // Broadcast guild unban event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("userId", userId);
        data.put("userName", userName);

        broadcastEvent("GUILD_UNBAN", data);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        logger.info("Member joined: {} (ID: {}) in guild: {} (ID: {})",
                member.getUser().getName(), member.getId(), guild.getName(), guild.getId());

        // Broadcast member join event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("member", DiscordUser.fromJdaMember(member).getUserData());
        data.put("joinTime", member.getTimeJoined().toInstant().toEpochMilli());

        broadcastEvent("GUILD_MEMBER_JOIN", data);
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        String userId = event.getUser().getId();
        String userName = event.getUser().getName();

        logger.info("Member left: {} (ID: {}) from guild: {} (ID: {})",
                userName, userId, guild.getName(), guild.getId());

        // Broadcast member leave event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("userId", userId);
        data.put("userName", userName);

        broadcastEvent("GUILD_MEMBER_REMOVE", data);
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String oldNickname = event.getOldNickname() != null ? event.getOldNickname() : member.getUser().getName();
        String newNickname = event.getNewNickname() != null ? event.getNewNickname() : member.getUser().getName();

        logger.info("Member nickname updated: {} -> {} for user: {} (ID: {}) in guild: {} (ID: {})",
                oldNickname, newNickname, member.getUser().getName(), member.getId(),
                guild.getName(), guild.getId());

        // Broadcast member update event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("userId", member.getId());
        data.put("userName", member.getUser().getName());
        data.put("oldNickname", oldNickname);
        data.put("newNickname", newNickname);

        broadcastEvent("GUILD_MEMBER_UPDATE_NICKNAME", data);
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        List<Role> roles = event.getRoles();

        logger.info("Roles added to member: {} (ID: {}) in guild: {} (ID: {}): {}",
                member.getUser().getName(), member.getId(), guild.getName(), guild.getId(),
                roles.stream().map(Role::getName).collect(Collectors.joining(", ")));

        // Broadcast member role update event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("userId", member.getId());
        data.put("userName", member.getUser().getName());
        data.put("roles", roles.stream()
                .map(role -> {
                    Map<String, Object> roleData = new HashMap<>();
                    roleData.put("id", role.getId());
                    roleData.put("name", role.getName());
                    roleData.put("color", role.getColorRaw());
                    return roleData;
                })
                .collect(Collectors.toList()));

        broadcastEvent("GUILD_MEMBER_ROLE_ADD", data);
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        List<Role> roles = event.getRoles();

        logger.info("Roles removed from member: {} (ID: {}) in guild: {} (ID: {}): {}",
                member.getUser().getName(), member.getId(), guild.getName(), guild.getId(),
                roles.stream().map(Role::getName).collect(Collectors.joining(", ")));

        // Broadcast member role update event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("userId", member.getId());
        data.put("userName", member.getUser().getName());
        data.put("roles", roles.stream()
                .map(role -> {
                    Map<String, Object> roleData = new HashMap<>();
                    roleData.put("id", role.getId());
                    roleData.put("name", role.getName());
                    roleData.put("color", role.getColorRaw());
                    return roleData;
                })
                .collect(Collectors.toList()));

        broadcastEvent("GUILD_MEMBER_ROLE_REMOVE", data);
    }

    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        Guild guild = event.getGuild();
        Role role = event.getRole();

        logger.info("Role created: {} (ID: {}) in guild: {} (ID: {})",
                role.getName(), role.getId(), guild.getName(), guild.getId());

        // Broadcast role create event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("roleId", role.getId());
        data.put("roleName", role.getName());
        data.put("roleColor", role.getColorRaw());
        data.put("rolePosition", role.getPosition());

        broadcastEvent("ROLE_CREATE", data);
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        Guild guild = event.getGuild();
        Role role = event.getRole();

        logger.info("Role deleted: {} (ID: {}) in guild: {} (ID: {})",
                role.getName(), role.getId(), guild.getName(), guild.getId());

        // Broadcast role delete event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("roleId", role.getId());
        data.put("roleName", role.getName());

        broadcastEvent("ROLE_DELETE", data);
    }

    @Override
    public void onRoleUpdateName(RoleUpdateNameEvent event) {
        Guild guild = event.getGuild();
        Role role = event.getRole();
        String oldName = event.getOldName();
        String newName = event.getNewName();

        logger.info("Role name updated: {} -> {} (ID: {}) in guild: {} (ID: {})",
                oldName, newName, role.getId(), guild.getName(), guild.getId());

        // Broadcast role update event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("roleId", role.getId());
        data.put("oldRoleName", oldName);
        data.put("newRoleName", newName);

        broadcastEvent("ROLE_UPDATE_NAME", data);
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

        Guild guild = event.getGuild();
        Channel channel = event.getChannel();

        logger.info("Channel created: {} (ID: {}) in guild: {} (ID: {})",
                channel.getName(), channel.getId(), guild.getName(), guild.getId());

        // Broadcast channel create event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("channelId", channel.getId());
        data.put("channelName", channel.getName());
        data.put("channelType", channel.getType().name());

        broadcastEvent("CHANNEL_CREATE", data);
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

        Guild guild = event.getGuild();
        Channel channel = event.getChannel();

        logger.info("Channel deleted: {} (ID: {}) in guild: {} (ID: {})",
                channel.getName(), channel.getId(), guild.getName(), guild.getId());

        // Broadcast channel delete event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("channelId", channel.getId());
        data.put("channelName", channel.getName());
        data.put("channelType", channel.getType().name());

        broadcastEvent("CHANNEL_DELETE", data);
    }

    @Override
    public void onChannelUpdateName(ChannelUpdateNameEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

        Guild guild = event.getGuild();
        Channel channel = event.getChannel();

        logger.info("Channel name updated for channel (ID: {}) in guild: {} (ID: {})",
                channel.getId(), guild.getName(), guild.getId());

        // Broadcast channel update event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("channelId", channel.getId());
        data.put("channelName", channel.getName());
        data.put("channelType", channel.getType().name());

        broadcastEvent("CHANNEL_UPDATE_NAME", data);
    }

    @Override
    public void onEmojiAdded(EmojiAddedEvent event) {
        Guild guild = event.getGuild();
        RichCustomEmoji emoji = event.getEmoji();

        logger.info("Emoji added: {} (ID: {}) in guild: {} (ID: {})",
                emoji.getName(), emoji.getId(), guild.getName(), guild.getId());

        // Broadcast emoji added event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("emojiId", emoji.getId());
        data.put("emojiName", emoji.getName());
        data.put("emojiAnimated", emoji.isAnimated());
        data.put("emojiUrl", emoji.getImageUrl());

        broadcastEvent("EMOJI_ADDED", data);
    }

    @Override
    public void onEmojiRemoved(EmojiRemovedEvent event) {
        Guild guild = event.getGuild();
        RichCustomEmoji emoji = event.getEmoji();

        logger.info("Emoji removed: {} (ID: {}) in guild: {} (ID: {})",
                emoji.getName(), emoji.getId(), guild.getName(), guild.getId());

        // Broadcast emoji removed event
        Map<String, Object> data = new HashMap<>();
        data.put("guildId", guild.getId());
        data.put("guildName", guild.getName());
        data.put("emojiId", emoji.getId());
        data.put("emojiName", emoji.getName());

        broadcastEvent("EMOJI_REMOVED", data);
    }

    /**
     * Broadcasts a guild event to WebSocket clients.
     *
     * @param eventType The event type
     * @param guild The guild
     */
    private void broadcastGuildEvent(String eventType, Guild guild) {
        Map<String, Object> guildData = new HashMap<>();
        guildData.put("id", guild.getId());
        guildData.put("name", guild.getName());
        guildData.put("iconUrl", guild.getIconUrl());
        guildData.put("memberCount", guild.getMemberCount());
        guildData.put("ownerId", guild.getOwnerId());
        guildData.put("description", guild.getDescription());

        // Add features if available
        guildData.put("features", guild.getFeatures());

        // Broadcast event
        broadcastEvent(eventType, guildData);
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