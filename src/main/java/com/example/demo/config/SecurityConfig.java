package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http, 
        CorsConfigurationSource corsConfigurationSource) throws Exception {
    http
        // 1. Kích hoạt CORS. Nó sẽ sử dụng CorsConfigurationSource đã được định nghĩa.
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        
        // 2. Tắt CSRF vì đây là API RESTful, thường dùng JWT và không dựa vào session/cookies.
        .csrf(csrf -> csrf.disable())

        // 3. Quản lý Session: Đặt là STATELESS (Không lưu trạng thái session trên server) 
        //    thích hợp cho ứng dụng dùng JWT.
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        
        // 4. Định nghĩa các quy tắc phân quyền truy cập.
        .authorizeHttpRequests(auth -> auth
            // Cho phép truy cập Swagger/OpenAPI mà không cần xác thực
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
            
            // Cho phép truy cập các endpoint đăng ký/đăng nhập mà không cần xác thực
            .requestMatchers("/api/users/register", "/api/auth/login").permitAll()
            
            // Tất cả các request còn lại đều yêu cầu xác thực
            .anyRequest().authenticated()
        )
    return http.build();
}

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EVDMS API")
                        .version("1.0.0")
                        .description("API Documentation for Electric Vehicle Dealer Management System"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", 
                            new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")));
    }
}