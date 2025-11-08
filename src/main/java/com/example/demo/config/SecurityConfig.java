package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Arrays;
import java.util.List;

// --- Swagger Imports ---
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

// --- Custom Security Imports (JWT và OAuth2) ---
import com.example.demo.security.CustomOAuth2UserService;
import com.example.demo.security.OAuth2LoginFailureHandler;
import com.example.demo.security.OAuth2LoginSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    
    @Autowired
    private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080",
                "http://localhost:5173",
                "http://localhost:5174",
                "https://fall25-swp-be-production-9b48.up.railway.app",
                "http://fall25-swp-be-production-9b48.up.railway.app",
                "https://localhost:3000",
                "https://electric-vehicle-dealer-management.vercel.app",
                "https://127.0.0.1:3000"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Cấu hình Security
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable()) // Tắt CSRF vì đang dùng JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger & public endpoints
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**"
                        ).permitAll()
                        .requestMatchers(
                            "/oauth2/**", // Cho phép bắt đầu luồng
                            "/login/oauth2/code/google" // Cho phép callback
                        ).permitAll()
                        // Public APIs
                        .requestMatchers("/api/users/**", "/api/auth/login","/api/vehicles/**").permitAll()
                        // OPTIONS request cho preflight
                        .requestMatchers(HttpMethod.POST, "/api/test-drive/schedule").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/test-drive/schedule-list").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/payments/vnpay/return").permitAll()
                        .requestMatchers("/api/payments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dealers").permitAll()
                        .anyRequest().authenticated()
                ) 
                
                .oauth2Login(oauth2 -> oauth2
                    // Dùng service để kiểm tra user (giữ nguyên)
                    .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService) // Lấy từ field @Autowired
                    )
                    // Dùng handler thành công (để tạo JWT)
                    .successHandler(oAuth2LoginSuccessHandler) // Lấy từ field @Autowired
                    // Dùng handler thất bại (để báo lỗi)
                    .failureHandler(oAuth2LoginFailureHandler) // Lấy từ field @Autowired
                )
                
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        Server productionServer = new Server()
                .url("https://fall25-swp-be-production-9b48.up.railway.app")
                .description("Production Server (Railway)");

        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Development Server");

        return new OpenAPI()
                .info(new Info()
                        .title("EVDMS API")
                        .version("1.0.0")
                        .description("Electric Vehicle Dealer Management System"))
                .servers(Arrays.asList(productionServer, localServer))
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