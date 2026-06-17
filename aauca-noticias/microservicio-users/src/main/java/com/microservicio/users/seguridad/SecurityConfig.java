package com.microservicio.users.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Rutas públicas — autenticación ────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/user/login")              .permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/user/generar-hash")       .permitAll()

                        // ── Rutas públicas — datos de formulario ──────────────────────
                        .requestMatchers(HttpMethod.GET,  "/api/user/valores-filtro")     .permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/user/edificios")          .permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/user/edificios/todos")    .permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/user/habitaciones/**")    .permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/user/foto/**")            .permitAll()

                        // ── Rutas públicas — solicitudes ──────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes")             .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes/con-foto")    .permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/solicitudes/foto/**")     .permitAll()

                        // ── NUEVAS: disponibilidad y estado de email ───────────────────
                        // El solicitante aún no tiene cuenta — estos endpoints son públicos
                        .requestMatchers(HttpMethod.GET,  "/api/solicitudes/disponibilidad/**").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/solicitudes/estado-email")     .permitAll()

                        // ── Preflight CORS ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.OPTIONS, "/**")                       .permitAll()

                        // ── Solicitudes — Director y Celador ──────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/pendientes")
                        .hasAnyRole("DIRECTOR", "CELADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/solicitudes/*/aprobar")
                        .hasAnyRole("DIRECTOR", "CELADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/solicitudes/*/rechazar")
                        .hasAnyRole("DIRECTOR", "CELADOR")

                        // ── Solicitudes — solo Director ───────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes")
                        .hasRole("DIRECTOR")

                        // ── Todo lo demás requiere token ──────────────────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}