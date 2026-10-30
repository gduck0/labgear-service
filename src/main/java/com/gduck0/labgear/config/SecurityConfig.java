
package com.gduck0.labgear.config;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/accounts").permitAll()
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/equipment", "/equipment/*", "/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/equipment").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/equipment/*/name").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/equipment/*").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .usernameParameter("email")
                .successHandler((request, response, authentication) ->
                    response.setStatus(HttpServletResponse.SC_OK))
                .failureHandler((request, response, exception) ->
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessHandler((request, response, authentication) ->
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                .permitAll()
            );

        return http.build();
    }
}
