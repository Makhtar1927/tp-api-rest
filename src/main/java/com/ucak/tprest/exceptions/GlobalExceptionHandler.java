package com.ucak.tprest.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.ucak.tprest.dto.response.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Validation métier (400 Bad Request)
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                ex.getErrors().isEmpty() ? null : ex.getErrors()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. Bean validation standard (400 Bad Request)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Données invalides",
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 3. Corps JSON mal formé ou désérialisation invalide (ex: rôle inconnu, décimales invalides) (400 Bad Request)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            // Si la cause est notre ValidationException levée par un désérialiseur strict
            if (cause instanceof ValidationException valEx) {
                return handleValidationException(valEx);
            }

            // Cas spécifique d'une énumération invalide (ex: rôle non reconnu)
            if (cause instanceof InvalidFormatException ife && ife.getTargetType().isEnum()) {
                Map<String, String> errors = new HashMap<>();
                String fieldName = ife.getPath().isEmpty() ? "field" : ife.getPath().get(0).getFieldName();
                errors.put(fieldName, "Valeur non reconnue pour le rôle. Les valeurs valides sont USER ou ADMIN");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Données invalides", errors));
            }
            cause = cause.getCause();
        }

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Corps de requête JSON illisible ou mal formé"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 4. Identifiant mal formé dans l'URI (ex: /api/products/abc) (400 Bad Request)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Identifiant invalide ou mal formé dans l'URI"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 5. Mauvais identifiants de connexion (401 Unauthorized)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Email ou mot de passe incorrect"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 6. Droits insuffisants (403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Accès refusé : privilèges insuffisants"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 7. Ressource non trouvée (404 Not Found)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 8. Conflit d'email (409 Conflict)
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 9. Contrainte d'intégrité SQL au niveau PostgreSQL (ex: écritures concurrentes) (409 Conflict)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String msg = ex.getMessage();
        if (msg != null && (msg.contains("uk_users_email") || msg.toLowerCase().contains("email"))) {
            ErrorResponse response = new ErrorResponse(
                    HttpStatus.CONFLICT.value(),
                    "L'adresse email est déjà utilisée par un autre compte"
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Violation de contrainte de données"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 10. Fallback erreurs internes sans exposer de détails techniques (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Une erreur interne est survenue"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
