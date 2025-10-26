package com.example.demo.service;

import com.example.demo.dto.DealerDebtRiskDTO;
import com.example.demo.dto.InventorySpeedDTO;
import java.time.LocalDate;
import java.util.List;

public interface DealerAggregateService {
    // công nợ & logic rủi ro
    List<DealerDebtRiskDTO> aggregateDebtRisk(LocalDate from, LocalDate to);

    // dữ liệu bán hàng theo dealer (tổng, phân bổ) -> tái sử dụng SystemService nếu cần
    // trả doanh số per dealer between dates
    List<?> salesDataByDealer(LocalDate from, LocalDate to);

    // query tồn kho & tốc độ bán (sell-through)
    List<InventorySpeedDTO> inventoryAndSellSpeed(LocalDate from, LocalDate to);
}
