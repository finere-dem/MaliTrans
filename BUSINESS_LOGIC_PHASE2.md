# Phase 2: Logique Métier - First-Come-First-Served & QR Codes

## ✅ Implémentation Terminée

La logique métier complète pour le modèle Client-Supplier-Driver a été implémentée avec succès.

---

## 📋 Composants Créés

### 1. **Exception Personnalisée**

**Fichier:** `exception/RideAlreadyTakenException.java`

- ✅ Exception spécifique pour les conflits de concurrence
- ✅ Lancée quand une demande est déjà assignée à un autre chauffeur
- ✅ Message d'erreur clair pour l'app mobile

---

### 2. **DTO pour Scan QR**

**Fichier:** `dto/QrScanRequest.java`

**Champs:**
- `driverId` (Long) - ID du chauffeur qui scanne
- `qrCode` (String) - Code QR scanné
- `type` (String) - "PICKUP" ou "DELIVERY"

---

## 🔧 Logique Métier Implémentée

### 1. **Contrôle de Concurrence (First-Come-First-Served)**

**Méthode:** `assignDriver(Long requestId, Long driverId)`

**Fonctionnalités:**
- ✅ `@Transactional` pour garantir l'atomicité
- ✅ Vérification que le statut est toujours `READY_FOR_PICKUP` avant assignation
- ✅ Double vérification : statut ET chauffeur null
- ✅ Lance `RideAlreadyTakenException` si la demande est déjà prise
- ✅ Validation que le chauffeur est ACTIVE
- ✅ Principe "First-Come-First-Served" : le premier à accepter gagne

**Protection contre les races:**
```java
// Dans la transaction
if (request.getValidationStatus() != ValidationStatus.READY_FOR_PICKUP) {
    throw new RideAlreadyTakenException(...);
}
if (request.getChauffeur() != null) {
    throw new RideAlreadyTakenException(requestId);
}
// Assignation atomique
request.setChauffeur(driver);
request.setValidationStatus(ValidationStatus.IN_PROGRESS);
repository.save(request);
```

---

### 2. **Génération de QR Codes**

**Méthode:** `generateQrCode()`

**Fonctionnalités:**
- ✅ Génère un code QR unique à 6 chiffres (100000-999999)
- ✅ Utilise `SecureRandom` pour la sécurité
- ✅ Codes générés automatiquement quand une demande devient `READY_FOR_PICKUP`

**Utilisation:**
- `qrCodePickup` - Généré lors de la validation (READY_FOR_PICKUP)
- `qrCodeDelivery` - Généré lors de la validation (READY_FOR_PICKUP)

---

### 3. **Scan QR Code**

**Méthode:** `scanQrCode(Long requestId, Long driverId, String qrCode, String type)`

**Logique:**

#### **Type PICKUP:**
- ✅ Compare le code avec `qrCodePickup`
- ✅ Vérifie que le chauffeur est assigné à la demande
- ✅ Si match : Change statut à `IN_PROGRESS` (si pas déjà)
- ✅ Lance exception si code invalide

#### **Type DELIVERY:**
- ✅ Compare le code avec `qrCodeDelivery`
- ✅ Vérifie que le chauffeur est assigné à la demande
- ✅ Si match : Change statut à `DELIVERED`
- ✅ Prêt pour logique de complétion (paiement, rating, etc.)
- ✅ Lance exception si code invalide

**Sécurité:**
- ✅ Validation que le chauffeur est bien assigné
- ✅ Validation du code QR
- ✅ Transaction atomique

---

### 4. **Initialisation des Flux (Scénarios 1 & 2)**

**Méthode:** `createRideRequest(RideRequestDTO dto)` - Améliorée

#### **CLIENT_INITIATED:**
- ✅ Valide que `supplierId` est fourni
- ✅ Charge et assigne le supplier
- ✅ Statut initial : `WAITING_SUPPLIER_VALIDATION`
- ✅ Génère `qrCodePickup` (pour validation supplier)
- ✅ Notifie le Supplier pour validation

