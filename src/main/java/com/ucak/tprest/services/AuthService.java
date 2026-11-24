package com.ucak.tprest.services;

import com.ucak.tprest.dto.request.LoginRequest;
import com.ucak.tprest.dto.request.RegisterRequest;
import com.ucak.tprest.dto.response.AuthResponse;
import com.ucak.tprest.dto.response.UserResponse;
import com.ucak.tprest.entities.Role;
import com.ucak.tprest.entities.User;
import com.ucak.tprest.exceptions.EmailAlreadyExistsException;
import com.ucak.tprest.exceptions.ValidationException;
import com.ucak.tprest.repositories.UserRepository;
import com.ucak.tprest.security.JwtService;
import com.ucak.tprest.validation.DataValidator;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DataValidator validator;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       DataValidator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.validator = validator;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Règle du sujet : à l'inscription publique, la présence de role entraîne 400 et aucune création
        if (request.hasRoleField()) {
            throw new ValidationException("role", "L'attribution d'un rôle est interdite lors de l'inscription publique");
        }

        Map<String, String> errors = new HashMap<>();
        String cleanName = validator.validateAndNormalizeName(request.getName(), "name", errors);
        String cleanEmail = validator.validateAndNormalizeEmail(request.getEmail(), errors);
        validator.validatePassword(request.getPassword(), true, errors);
        validator.throwIfErrors(errors);

        // Vérification d'unicité de l'email
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new EmailAlreadyExistsException("L'adresse email est déjà utilisée par un autre compte");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(cleanName, cleanEmail, hashedPassword, Role.USER);
        User saved = userRepository.save(user);

        return new UserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        String cleanEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        // Comparaison avec BCrypt sans altérer le mot de passe reçu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, jwtService.getTtlSeconds());
    }
}
