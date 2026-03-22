package com.malitrans.transport.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigController {

    @Value("${google.maps.api-key}")
    private String googleMapsApiKey;

    @GetMapping("/maps-key")
    public Map<String, String> getMapsKey() {
        return Map.of("apiKey", googleMapsApiKey);
    }
}

