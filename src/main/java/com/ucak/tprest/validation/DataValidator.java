package com.ucak.tprest.validation;

import com.ucak.tprest.entities.Role;
import com.ucak.tprest.exceptions.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class DataValidator {

    // Regex RFC 5322 simplifiée mais robuste pour les emails
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Valide et normalise le nom (User ou Product).
     */
    public String validateAndNormalizeName(String name, String fieldName, Map<String, String> errors) {
        if (name == null || name.trim().isEmpty()) {
            errors.put(fieldName, "Le nom est obligatoire et ne doit pas être composé uniquement d'espaces");
            return null;
        }
        String clean = name.trim();
        if (clean.length() < 2 || clean.length() > 100) {
            errors.put(fieldName, "Le nom doit comporter entre 2 et 100 caractères après suppression des espaces aux extrémités");
            return null;
        }
        return clean;
    }

    /**
     * Valide et normalise l'email.
     */
    public String validateAndNormalizeEmail(String email, Map<String, String> errors) {
        if (email == null || email.trim().isEmpty()) {
            errors.put("email", "L'adresse email est obligatoire");
            return null;
        }
        String clean = email.trim().toLowerCase(Locale.ROOT);
        if (clean.length() > 254) {
            errors.put("email", "L'adresse email ne doit pas dépasser 254 caractères");
            return null;
        }
        if (!EMAIL_PATTERN.matcher(clean).matches()) {
            errors.put("email", "Le format de l'adresse email est invalide");
            return null;
        }
        return clean;
    }

    /**
     * Valide le mot de passe sans altérer ses espaces.
     */
    public void validatePassword(String password, boolean required, Map<String, String> errors) {
        if (password == null) {
            if (required) {
                errors.put("password", "Le mot de passe est obligatoire");
            }
            return;
        }
        int len = password.length();
        if (len < 8 || len > 72) {
            errors.put("password", "Le mot de passe doit comporter entre 8 et 72 caractères");
        }
    }

    /**
     * Valide le rôle.
     */
    public void validateRole(Role role, Map<String, String> errors) {
        if (role == null) {
            errors.put("role", "Le rôle est obligatoire et doit être USER ou ADMIN");
        }
    }

    /**
     * Valide la description du produit.
     */
    public String validateDescription(String description, Map<String, String> errors) {
        if (description == null || description.isEmpty()) {
            return null;
        }
        if (description.length() > 1000) {
            errors.put("description", "La description ne doit pas dépasser 1000 caractères");
            return null;
        }
        return description;
    }

    /**
     * Valide le prix du produit.
     */
    public void validatePrice(BigDecimal price, Map<String, String> errors) {
        if (price == null) {
            errors.put("price", "Le prix est obligatoire");
            return;
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            errors.put("price", "Le prix doit être strictement supérieur à zéro");
            return;
        }
        if (price.scale() > 2) {
            errors.put("price", "Le prix ne doit pas comporter plus de deux chiffres après la virgule");
        }
    }

    public void throwIfErrors(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
