package com.malitrans.transport.controller;

import com.malitrans.transport.dto.LocationMessage;
import com.malitrans.transport.service.TrackingService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @MessageMapping("/driver/location")
    public void handleDriverLocation(LocationMessage message) {
        trackingService.publishDriverLocation(message);
    }
}