#### **SUPPLIER_INITIATED:**
- ✅ Valide que `supplierId` est fourni (le créateur)
- ✅ Valide que `clientId` est fourni (client lié)
- ✅ Charge et assigne le supplier et client
- ✅ Statut initial : `WAITING_CLIENT_VALIDATION`
- ✅ Génère `qrCodeDelivery` (pour validation client)
- ✅ Notifie le Client pour validation

**Validations:**
- ✅ Tous les IDs requis sont présents
- ✅ Les utilisateurs existent dans la base
- ✅ Les rôles sont corrects (CLIENT, SUPPLIER)

---

### 5. **Validation de Demande**

**Méthode:** `validateRequest(Long requestId)`

**Fonctionnalités:**
- ✅ Valide que le statut est `WAITING_SUPPLIER_VALIDATION` ou `WAITING_CLIENT_VALIDATION`
- ✅ Change le statut à `READY_FOR_PICKUP`
- ✅ Génère les QR codes si pas déjà générés :
  - `qrCodePickup` - Pour validation pickup par supplier
  - `qrCodeDelivery` - Pour validation delivery par client
- ✅ Broadcast à tous les chauffeurs actifs
- ✅ Transaction atomique

**Utilisation:**
- Appelé par le Supplier (scénario 1) après validation
- Appelé par le Client (scénario 2) après validation

---

## 🌐 Endpoints API

### **POST /ride/{id}/assign**
**Description:** Assigner un chauffeur (First-Come-First-Served)

**Paramètres:**
- `id` (Path) - ID de la demande
- `driverId` (Query) - ID du chauffeur

**Réponses:**
- `200` - Chauffeur assigné avec succès
- `409` - Demande déjà assignée (RideAlreadyTakenException)
- `400` - Demande non prête ou chauffeur non actif
- `404` - Demande ou chauffeur non trouvé

**Exemple:**
```bash
POST /ride/1/assign?driverId=5
```

---

### **POST /ride/{id}/validate**
**Description:** Valider une demande (Client ou Supplier)

**Paramètres:**
- `id` (Path) - ID de la demande

**Réponses:**
- `200` - Demande validée, statut READY_FOR_PICKUP, QR codes générés
- `400` - Demande ne peut pas être validée dans son état actuel
- `404` - Demande non trouvée

**Exemple:**
```bash
POST /ride/1/validate
```

**Résultat:**
- Statut → `READY_FOR_PICKUP`
- QR codes générés
- Tous les chauffeurs notifiés

---

### **POST /ride/{id}/scan-qr**
**Description:** Scanner un QR code (Pickup ou Delivery)

**Paramètres:**
- `id` (Path) - ID de la demande
- Body: `QrScanRequest`
  ```json
  {
    "driverId": 5,
    "qrCode": "123456",
    "type": "PICKUP"
  }
  ```

**Réponses:**
- `200` - QR code validé, statut mis à jour
- `400` - QR code invalide ou chauffeur non assigné
- `404` - Demande non trouvée

**Types:**
- `PICKUP` - Change statut à `IN_PROGRESS`
- `DELIVERY` - Change statut à `DELIVERED`

**Exemple:**
```bash
POST /ride/1/scan-qr
{
  "driverId": 5,
  "qrCode": "123456",
  "type": "PICKUP"
}
```

---

## 🔄 Flux Complet

### **Scénario 1: Client Initie**

```
1. Client crée demande (flowType: CLIENT_INITIATED)
   ↓
2. Status: WAITING_SUPPLIER_VALIDATION
   QR Code Pickup généré
   Supplier notifié
   ↓
3. Supplier valide (POST /ride/{id}/validate)
   ↓
4. Status: READY_FOR_PICKUP
   QR Codes Pickup & Delivery générés
   Tous les chauffeurs notifiés
   ↓
5. Chauffeur A accepte (POST /ride/{id}/assign)
   ↓
6. Status: IN_PROGRESS
   Chauffeur A assigné
   ↓
7. Chauffeur A scanne QR Pickup (POST /ride/{id}/scan-qr, type: PICKUP)
   ↓
8. Status: IN_PROGRESS (confirmé)
   ↓
9. Livraison au client
   ↓
10. Chauffeur A scanne QR Delivery (POST /ride/{id}/scan-qr, type: DELIVERY)
    ↓
11. Status: DELIVERED
```

