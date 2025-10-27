package com.example.demo.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor
public class SystemStatusDTO {
    // số đơn theo trạng thái
    private Map<String, Long> orderStatusCounts;
    // tổng doanh số
    private BigDecimal totalSales;
}
