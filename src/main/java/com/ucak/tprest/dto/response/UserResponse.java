package com.ucak.tprest.dto.response;

import com.ucak.tprest.entities.Role;

public record UserResponse(
    Long id,
    String name,
    String email,
    Role role
) {}
