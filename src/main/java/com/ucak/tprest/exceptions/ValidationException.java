package com.ucak.tprest.exceptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = Collections.emptyMap();
    }

    public ValidationException(String field, String message) {
        super("Données invalides");
        Map<String, String> map = new HashMap<>();
        map.put(field, message);
        this.errors = map;
    }

    public ValidationException(Map<String, String> errors) {
        super("Données invalides");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
