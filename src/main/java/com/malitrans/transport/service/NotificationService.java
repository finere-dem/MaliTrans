package com.malitrans.transport.service;

import com.malitrans.transport.model.RideRequest;

/**
 * Service interface for handling notifications.
 * Currently uses mock implementations (System.out.println).
 * Ready for Firebase Cloud Messaging (FCM) integration.
 */
public interface NotificationService {

    /**
     * Notify all available drivers about a new ride request ready for pickup
     * @param request The ride request that is ready for pickup
     */
    void notifyDriversOfReadyRequest(RideRequest request);

    /**
     * Notify the supplier that validation is needed (CLIENT_INITIATED flow)
     * @param request The ride request waiting for supplier validation
     */
    void notifySupplierForValidation(RideRequest request);

    /**
     * Notify the client that validation is needed (SUPPLIER_INITIATED flow)
     * @param request The ride request waiting for client validation
     */
    void notifyClientForValidation(RideRequest request);

    /**
     * Notify the driver that they have been assigned to a delivery
     * @param request The ride request assigned to the driver
     */
    void notifyDriverOfAssignment(RideRequest request);
}

