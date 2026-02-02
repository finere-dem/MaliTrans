# 🗑️ REQUÊTES SQL POUR SUPPRIMER TOUTES LES COURSES

**Base de données:** PostgreSQL  
**Table:** `ride_request`  
**Base:** `delivery_app`

---

## ⚠️ ATTENTION

**Ces requêtes supprimeront TOUTES les courses de la base de données.**
- Les utilisateurs (clients, chauffeurs, suppliers) ne seront **PAS** supprimés
- Seules les courses (`ride_request`) seront supprimées
- Les relations `@ManyToOne` vers `Utilisateur` sont préservées (pas de CASCADE DELETE)

---

## 📋 REQUÊTES SQL

### Option 1 : Suppression Simple (Recommandée)

```sql
-- Vérifier le nombre de courses avant suppression
SELECT COUNT(*) FROM ride_request;

-- Supprimer toutes les courses
DELETE FROM ride_request;

-- Vérifier que tout est supprimé
SELECT COUNT(*) FROM ride_request;
```

### Option 2 : Suppression avec Réinitialisation de la Séquence (Si vous voulez repartir à ID=1)

```sql
-- Supprimer toutes les courses
DELETE FROM ride_request;

-- Réinitialiser la séquence d'auto-increment pour repartir à 1
ALTER SEQUENCE ride_request_id_seq RESTART WITH 1;

-- Vérifier
SELECT COUNT(*) FROM ride_request;
SELECT nextval('ride_request_id_seq'); -- Devrait retourner 1
```

### Option 3 : Suppression Sécurisée avec Transaction (Rollback possible)

```sql
-- Démarrer une transaction
BEGIN;

-- Vérifier le nombre de courses
SELECT COUNT(*) FROM ride_request;

-- Supprimer toutes les courses
DELETE FROM ride_request;

-- Vérifier le résultat
SELECT COUNT(*) FROM ride_request;

-- Si tout est OK, valider :
COMMIT;

-- OU si vous voulez annuler :
-- ROLLBACK;
```

### Option 4 : Suppression avec Filtre (Pour tester)

```sql
-- Supprimer seulement les courses avec un statut spécifique
DELETE FROM ride_request WHERE validation_status = 'COMPLETED';

-- Ou supprimer les courses créées avant une date
DELETE FROM ride_request WHERE created_at < '2024-01-01';

-- Ou supprimer toutes sauf celles d'un client spécifique
DELETE FROM ride_request WHERE client_id != 1;
```

---

## 🔍 REQUÊTES DE VÉRIFICATION

### Avant suppression
```sql
-- Compter toutes les courses
SELECT COUNT(*) as total_courses FROM ride_request;

-- Voir la répartition par statut
SELECT validation_status, COUNT(*) as count 
FROM ride_request 
GROUP BY validation_status 
ORDER BY count DESC;

-- Voir les dernières courses créées
SELECT id, origin, destination, validation_status, created_at 
FROM ride_request 
ORDER BY created_at DESC 
LIMIT 10;
```

### Après suppression
```sql
-- Vérifier que tout est supprimé
SELECT COUNT(*) FROM ride_request;

-- Vérifier que les utilisateurs existent toujours
SELECT COUNT(*) FROM utilisateur;

-- Vérifier la structure de la table (devrait être vide mais structure intacte)
SELECT * FROM ride_request LIMIT 1;
```

---

## 🧪 TEST DU COMPORTEMENT DE L'APPLICATION

### Scénarios à tester après suppression :

1. **GET /api/ride/ready**
   ```bash
   curl http://localhost:8080/api/ride/ready
   ```
   **Attendu :** Liste vide `[]`

2. **GET /api/ride/client/history**
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/ride/client/history
   ```
   **Attendu :** Liste vide `[]`

3. **GET /api/ride/chauffeur/history**
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/ride/chauffeur/history?page=1&limit=20
   ```
   **Attendu :** PaginatedResponse avec `data: []` et `totalElements: 0`

4. **GET /api/ride/chauffeur/active**
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/ride/chauffeur/active
   ```
   **Attendu :** Liste vide `[]`

5. **POST /api/ride** (Créer une nouvelle course)
   ```bash
   curl -X POST http://localhost:8080/api/ride \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "origin": "Point A",
       "destination": "Point B",
       "flowType": "CLIENT_INITIATED",
       "price": 1000.0
     }'
   ```
   **Attendu :** Nouvelle course créée avec `id: 1` (si séquence réinitialisée)

---

## 📝 REQUÊTE COMPLÈTE RECOMMANDÉE (Copier-Coller)

```sql
-- ============================================
-- SCRIPT DE SUPPRESSION COMPLÈTE DES COURSES
-- ============================================

-- 1. Vérifier avant suppression
SELECT 
    COUNT(*) as total_courses,
    COUNT(DISTINCT client_id) as unique_clients,
    COUNT(DISTINCT chauffeur_id) as unique_drivers
FROM ride_request;

-- 2. Voir la répartition par statut
SELECT 
    validation_status, 
    COUNT(*) as count 
FROM ride_request 
GROUP BY validation_status 
ORDER BY count DESC;

-- 3. Supprimer toutes les courses
DELETE FROM ride_request;

-- 4. Réinitialiser la séquence (optionnel - pour repartir à ID=1)
ALTER SEQUENCE ride_request_id_seq RESTART WITH 1;

-- 5. Vérifier après suppression
SELECT COUNT(*) as remaining_courses FROM ride_request;

-- 6. Vérifier que les utilisateurs existent toujours
SELECT COUNT(*) as total_users FROM utilisateur;
```

---

## 🚨 EN CAS DE PROBLÈME

### Si erreur de contrainte de clé étrangère :
```sql
-- Désactiver temporairement les contraintes (ATTENTION : Dangereux)
SET session_replication_role = 'replica';
DELETE FROM ride_request;
SET session_replication_role = 'origin';
```

### Si vous voulez restaurer depuis une sauvegarde :
```sql
-- Restaurer depuis un dump (si vous avez fait une sauvegarde)
-- psql -U postgres -d delivery_app < backup.sql
```

---

## 💡 CONSEIL

**Avant de supprimer, faites une sauvegarde :**
```bash
# Depuis le terminal
pg_dump -U postgres -d delivery_app -t ride_request > ride_request_backup.sql

# Pour restaurer plus tard :
# psql -U postgres -d delivery_app < ride_request_backup.sql
```

---

**FIN DU GUIDE**
