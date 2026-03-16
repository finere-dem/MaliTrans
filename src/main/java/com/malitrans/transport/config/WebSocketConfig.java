package com.malitrans.transport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Enregistrement de l'endpoint websocket
        registry.addEndpoint("/ws-tracking")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Activation d'un broker simple en mémoire pour les abonnements
        registry.enableSimpleBroker("/topic");
        // Les messages envoyés au serveur doivent commencer par /app
        registry.setApplicationDestinationPrefixes("/app");
    }
}
