package com.microservicio.noticias.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilterNoticias jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Preflight OPTIONS sin token ───────────────────────────────
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── Imágenes estáticas: público sin token ─────────────────────
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        // ── Noticias públicas ─────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/api/noticia/listar/publicadas",
                                "/api/noticia/listar/urgentes",
                                "/api/noticia/listar/destacadas",
                                "/api/noticia/{id}"
                        ).permitAll()

                        // ── Noticias para usuario autenticado ─────────────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/api/noticia/listar/para-usuario"
                        ).hasAnyRole("RESIDENTE", "CELADOR", "DIRECTOR")

                        .requestMatchers(HttpMethod.GET,
                                "/api/noticia/listar"
                        ).hasAnyRole("CELADOR", "DIRECTOR")

                        // ── Crear / editar noticias ───────────────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/api/noticia/crear"
                        ).hasAnyRole("CELADOR", "DIRECTOR")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/noticia/actualizar/**"
                        ).hasAnyRole("CELADOR", "DIRECTOR")

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/noticia/actualizar/**",
                                "/api/noticia/archivar/**",
                                "/api/noticia/publicar/**"
                        ).hasAnyRole("CELADOR", "DIRECTOR")

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/noticia/autorizar/**"
                        ).hasRole("DIRECTOR")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/noticia/eliminar/**"
                        ).hasAnyRole("CELADOR", "DIRECTOR")

                        // ── Destinatarios ─────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/api/destinatario/tipos",
                                "/api/destinatario/tipos/todos",
                                "/api/destinatario/noticia/**"
                        ).authenticated()

                        .requestMatchers(HttpMethod.POST,   "/api/destinatario/**").hasAnyRole("CELADOR", "DIRECTOR")
                        .requestMatchers(HttpMethod.PUT,    "/api/destinatario/**").hasAnyRole("CELADOR", "DIRECTOR")
                        .requestMatchers(HttpMethod.PATCH,  "/api/destinatario/**").hasAnyRole("CELADOR", "DIRECTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/destinatario/**").hasAnyRole("CELADOR", "DIRECTOR")

                        // ── Lecturas ──────────────────────────────────────────────────
                        // Registrar lectura — cualquier usuario autenticado
                        .requestMatchers(HttpMethod.POST,
                                "/api/lectura/**"
                        ).authenticated()

                        // Ver si ya leyó — cualquier usuario autenticado
                        .requestMatchers(HttpMethod.GET,
                                "/api/lectura/*/leida",
                                "/api/lectura/*/total"
                        ).authenticated()

                        // Ver tabla de lectores — solo Director y Celador
                        .requestMatchers(HttpMethod.GET,
                                "/api/lectura/*/lectores"
                        ).hasAnyRole("DIRECTOR", "CELADOR")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}