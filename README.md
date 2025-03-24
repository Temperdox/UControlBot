# UControlBot - Discord Bot with Web Interface

A Discord bot with a web-based control panel that mirrors Discord's UI. The bot provides a way to monitor and interact with Discord servers and direct messages through a customizable web interface.

## Features

- Real-time message monitoring and sending
- Web interface that resembles Discord's UI
- Direct message support
- Server management
- WebSocket integration for real-time updates
- Role and permission management
- User status tracking
- RESTful API for integration with other systems

## Prerequisites

- Java 17 or higher
- Maven
- Discord Bot Account with Token
- Web Server (for deployment)

## Setup and Configuration

### Creating a Discord Bot

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Click "New Application" and give it a name
3. Navigate to the "Bot" tab and click "Add Bot"
4. Under the "Privileged Gateway Intents" section, enable:
   - Server Members Intent
   - Message Content Intent
   - Presence Intent
5. Copy your bot token (you'll need this for configuration)
6. Invite the bot to your server using the OAuth2 URL generator:
   - Select "bot" scope
   - Select appropriate permissions (Administrator for full access)
   - Open the generated URL and select your server

### Configuration

Create or modify the `application.properties` file with your Discord bot token and other settings:

```properties
# Server configuration
server.port=${API_PORT:8080}
server.address=${API_HOST:localhost}

# Discord bot configuration
discord.token=${DISCORD_TOKEN:YOUR_BOT_TOKEN_HERE}
discord.owner-id=${OWNER_ID:YOUR_DISCORD_USER_ID}
discord.command-prefix=${COMMAND_PREFIX:!}
discord.cache-expiry=${CACHE_EXPIRY:300}

# Spring Boot configuration
spring.application.name=discord-bot
spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER

# Static resources configuration
spring.web.resources.static-locations=classpath:/static/

# Set default resource chain cache to false during development
spring.web.resources.chain.cache=false

# Logging configuration
logging.level.root=INFO
logging.level.com.cottonlesergal.ucontrolbot=INFO
logging.level.org.springframework.web=INFO
server.error.include-stacktrace=always
server.error.include-message=always
server.error.include-exception=true
spring.mvc.log-request-details=true

# Jackson JSON configuration
spring.jackson.serialization.FAIL_ON_EMPTY_BEANS=false

spring.main.allow-circular-references=true
```

Replace `YOUR_BOT_TOKEN_HERE` with your Discord bot token and `YOUR_DISCORD_USER_ID` with your Discord user ID.

### Running Locally

1. Clone the repository
2. Configure `application.properties` with your Discord bot token
3. Build the project using Maven:
   ```
   mvn clean package
   ```
4. Run the application:
   ```
   java -jar target/ucontrolbot-1.0.0.jar
   ```
5. Access the web interface at `http://localhost:8080`

### Deployment to a Server

#### Option 1: Deploy as a Standalone JAR

1. Build the project using Maven:
   ```
   mvn clean package
   ```
2. Copy the JAR file to your server
3. Create or update the `application.properties` file on your server
4. Run the application:
   ```
   java -jar ucontrolbot-1.0.0.jar
   ```

#### Option 2: Deploy with Docker

1. Create a Dockerfile:
   ```Dockerfile
   FROM openjdk:17-jdk-slim
   WORKDIR /app
   COPY target/ucontrolbot-1.0.0.jar app.jar
   COPY application.properties application.properties
   EXPOSE 8080
   CMD ["java", "-jar", "app.jar"]
   ```

2. Build the Docker image:
   ```
   docker build -t ucontrolbot .
   ```

3. Run the Docker container:
   ```
   docker run -p 8080:8080 ucontrolbot
   ```

#### Option 3: Deploy with a Process Manager (e.g., systemd)

1. Create a systemd service file:
   ```
   [Unit]
   Description=UControlBot Discord Bot
   After=network.target

   [Service]
   User=youruser
   WorkingDirectory=/path/to/bot
   ExecStart=/usr/bin/java -jar ucontrolbot-1.0.0.jar
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```

2. Save this file to `/etc/systemd/system/ucontrolbot.service`
3. Enable and start the service:
   ```
   sudo systemctl enable ucontrolbot
   sudo systemctl start ucontrolbot
   ```

## Web API Documentation

The bot provides a comprehensive REST API for integration with other systems. The API is based on Spring Boot and follows RESTful principles.

### Base URL

All API endpoints are prefixed with `/api`.

### Authentication

The API does not currently implement authentication, so it should only be deployed in a secure environment or behind a proxy with authentication.

### Key Endpoints

#### Bot Information

- `GET /api/bot/info` - Get information about the bot
- `GET /api/bot/info/owner` - Get information about the bot owner

#### Guilds (Servers)

- `GET /api/guilds` - Get all guilds the bot has access to
- `GET /api/guilds/{guildId}` - Get information about a specific guild
- `GET /api/guilds/{guildId}/channels` - Get all channels in a guild
- `GET /api/guilds/{guildId}/members` - Get all members in a guild

#### Channels

- `GET /api/channels/{channelId}` - Get information about a specific channel
- `GET /api/channels/{channelId}/messages` - Get messages from a channel
- `POST /api/channels/{channelId}/messages` - Send a message to a channel
- `PATCH /api/channels/{channelId}/messages/{messageId}` - Edit a message
- `DELETE /api/channels/{channelId}/messages/{messageId}` - Delete a message

#### Users

- `GET /api/users` - Get all users the bot can see
- `GET /api/users/{userId}` - Get information about a specific user
- `GET /api/users/{userId}/dm` - Get the DM channel for a user
- `POST /api/users/{userId}/dm` - Create a DM channel with a user

### WebSocket API

The bot also provides a WebSocket API for real-time updates at `/ws`. The WebSocket API uses a custom protocol based on JSON messages.

#### Message Types

1. **Connection Types**
   - `WELCOME` - Sent when a client connects
   - `IDENTIFY` - Used by clients to identify themselves
   - `PING`/`PONG` - Used for keepalive
   - `ACK` - Acknowledges an action

2. **Subscription Types**
   - `SUBSCRIBE_GUILD` - Subscribe to events from a guild
   - `SUBSCRIBE_CHANNEL` - Subscribe to events from a channel
   - `SUBSCRIBE_DM` - Subscribe to events from a DM
   - `UNSUBSCRIBE_GUILD` - Unsubscribe from guild events
   - `UNSUBSCRIBE_CHANNEL` - Unsubscribe from channel events
   - `UNSUBSCRIBE_DM` - Unsubscribe from DM events

3. **Event Types**
   - `MESSAGE_RECEIVED` - A new message was received
   - `MESSAGE_UPDATE` - A message was updated
   - `MESSAGE_DELETE` - A message was deleted
   - `USER_UPDATE_STATUS` - A user's status changed
   - `TYPING_START` - A user started typing

#### Example WebSocket Message

```json
{
  "type": "MESSAGE_RECEIVED",
  "data": {
    "id": "123456789012345678",
    "content": "Hello, world!",
    "channelId": "123456789012345678",
    "timestamp": 1647123456789,
    "author": {
      "id": "123456789012345678",
      "username": "Username",
      "discriminator": "1234",
      "avatarUrl": "https://cdn.discordapp.com/avatars/123456789012345678/abcdef.png",
      "bot": false
    }
  }
}
```

## Web Interface Usage

The web interface resembles Discord's UI and provides similar functionality:

1. **Server List** - On the left side, showing all servers the bot is in
2. **Channel List** - Showing all channels in the selected server
3. **Message Area** - Shows messages in the selected channel
4. **User List** - Shows users in the selected server
5. **DM List** - Shows direct message conversations

### Key Features

- **Real-time Updates** - Messages and status changes appear in real-time
- **Send Messages** - Send messages to channels or DMs
- **User Status** - See user status (online, idle, DND, offline)
- **Message History** - View message history with infinite scrolling
- **Typing Indicators** - See when users are typing

## Troubleshooting

### Common Issues

1. **Bot doesn't connect to Discord**
   - Check your bot token in `application.properties`
   - Ensure you've enabled the required intents in the Discord Developer Portal

2. **Missing Permissions**
   - Check that the bot has the necessary permissions in your Discord server

3. **WebSocket Connection Issues**
   - Check your firewall settings
   - Ensure the WebSocket endpoint is accessible

4. **UI Not Loading Properly**
   - Clear your browser cache
   - Check the browser console for errors

### Logs

Logs are available in the console and are saved to the `logs` directory.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
