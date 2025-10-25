package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Bean để mã hóa mật khẩu
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean định nghĩa Cấu hình CORS (RẤT QUAN TRỌNG ĐỂ FIX LỖI DEPLOYMENT)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Cần thay đổi các URL này để phù hợp với URL Frontend của bạn
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000", // Ví dụ: Frontend chạy trên localhost
            "https://ten-frontend-cua-ban.railway.app" // URL thật của Frontend trên Railway
            // Nếu bạn cần cho phép tất cả các domain, dùng: Arrays.asList("*") (không khuyến khích cho Production)
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*")); // Cho phép tất cả headers
        configuration.setAllowCredentials(true); // Cho phép gửi cookies/Authorization headers

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Áp dụng cấu hình cho tất cả các đường dẫn
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, 
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        
        // SỬA LỖI BIÊN DỊCH: Chuỗi tất cả các phương thức và kết thúc bằng .build()
        return http
                // 1. Kích hoạt CORS (sử dụng Bean corsConfigurationSource ở trên)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                
                // 2. Tắt CSRF cho API RESTful (dùng JWT)
                .csrf(csrf -> csrf.disable())

                // 3. Quản lý Session: Đặt là STATELESS (Không lưu trạng thái session trên server)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 4. Định nghĩa các quy tắc phân quyền truy cập.
                .authorizeHttpRequests(auth -> auth
                    // Cho phép truy cập Swagger/OpenAPI
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                    
                    // Cho phép truy cập các endpoint đăng ký/đăng nhập
                    .requestMatchers("/api/users/register", "/api/auth/login").permitAll()
                    
                    // Thêm quyền truy cập cho H2 Console (chỉ cho Dev)
                    // .requestMatchers("/h2-console/**").permitAll() 

                    // Tất cả các request còn lại đều yêu cầu xác thực
                    .anyRequest().authenticated()
                )
                
                // Nếu bạn có JWT Filter, hãy thêm nó ở đây:
                // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Kết thúc cấu hình
                .build();
    }

    // Bean cấu hình OpenAPI/Swagger
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
