# TP API REST Sécurisée avec Spring Boot - UCAK
**Université Cheikh Ahmadou Bamba (UCAK)**  
**Module :** L3 JAVA EE - SAO  

---

## I. Composants et Versions

| Composant | Technologie / Dépendance | Version |
| :--- | :--- | :--- |
| **Java** | OpenJDK (LTS) | 21 |
| **Spring Boot** | Framework applicatif | 3.3.5 |
| **Maven** | Gestionnaire de build | 3.9.9 |
| **Spring Web** | `spring-boot-starter-web` | 3.3.5 |
| **Sécurité** | `spring-boot-starter-security` | 3.3.5 |
| **Validation** | `spring-boot-starter-validation` | 3.3.5 |
| **JPA** | `spring-boot-starter-data-jpa` | 3.3.5 |
| **PostgreSQL** | `org.postgresql:postgresql` | 42.7.4 |
| **JWT** | `io.jsonwebtoken:jjwt-*` | 0.12.6 |
| **Documentation (Swagger)** | `springdoc-openapi-starter-webmvc-ui` | 2.6.0 |

---

## II. Configuration PostgreSQL et Persistance

### 1. Commandes de création de la base et du rôle
Exécuter avec un compte d'administration PostgreSQL (`postgres`) :

```sql
-- Création du rôle applicatif tp_api (non superutilisateur, sans droits d'administration)
CREATE ROLE tp_api WITH LOGIN PASSWORD 'votre_mot_de_passe_local';
ALTER ROLE tp_api WITH NOSUPERUSER NOCREATEDB NOCREATEROLE;

-- Création de la base de données tp_rest appartenant à tp_api
CREATE DATABASE tp_rest OWNER tp_api;

-- Attribution des privilèges sur le schéma public de tp_rest
\c tp_rest
GRANT ALL ON SCHEMA public TO tp_api;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO tp_api;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO tp_api;
```

### 2. Variables d'environnement requises

| Variable | Exemple documentaire | Rôle |
| :--- | :--- | :--- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/tp_rest` | URL JDBC de la base de données |
| `DB_USERNAME` | `tp_api` | Identifiant PostgreSQL non-administrateur |
| `DB_PASSWORD` | `<mot_de_passe_local_a_definir>` | Mot de passe de `tp_api` |
| `JWT_SECRET_BASE64` | `<cle_base64_generee_localement>` | Clé secrète de signature HMAC-SHA256 (>= 256 bits) |
| `JWT_TTL_SECONDS` | `900` | Durée de validité du jeton JWT (15 minutes) |
| `SPRING_PROFILES_ACTIVE` | `tp` | Profil d'exécution de l'application |

Un modèle sans secret est disponible dans `.env.example`.

### 3. Conservation des données : `ddl-auto=update`
Le profil `tp` utilise `spring.jpa.hibernate.ddl-auto=update`. Contrairement à `create` ou `create-drop` qui réinitialisent la base à chaque cycle de vie de l'application, `update` met à jour la structure sans supprimer les enregistrements existants, assurant ainsi la persistance des utilisateurs et des produits entre les redémarrages.

---

## III. Initialisation du premier Administrateur (Bootstrap)

Pour initialiser le compte ADMIN sur une base neuve :

1. Définir temporairement les variables de bootstrap en complément du profil `tp` :
   ```bash
   export SPRING_PROFILES_ACTIVE=tp,bootstrap
   export BOOTSTRAP_ADMIN_NAME="Administrateur"
   export BOOTSTRAP_ADMIN_EMAIL="admin@ucak.edu.sn"
   export BOOTSTRAP_ADMIN_PASSWORD="AdminPassword2026!"
   ```
2. Lancer l'application :
   ```bash
   ./mvnw spring-boot:run
   ```
3. L'exécuteur `AdminBootstrapRunner` valide les critères de sécurité, hache le mot de passe avec BCrypt et crée le compte ADMIN s'il n'en existe aucun. Si un ADMIN existe déjà, l'opération est ignorée sans altérer les données.
4. Arrêter l'application, retirer le profil `bootstrap` ainsi que les variables `BOOTSTRAP_ADMIN_*`, puis redémarrer avec `SPRING_PROFILES_ACTIVE=tp`.

---

## IV. Choix d'Architecture et Sécurité

1. **Architecture en couches & DTOs :** Les contrôleurs HTTP délèguent le traitement métier aux services. Les entités JPA restent confinées à la persistance et ne sont jamais exposées directement dans l'API. Les identifiants sont générés côté serveur.
2. **Stateless & CSRF :** L'authentification repose exclusivement sur des jetons JWT transmis via l'en-tête `Authorization: Bearer <token>`. Aucun cookie ni session HTTP n'est utilisé côté serveur (`SessionCreationPolicy.STATELESS`), ce qui rend la protection CSRF superflue.
3. **Contrôle d'accès temps réel :** À chaque requête protégée, le filtre extrait l'identifiant sujet du JWT et vérifie l'existence et le rôle actuel du compte en base de données. Un compte supprimé ou rétrogradé voit ses accès actualisés immédiatement.
4. **Gestion uniforme des erreurs :** Les exceptions retournent une structure JSON standard `{ "status": ..., "message": "...", "errors": { ... } }` avec les statuts HTTP adaptés (400, 401, 403, 404, 409). Les traces techniques, mots de passe et requêtes SQL internes ne sont jamais divulgués.

---

## V. Documentation et Démonstration via Swagger UI

* **URL d'accès :** `http://localhost:8080/swagger-ui/index.html`
* **Spécification OpenAPI :** `http://localhost:8080/v3/api-docs`

### Parcours de démonstration :
1. Accéder à Swagger UI sans authentification.
2. Appeler `POST /api/auth/register` pour inscrire un utilisateur (rôle attribué : `USER`).
3. Appeler `POST /api/auth/login` pour récupérer le jeton `accessToken`.
4. Cliquer sur **Authorize** en haut à droite, coller le jeton brut (sans `Bearer`) et valider.
5. Consulter les produits via `GET /api/products` (Succès 200).
6. Tenter de créer un produit ou de lister les utilisateurs avec le compte USER (Refus 403).
7. Se reconnecter avec le compte ADMIN, renouveler le jeton dans **Authorize**, puis tester la création, modification, consultation et suppression des produits et des utilisateurs.

---

## VI. Exécution des Tests

Pour exécuter la suite de tests automatisée :
```bash
./mvnw test
```
