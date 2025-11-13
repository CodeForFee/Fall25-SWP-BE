package com.example.demo.service;

import com.example.demo.dto.FeedbackDTO;
import com.example.demo.entity.Feedback;
import com.example.demo.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    public Feedback saveFeedback(FeedbackDTO feedbackDTO) {
        // Cách cũ:
        // Feedback feedback = new Feedback();
        // feedback.setCustomerName(feedbackDTO.getCustomerName());
        // feedback.setEmail(feedbackDTO.getEmail());
        // ... (v.v.)

        // Cách mới với @Builder (ngắn gọn và rõ ràng hơn)
        Feedback feedback = Feedback.builder()
                .customerName(feedbackDTO.getCustomerName())
                .email(feedbackDTO.getEmail())
                .rating(feedbackDTO.getRating())
                .message(feedbackDTO.getMessage())
                .build();
        // lưu ý: createdAt sẽ được tự động gán nhờ @PrePersist

        // Lưu vào CSDL
        return feedbackRepository.save(feedback);
    }
}