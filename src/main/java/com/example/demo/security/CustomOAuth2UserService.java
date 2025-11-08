// src/main/java/com/example/demo/security/CustomOAuth2UserService.java
package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor; 

import java.util.Optional;

@Service
@RequiredArgsConstructor 
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        
        OAuth2User oauth2User = super.loadUser(userRequest); 
        String email = oauth2User.getAttribute("email");

        // LOGIC: Tìm user trong CSDL
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            // NẾU KHÔNG CÓ -> Ném lỗi
            // Lỗi này sẽ bị OAuth2LoginFailureHandler (Bước 3.3) bắt
            throw new OAuth2AuthenticationException(
                new OAuth2Error("USER_NOT_FOUND"), 
                "Tài khoản Google với email " + email + " không tồn tại."
            );
        }

        User user = userOptional.get();
        user.setFullName(oauth2User.getAttribute("name"));
        userRepository.save(user);

        return oauth2User; // Trả về user cho SuccessHandler (Bước 3.2)
    }
}