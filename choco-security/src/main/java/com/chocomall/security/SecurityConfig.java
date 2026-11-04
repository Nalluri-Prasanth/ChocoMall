package com.chocomall.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless, secure-by-default web security shared by both applications. The application provides exactly one
 * {@link UserDetailsService} (admin accounts or members) and configures {@code secure.*} properties.
 */
@Configuration
@ComponentScan("com.chocomall.security")
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties props;
    private final JwtTokenService tokens;
    private final UserDetailsService userDetailsService;
    private final RestAuthHandlers handlers;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String[] ignored = props.ignoredUrls().toArray(String[]::new);
        http
                .csrf(csrf -> csrf.disable()) // stateless API: no cookies authenticate requests
                .cors(cors -> cors.configurationSource(corsSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    if (ignored.length > 0) {
                        auth.requestMatchers(ignored).permitAll();
                    }
                    if (props.dynamicAuthorization()) {
                        auth.anyRequest().access(new DynamicAuthorizationManager(props.authenticatedUrls()));
                    } else {
                        auth.anyRequest().authenticated();
                    }
                })
                .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .addFilterBefore(new JwtAuthenticationFilter(tokens, userDetailsService, props),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration cors = new CorsConfiguration();
        List<String> origins = props.corsAllowedOrigins();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (!origins.isEmpty()) {
            source.registerCorsConfiguration("/**", cors);
        }
        return source;
    }
}
