# Variables d'environnement - MaliTrans (Woyo)

Ce document liste toutes les variables d'environnement nécessaires au fonctionnement de l'application MaliTrans.

## 🔐 Variables requises pour la production

Ces variables **DOIVENT** être définies en production. Ne laissez jamais de valeurs par défaut en production.

### Base de données PostgreSQL (Aiven)

| Variable | Description | Exemple | Obligatoire |
|----------|-------------|---------|-------------|
| `DB_URL` | URL complète de connexion à la base de données PostgreSQL | `jdbc:postgresql://host:port/database?sslmode=require` | ✅ Oui |
| `DB_USERNAME` | Nom d'utilisateur de la base de données | `avnadmin` | ✅ Oui |
| `DB_PASSWORD` | Mot de passe de la base de données | `VotreMotDePasseSecret` | ✅ Oui |

### Sécurité JWT

| Variable | Description | Exemple | Obligatoire |
|----------|-------------|---------|-------------|
| `JWT_SECRET` | Clé secrète pour signer et vérifier les tokens JWT (minimum 256 bits recommandé) | Généré avec `openssl rand -base64 32` | ✅ Oui |

## 🛠️ Configuration par défaut (développement local)

Le fichier `application.properties` contient des valeurs par défaut pour faciliter le développement local. Ces valeurs **NE DOIVENT JAMAIS** être utilisées en production.

**Valeurs par défaut (développement uniquement) :**
- `DB_URL` : `jdbc:postgresql://localhost:5432/malitrans?sslmode=prefer` (PostgreSQL local)
- `DB_USERNAME` : `avnadmin`
- `DB_PASSWORD` : `YOUR_PASSWORD_HERE` (⚠️ **À DÉFINIR** - pas de valeur par défaut)
- `JWT_SECRET` : `YOUR_JWT_SECRET_HERE` (⚠️ **À DÉFINIR** - pas de valeur par défaut)

**Note :** Pour utiliser Aiven au lieu de PostgreSQL local, définissez la variable d'environnement `DB_URL` avec votre URL Aiven.

### 🔒 Profil de production

Un fichier `application-prod.properties` a été créé pour la production. Ce profil **N'A PAS** de valeurs par défaut et **EXIGE** que toutes les variables d'environnement soient définies.

**Pour utiliser le profil de production :**
```bash
# Via la ligne de commande
java -jar app.jar --spring.profiles.active=prod

# Ou définir la variable d'environnement
export SPRING_PROFILES_ACTIVE=prod
```

**Dans IntelliJ IDEA :**
- Run Configurations → Active profiles : `prod`
- Assurez-vous que toutes les variables d'environnement sont définies

## 📝 Comment générer une clé JWT sécurisée

Pour générer une clé secrète JWT sécurisée (256 bits) :

```bash
# Linux/Mac
openssl rand -base64 32

# Windows (PowerShell)
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

## 🚀 Configuration selon l'environnement

### Développement local

**Option 1 : Utiliser les valeurs par défaut** (recommandé pour le dev)
- Aucune configuration nécessaire, l'application utilisera les valeurs par défaut de `application.properties`

**Option 2 : Surcharger avec des variables d'environnement**
- Définissez les variables dans votre IDE (IntelliJ IDEA) ou dans un fichier `.env` (non versionné)
- Voir le guide IntelliJ ci-dessous

**Option 3 : Utiliser le profil prod en local** (pour tester la configuration)
- Activez le profil `prod` et définissez toutes les variables d'environnement
- Utile pour vérifier que votre configuration de production fonctionne

### Production

Définissez les variables d'environnement dans votre plateforme de déploiement :

- **Heroku** : `heroku config:set DB_PASSWORD=votre_mot_de_passe`
- **Docker** : Via `docker run -e DB_PASSWORD=...` ou `docker-compose.yml`
- **Kubernetes** : Via `ConfigMap` ou `Secrets`
- **Aiven** : Via les variables d'environnement de votre service

## ⚠️ Sécurité

1. **Ne jamais commiter** les valeurs réelles des secrets dans Git
2. **Ne jamais utiliser** les valeurs par défaut en production
3. **Utiliser des secrets managers** (AWS Secrets Manager, HashiCorp Vault, etc.) en production
4. **Rotater régulièrement** les clés JWT et mots de passe
5. Le fichier `.env` est déjà dans `.gitignore` - utilisez-le pour le développement local

## 📋 Checklist de déploiement

Avant de déployer en production, vérifiez que :

- [ ] Le profil `prod` est activé (`--spring.profiles.active=prod` ou `SPRING_PROFILES_ACTIVE=prod`)
- [ ] `DB_URL` est défini avec l'URL de production
- [ ] `DB_USERNAME` est défini avec l'utilisateur de production
- [ ] `DB_PASSWORD` est défini avec un mot de passe fort
- [ ] `JWT_SECRET` est défini avec une clé générée aléatoirement (min 256 bits)
- [ ] Aucune valeur par défaut n'est utilisée en production (le profil `prod` n'en a pas)
- [ ] Les secrets sont stockés de manière sécurisée (secrets manager)
- [ ] L'application démarre sans erreur avec le profil `prod`

---

## 🎯 Guide IntelliJ IDEA - Configuration des variables d'environnement

### 🚀 Démarrage rapide (Développement local)

**Aucune configuration nécessaire !** L'application utilise maintenant des valeurs par défaut pour le développement local. Vous pouvez simplement lancer l'application sans configuration supplémentaire.

### Configuration avancée

Si vous souhaitez surcharger les valeurs par défaut ou tester le profil de production :

### Méthode 1 : Utiliser les valeurs par défaut (Recommandé pour le dev)

1. **Lancer l'application directement**
   - Cliquez sur le bouton **Run** (▶️) ou **Debug** (🐛)
   - L'application utilisera automatiquement les valeurs par défaut de `application.properties`
   - Aucune configuration supplémentaire nécessaire

### Méthode 2 : Surcharger avec des variables d'environnement

1. **Ouvrir les Run Configurations**
   - Cliquez sur le menu déroulant en haut à droite (à côté du bouton Run/Debug)
   - Sélectionnez **"Edit Configurations..."**
   - Ou utilisez le raccourci : `Shift + Alt + F10` puis `0` (zéro)

2. **Sélectionner votre configuration**
   - Dans la liste de gauche, sélectionnez votre configuration Spring Boot (ex: `MaliTransApplication`)
   - Si elle n'existe pas, créez-en une nouvelle :
     - Cliquez sur **"+"** en haut à gauche
     - Sélectionnez **"Spring Boot"**
     - Nommez-la (ex: `MaliTransApplication`)
     - Dans **"Main class"**, sélectionnez `com.malitrans.transport.MaliTransApplication`

3. **Configurer les variables d'environnement**
   - Dans la section **"Environment variables"**, cliquez sur le bouton **"..."** (trois points)
   - Cliquez sur **"+"** pour ajouter chaque variable :
     - `DB_URL` = `jdbc:postgresql://votre-host-aiven:port/database?sslmode=require` (ex: `jdbc:postgresql://pg-xxx.l.aivencloud.com:24759/defaultdb?sslmode=require`)
     - `DB_USERNAME` = `avnadmin`
     - `DB_PASSWORD` = `YOUR_PASSWORD_HERE` (votre mot de passe réel)
     - `JWT_SECRET` = `YOUR_JWT_SECRET_HERE` (générez-en une avec `openssl rand -base64 32`)
   - Cliquez sur **"OK"** pour fermer la fenêtre
   - Cliquez sur **"Apply"** puis **"OK"** pour sauvegarder

