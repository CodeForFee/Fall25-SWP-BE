package com.example.demo.service;

import com.example.demo.dto.TestDriveRequestDTO;
import com.example.demo.entity.Dealer; // Cập nhật
import com.example.demo.entity.TestDriveRequest;
import com.example.demo.repository.DealerRepository; // Cập nhật
import com.example.demo.repository.TestDriveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestDriveService {

    private final TestDriveRepository testDriveRepository;
    private final DealerRepository dealerRepository; // <-- Cập nhật

    /**
     * Lưu đơn của khách hàng
     */
    public TestDriveRequest scheduleTestDrive(TestDriveRequestDTO dto) {
        
        // 1. Tìm 'Dealer' từ 'dealerId'
        Dealer dealer = dealerRepository.findById(dto.getDealerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Dealer với ID: " + dto.getDealerId()));

        // 2. Tạo đơn lái thử
        TestDriveRequest newRequest = new TestDriveRequest();
        newRequest.setCustomerName(dto.getCustomerName());
        newRequest.setCustomerEmail(dto.getCustomerEmail());
        newRequest.setPhoneNumber(dto.getPhoneNumber());
        newRequest.setTime(dto.getTime());
        
        // 3. Set đối tượng Dealer
        newRequest.setDealer(dealer);
        
        return testDriveRepository.save(newRequest);
    }

    
    public List<TestDriveRequest> getDanhSachLichLaiThu(Integer dealerId) { // <-- Cập nhật
        // Gọi hàm query mới
        return testDriveRepository.findByDealerId(dealerId); 
    }
}