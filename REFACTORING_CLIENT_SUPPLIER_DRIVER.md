# Refactoring Massif : Modèle Client-Supplier-Driver

## ✅ Refactoring Terminé

Le système a été complètement refactoré pour passer du modèle "Bidding/InDrive" au modèle "Client-Supplier-Driver" selon le cahier des charges.

---

## 📋 Changements Effectués

### 1. **Rôles Mis à Jour**

**Fichier:** `model/Role.java`

**Rôles disponibles:**
- ✅ `CLIENT` - Client final
- ✅ `CHAUFFEUR` - Chauffeur/Livreur
- ✅ `SUPPLIER` - Fournisseur (shop/vendor)
- ✅ `ADMIN` - Administrateur

---

### 2. **Modèle Utilisateur Étendu**

**Fichier:** `model/Utilisateur.java`

**Nouveaux champs:**
- ✅ `status` (UserStatus enum) - PENDING_VALIDATION, ACTIVE, SUSPENDED
  - Les chauffeurs commencent avec `PENDING_VALIDATION`
- ✅ `companyName` (String) - Nom de l'entreprise (pour les Suppliers)
- ✅ `address` (String) - Adresse de l'utilisateur
- ✅ `phone` (String) - Numéro de téléphone

**Enum UserStatus:**
- `PENDING_VALIDATION` - En attente de validation (Drivers start here)
- `ACTIVE` - Actif
- `SUSPENDED` - Suspendu

---

### 3. **Nouvelle Entité : Guarantor (Garants)**

**Fichier:** `model/Guarantor.java`

**Champs:**
- `id` (Long)
- `name` (String) - Nom du garant
- `phone` (String) - Téléphone du garant
- `address` (String) - Adresse du garant
- `relation` (String) - Relation (ex: "father", "friend", "brother")
- `driver` (ManyToOne Utilisateur) - Le chauffeur qui a ce garant

**Repository:** `GuarantorRepository`
- `findByDriver(Utilisateur)` - Trouve tous les garants d'un chauffeur

**Règle métier:** Un chauffeur doit avoir 2 garants selon les spécifications.

---

### 4. **RideRequest Refactoré**

**Fichier:** `model/RideRequest.java`

**Champs supprimés:**
- ❌ Relation `offers` (OneToMany RideOffer) - **SUPPRIMÉE**
- ❌ Statut basé sur bidding - **REMPLACÉ**

**Nouveaux champs:**
- ✅ `supplier` (ManyToOne Utilisateur) - Le fournisseur (shop/vendor)
- ✅ `flowType` (FlowType enum) - CLIENT_INITIATED ou SUPPLIER_INITIATED
- ✅ `validationStatus` (ValidationStatus enum) - Statut de validation
- ✅ `qrCodePickup` (String) - Token pour validation par le fournisseur
- ✅ `qrCodeDelivery` (String) - Token pour validation par le client
- ✅ `price` (Double) - Prix fixe (plus de négociation)

**Enums:**

**FlowType:**
- `CLIENT_INITIATED` - Initiated by Client
- `SUPPLIER_INITIATED` - Initiated by Supplier

**ValidationStatus:**
- `WAITING_SUPPLIER_VALIDATION` - En attente de validation par le fournisseur
- `WAITING_CLIENT_VALIDATION` - En attente de validation par le client
- `READY_FOR_PICKUP` - Prêt pour la collecte (broadcast aux chauffeurs)
- `IN_PROGRESS` - En cours de livraison
- `DELIVERED` - Livré

---

### 5. **Entité RideOffer Supprimée**

**Fichiers supprimés:**
- ❌ `model/RideOffer.java`
- ❌ `model/RideOfferStatus.java`
- ❌ `repository/RideOfferRepository.java`
- ❌ `dto/RideOfferDTO.java`
- ❌ `mapper/RideOfferMapper.java`
- ❌ `service/RideOfferService.java`
- ❌ `controller/RideOfferController.java`
- ❌ `model/RideRequestStatus.java` (remplacé par ValidationStatus)

---

### 6. **Logique Métier Mise à Jour**

**Fichier:** `service/RideRequestService.java`

**Nouvelles méthodes:**
- `createRideRequest()` - Crée une demande avec logique de flowType
  - Si `CLIENT_INITIATED` → Status `WAITING_SUPPLIER_VALIDATION` + QR code pickup
  - Si `SUPPLIER_INITIATED` → Status `WAITING_CLIENT_VALIDATION` + QR code delivery
- `getReadyForPickupRequests()` - Liste les demandes prêtes (READY_FOR_PICKUP)
- `updateValidationStatus()` - Met à jour le statut de validation
- `assignDriver()` - Assigne un chauffeur à une demande prête
- `historyForSupplier()` - Historique pour les fournisseurs

**Flux de validation:**
1. **CLIENT_INITIATED:**
   - Client crée demande → `WAITING_SUPPLIER_VALIDATION`
   - Supplier valide → `READY_FOR_PICKUP` (broadcast aux chauffeurs)

2. **SUPPLIER_INITIATED:**
   - Supplier crée demande → `WAITING_CLIENT_VALIDATION`
   - Client valide → `READY_FOR_PICKUP` (broadcast aux chauffeurs)

