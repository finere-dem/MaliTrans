# Analyse du projet MaliTrans & Intégration FCM

## 1. Résumé de l'existant

### 1.1 OTP / Email

| Élément | Présent ? | Détail |
|--------|-----------|--------|
| **Envoi d'email** | ❌ **Non** | Aucune dépendance `JavaMailSender`, aucun service d'email. |
| **Envoi OTP** | ✅ **Oui (SMS uniquement)** | OTP envoyé **par SMS** via l'interface `SmsService`. |
| **Classe principale OTP** | `OtpService` | `src/main/java/com/malitrans/transport/service/OtpService.java` |
| **Implémentation SMS** | `MockSmsService` | Implémente `SmsService`, marquée `@Primary`. Envoi = log console (`System.out` / `logger.info`). Pas d'envoi réel. |
| **Où l'OTP est déclenché** | `AuthServiceImpl.register()` | Après `utilisateurRepository.save(utilisateur)`, appel à `otpService.createOtpForRegistration(utilisateur)` (ligne 101). |

**Conclusion OTP :** Pas d'email. Flux actuel : **Inscription → OTP généré et envoyé via `SmsService` (mock console)**. Pour un envoi réel, il faudrait une implémentation réelle de `SmsService` (ex. Twilio, AWS SNS) ou, si vous le souhaitez, ajouter un canal email en parallèle (nouveau service + configuration).

---

### 1.2 Rides (Courses)

| Élément | Présent ? | Détail |
|--------|-----------|--------|
| **Service de création de course** | ✅ **Oui** | `RideRequestService` – méthode `createRideRequest(...)`. |
| **Contrôleur** | ✅ **Oui** | `RideRequestController` – `POST /ride` appelle `service.createRideRequest(dto, currentUserId, currentUserRole)`. |
| **Notifications déjà branchées** | ✅ **Oui** | `RideRequestService` injecte `NotificationService` et l’appelle aux bons endroits. |

**Points d’appel à `NotificationService` dans `RideRequestService` :**

| Méthode | Quand | Appel |
|---------|--------|--------|
| `createRideRequest()` | Course créée en **CLIENT_INITIATED** (statut direct `READY_FOR_PICKUP`) | `notificationService.notifyDriversOfReadyRequest(saved)` |
| `createRideRequest()` | Course créée en **SUPPLIER_INITIATED** (statut `WAITING_CLIENT_VALIDATION`) | `notificationService.notifyClientForValidation(saved)` |
| `validateRequest()` | Client/Supplier valide → statut `READY_FOR_PICKUP` | `notificationService.notifyDriversOfReadyRequest(saved)` |
| `assignDriver()` | Chauffeur assigné à la course | `notificationService.notifyDriverOfAssignment(saved)` |

**Conclusion Ride :** La logique métier Ride est cohérente et les **notifications sont déjà intégrées dans le flux réel**. L’implémentation actuelle de `NotificationService` est un **mock** (logs + `System.out`). Il suffit de faire en sorte que cette couche envoie de vraies notifications FCM au lieu d’afficher dans la console.

---

### 1.3 Notifications & FCM

| Élément | Présent ? | Détail |
|--------|-----------|--------|
| **Interface NotificationService** | ✅ **Oui** | 4 méthodes : `notifyDriversOfReadyRequest`, `notifySupplierForValidation`, `notifyClientForValidation`, `notifyDriverOfAssignment`. |
| **Implémentation** | ✅ **Oui (mock)** | `NotificationServiceImpl` – logs détaillés, lit déjà `user.getFcmToken()` pour chaque destinataire, mais n’envoie pas de push. |
| **Champ FCM côté utilisateur** | ✅ **Oui** | `Utilisateur.fcmToken` présent. |
| **Endpoint pour enregistrer le token FCM** | ❌ **Non** | Aucun `PUT/PATCH .../fcm-token` ou équivalent trouvé. À ajouter pour que l’app mobile enregistre le token. |

---

## 2. Proposition d’intégration FCM (dans le flux réel)

Objectif : **ne pas créer un contrôleur de test FCM isolé**, mais faire envoyer de vraies notifications FCM aux bons moments en réutilisant le flux Ride existant.

### 2.1 Où injecter FCM

- **RideRequestService** : ne pas le modifier. Il appelle déjà `NotificationService`.
- **NotificationServiceImpl** : c’est ici qu’il faut **injecter un `FcmService`** (ou équivalent) et, pour chaque méthode, appeler FCM en plus ou à la place des `System.out`.

