package com.example.demo.controller;

import com.example.demo.dto.Mailbody;
import com.example.demo.entity.ForgotPassword;
import com.example.demo.entity.User;
import com.example.demo.repository.ForgotPasswordRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import com.example.demo.util.ChangePassword;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

@RestController
@RequestMapping("/forgotpassword")

public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final PasswordEncoder passwordEncoder;

    public ForgotPasswordController(UserRepository userRepository, EmailService emailService, ForgotPasswordRepository forgotPasswordRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.forgotPasswordRepository = forgotPasswordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/verifyMail/{email}")
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
                .expiryDate(new Date(System.currentTimeMillis()+70*1000))
                .user(user)
                .build();

        emailService.sendSimpleMessage(mailbody);
        forgotPasswordRepository.save(fp);

        return ResponseEntity.ok("Email sent for verification successful!");

    }

    @PostMapping("/verifyOtp/{otp}/{email}")
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

    @PostMapping("/changePassword/{email}")
    public ResponseEntity<String> changePasswordHandler(@RequestBody ChangePassword changePassword, @PathVariable String email) {
        if(!Objects.equals(changePassword.password(), changePassword.repeatPassword()))
            return new ResponseEntity<>("Please enter password again", HttpStatus.EXPECTATION_FAILED);

        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email, encodedPassword);

        return ResponseEntity.ok("Password changed!");
    }



    private Integer otpGenerator(){
        Random random = new Random();
        return random.nextInt(100_000,999_999);

    }
}