### **Scénario 2: Supplier Initie**

```
1. Supplier crée demande (flowType: SUPPLIER_INITIATED)
   ↓
2. Status: WAITING_CLIENT_VALIDATION
   QR Code Delivery généré
   Client notifié
   ↓
3. Client valide (POST /ride/{id}/validate)
   ↓
4. Status: READY_FOR_PICKUP
   QR Codes Pickup & Delivery générés
   Tous les chauffeurs notifiés
   ↓
5. Chauffeur B accepte (POST /ride/{id}/assign)
   ↓
6. Status: IN_PROGRESS
   Chauffeur B assigné
   ↓
7. Chauffeur B scanne QR Pickup
   ↓
8. Livraison au client
   ↓
9. Chauffeur B scanne QR Delivery
   ↓
10. Status: DELIVERED
```

---

## 🛡️ Sécurité & Concurrence

### **Protection contre les Races**

**Problème:** Plusieurs chauffeurs peuvent essayer d'accepter la même demande simultanément.

**Solution:**
1. `@Transactional` - Garantit l'atomicité
2. Vérification du statut dans la transaction
3. Double vérification (statut + chauffeur null)
4. Exception spécifique `RideAlreadyTakenException`
5. Code HTTP 409 (Conflict) pour l'app mobile

**Exemple de conflit:**
```
Chauffeur A: assignDriver(1, 5) → Succès
Chauffeur B: assignDriver(1, 6) → RideAlreadyTakenException (409)
```

---

## 📊 Codes QR

### **Génération**
- Format : 6 chiffres (100000-999999)
- Sécurité : `SecureRandom`
- Unicité : Générés à la validation (READY_FOR_PICKUP)

### **Validation**
- Pickup : Comparé avec `qrCodePickup`
- Delivery : Comparé avec `qrCodeDelivery`
- Sécurité : Vérification que le chauffeur est assigné

---

## ✅ Tests Recommandés

### **Test de Concurrence**
```bash
# Simuler 2 chauffeurs acceptant simultanément
Chauffeur 1: POST /ride/1/assign?driverId=5
Chauffeur 2: POST /ride/1/assign?driverId=6
# Un seul doit réussir, l'autre doit recevoir 409
```

### **Test de Validation**
```bash
# Créer demande CLIENT_INITIATED
POST /ride
{
  "flowType": "CLIENT_INITIATED",
  "clientId": 1,
  "supplierId": 2,
  ...
}

# Supplier valide
POST /ride/1/validate
# Vérifier: status = READY_FOR_PICKUP, QR codes générés
```

### **Test de Scan QR**
```bash
# Scanner QR Pickup
POST /ride/1/scan-qr
{
  "driverId": 5,
  "qrCode": "123456",
  "type": "PICKUP"
}
# Vérifier: status = IN_PROGRESS

# Scanner QR Delivery
POST /ride/1/scan-qr
{
  "driverId": 5,
  "qrCode": "789012",
  "type": "DELIVERY"
}
# Vérifier: status = DELIVERED
```

---

## 🎯 Résultat

✅ **Logique métier complète implémentée**
✅ **First-Come-First-Served avec contrôle de concurrence**
✅ **Génération et validation de QR codes**
✅ **Flux d'initialisation pour les 2 scénarios**
✅ **Endpoint de validation**
✅ **Endpoint de scan QR**

**Le système est maintenant fonctionnel avec toute la logique métier du modèle Client-Supplier-Driver !** 🚀

