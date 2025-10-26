package com.example.demo.service;

import com.example.demo.dto.SystemStatusDTO;
import com.example.demo.dto.DealerPerformanceDTO;
import java.time.LocalDate;
import java.util.List;

public interface SystemService {
    // Trạng thái đơn hàng & tổng doanh số toàn hệ thống
    SystemStatusDTO aggregateSystemStatus(LocalDate from, LocalDate to);

    // Hiệu suất từng dealer: trả list theo dealer
    List<DealerPerformanceDTO> dealerPerformance(LocalDate from, LocalDate to);
}
