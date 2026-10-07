package com.filevault.config;

import com.filevault.security.JwtAuthenticationFilter;
import com.filevault.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, PasswordEncoder passwordEncoder) 
            throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder)
                .and()
                .build();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(new AntPathRequestMatcher("/api/auth/**", "POST")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/password-reset/**", "POST")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/public/**", "GET")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/categories/**", "GET")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/files/public/**", "GET")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/files/category/**", "GET")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/files/*/view")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/files/*/like")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/admin/*/subscribe")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/chat/public", "POST")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/chat/admin", "POST")).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/analytics/**")).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/admin/*/debug/**", "GET")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/admin/**")).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/user/**")).hasRole("USER")
                .requestMatchers(new AntPathRequestMatcher("/api/files/upload", "POST")).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/files/**", "DELETE")).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/files/**", "PUT")).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/access-requests/request/**", "POST")).hasRole("USER")
                .requestMatchers(new AntPathRequestMatcher("/api/access-requests/user/**", "GET")).hasRole("USER")
                .requestMatchers(new AntPathRequestMatcher("/api/access-requests/admin/**")).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/access-requests/approve", "POST")).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/access-requests/reject", "POST")).hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.getWriter().write("Unauthorized");
                });
        
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
