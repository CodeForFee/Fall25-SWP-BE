package com.example.demo.service;

import com.example.demo.dto.Mailbody;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private final SendGrid sendGrid;

    public EmailService() {
        // 1. Lấy API Key từ biến môi trường
        String apiKey = System.getenv("SENDGRID_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Biến môi trường SENDGRID_API_KEY không được tìm thấy!");
        }
        this.sendGrid = new SendGrid(apiKey);
    }

    public void sendSimpleMessage(Mailbody mailbody) {
        
        // 3. Email "From" - RẤT QUAN TRỌNG
        Email from = new Email("anhndtse180244@fpt.edu.vn"); 

        Email to = new Email(mailbody.to());
        String subject = mailbody.subject();
        Content content = new Content("text/plain", mailbody.text());

        // Tạo đối tượng Mail của SendGrid
        Mail mail = new Mail(from, subject, to, content);

        // 4. Tạo HTTP Request để gửi mail
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send"); // API endpoint của SendGrid
            request.setBody(mail.build());

            // 5. Gửi request (Đây là lúc gọi API qua HTTP, không dùng SMTP)
            Response response = this.sendGrid.api(request);

            // Ghi log lại để kiểm tra (tùy chọn)
            System.out.println("SendGrid Status Code: " + response.getStatusCode());
            
            // Nếu SendGrid trả về lỗi (ví dụ: 401, 403, 400)
            if (response.getStatusCode() >= 400) {
                System.err.println("SendGrid Error Body: " + response.getBody());
                // Ném lỗi này để @Transactional bên TestDriveService có thể rollback
                throw new RuntimeException("Lỗi khi gửi mail từ SendGrid: " + response.getBody());
            }

        } catch (IOException ex) {
            // Ném lỗi này để @Transactional có thể rollback
            throw new RuntimeException("Lỗi I/O khi gửi mail: " + ex.getMessage(), ex);
        }
    }
}