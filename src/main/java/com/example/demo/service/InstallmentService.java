package com.example.demo.service;

import com.example.demo.dto.InstallmentPlanDTO;
import com.example.demo.dto.InstallmentRequest;
import com.example.demo.entity.InstallmentSchedule;
import java.util.List;

public interface InstallmentService {
    InstallmentPlanDTO previewInstallmentPlan(InstallmentRequest request);
    List<InstallmentSchedule> generateSchedule(Integer orderId, InstallmentRequest request);
    InstallmentSchedule markInstallmentPaid(Integer scheduleId);
    List<InstallmentSchedule> getSchedulesByOrderId(Integer orderId);
}
