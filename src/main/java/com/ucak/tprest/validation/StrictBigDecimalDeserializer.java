package com.ucak.tprest.validation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.ucak.tprest.exceptions.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;

public class StrictBigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            // Vérifier que le format est numérique valide
            BigDecimal value = new BigDecimal(text);

            // Vérifier le nombre de décimales dans la chaîne d'origine (y compris les zéros de fin comme 1.230)
            int dotIndex = text.indexOf('.');
            if (dotIndex >= 0) {
                int decimalsCount = text.length() - dotIndex - 1;
                if (decimalsCount > 2) {
                    throw new ValidationException("price", "Le prix ne doit pas comporter plus de deux chiffres après la virgule");
                }
            }

            return value;
        } catch (NumberFormatException e) {
            throw new ValidationException("price", "Le prix doit être un nombre décimal valide");
        }
    }
}