3. **Après validation:**
   - Status devient `READY_FOR_PICKUP`
   - Tous les chauffeurs actifs sont notifiés
   - Un chauffeur peut s'assigner → Status `IN_PROGRESS`
   - Livraison terminée → Status `DELIVERED`

---

### 7. **Notifications Mises à Jour**

**Fichier:** `service/NotificationService.java`

**Nouvelles méthodes:**
- `notifyDriversOfReadyRequest()` - Notifie les chauffeurs d'une demande prête
- `notifySupplierForValidation()` - Notifie le fournisseur qu'une validation est nécessaire
- `notifyClientForValidation()` - Notifie le client qu'une validation est nécessaire
- `notifyDriverOfAssignment()` - Notifie le chauffeur qu'il a été assigné

**Anciennes méthodes supprimées:**
- ❌ `notifyDriversOfNewRequest()`
- ❌ `notifyClientOfNewOffer()`
- ❌ `notifyDriverOfAcceptance()`

---

### 8. **Controller Mis à Jour**

**Fichier:** `controller/RideRequestController.java`

**Nouveaux endpoints:**
- `GET /ride/ready` - Liste les demandes prêtes pour la collecte
- `POST /ride/{id}/assign` - Assigne un chauffeur à une demande
- `PATCH /ride/{id}/validation-status` - Met à jour le statut de validation
- `GET /ride/supplier/{supplierId}` - Historique fournisseur

**Endpoints supprimés:**
- ❌ `GET /ride/open` (remplacé par `/ride/ready`)
- ❌ `POST /ride/{id}/accept` (remplacé par `/ride/{id}/assign`)

---

### 9. **DTOs Mis à Jour**

**RideRequestDTO:**
- ✅ `supplierId` - ID du fournisseur
- ✅ `flowType` (String) - "CLIENT_INITIATED" ou "SUPPLIER_INITIATED"
- ✅ `validationStatus` (String) - Statut de validation
- ✅ `qrCodePickup` - QR code pour pickup
- ✅ `qrCodeDelivery` - QR code pour delivery
- ✅ `price` - Prix fixe

**Champs supprimés:**
- ❌ Références à `offers`

---

### 10. **Mappers Corrigés**

**RideRequestMapper:**
- ✅ Mapping FlowType et ValidationStatus vers String
- ✅ Méthodes `map()` pour conversion enum → String

**UtilisateurMapper:**
- ✅ Ignore les nouveaux champs (companyName, address, status) dans toEntity()

---

## 🔄 Nouveau Flux de Fonctionnement

### **Scénario 1: Client Initie**
```
1. Client crée demande (flowType: CLIENT_INITIATED)
   ↓
2. Status: WAITING_SUPPLIER_VALIDATION
   QR Code Pickup généré
   ↓
3. Supplier notifié pour validation
   ↓
4. Supplier valide la demande
   ↓
5. Status: READY_FOR_PICKUP
   Tous les chauffeurs actifs notifiés
   ↓
6. Chauffeur s'assigne
   ↓
7. Status: IN_PROGRESS
   ↓
8. Livraison terminée
   ↓
9. Status: DELIVERED
```

### **Scénario 2: Supplier Initie**
```
1. Supplier crée demande (flowType: SUPPLIER_INITIATED)
   ↓
2. Status: WAITING_CLIENT_VALIDATION
   QR Code Delivery généré
   ↓
3. Client notifié pour validation
   ↓
4. Client valide la demande
   ↓
5. Status: READY_FOR_PICKUP
   Tous les chauffeurs actifs notifiés
   ↓
6. Chauffeur s'assigne
   ↓
7. Status: IN_PROGRESS
   ↓
8. Livraison terminée
   ↓
9. Status: DELIVERED
```

---

## 📊 Structure de la Base de Données

### **Tables Créées:**
1. **utilisateur**
   - Champs: id, username, password, firstName, lastName, phone, companyName, address, vehicleType, rating, fcmToken, role, status, enabled

2. **guarantor**
   - Champs: id, name, phone, address, relation, driver_id

3. **ride_request**
   - Champs: id, origin, destination, client_id, supplier_id, chauffeur_id, flow_type, validation_status, qr_code_pickup, qr_code_delivery, price, created_at

4. **note**
   - Champs: id, from_user_id, to_user_id, rating, comment, created_at

5. **validation**
   - Champs: id, token, user_id, expiry_date

---

## ✅ Validation du Refactoring

### **Tests de Compilation:**
- ✅ Compilation réussie sans erreurs
- ✅ Tous les mappers fonctionnent correctement
- ✅ Aucune référence à RideOffer restante

### **Architecture:**
- ✅ Modèle Client-Supplier-Driver implémenté
- ✅ Système de validation avec QR codes
- ✅ FlowType pour gérer les deux sens de création
- ✅ Guarantors pour les chauffeurs
- ✅ Status de validation complet

---

## 🎯 Résultat Final

✅ **Refactoring complet terminé**
✅ **Modèle Bidding complètement supprimé**
✅ **Nouveau modèle Client-Supplier-Driver opérationnel**
✅ **Prêt pour implémentation selon le cahier des charges**

**Le système est maintenant conforme aux spécifications du cahier des charges !** 🚀

