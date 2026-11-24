# COMPTE RENDU DE TP : API REST SÉCURISÉE AVEC SPRING BOOT
**Établissement :** Université Cheikh Ahmadou Bamba (UCAK)  
**Formation :** Licence 3 Informatique (L3 JAVA EE - SAO)  
**Date limite de remise :** 25 Novembre 2026  

---

## 1. Introduction et Objectifs du Projet

Ce travail pratique a pour objectif la conception et le développement d'une API REST sécurisée permettant la gestion de produits et d'utilisateurs. L'application est développée avec le framework **Spring Boot** et s'appuie sur une base de données **PostgreSQL**.

L'accès aux ressources est protégé par une authentification par email et mot de passe délivrant un jeton **JWT (JSON Web Token)** signé en HMAC-SHA256. L'autorisation d'accès aux différentes routes est déterminée selon le rôle de l'utilisateur (`USER` ou `ADMIN`). L'application accorde une importance primordiale à :
* La validation stricte et la normalisation des données entrantes.
* L'architecture en couches et l'utilisation exclusive de DTOs pour les échanges HTTP.
* La gestion centralisée et uniforme des erreurs HTTP.
* La persistance fiable des données sans perte entre redémarrages.
* La documentation interactive et la démonstration via **Swagger UI (OpenAPI 3)**.

---

## 2. Environnement et Architecture Technique

### 2.1. Tableau des composants retenus

| Composant | Technologie / Librairie | Version | Rôle |
| :--- | :--- | :--- | :--- |
| **Langage** | Java OpenJDK (LTS) | 21 | Environnement d'exécution |
| **Framework** | Spring Boot | 3.3.5 | Socle applicatif REST et injection de dépendances |
| **Build** | Apache Maven | 3.9.9 | Gestion des dépendances et du cycle de vie |
| **Persistance** | Spring Data JPA / Hibernate | 3.3.5 / 6.5.3 | Mappage objet-relationnel (ORM) |
| **Base de données** | PostgreSQL | 18 | Système de gestion de base de données relationnelle |
| **Sécurité** | Spring Security | 3.3.5 | Contrôle des accès et filtres HTTP |
| **Authentification**| JJWT (`io.jsonwebtoken`) | 0.12.6 | Génération, signature et validation des jetons JWT |
| **Documentation** | Springdoc OpenAPI Starter WebMVC | 2.6.0 | Génération de Swagger UI |

### 2.2. Organisation en couches et Packages

L'application respecte les principes de séparation des responsabilités :
```
com.ucak.tprest/
├── bootstrap/       # Initialisation sécurisée du premier administrateur (Profil bootstrap)
├── config/          # Configuration Spring Security, beans et OpenAPI Swagger
├── controllers/     # Contrôleurs REST (reçoivent les requêtes HTTP, appellent les services)
├── dto/
│   ├── request/     # DTOs de requête d'entrée (Login, Register, User, Product)
│   └── response/    # DTOs de réponse (UserResponse, ProductResponse, AuthResponse, ErrorResponse)
├── entities/        # Entités JPA (User, Product, Role enum)
├── exceptions/      # Exceptions métier et GlobalExceptionHandler centralisé
├── repositories/    # Interfaces Spring Data JPA pour l'accès aux données
├── security/        # Service JWT, filtre d'authentification par requête, entry points 401/403
├── services/        # Logique métier et orchestrations des opérations
└── validation/      # Validateur strict des données et désérialiseur de prix
```

---

## 3. Configuration PostgreSQL et Persistance

### 3.1. Gestion des privilèges du rôle `tp_api`
Conformément aux contraintes de sécurité, l'application ne se connecte jamais avec le superutilisateur `postgres`. Un rôle applicatif dédié `tp_api` a été créé avec des droits strictement limités :
* **Propriétaire** de la base `tp_rest`.
* Attributs de sécurité : `NOSUPERUSER NOCREATEDB NOCREATEROLE`.
* Privilèges complets accordés sur le schéma `public` de la base `tp_rest` pour la manipulation des tables et séquences.

