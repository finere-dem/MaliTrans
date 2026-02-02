package com.malitrans.transport.exception;

public class RideAlreadyTakenException extends RuntimeException {
    public RideAlreadyTakenException(String message) {
        super(message);
    }
    
    public RideAlreadyTakenException(Long rideId) {
        super("Ride request with ID " + rideId + " has already been assigned to another driver");
    }
}

