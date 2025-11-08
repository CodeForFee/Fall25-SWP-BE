// src/main/java/com/example/demo/security/OAuth2LoginFailureHandler.java
package com.example.demo.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        
        // Lấy thông điệp lỗi, ví dụ "USER_NOT_FOUND"
        String errorCode = exception.getMessage(); 

        // Redirect về trang login của FE kèm thông báo lỗi
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login-success")
                .queryParam("googleError", errorCode) // hoặc true
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}