4. **Lancer l'application**
   - Utilisez le bouton **Run** (▶️) ou **Debug** (🐛) en haut à droite
   - L'application utilisera maintenant les variables d'environnement définies

### Méthode 3 : Utiliser le profil de production (Pour tester la config prod)

1. **Ouvrir les Run Configurations**
   - Cliquez sur le menu déroulant en haut à droite
   - Sélectionnez **"Edit Configurations..."**

2. **Activer le profil prod**
   - Dans la section **"Active profiles"**, entrez : `prod`
   - ⚠️ **Important** : Le profil `prod` n'a pas de valeurs par défaut, vous DEVEZ définir toutes les variables d'environnement

3. **Configurer les variables d'environnement** (OBLIGATOIRE avec le profil prod)
   - Dans la section **"Environment variables"**, cliquez sur **"..."**
   - Ajoutez toutes les variables requises :
     - `DB_URL` = `jdbc:postgresql://...`
     - `DB_USERNAME` = `votre_username`
     - `DB_PASSWORD` = `votre_mot_de_passe`
     - `JWT_SECRET` = `votre_cle_secrete` (générée avec `openssl rand -base64 32`)

4. **Lancer l'application**
   - L'application utilisera le profil `prod` et exigera toutes les variables d'environnement

### Méthode 4 : Via un fichier .env (Alternative)

1. **Créer un fichier `.env` à la racine du projet** (déjà dans `.gitignore`)
   ```
   DB_URL=jdbc:postgresql://votre-host-aiven:port/database?sslmode=require
   DB_USERNAME=avnadmin
   DB_PASSWORD=VotreMotDePasse
   JWT_SECRET=VotreCleSecreteJWT
   ```

2. **Installer le plugin EnvFile** (optionnel)
   - `File` → `Settings` → `Plugins`
   - Recherchez "EnvFile" et installez-le
   - Dans les Run Configurations, activez "EnvFile" et sélectionnez votre fichier `.env`

### Méthode 5 : Via les VM Options (Moins recommandé)

Dans les Run Configurations, section **"VM options"**, ajoutez :
```
-DDB_URL=jdbc:postgresql://... -DDB_USERNAME=avnadmin -DDB_PASSWORD=... -DJWT_SECRET=...
```

⚠️ **Note** : Cette méthode est moins propre et plus difficile à maintenir.

### 🔍 Vérification

Pour vérifier que les variables sont bien chargées, ajoutez temporairement dans votre code :
```java
@Value("${spring.datasource.username}")
private String dbUsername;

@PostConstruct
public void checkEnv() {
    System.out.println("DB Username: " + dbUsername);
}
```

Ou consultez les logs au démarrage de l'application Spring Boot.

### 💡 Astuces

**Créer des Run Configurations séparées :**
- `MaliTransApplication - Dev` : Aucun profil, utilise les valeurs par défaut
- `MaliTransApplication - Dev Custom` : Aucun profil, mais avec variables d'environnement personnalisées
- `MaliTransApplication - Prod` : Profil `prod`, avec toutes les variables d'environnement définies (pour tester la config prod en local)

**Vérifier quel profil est actif :**
- Regardez les logs au démarrage : `The following profiles are active: prod` (ou aucun si vous utilisez le profil par défaut)
