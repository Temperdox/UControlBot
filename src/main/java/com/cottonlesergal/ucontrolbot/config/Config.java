package com.cottonlesergal.ucontrolbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration handler for the Discord bot.
 * Uses Spring Boot's property injection to load configuration.
 */
@Configuration
@PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:config.properties", ignoreResourceNotFound = true)
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    // Default values
    private static final int DEFAULT_API_PORT = 8080;
    private static final String DEFAULT_API_HOST = "localhost";
    private static final long DEFAULT_CACHE_EXPIRY = 300; // 5 minutes in seconds

    // Discord bot token from properties
    @Value("${discord.token}")
    private String token;

    // API configuration from properties
    @Value("${server.port:${api.port:8080}}")
    private int apiPort;

    @Value("${server.address:${api.host:localhost}}")
    private String apiHost;

    // Owner ID from properties
    @Value("${discord.owner-id}")
    private String ownerId;

    // Command prefix from properties
    @Value("${discord.command-prefix}")
    private String commandPrefix;

    // Cache expiry from properties
    @Value("${discord.cache-expiry}")
    private long cacheExpirySeconds;

    /**
     * Initializes configuration by logging the loaded values.
     */
    @PostConstruct
    public void init() {
        if (token == null || token.trim().isEmpty()) {
            logger.error("Discord bot token not configured. Please set it in application.properties or config.properties or as an environment variable.");
            throw new IllegalStateException("Discord bot token not configured");
        }

        logger.info("Configuration loaded successfully");
        logger.info("API will run on {}:{}", apiHost, apiPort);
    }

    /**
     * Gets the Discord bot token.
     *
     * @return Discord bot token
     */
    public String getToken() {
        return token;
    }

    /**
     * Gets the API port.
     *
     * @return API port
     */
    public int getApiPort() {
        return apiPort;
    }

    /**
     * Gets the API host.
     *
     * @return API host
     */
    public String getApiHost() {
        return apiHost;
    }

    /**
     * Gets the bot owner ID.
     *
     * @return Owner ID
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Gets the command prefix.
     *
     * @return Command prefix
     */
    public String getCommandPrefix() {
        return commandPrefix;
    }

    /**
     * Gets the cache expiry time in seconds.
     *
     * @return Cache expiry time in seconds
     */
    public long getCacheExpirySeconds() {
        return cacheExpirySeconds;
    }

    /**
     * Gets the API base URL.
     *
     * @return API base URL
     */
    public String getApiBaseUrl() {
        return "http://" + apiHost + ":" + apiPort;
    }

    /**
     * Gets the WebSocket URL.
     *
     * @return WebSocket URL
     */
    public String getWebSocketUrl() {
        return "ws://" + apiHost + ":" + apiPort + "/ws";
    }

    /**
     * Creates a sample config.properties file with default values.
     *
     * @param path Path to save the file
     * @throws IOException If an I/O error occurs
     */
    public static void createSampleConfig(Path path) throws IOException {
        // Read the sample config from classpath resources
        ClassPathResource resource = new ClassPathResource("sample-config.properties");
        if (resource.exists()) {
            Files.copy(resource.getInputStream(), path);
            logger.info("Created sample config file at {}", path);
        } else {
            // Create a basic config file
            String content = """
                # Discord Bot Configuration
                discord.token=your_discord_token_here
                command.prefix=!

                # Web API Configuration
                api.host=localhost
                api.port=8080

                # Bot Owner Information
                owner.id=your_discord_id_here

                # Cache Settings
                cache.expiry=300

                # Logging Configuration
                logging.level=INFO
                """;
            Files.writeString(path, content);
            logger.info("Created basic config file at {}", path);
        }
    }
}