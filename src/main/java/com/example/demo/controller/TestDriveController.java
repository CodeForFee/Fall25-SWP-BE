package com.example.demo.controller;

import com.example.demo.dto.TestDriveRequestDTO;
import com.example.demo.entity.TestDriveRequest;
import com.example.demo.service.TestDriveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-drive")
@RequiredArgsConstructor
public class TestDriveController {

    private final TestDriveService testDriveService;

    /**
     * API cho khách hàng đặt lịch
     */
    @PostMapping("/schedule")
    public ResponseEntity<String> scheduleTestDrive(@RequestBody TestDriveRequestDTO requestDTO) {
        try {
            testDriveService.scheduleTestDrive(requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                                 .body("Đã gửi yêu cầu lái thử thành công.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * API cho nhân viên xem danh sách
     */
    @GetMapping("/schedule-list")
    public ResponseEntity<List<TestDriveRequest>> getDanhSachTheoHang(
            @RequestParam Integer dealerId) { 
        
        List<TestDriveRequest> danhSach = testDriveService.getDanhSachLichLaiThu(dealerId);
        return ResponseEntity.ok(danhSach); 
    }
}