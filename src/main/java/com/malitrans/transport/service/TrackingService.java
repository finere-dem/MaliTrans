package com.malitrans.transport.service;

import com.malitrans.transport.dto.LocationMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrackingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, LocationMessage> lastLocations = new ConcurrentHashMap<>();

    public TrackingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishDriverLocation(LocationMessage message) {
        if (message == null || message.getRideId() == null) {
            return;
        }

        if (message.getTimestamp() == null || message.getTimestamp().isBlank()) {
            message.setTimestamp(Instant.now().toString());
        }

        lastLocations.put(message.getRideId(), message);
        messagingTemplate.convertAndSend("/topic/ride/" + message.getRideId(), message);
    }

    public Optional<LocationMessage> getLastLocation(Long rideId) {
        if (rideId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(lastLocations.get(rideId));
    }
}
