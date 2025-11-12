package com.example.demo.service;

import com.example.demo.dto.TestDriveRequestDTO;
import com.example.demo.entity.Dealer; 
import com.example.demo.entity.TestDriveRequest;
import com.example.demo.entity.TestDriveRequest.TestDriveStatus;
import com.example.demo.repository.DealerRepository; 
import com.example.demo.repository.TestDriveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.demo.dto.Mailbody;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        newRequest.setDealer(dealer);
        newRequest.setDate(dto.getDate());
        newRequest.setRequestTime(dto.getRequestTime());
        newRequest.setNote(dto.getNote());
        newRequest.setStatus(TestDriveStatus.PENDING);
        
        return testDriveRepository.save(newRequest);
    }

    public List<TestDriveRequest> findByDealerIdAndStatus(Integer dealerId) {
        return testDriveRepository.findByDealerIdAndStatus(dealerId, TestDriveStatus.PENDING);
    }

    public List<TestDriveRequest> getDanhSachLichLaiThu(Integer dealerId) {
        return testDriveRepository.findByDealerId(dealerId);
    }

    /**
     * Nhân viên gọi hàm này để XÁC NHẬN lịch hẹn.
     */
    public TestDriveRequest confirmTestDrive(Long requestId) {
        // Tìm đơn
        TestDriveRequest request = testDriveRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn với ID: " + requestId));

        // Kiểm tra trạng thái
        if (request.getStatus() != TestDriveStatus.PENDING) {
            throw new RuntimeException("Đơn này đã được xử lý (không ở trạng thái PENDING).");
        }

        // Cập nhật trạng thái
        request.setStatus(TestDriveStatus.CONFIRMED);
        TestDriveRequest savedRequest = testDriveRepository.save(request);

        // Gửi email XÁC NHẬN THÀNH CÔNG
        sendConfirmationEmailToCustomer(savedRequest);

        return savedRequest;
    }

    private void sendConfirmationEmailToCustomer(TestDriveRequest request) {
        
        String customerEmail = request.getCustomerEmail();
        String customerName = request.getCustomerName();
        String PhoneNumber = request.getPhoneNumber();
        String dealerName = request.getDealer().getName();
        String dealerAddress = request.getDealer().getAddress();
        LocalDate date = request.getDate();
        LocalDateTime requestTime = request.getRequestTime();
        String carModel = request.getCarModel();
        String subject = "Xác nhận lịch hẹn lái thử tại " + dealerName;
        String body = "Chào " + customerName + ",\n\n" +
                      "Cảm ơn bạn đã đặt lịch lái thử tại " + dealerName + ". \n\n" +
                      "Chúng tôi đã nhận được thông tin lịch hẹn của bạn:\n" +
                      "--------------------------------\n" +
                      "Mẫu xe: " + carModel + "\n" +
                      "Giờ hẹn: " + requestTime + "\n" +
                      "Ngày hẹn: " + date + "\n" +
                      "Địa điểm: " + dealerAddress + "\n" +
                      "Số điện thoại: " + PhoneNumber + "\n" +
                      "--------------------------------\n\n" +
                      "Nhân viên của chúng tôi sẽ sớm liên hệ với bạn để xác nhận lần cuối.\n\n" +
                      "Trân trọng,\n" +
                      "Đội ngũ " + dealerName;

        Mailbody mailbody = Mailbody.builder()
                .to(customerEmail)
                .subject(subject)
                .text(body)
                .build();

        emailService.sendSimpleMessage(mailbody);
    }


    /**
     * Nhân viên gọi hàm này để TỪ CHỐI lịch hẹn.
     */
    public TestDriveRequest rejectTestDrive(Long requestId, String reason) { // Thêm lý do từ chối
        // Tìm đơn
        TestDriveRequest request = testDriveRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn với ID: " + requestId));

        // Kiểm tra trạng thái
        if (request.getStatus() != TestDriveStatus.PENDING) {
            throw new RuntimeException("Đơn này đã được xử lý (không ở trạng thái PENDING).");
        }

        // Cập nhật trạng thái
        request.setStatus(TestDriveStatus.REJECTED);
        TestDriveRequest savedRequest = testDriveRepository.save(request);

        // Gửi email THÔNG BÁO TỪ CHỐI
        sendRejectionEmailToCustomer(savedRequest, reason);

        return savedRequest;
    }
    
    private void sendRejectionEmailToCustomer(TestDriveRequest request, String reason) {
        String customerEmail = request.getCustomerEmail();
        String customerName = request.getCustomerName();
        String dealerName = request.getDealer().getName();
        LocalDate date = request.getDate();
        LocalDateTime requestTime = request.getRequestTime();
        String carModel = request.getCarModel();

        String subject = "Thông báo về lịch hẹn lái thử tại " + dealerName;
        String body = "Chào " + customerName + ",\n\n" +
                "Chúng tôi rất tiếc phải thông báo rằng lịch hẹn lái thử của bạn cho xe " + carModel + 
                " vào lúc " + requestTime + ", ngày " + date + " tại " + dealerName + " không thể được xác nhận.\n\n" +
                "Lý do: " + reason + "\n\n" +
                "Xin vui lòng liên hệ lại với chúng tôi hoặc đặt một lịch hẹn khác vào thời điểm thuận tiện hơn.\n\n" +
                "Chúng tôi xin lỗi vì sự bất tiện này.\n" +
                "Trân trọng,\n" +
                "Đội ngũ " + dealerName;

        Mailbody mailbody = Mailbody.builder()
                .to(customerEmail)
                .subject(subject)
                .text(body)
                .build();

        emailService.sendSimpleMessage(mailbody);
    }
}
