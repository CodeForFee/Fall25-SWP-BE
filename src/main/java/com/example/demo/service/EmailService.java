package com.example.demo.service;

import com.example.demo.dto.Mailbody;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {


    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender){
        this.javaMailSender = javaMailSender;
    }

    public void sendSimpleMessage(Mailbody mailbody){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(mailbody.to());
        message.setFrom("ndta2409@gmail.com");
        message.setSubject(mailbody.subject());
        message.setText(mailbody.text());

        javaMailSender.send(message);
    }

}
