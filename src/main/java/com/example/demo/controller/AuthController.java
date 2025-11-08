package com.example.demo.controller;

// --- Imports từ AuthController cũ ---
import com.example.demo.dto.LoginDTO;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// --- Imports từ ForgotPasswordController ---
import com.example.demo.dto.Mailbody;
import com.example.demo.entity.ForgotPassword;
import com.example.demo.entity.User;
import com.example.demo.repository.ForgotPasswordRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import com.example.demo.util.ChangePassword;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

@RestController
@RequestMapping("/api/auth") // <-- Base path cho tất cả
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for authentication and password reset") // <-- Sửa lại tên Tag
public class AuthController {

    // === GỘP TẤT CẢ DEPENDENCIES ===
    private final UserService userService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập và nhận JWT token")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        try {
            String token = userService.loginUser(loginDTO);
            var user = userService.getUserByEmail(loginDTO.getEmail());

            return ResponseEntity.ok(Map.of(
                    // "user Id",user.getUserId(), // <-- Sửa lỗi cú pháp
                    "userId", user.getUserId(), 
                    "token", token,
                    "role", user.getRole(),
                    "message", "Đăng nhập thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // === CÁC API CỦA FORGOTPASSWORD ĐÃ ĐƯỢC CHUYỂN VÀO ĐÂY ===

    @PostMapping("/forgot/verify-mail/{email}") // <-- Đổi path
    @Operation(summary = "Gửi OTP quên mật khẩu qua email")
    public ResponseEntity<String> verifyMail(@PathVariable String email) {
        User user = userRepository.findByEmail(email).
                orElseThrow(() -> new UsernameNotFoundException("Please provide a valid email"));

        int otp = otpGenerator();
        Mailbody mailbody = Mailbody.builder()
                .to(email)
                .text("This is OTP: " + otp)
                .subject("OTP for Forgot Password request")
                .build();

        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expiryDate(new Date(System.currentTimeMillis()+70*1000)) // 70 giây
                .user(user)
                .build();

        emailService.sendSimpleMessage(mailbody);
        forgotPasswordRepository.save(fp);

        return ResponseEntity.ok("Email sent for verification successful!");
    }

    @PostMapping("/forgot/verify-otp/{otp}/{email}") // <-- Đổi path
    @Operation(summary = "Xác thực OTP")
    public ResponseEntity<String> verifyOtp(@PathVariable Integer otp, @PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Please provide a valid email"));

        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp,user). orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (fp.getExpiryDate().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.delete(fp);
            return new ResponseEntity<>("OTP expired!", HttpStatus.EXPECTATION_FAILED);
        }

        return ResponseEntity.ok("OTP verified!");
    }

    @PostMapping("/forgot/change-password/{email}") // <-- Đổi path
    @Operation(summary = "Đổi mật khẩu sau khi xác thực OTP")
    public ResponseEntity<String> changePasswordHandler(@RequestBody ChangePassword changePassword, @PathVariable String email) {
        if(!Objects.equals(changePassword.password(), changePassword.repeatPassword()))
            return new ResponseEntity<>("Please enter password again", HttpStatus.EXPECTATION_FAILED);

        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email, encodedPassword); // Cần đảm bảo hàm này có trong UserRepository

        return ResponseEntity.ok("Password changed!");
    }

    private Integer otpGenerator(){
        Random random = new Random();
        return random.nextInt(100_000,999_999);
    }
}