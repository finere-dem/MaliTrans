# Refactoring: Modèle de Négociation (Bidding Model)

## ✅ Changements Implémentés

### 1. Nouvelles Entités et Enums

#### RideOfferStatus (nouveau)
- `PENDING` - Offre en attente
- `ACCEPTED` - Offre acceptée par le client
- `REJECTED` - Offre rejetée (automatiquement quand une autre est acceptée)

#### RideOffer (nouvelle entité)
- `id` - Identifiant unique
- `price` (Double) - Prix proposé par le chauffeur
- `driver` (ManyToOne Utilisateur) - Chauffeur qui soumet l'offre
- `rideRequest` (ManyToOne RideRequest) - Demande de trajet concernée
- `status` (RideOfferStatus) - Statut de l'offre
- `createdAt` (LocalDateTime) - Date de création

### 2. Modifications des Entités Existantes

#### RideRequestStatus (modifié)
**Anciens statuts (dépréciés):**
- `PENDING` → Remplacé par `OPEN_FOR_BIDS`
- `ACCEPTED` → Remplacé par `CONFIRMED`

**Nouveaux statuts:**
- `OPEN_FOR_BIDS` - Demande ouverte aux offres des chauffeurs
- `CONFIRMED` - Demande confirmée (une offre a été acceptée)
- `COMPLETED` - Trajet terminé
- `CANCELLED` - Trajet annulé

#### RideRequest (modifié)
- Ajout de la relation `@OneToMany` vers `RideOffer`
- Statut par défaut changé à `OPEN_FOR_BIDS`
- Le champ `chauffeur` est maintenant assigné uniquement quand une offre est acceptée

### 3. Nouveaux Composants

#### RideOfferRepository
Méthodes disponibles:
- `findByRideRequest(RideRequest)` - Toutes les offres pour une demande
- `findByDriver(Utilisateur)` - Toutes les offres d'un chauffeur
- `findByRideRequestAndStatus(...)` - Offres filtrées par statut

#### RideOfferDTO
- `id`, `price`, `driverId`, `driverUsername`, `rideRequestId`, `status`, `createdAt`

#### RideOfferMapper
- Mapping automatique Entity ↔ DTO avec MapStruct

#### RideOfferService
**Méthodes principales:**

1. **`submitOffer(rideRequestId, driverId, price)`**
   - Permet à un chauffeur de soumettre une offre (bid)
   - Validation: la demande doit être `OPEN_FOR_BIDS`
   - Empêche les offres multiples du même chauffeur pour la même demande

2. **`getOffers(rideRequestId)`**
   - Retourne toutes les offres pour une demande de trajet
   - Permet au client de comparer les prix proposés

3. **`acceptOffer(offerId)`**
   - Le client accepte une offre spécifique
   - Met automatiquement toutes les autres offres en `REJECTED`
   - Met à jour le `RideRequest`:
     - Statut → `CONFIRMED`
     - Assignation du chauffeur depuis l'offre acceptée

#### RideOfferController
**Endpoints:**

1. **POST `/offers/ride/{rideRequestId}`** (Chauffeur)
   - Paramètres: `driverId`, `price`
   - Soumet une offre pour une demande

2. **GET `/offers/ride/{rideRequestId}`** (Client)
   - Retourne toutes les offres pour une demande

3. **POST `/offers/{offerId}/accept`** (Client)
   - Accepte une offre spécifique

### 4. Modifications des Composants Existants

#### RideRequestService
- `createRideRequest()` - Crée automatiquement avec statut `OPEN_FOR_BIDS`
- `acceptRideRequest()` - **DÉPRÉCIÉ** (utiliser `RideOfferService.acceptOffer()` à la place)
- **Nouveau:** `getOpenRideRequests()` - Liste toutes les demandes ouvertes aux offres

#### RideRequestController
- **Nouveau:** `GET /ride/open` - Liste les demandes ouvertes (pour que les chauffeurs voient les opportunités)
- `POST /ride/{id}/accept` - **DÉPRÉCIÉ** (utiliser `/offers/{offerId}/accept` à la place)

## 🔄 Flux de Fonctionnement

### Pour le Client:
1. **Créer une demande** → `POST /ride` → Statut: `OPEN_FOR_BIDS`
2. **Voir les offres** → `GET /offers/ride/{rideRequestId}` → Liste des prix proposés
3. **Accepter une offre** → `POST /offers/{offerId}/accept` → Statut: `CONFIRMED`, chauffeur assigné

### Pour le Chauffeur:
1. **Voir les demandes ouvertes** → `GET /ride/open` → Liste des demandes disponibles
2. **Soumettre une offre** → `POST /offers/ride/{rideRequestId}?driverId=X&price=Y` → Offre créée avec statut `PENDING`
3. **Attendre la réponse** → Le client peut accepter ou non

## 📋 Migration Notes

### Changements de Statuts
- Les anciennes demandes avec `PENDING` doivent être migrées vers `OPEN_FOR_BIDS`
- Les anciennes demandes avec `ACCEPTED` doivent être migrées vers `CONFIRMED`

### Endpoints Dépréciés
- `POST /ride/{id}/accept` - Utiliser `/offers/{offerId}/accept` à la place
- La méthode `acceptRideRequest()` dans `RideRequestService` est marquée `@Deprecated`

### Compatibilité
- Les endpoints dépréciés sont conservés pour compatibilité mais ne devraient plus être utilisés
- L'ancienne logique d'assignation automatique est remplacée par le modèle de négociation

## 🎯 Avantages du Nouveau Modèle

1. **Flexibilité des prix** - Les chauffeurs fixent leurs propres tarifs
2. **Concurrence** - Les clients peuvent comparer plusieurs offres
3. **Transparence** - Tous les prix sont visibles avant acceptation
4. **Marché libre** - Modèle similaire à InDrive, adapté au contexte malien

## ⚠️ Points d'Attention

1. **Validation des prix** - Actuellement aucune validation minimale/maximale
2. **Expiration des offres** - Pas de système d'expiration automatique
3. **Notifications** - Pas de système de notification quand une offre est soumise/acceptée
4. **Limite d'offres** - Un chauffeur peut soumettre une seule offre par demande (en attente)

## 🚀 Prochaines Étapes Recommandées

1. Ajouter validation des prix (min/max)
2. Implémenter système de notifications
3. Ajouter expiration automatique des offres
4. Ajouter endpoint pour retirer une offre (chauffeur)
5. Ajouter statistiques (moyenne des prix, nombre d'offres, etc.)

