package com.gduck0.labgear.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/accounts").permitAll()
                .requestMatchers(HttpMethod.GET, "/health", "/csrf", "/equipment", "/equipment/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .usernameParameter("email")
                .successHandler((request, response, authentication) -> response.setStatus(204))
                .failureHandler((request, response, exception) -> response.sendError(401))
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204))
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }
}
