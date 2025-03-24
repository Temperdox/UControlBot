package com.cottonlesergal.ucontrolbot.config;

import com.cottonlesergal.ucontrolbot.listeners.GuildListener;
import com.cottonlesergal.ucontrolbot.listeners.MessageListener;
import com.cottonlesergal.ucontrolbot.listeners.UserListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ListenerConfig {

    @Bean
    public GuildListener guildListener() {
        return new GuildListener();
    }

    @Bean
    public MessageListener messageListener() {
        return new MessageListener();
    }

    @Bean
    public UserListener userListener() {
        return new UserListener();
    }
}