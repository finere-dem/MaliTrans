package com.malitrans.transport.controller;

import com.malitrans.transport.dto.LocationMessage;
import com.malitrans.transport.service.TrackingService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

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

    @PostMapping("/tracking/location")
    @ResponseBody
    @PreAuthorize("hasAuthority('CHAUFFEUR')")
    public ResponseEntity<?> publishDriverLocationHttp(@RequestBody LocationMessage message) {
        if (message == null || message.getRideId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "rideId obligatoire"));
        }

        trackingService.publishDriverLocation(message);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
