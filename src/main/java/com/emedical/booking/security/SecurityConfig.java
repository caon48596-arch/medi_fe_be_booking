package com.emedical.booking.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                // Cấu hình CORS cho Frontend React
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Tắt CSRF để React frontend gọi REST API thông suốt
                .csrf(csrf -> csrf.disable())

                // 1. Phân quyền truy cập API
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập tĩnh và các API công khai
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/assets/**",
                                "/static/**",
                                "/auth/**",
                                "/user/register",
                                "/user/verify-otp",
                                "/user/verify-login-otp",
                                "/user/login",
                                "/user/logout",
                                "/user/profile",
                                "/user/get",
                                "/user/booking",
                                "/user/all",
                                "/doctor/register",
                                "/doctor/verify-otp",
                                "/doctor/verify-login-otp",
                                "/doctor/login",
                                "/doctor/logout",
                                "/doctor/list",
                                "/doctor/all",
                                "/doctor/*/slots",
                                "/doctor-slots/**",
                                "/appointment/**",
                                "/medical-records/**")
                        .permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/doctor/**").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "DOCTOR", "ADMIN")

                        .anyRequest().authenticated())

                // 2. Sử dụng session phía server
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))

                // 3. Tắt HTTP Basic và form login mặc định
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 4. Mã hóa mật khẩu bằng BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
