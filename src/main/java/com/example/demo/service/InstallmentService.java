package com.example.demo.service;

import com.example.demo.dto.InstallmentRequest;
import com.example.demo.dto.InstallmentScheduleDTO;

import java.util.List;

/**
 * Interface định nghĩa các hành vi của Service xử lý trả góp
 */
public interface InstallmentService {

    /**
     * Tạo kế hoạch trả góp (chia tiền thành nhiều kỳ)
     */
    List<InstallmentScheduleDTO> createInstallmentPlan(InstallmentRequest req);

    /**
     * Lấy danh sách các kỳ trả góp theo Payment ID
     */
    List<InstallmentScheduleDTO> getInstallments(Integer paymentId);

    /**
     * Đánh dấu một kỳ là đã thanh toán
     */
    void markAsPaid(Integer transactionId, String method);
}
