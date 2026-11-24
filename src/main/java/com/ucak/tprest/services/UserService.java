package com.ucak.tprest.services;

import com.ucak.tprest.dto.request.UserCreateRequest;
import com.ucak.tprest.dto.request.UserUpdateRequest;
import com.ucak.tprest.dto.response.UserResponse;
import com.ucak.tprest.entities.User;
import com.ucak.tprest.exceptions.EmailAlreadyExistsException;
import com.ucak.tprest.exceptions.ResourceNotFoundException;
import com.ucak.tprest.repositories.UserRepository;
import com.ucak.tprest.validation.DataValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataValidator validator;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       DataValidator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'identifiant " + id));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        Map<String, String> errors = new HashMap<>();
        String cleanName = validator.validateAndNormalizeName(request.getName(), "name", errors);
        String cleanEmail = validator.validateAndNormalizeEmail(request.getEmail(), errors);
        validator.validatePassword(request.getPassword(), true, errors);
        validator.validateRole(request.getRole(), errors);
        validator.throwIfErrors(errors);

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new EmailAlreadyExistsException("L'adresse email est déjà utilisée par un autre compte");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(cleanName, cleanEmail, hashedPassword, request.getRole());
        User saved = userRepository.save(user);

        return new UserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'identifiant " + id));

        Map<String, String> errors = new HashMap<>();
        String cleanName = validator.validateAndNormalizeName(request.getName(), "name", errors);
        String cleanEmail = validator.validateAndNormalizeEmail(request.getEmail(), errors);
        validator.validateRole(request.getRole(), errors);

        // Si le mot de passe est fourni, il doit être non-null et valide. S'il est omis, conserver le hachage existant.
        boolean updatePassword = (request.getPassword() != null && !request.getPassword().isEmpty());
        if (updatePassword) {
            validator.validatePassword(request.getPassword(), false, errors);
        }
        validator.throwIfErrors(errors);

        // Si l'email change, vérifier qu'il n'appartient pas à un autre compte
        if (!user.getEmail().equalsIgnoreCase(cleanEmail) && userRepository.existsByEmail(cleanEmail)) {
            throw new EmailAlreadyExistsException("L'adresse email est déjà utilisée par un autre compte");
        }

        user.setName(cleanName);
        user.setEmail(cleanEmail);
        user.setRole(request.getRole());

        if (updatePassword) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(user);
        return new UserResponse(updated.getId(), updated.getName(), updated.getEmail(), updated.getRole());
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'identifiant " + id));
        userRepository.delete(user);
    }
}
