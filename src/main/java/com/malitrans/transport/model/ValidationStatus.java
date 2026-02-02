package com.malitrans.transport.model;



public enum ValidationStatus {
    WAITING_SUPPLIER_VALIDATION,   // Legacy (à garder pour l'instant)
    WAITING_CLIENT_VALIDATION,
    READY_FOR_PICKUP,              // Prêt pour la collecte
    DRIVER_ACCEPTED,               // Chauffeur assigné
    IN_PROGRESS,                   // <--- AJOUTEZ CECI (C'est ce que l'erreur demande)
    IN_TRANSIT,                    // (Vous pouvez garder celui-ci si vous l'utilisez ailleurs)
    COMPLETED,
    CANCELED                       // Ajoutez aussi CANCELED au cas où
}

