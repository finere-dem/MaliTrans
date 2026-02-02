# Migration H2 → PostgreSQL

## ✅ Changements Effectués

### 1. **pom.xml** - Dépendances

**Ajouté:**
- ✅ `org.postgresql:postgresql` - Driver PostgreSQL pour production

**Commenté:**
- ⚠️ `com.h2database:h2` - Conservé en commentaire pour tests locaux si nécessaire

### 2. **application.properties** - Configuration

**Configuration PostgreSQL:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/delivery_app
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=password
```

**Configuration JPA/Hibernate:**
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
```

**Important:**
- ✅ `ddl-auto=update` - Les tables sont créées/mises à jour automatiquement, les données sont conservées entre redémarrages
- ✅ `PostgreSQLDialect` - Utilise le dialecte PostgreSQL pour les requêtes SQL

---

## 📋 Étapes pour Démarrer

### 1. Installer PostgreSQL (si pas déjà fait)

**Windows:**
- Télécharger depuis https://www.postgresql.org/download/windows/
- Installer avec les paramètres par défaut
- Notez le mot de passe du superutilisateur `postgres`

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
```

**macOS:**
```bash
brew install postgresql
brew services start postgresql
```

### 2. Créer la Base de Données

**Via psql (ligne de commande):**
```bash
# Se connecter à PostgreSQL
psql -U postgres

# Créer la base de données
CREATE DATABASE delivery_app;

# Vérifier la création
\l

# Quitter
\q
```

**Via pgAdmin (interface graphique):**
1. Ouvrir pgAdmin
2. Se connecter au serveur PostgreSQL
3. Clic droit sur "Databases" → "Create" → "Database"
4. Nom: `delivery_app`
5. Cliquer "Save"

### 3. Mettre à Jour le Mot de Passe (si nécessaire)

**Dans `application.properties`:**
```properties
spring.datasource.password=VOTRE_MOT_DE_PASSE
```

**Changer le mot de passe PostgreSQL (si nécessaire):**
```bash
psql -U postgres
ALTER USER postgres PASSWORD 'nouveau_mot_de_passe';
```

### 4. Démarrer l'Application

```bash
mvn spring-boot:run
```

**Hibernate va automatiquement:**
- ✅ Se connecter à PostgreSQL
- ✅ Créer les tables si elles n'existent pas
- ✅ Mettre à jour le schéma si nécessaire (ajout de colonnes, etc.)
- ✅ Conserver les données existantes

---

## 🔍 Vérification

### Vérifier la Connexion

**Dans les logs au démarrage, vous devriez voir:**
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

**Si erreur de connexion:**
- Vérifier que PostgreSQL est démarré
- Vérifier le nom de la base de données (`delivery_app`)
- Vérifier le username/password dans `application.properties`
- Vérifier que le port 5432 est accessible

### Vérifier les Tables Créées

**Via psql:**
```bash
psql -U postgres -d delivery_app
\dt
```

**Vous devriez voir:**
- `utilisateur`
- `ride_request`
- `ride_offer`
- `note`
- `validation`

---

## 🔄 Retour à H2 (si nécessaire)

Si vous voulez revenir à H2 pour les tests :

1. **Dans `pom.xml`:** Décommenter la dépendance H2
2. **Dans `application.properties`:** Remplacer par :
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
```

---

## ⚠️ Notes Importantes

### Conservation des Données
- ✅ Avec `ddl-auto=update`, les données sont **conservées** entre les redémarrages
- ✅ Les tables sont créées automatiquement au premier démarrage
- ✅ Les modifications de schéma (ajout de colonnes) sont appliquées automatiquement

### Sécurité
- ⚠️ **Changez le mot de passe** dans `application.properties` avant la production
- ⚠️ Utilisez des variables d'environnement pour les credentials en production :
```properties
spring.datasource.password=${DB_PASSWORD}
```

### Performance
- `spring.jpa.show-sql=false` - Désactive l'affichage des requêtes SQL (meilleure performance)
- `hibernate.format_sql=true` - Formate les requêtes si `show-sql=true`

---

## 🎯 Résultat

✅ **Migration vers PostgreSQL terminée**
✅ **Configuration prête pour la production**
✅ **Données conservées entre redémarrages**
✅ **Tables créées automatiquement**

**L'application est maintenant prête pour utiliser PostgreSQL !** 🚀

