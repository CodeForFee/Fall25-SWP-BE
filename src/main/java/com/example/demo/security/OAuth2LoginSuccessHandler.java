package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.entity.User.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.JwtService; 

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    
    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        // Lấy user từ CSDL (đã được xác thực)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Lỗi không tìm thấy user sau khi OAuth2 xác thực."));

        // === GỌI HÀM TẠO TOKEN ===
        // 1. Lấy email từ user
        String userEmail = user.getEmail();
        
        //2. Lấy role từ user
        Role userRole = user.getRole(); // <-- ĐIỀU CHỈNH DÒNG NÀY NẾU CẦN
        String userRoleStr = userRole.name();
        
        // 3. Tạo JWT
        String jwtToken = jwtService.generateToken(userEmail, userRoleStr);

        // Xây dựng URL redirect về FE, kèm theo token
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login-success")
                .queryParam("token", jwtToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}