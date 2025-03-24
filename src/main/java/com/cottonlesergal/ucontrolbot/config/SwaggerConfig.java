package com.cottonlesergal.ucontrolbot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for Swagger/OpenAPI documentation.
 */
@Configuration
public class SwaggerConfig {

    private final Config config;

    /**
     * Initializes the Swagger configuration with the bot config.
     *
     * @param config The bot configuration
     */
    @Autowired
    public SwaggerConfig(Config config) {
        this.config = config;
    }

    /**
     * Configures the OpenAPI documentation.
     *
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Discord Bot API")
                        .description("API for interacting with a Java Discord bot using JDA")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("UControl Bot")
                                .url("https://github.com/yourusername/ucontrolbot")
                                .email("your.email@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(config.getApiBaseUrl())
                                .description("Local development server")
                ));
    }
}