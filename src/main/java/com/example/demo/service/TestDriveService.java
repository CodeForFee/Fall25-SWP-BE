package com.example.demo.service;

import com.example.demo.dto.TestDriveRequestDTO;
import com.example.demo.entity.Dealer; 
import com.example.demo.entity.TestDriveRequest;
import com.example.demo.repository.DealerRepository; 
import com.example.demo.repository.TestDriveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.demo.dto.Mailbody;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestDriveService {

    private final TestDriveRepository testDriveRepository;
    private final DealerRepository dealerRepository;
    private final EmailService emailService;

    /**
     * Lưu đơn của khách hàng
     */
    public TestDriveRequest scheduleTestDrive(TestDriveRequestDTO dto) {
        
        // 1. Tìm Dealer
        Dealer dealer = dealerRepository.findById(dto.getDealerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Dealer với ID: " + dto.getDealerId()));

        // 2. Tạo đơn lái thử
        TestDriveRequest newRequest = new TestDriveRequest();
        newRequest.setCustomerName(dto.getCustomerName());
        newRequest.setCustomerEmail(dto.getCustomerEmail());
        newRequest.setPhoneNumber(dto.getPhoneNumber());
        newRequest.setCarModel(dto.getCarModel());
        newRequest.setDate(dto.getDate());
        newRequest.setTime(dto.getTime());
        newRequest.setDealer(dealer);
        
        TestDriveRequest savedRequest = testDriveRepository.save(newRequest);
        sendConfirmationEmailToCustomer(savedRequest);
        return savedRequest;
    }

    public List<TestDriveRequest> getDanhSachLichLaiThu(Integer dealerId) {
        return testDriveRepository.findByDealerId(dealerId); 
    }

    private void sendConfirmationEmailToCustomer(TestDriveRequest request) {
        
        String customerEmail = request.getCustomerEmail();
        String customerName = request.getCustomerName();
        String PhoneNumber = request.getPhoneNumber();
        String dealerName = request.getDealer().getName();
        String dealerAddress = request.getDealer().getAddress();
        String date = request.getDate();
        String time = request.getTime();
        String carModel = request.getCarModel();
        String subject = "Xác nhận lịch hẹn lái thử tại " + dealerName;
        String body = "Chào " + customerName + ",\n\n" +
                      "Cảm ơn bạn đã đặt lịch lái thử tại " + dealerName + ". \n\n" +
                      "Chúng tôi đã nhận được thông tin lịch hẹn của bạn:\n" +
                      "--------------------------------\n" +
                      "Mẫu xe: " + carModel + "\n" +
                      "Giờ hẹn: " + time + "\n" +
                      "Ngày hẹn: " + date + "\n" +
                      "Địa điểm: " + dealerAddress + "\n" +
                      "Số điện thoại: " + PhoneNumber + "\n" +
                      "--------------------------------\n\n" +
                      "Nhân viên của chúng tôi sẽ sớm liên hệ với bạn để xác nhận lần cuối.\n\n" +
                      "Trân trọng,\n" +
                      "Đội ngũ " + dealerName;

        // Tạo Mailbody DTO (giống như cách bạn làm ở ForgotPassword)
        Mailbody mailbody = Mailbody.builder()
                .to(customerEmail)
                .subject(subject)
                .text(body)
                .build();

        emailService.sendSimpleMessage(mailbody);
    }
}