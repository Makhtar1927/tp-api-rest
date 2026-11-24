package com.ucak.tprest.bootstrap;

import com.ucak.tprest.entities.Role;
import com.ucak.tprest.entities.User;
import com.ucak.tprest.repositories.UserRepository;
import com.ucak.tprest.validation.DataValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Profile("bootstrap")
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataValidator validator;

    @Value("${BOOTSTRAP_ADMIN_NAME:#{null}}")
    private String adminName;

    @Value("${BOOTSTRAP_ADMIN_EMAIL:#{null}}")
    private String adminEmail;

    @Value("${BOOTSTRAP_ADMIN_PASSWORD:#{null}}")
    private String adminPassword;

    public AdminBootstrapRunner(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                DataValidator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Override
    public void run(String... args) {
        log.info("Démarrage du profil bootstrap pour l'initialisation du premier administrateur...");

        // 1. Vérifier si un ADMIN existe déjà
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.warn("Bootstrap ignoré : Un compte administrateur (ADMIN) existe déjà en base de données. Aucun compte n'a été modifié.");
            return;
        }

        // 2. Validation des variables d'environnement requises
        Map<String, String> errors = new HashMap<>();
        String cleanName = validator.validateAndNormalizeName(adminName, "BOOTSTRAP_ADMIN_NAME", errors);
        String cleanEmail = validator.validateAndNormalizeEmail(adminEmail, errors);
        validator.validatePassword(adminPassword, true, errors);
        validator.throwIfErrors(errors);

        // 3. Vérifier si l'email est déjà pris par un compte non-admin
        if (userRepository.existsByEmail(cleanEmail)) {
            String errorMsg = "Arrêt de l'initialisation : L'email " + cleanEmail + " est déjà attribué à un compte existant. Le compte existant ne peut être promu.";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 4. Création de l'administrateur
        String hashedPassword = passwordEncoder.encode(adminPassword);
        User admin = new User(cleanName, cleanEmail, hashedPassword, Role.ADMIN);
        userRepository.save(admin);

        log.info("Succès du bootstrap : Premier compte ADMIN créé avec succès (Email: {}). Arrêtez l'application et retirez le profil bootstrap.", cleanEmail);
    }
}
