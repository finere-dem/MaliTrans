package com.malitrans.transport.dto;

public class LocationMessage {
    private Long rideId;
    private double latitude;
    private double longitude;
    private String timestamp;

    public LocationMessage() {
    }

    public LocationMessage(Long rideId, double latitude, double longitude, String timestamp) {
        this.rideId = rideId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public Long getRideId() {
        return rideId;
    }

    public void setRideId(Long rideId) {
        this.rideId = rideId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
