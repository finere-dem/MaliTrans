package com.malitrans.transport.controller;

import com.malitrans.transport.dto.LocationMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class TrackingController {

    private final SimpMessagingTemplate messagingTemplate;

    public TrackingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/driver/location")
    public void handleDriverLocation(LocationMessage message) {
        // Rediffusion instantanée du message vers le topic de la course correspondante
        // Les clients (ex: l'app client Flutter) s'abonneront à /topic/ride/{rideId}
        String destination = "/topic/ride/" + message.getRideId();
        messagingTemplate.convertAndSend(destination, message);
    }
}
