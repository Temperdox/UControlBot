package com.cottonlesergal.ucontrolbot.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.EnumSet;

/**
 * Provider for the JDA (Java Discord API) instance.
 * Responsible for creating, configuring, and managing the lifecycle of the JDA client.
 */
@Component
public class JdaProvider {
    private static final Logger logger = LoggerFactory.getLogger(JdaProvider.class);

    private final Config config;
    private JDA jda;

    /**
     * Initializes the JDA provider with configuration.
     *
     * @param config The configuration
     */
    @Autowired
    public JdaProvider(Config config) {
        this.config = config;
        logger.info("JDA Provider created");
    }

    /**
     * Initializes the JDA instance.
     * Called after bean initialization.
     */
    @PostConstruct
    public void initializeJda() {
        logger.info("Initializing JDA...");

        try {
            // Initialize JDA with necessary intents
            JDABuilder builder = JDABuilder.createDefault(config.getToken());

            // Configure gateway intents
            builder.enableIntents(EnumSet.of(
                    GatewayIntent.GUILD_MEMBERS,      // For member events
                    GatewayIntent.GUILD_PRESENCES,    // For user statuses
                    GatewayIntent.GUILD_MESSAGES,     // For message events
                    GatewayIntent.MESSAGE_CONTENT,    // For message content
                    GatewayIntent.DIRECT_MESSAGES     // For DM events
            ));

            // Configure cache
            builder.enableCache(EnumSet.of(
                    CacheFlag.ONLINE_STATUS,          // Cache online statuses
                    CacheFlag.ACTIVITY,               // Cache user activities
                    CacheFlag.MEMBER_OVERRIDES,       // Cache permission overrides
                    CacheFlag.ROLE_TAGS,              // Cache role tags
                    CacheFlag.EMOJI                   // Cache emojis
            ));

            // Configure member chunking for guild member caching
            builder.setChunkingFilter(ChunkingFilter.ALL);

            // Build JDA instance
            this.jda = builder.build();
            this.jda.awaitReady(); // Wait for JDA to be ready
            logger.info("JDA initialized and ready");
            logger.info("Connected to Discord as {}", jda.getSelfUser().getAsTag());
            logger.info("Bot is in {} servers", jda.getGuilds().size());
        } catch (Exception e) {
            logger.error("Failed to initialize JDA", e);
            throw new RuntimeException("Failed to initialize Discord bot", e);
        }
    }

    /**
     * Provides the JDA instance as a bean for dependency injection.
     *
     * @return The JDA instance
     */
    @Bean
    public JDA getJda() {
        return jda;
    }

    /**
     * Shuts down the JDA instance.
     * Called before bean destruction.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down JDA...");

        // Shutdown JDA
        if (jda != null) {
            jda.shutdown();
            logger.info("JDA shutdown complete");
        }
    }
}