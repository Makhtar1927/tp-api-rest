package com.ucak.tprest.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Désactivation CSRF (API REST sans session ni cookies)
            .csrf(AbstractHttpConfigurer::disable)

            // 2. Désactivation des formulaires et de HTTP Basic
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)

            // 3. Gestion de session strictement Stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. Gestionnaires d'erreurs d'accès uniformes JSON (401 et 403)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // 5. Règles d'autorisation des requêtes HTTP
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics d'authentification
                .requestMatchers("/api/auth/**").permitAll()

                // Documentation OpenAPI / Swagger UI & Racine
                .requestMatchers(
                    "/",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml"
                ).permitAll()

                // Consultation des produits : accessible aux rôles USER et ADMIN
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").hasAnyRole("USER", "ADMIN")

                // Écriture sur les produits (POST, PUT, DELETE) : réservée au rôle ADMIN
                .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                // Gestion complète des utilisateurs : réservée au rôle ADMIN
                .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")

                // Toute autre requête nécessite d'être authentifiée
                .anyRequest().authenticated()
            )

            // 6. Ajout du filtre JWT avant le filtre d'authentification par mot de passe
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
