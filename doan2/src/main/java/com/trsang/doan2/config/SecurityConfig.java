package com.trsang.doan2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.trsang.doan2.repositories.IUserRepository;
import com.trsang.doan2.security.JwtAuthenticationFilter;
import com.trsang.doan2.security.UserDetailsImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfig corsConfig;
    private final IUserRepository userRepository;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CorsConfig corsConfig, IUserRepository userRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfig = corsConfig;
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .map(UserDetailsImpl::build)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowCredentials(true);
                    config.addAllowedOriginPattern("*"); // Security requires pattern for credentials
                    config.addAllowedHeader("*");
                    config.addAllowedMethod("*");
                    config.addExposedHeader("Authorization");
                    config.addExposedHeader("Content-Type");
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/ollama/models/**").permitAll()
                        .requestMatchers("/api/ollama/chat/**").permitAll() // Make chat API public too for testing
                        // Swagger UI and API docs endpoints
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Secured endpoints
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(corsConfig.corsFilter(), SecurityContextHolderFilter.class)  // Add CORS filter before security filters
                .build();
    }   
}
