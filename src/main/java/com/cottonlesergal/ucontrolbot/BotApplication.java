package com.cottonlesergal.ucontrolbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Main Spring Boot application class.
 * Entry point for the Discord bot application.
 */
@SpringBootApplication
public class BotApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BotApplication.class, args);
    }

    /**
     * Configures web MVC to forward non-API requests to the single page application.
     *
     * @return WebMvcConfigurer instance
     */
    @Bean
    public WebMvcConfigurer forwardToIndex() {
        return new WebMvcConfigurer() {

        };
    }
}