### 3.2. Persistance avec `ddl-auto=update`
Dans le profil `tp`, la propriété `spring.jpa.hibernate.ddl-auto` est définie sur `update` :
* **Justification :** Contrairement à `create` ou `create-drop` qui détruisent le schéma et les données lors du cycle de vie de l'application, `update` conserve l'intégralité des enregistrements en base entre deux démarrages successifs, tout en appliquant les ajustements de structure nécessaires.
* **Vérification réalisée :** Une entité temporaire a été déployée, la table a été constatée en base PostgreSQL, puis supprimée proprement après validation.

---

## 4. Modélisation et Validations Strictes des Données

Toutes les données reçues par l'API font l'objet d'un contrôle avant persistance :

1. **Nom (`name` pour Utilisateur et Produit) :**
   * Suppression obligatoire des espaces périphériques (`trim()`).
   * Longueur comprise entre 2 et 100 caractères après nettoyage.
   * Rejet immédiat en `400 Bad Request` si la valeur est absente, vide ou constituée uniquement d'espaces.
2. **Email (`email`) :**
   * Nettoyage des espaces et conversion en minuscules de manière indépendante de la langue du système via `Locale.ROOT`.
   * Format d'adresse RFC valide et longueur maximale de 254 caractères.
   * Contrôle d'unicité préalable au niveau service et garantie concurrente par contrainte SQL unique (`uk_users_email`). Rejet en `409 Conflict` en cas de doublon.