Donc : **FCM branché dans `NotificationServiceImpl`**, qui reste appelé par `RideRequestService` comme aujourd’hui.

### 2.2 Fichiers à ajouter / modifier

1. **Créer une interface + implémentation FCM**
   - Ex. `FcmService.sendToToken(String fcmToken, String title, String body, Map<String, String> data)` (et éventuellement `sendToTokens(List<String> tokens, ...)` pour « Nouveau trajet » à plusieurs chauffeurs).
   - Implémentation utilisant le SDK Firebase Admin (ou HTTP v1) avec le fichier `service-account.json` (déjà présent dans `src/main/resources`).

2. **Modifier `NotificationServiceImpl`**
   - Injecter `FcmService`.
   - Dans `notifyDriversOfReadyRequest(request)` : pour chaque chauffeur actif ayant un `fcmToken` non vide, appeler `FcmService.sendToToken(..., "Nouveau trajet", "Une course est disponible : ...", map("rideId", request.getId().toString()))`.
   - Même idée pour `notifySupplierForValidation`, `notifyClientForValidation`, `notifyDriverOfAssignment` : un titre + un corps + éventuellement un payload `rideId`, `type`, etc.

3. **Endpoint pour enregistrer le token FCM**
   - Ex. `PATCH /api/users/me/fcm-token` ou `PUT /api/users/me/fcm-token` avec body `{ "fcmToken": "..." }`.
   - Dans le handler : récupérer l’utilisateur courant (`SecurityUtil.getCurrentUserId()` ou équivalent), charger l’entité `Utilisateur`, setter `setFcmToken(dto.getFcmToken())`, sauvegarder.
   - À placer dans `UserController` ou `AuthController`, protégé par authentification.

4. **OTP et FCM**
   - Aujourd’hui l’OTP est envoyé par SMS (mock). Ajouter une **notification push** « Code de vérification envoyé » ou « Vérifiez votre téléphone pour le code » est **optionnel** et plutôt secondaire par rapport au SMS. Si vous voulez une notif push en plus de l’SMS après envoi OTP, on peut appeler `NotificationService` (ou FCM) depuis `OtpService` après `smsService.sendOtp(...)` – à discuter selon le besoin (souvent le SMS suffit pour l’OTP).

---

## 3. Résumé des modifications proposées

| # | Fichier / composant | Action |
|---|---------------------|--------|
| 1 | **FcmService** (interface) | Créer – méthode(s) d’envoi vers token(s). |
| 2 | **FcmServiceImpl** (ou FcmService avec Firebase Admin) | Créer – lecture `service-account.json`, envoi via Firebase. |
| 3 | **NotificationServiceImpl** | Modifier – injecter `FcmService`, dans chaque méthode : pour chaque destinataire avec `fcmToken` non null, appeler FCM avec titre/corps/data adaptés. Garder les logs si besoin. |
| 4 | **UserController** (ou AuthController) | Ajouter endpoint `PATCH /users/me/fcm-token` (body `{ "fcmToken": "..." }`), mettre à jour `Utilisateur.fcmToken` pour l’utilisateur connecté. |
| 5 | **RideRequestService** | Aucun changement – il appelle déjà `NotificationService`. |
| 6 | **OtpService / Auth** | Pas obligatoire d’ajouter FCM ici ; possible plus tard si vous voulez une notif push en plus du SMS pour l’OTP. |

---

## 4. Cohérence métier avant FCM

- **Ride** : Création, validation, assignation chauffeur, états (`READY_FOR_PICKUP`, `DRIVER_ACCEPTED`, `IN_TRANSIT`, `COMPLETED`) et notifications métier sont alignés. Les appels à `NotificationService` sont aux bons endroits.
- **OTP** : Un seul canal (SMS via `SmsService`), pas d’email. Pour FCM, rien à changer côté métier OTP sauf si vous ajoutez volontairement une notif push « OTP envoyé ».
- **FCM** : Le modèle (`Utilisateur.fcmToken`) et le flux (NotificationService appelé après création / validation / assignation) sont prêts. Il ne manque que l’implémentation réelle d’envoi (FcmService) et l’endpoint d’enregistrement du token.

En résumé : vous pouvez intégrer FCM directement dans le flux réel en ajoutant un **FcmService**, en l’utilisant dans **NotificationServiceImpl**, et en exposant **PATCH /users/me/fcm-token** pour enregistrer le token par utilisateur.
