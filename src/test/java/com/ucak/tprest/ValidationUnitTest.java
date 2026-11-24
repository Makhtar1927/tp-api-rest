package com.ucak.tprest;

import com.ucak.tprest.entities.Role;
import com.ucak.tprest.exceptions.ValidationException;
import com.ucak.tprest.validation.DataValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUnitTest {

    private DataValidator validator;
    private Map<String, String> errors;

    @BeforeEach
    void setUp() {
        validator = new DataValidator();
        errors = new HashMap<>();
    }

    // --- TESTS DU NOM (USER & PRODUCT) ---

    @Test
    @DisplayName("Nom valide et aux limites (2 et 100 caractères, nettoyage des espaces)")
    void testNameValidAndBoundaries() {
        assertEquals("ab", validator.validateAndNormalizeName("  ab  ", "name", errors));
        assertEquals("Clavier", validator.validateAndNormalizeName("   Clavier   ", "name", errors));

        String hundredChars = "a".repeat(100);
        assertEquals(hundredChars, validator.validateAndNormalizeName(" " + hundredChars + " ", "name", errors));
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Nom invalide : absent, null, vide, espaces seuls, 1 caractère ou 101 caractères")
    void testNameInvalid() {
        validator.validateAndNormalizeName(null, "name", errors);
        validator.validateAndNormalizeName("", "name", errors);
        validator.validateAndNormalizeName("     ", "name", errors);
        validator.validateAndNormalizeName(" a ", "name", errors);
        validator.validateAndNormalizeName(" " + "a".repeat(101) + " ", "name", errors);

        assertFalse(errors.isEmpty());
    }

    // --- TESTS DE L'EMAIL ---

    @Test
    @DisplayName("Email valide et aux limites : normalisation minuscules et trim, jusqu'à 254 caractères")
    void testEmailValid() {
        assertEquals("test@example.com", validator.validateAndNormalizeEmail("  TEST@Example.COM  ", errors));
        assertTrue(errors.isEmpty());

        // Construction d'un email valide de 254 caractères
        String local = "a".repeat(64);
        String domainPart = "b".repeat(60) + ".";
        String domain = domainPart + domainPart + "com"; // Total email <= 254
        String longEmail = local + "@" + domain;
        if (longEmail.length() <= 254) {
            String clean = validator.validateAndNormalizeEmail(longEmail, errors);
            assertNotNull(clean);
        }
    }

    @Test
    @DisplayName("Email invalide : absent, null, format incorrect, dépassement 254 caractères")
    void testEmailInvalid() {
        validator.validateAndNormalizeEmail(null, errors);
        validator.validateAndNormalizeEmail("notanemail", errors);
        validator.validateAndNormalizeEmail("bad@domain", errors);
        validator.validateAndNormalizeEmail("a".repeat(250) + "@domain.com", errors); // > 254

        assertFalse(errors.isEmpty());
    }

    // --- TESTS DU MOT DE PASSE ---

    @Test
    @DisplayName("Mot de passe valide et aux limites : 8 à 72 caractères, conservation stricte des espaces")
    void testPasswordValid() {
        validator.validatePassword("password", true, errors);
        validator.validatePassword(" 123456 ", true, errors); // Espaces conservés
        validator.validatePassword("a".repeat(72), true, errors);

        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Mot de passe invalide : absent à la création, null, 7 caractères, 73 caractères")
    void testPasswordInvalid() {
        validator.validatePassword(null, true, errors);
        validator.validatePassword("1234567", true, errors); // 7 caractères
        validator.validatePassword("a".repeat(73), true, errors); // 73 caractères

        assertFalse(errors.isEmpty());
    }

    // --- TESTS DU RÔLE ---

    @Test
    @DisplayName("Rôle valide : USER et ADMIN")
    void testRoleValid() {
        validator.validateRole(Role.USER, errors);
        validator.validateRole(Role.ADMIN, errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Rôle invalide : null")
    void testRoleInvalid() {
        validator.validateRole(null, errors);
        assertFalse(errors.isEmpty());
    }

    // --- TESTS DE LA DESCRIPTION DU PRODUIT ---

    @Test
    @DisplayName("Description valide : null, vide, jusqu'à 1000 caractères")
    void testDescriptionValid() {
        assertNull(validator.validateDescription(null, errors));
        assertNull(validator.validateDescription("", errors));
        assertEquals("Description", validator.validateDescription("Description", errors));

        String thousandChars = "x".repeat(1000);
        assertEquals(thousandChars, validator.validateDescription(thousandChars, errors));
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Description invalide : 1001 caractères")
    void testDescriptionInvalid() {
        validator.validateDescription("x".repeat(1001), errors);
        assertFalse(errors.isEmpty());
    }

    // --- TESTS DU PRIX DU PRODUIT ---

    @Test
    @DisplayName("Prix valide : strictement positif, au plus deux décimales (0.01, 1, 12.34)")
    void testPriceValid() {
        validator.validatePrice(new BigDecimal("0.01"), errors);
        validator.validatePrice(new BigDecimal("1"), errors);
        validator.validatePrice(new BigDecimal("12.34"), errors);
        validator.validatePrice(new BigDecimal("12.3"), errors);

        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Prix invalide : absent, zéro, négatif, 1.234, 1.230")
    void testPriceInvalid() {
        validator.validatePrice(null, errors);
        validator.validatePrice(BigDecimal.ZERO, errors);
        validator.validatePrice(new BigDecimal("-5.00"), errors);
        validator.validatePrice(new BigDecimal("1.234"), errors); // 3 décimales
        validator.validatePrice(new BigDecimal("1.230"), errors); // 3 décimales

        assertFalse(errors.isEmpty());
    }
}
