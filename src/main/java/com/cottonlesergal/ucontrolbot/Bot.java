package com.cottonlesergal.ucontrolbot;

import com.cottonlesergal.ucontrolbot.api.WebServer;
import com.cottonlesergal.ucontrolbot.config.Config;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Main entry point for the Discord bot application.
 * Uses JDA client from JdaProvider and manages the web server.
 */
@Component
public class Bot implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);

    private final Config config;
    private final WebServer webServer;
    private final JDA jda;
    private final long startTime;

    /**
     * Initializes the bot with configuration, JDA client, and web server.
     * With Spring Boot, Config, WebServer, and JDA are automatically injected.
     *
     * @param config The configuration
     * @param webServer The web server
     * @param jda The JDA instance (provided by JdaProvider)
     */
    @Autowired
    public Bot(Config config, WebServer webServer, JDA jda) {
        this.config = config;
        this.webServer = webServer;
        this.jda = jda;
        this.startTime = System.currentTimeMillis();

        logger.info("Bot instance created");
    }

    /**
     * Gets the JDA instance.
     *
     * @return JDA instance
     */
    public JDA getJda() {
        return jda;
    }

    /**
     * Gets the configuration.
     *
     * @return Config instance
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Gets the web server.
     *
     * @return WebServer instance
     */
    public WebServer getWebServer() {
        return webServer;
    }

    /**
     * Gets the bot's start time.
     *
     * @return Start time in milliseconds
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Runs when the application starts.
     * This is provided by Spring Boot's CommandLineRunner interface.
     */
    @Override
    public void run(String... args) {
        // This method is called after the application is fully initialized
        logger.info("Bot and web server are fully operational");

        triggerDmListRefresh();
    }

    /**
     * Triggers a refresh of the DM list for all connected clients.
     * Called during startup and when joining new guilds.
     */
    private void triggerDmListRefresh() {
        Map<String, Object> data = new HashMap<>();
        // You can optionally add metadata here
        data.put("source", "bot_startup");
        data.put("timestamp", System.currentTimeMillis());

        // Broadcast the refresh event
        if (webServer != null) {
            webServer.broadcastEvent("REFRESH_DM_LIST", data);
            logger.info("DM list refresh triggered on startup");
        }
    }
}