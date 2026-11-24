package com.ucak.tprest.controllers;

import com.ucak.tprest.dto.request.LoginRequest;
import com.ucak.tprest.dto.request.RegisterRequest;
import com.ucak.tprest.dto.response.AuthResponse;
import com.ucak.tprest.dto.response.ErrorResponse;
import com.ucak.tprest.dto.response.UserResponse;
import com.ucak.tprest.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Opérations publiques d'inscription et de connexion")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Inscrire un compte USER", description = "Accessible publiquement. Attribue automatiquement le rôle USER. La présence d'un champ 'role' est rejetée en 400.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compte créé avec succès",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou champ rôle présent",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Adresse email déjà utilisée",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/users/{id}")
                .buildAndExpand(userResponse.id())
                .toUri();
        return ResponseEntity.created(location).body(userResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Authentifier et émettre un JWT", description = "Accessible publiquement. Retourne un accessToken signé en HS256 avec durée de validité.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentification réussie",
                content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Corps de requête mal formé",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Identifiants incorrects",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }
}
