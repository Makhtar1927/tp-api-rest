package com.ucak.tprest.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;

public class RegisterRequest {

    private String name;
    private String email;
    private String password;

    // Capture tout champ non prévu, notamment "role" pour rejeter en 400
    private final Map<String, Object> extraFields = new HashMap<>();

    public RegisterRequest() {
    }

    public RegisterRequest(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JsonAnySetter
    public void setExtraField(String key, Object value) {
        this.extraFields.put(key, value);
    }

    public boolean hasRoleField() {
        return extraFields.containsKey("role");
    }

    public Map<String, Object> getExtraFields() {
        return extraFields;
    }
}