3. **Mot de passe (`password`) :**
   * Longueur contrôlée entre 8 et 72 caractères (limite native de l'algorithme BCrypt).
   * **Conservation stricte et intégrale du contenu** (les espaces intérieurs et extérieurs ne sont jamais altérés).
   * Hachage avec `BCryptPasswordEncoder` avant enregistrement. Aucun mot de passe en clair ni hachage n'apparaît dans les réponses JSON ou les journaux.
4. **Rôle (`role`) :**
   * Énumération stricte : `USER` ou `ADMIN`, stockée sous son libellé textuel (`EnumType.STRING`).
   * À l'inscription publique, la présence du champ `role` provoque un rejet immédiat en `400 Bad Request`.
5. **Produit (`price` et `description`) :**
   * `description` : optionnelle, limitée à 1 000 caractères (jamais tronquée automatiquement).
   * `price` : obligatoire, de type `BigDecimal`, strictement supérieur à zéro.
   * **Règle des deux décimales :** Implémentation d'un désérialiseur spécifique `StrictBigDecimalDeserializer` qui analyse le flux textuel JSON pour refuser toute valeur comportant plus de deux chiffres après la virgule, y compris les représentations telles que `1.230` ou `1.234`, sans procéder à aucun arrondi implicite.

---

## 5. Initialisation du premier Administrateur (Bootstrap)

Pour respecter l'interdiction des mots de passe par défaut et des routes publiques d'initialisation :
1. Un composant `AdminBootstrapRunner` (`CommandLineRunner`) a été créé sous le profil conditionnel `bootstrap`.
2. Il lit les variables d'environnement temporaires :
   * `BOOTSTRAP_ADMIN_NAME`
   * `BOOTSTRAP_ADMIN_EMAIL`
   * `BOOTSTRAP_ADMIN_PASSWORD`
3. **Contrôles de sécurité appliqués :**
   * Si un compte `ADMIN` existe déjà : l'exécution est ignorée avec un avertissement dans les logs sans modifier les comptes existants (idempotence vérifiée).
   * Si aucun `ADMIN` n'existe mais que l'email est déjà détenu par un `USER` : le processus lève une exception bloquante pour empêcher toute promotion illégitime.
   * Si les conditions sont réunies : validation, hachage BCrypt et enregistrement avec le rôle `ADMIN`.
4. L'application est ensuite arrêtée, le profil `bootstrap` retiré, et le serveur redémarré avec `tp` seul.

---

## 6. Sécurité et Authentification par JWT

### 6.1. Politique de sécurité Stateless & CSRF
* Le filtre de sécurité Spring Security est configuré en mode strictement sans état (`SessionCreationPolicy.STATELESS`).
* Formulaires de connexion (`formLogin`) et authentification HTTP Basic sont désactivés.
* **Désactivation de CSRF :** L'API étant exclusivement sans session et l'authentification reposant uniquement sur l'en-tête HTTP `Authorization: Bearer <token>` (sans recours aux cookies de navigation), les attaques de type Cross-Site Request Forgery ne sont pas applicables, justifiant la désactivation du mécanisme CSRF.

### 6.2. Signature et cycle de vie des Tokens JWT
* Signature cryptographique : Algorithme **HMAC-SHA256 (HS256)**.
* Clé secrète : Décodée depuis une chaîne Base64 (`JWT_SECRET_BASE64`) avec vérification stricte d'une entropie minimale de 32 octets (256 bits).
* Durée de validité : Externalisée (`JWT_TTL_SECONDS=900`, soit 15 minutes).
* Payload : Contient l'identifiant numérique stable de l'utilisateur comme `Subject` (`sub`), l'email, la date d'émission (`iat`) et la date d'expiration (`exp`). Aucun mot de passe ni hachage n'y figure.

### 6.3. Contrôle en base à chaque requête (`JwtAuthFilter`)
À chaque appel d'une route protégée :
1. Le filtre extrait le token et valide sa signature ainsi que sa date d'expiration.
2. L'identifiant utilisateur est extrait du sujet et le compte est rechargé en base de données.
3. Si le compte a été supprimé entre-temps $\to$ la requête est rejetée en `401 Unauthorized`.
4. Si le rôle a été modifié en base (ex : rétrogradation d'`ADMIN` à `USER`) $\to$ les nouveaux droits sont immédiatement appliqués, révoquant les privilèges administrateur sans attendre l'expiration du token.
5. Les autorités Spring Security sont injectées sous le format standard `ROLE_USER` et `ROLE_ADMIN`.

---

## 7. Gestion Centralisée des Erreurs HTTP

La classe `@RestControllerAdvice GlobalExceptionHandler` intercepte toutes les anomalies pour produire une réponse JSON standardisée :

```json
{
  "status": 400,
  "message": "Données invalides",
  "errors": {
    "price": "Le prix ne doit pas comporter plus de deux chiffres après la virgule"
  }
}
```

### Synthèse des statuts retournés :
* **400 Bad Request :** Corps JSON illisible, rôle non reconnu, identifiant d'URI mal formé (`/api/products/abc`), contraintes de validation de champ non satisfaites.
* **401 Unauthorized :** Identifiants incorrects (message générique identique pour email inconnu ou mot de passe erroné), token absent, altéré ou expiré, utilisateur supprimé.
* **403 Forbidden :** Utilisateur authentifié mais rôle insuffisant (ex : compte `USER` tentant un `POST /api/products` ou `GET /api/users`).
* **404 Not Found :** Ressource introuvable par identifiant pour un appelant autorisé.
* **409 Conflict :** Adresse email déjà utilisée lors d'une inscription, création ou modification de compte.

---

## 8. Documentation et Démonstration via Swagger UI

* **Interface :** `http://localhost:8080/swagger-ui/index.html`
* **Spécification JSON :** `http://localhost:8080/v3/api-docs`

### Configuration OpenAPI :
* Schéma de sécurité HTTP Bearer nommé `bearerAuth` déclaré au format JWT.
* Les endpoints d'authentification (`/api/auth/**`) et les ressources Swagger sont configurés en accès public.
* Les opérations métier sont protégées par le cadenas de sécurité OpenAPI.
* Le bouton **Authorize** permet d'injecter directement le JWT brut pour exécuter l'ensemble des scénarios de démonstration.

---

## 9. Résultats des Tests et Validation

Une suite de tests automatisée sous **JUnit 5** valide les règles métier et les cas aux limites :
* **Tests exécutés :** 12 cas de test unitaire sur `DataValidator`.
* **Couverture :** Noms valides et bornes (2 et 100 car.), rejet des espaces seuls, normalisation email, rejet format et > 254 car., mot de passe (8 à 72 car., préservation des espaces), contrôle strict du prix (rejet de 0, négatif, `1.230` et `1.234`), description (jusqu'à 1 000 car., rejet de 1 001 car.).
* **Résultat du build :** `BUILD SUCCESS`.
* **Idempotence du bootstrap :** Testée et validée (l'administrateur existant n'est pas altéré lors d'une réactivation du profil).

---

## 10. Conclusion

L'ensemble des exigences fonctionnelles, de sécurité, de persistance et de documentation prescrites par le sujet a été scrupuleusement respecté. Le projet est prêt pour la remise finale et la soutenance orale.
