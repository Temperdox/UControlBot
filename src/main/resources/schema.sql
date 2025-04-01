-- Discord Bot Database Schema

-- Users table to store all users
CREATE TABLE IF NOT EXISTS users (
                                     id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    discriminator VARCHAR(10),
    global_name VARCHAR(255),
    display_name VARCHAR(255),
    avatar_url VARCHAR(512),
    is_bot BOOLEAN DEFAULT FALSE,
    is_owner BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) DEFAULT 'offline',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

-- Guilds (servers) table
CREATE TABLE IF NOT EXISTS guilds (
                                      id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    icon_url VARCHAR(512),
    owner_id VARCHAR(255),
    member_count INT DEFAULT 0,
    description TEXT,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
    );

-- Channels table
CREATE TABLE IF NOT EXISTS channels (
                                        id VARCHAR(255) PRIMARY KEY,
    guild_id VARCHAR(255),
    parent_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    topic TEXT,
    position INT DEFAULT 0,
    is_nsfw BOOLEAN DEFAULT FALSE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES channels(id) ON DELETE SET NULL
    );

-- Messages table
CREATE TABLE IF NOT EXISTS messages (
                                        id VARCHAR(255) PRIMARY KEY,
    channel_id VARCHAR(255) NOT NULL,
    author_id VARCHAR(255) NOT NULL,
    content TEXT,
    timestamp BIGINT NOT NULL,
    edited_timestamp BIGINT,
    referenced_message_id VARCHAR(255),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (channel_id) REFERENCES channels(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (referenced_message_id) REFERENCES messages(id) ON DELETE SET NULL
    );

-- Attachments table
CREATE TABLE IF NOT EXISTS attachments (
                                           id VARCHAR(255) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    url VARCHAR(512) NOT NULL,
    content_type VARCHAR(255),
    size BIGINT,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
    );

-- Embeds table
CREATE TABLE IF NOT EXISTS embeds (
                                      id INT AUTO_INCREMENT PRIMARY KEY,
                                      message_id VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    description TEXT,
    url VARCHAR(512),
    color INT,
    timestamp BIGINT,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
    );

-- Embed fields table
CREATE TABLE IF NOT EXISTS embed_fields (
                                            id INT AUTO_INCREMENT PRIMARY KEY,
                                            embed_id INT NOT NULL,
                                            name VARCHAR(255) NOT NULL,
    value TEXT NOT NULL,
    is_inline BOOLEAN DEFAULT FALSE,
    position INT DEFAULT 0,
    FOREIGN KEY (embed_id) REFERENCES embeds(id) ON DELETE CASCADE
    );

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
                                     id VARCHAR(255) PRIMARY KEY,
    guild_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    color INT DEFAULT 0,
    position INT DEFAULT 0,
    permissions BIGINT DEFAULT 0,
    is_mentionable BOOLEAN DEFAULT FALSE,
    is_hoisted BOOLEAN DEFAULT FALSE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
    );

-- User roles (many-to-many relationship)
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id VARCHAR(255) NOT NULL,
    role_id VARCHAR(255) NOT NULL,
    guild_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
    );

-- Guild members table (including user-specific data within a guild)
CREATE TABLE IF NOT EXISTS guild_members (
                                             user_id VARCHAR(255) NOT NULL,
    guild_id VARCHAR(255) NOT NULL,
    nickname VARCHAR(255),
    joined_at BIGINT,
    PRIMARY KEY (user_id, guild_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
    );

-- Direct message channels table
CREATE TABLE IF NOT EXISTS dm_channels (
                                           id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    last_message_id VARCHAR(255),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (last_message_id) REFERENCES messages(id) ON DELETE SET NULL
    );

-- Reactions table
CREATE TABLE IF NOT EXISTS reactions (
                                         message_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    emoji VARCHAR(255) NOT NULL,
    PRIMARY KEY (message_id, user_id, emoji),
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Typing indicators (ephemeral)
CREATE TABLE IF NOT EXISTS typing_indicators (
                                                 user_id VARCHAR(255) NOT NULL,
    channel_id VARCHAR(255) NOT NULL,
    timestamp BIGINT NOT NULL,
    PRIMARY KEY (user_id, channel_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (channel_id) REFERENCES channels(id) ON DELETE CASCADE
    );

-- Indexes for faster queries
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_channels_guild_id ON channels(guild_id);
CREATE INDEX idx_channels_parent_id ON channels(parent_id);
CREATE INDEX idx_messages_channel_id ON messages(channel_id);
CREATE INDEX idx_messages_author_id ON messages(author_id);
CREATE INDEX idx_guild_members_guild_id ON guild_members(guild_id);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_guild_id ON user_roles(guild_id);