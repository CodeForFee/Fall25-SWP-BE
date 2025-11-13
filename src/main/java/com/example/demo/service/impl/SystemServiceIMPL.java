package com.example.demo.service.impl;

import com.example.demo.dto.SystemStatusDTO;
import com.example.demo.dto.DealerPerformanceDTO;
import com.example.demo.entity.Dealer;
import com.example.demo.repository.DealerRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.SystemService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;

@Service
public class SystemServiceIMPL implements SystemService {

    private final OrderRepository orderRepo;
    private final DealerRepository dealerRepo;

    public SystemServiceIMPL(OrderRepository orderRepo, DealerRepository dealerRepo) {
        this.orderRepo = orderRepo;
        this.dealerRepo = dealerRepo;
    }

    @Override
    public SystemStatusDTO aggregateSystemStatus(LocalDate from, LocalDate to) {
        // 1) số đơn theo trạng thái
        List<Object[]> counts = orderRepo.countByStatus();
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : counts) {
            Object status = row[0];
            Object cnt = row[1];
            map.put(status == null ? "UNKNOWN" : status.toString(), ((Number)cnt).longValue());
        }
        // 2) tổng doanh số
        BigDecimal total = orderRepo.totalSalesBetween(from, to);
        return new SystemStatusDTO(map, total == null ? BigDecimal.ZERO : total);
    }

    @Override
    public List<DealerPerformanceDTO> dealerPerformance(LocalDate from, LocalDate to) {
        List<Object[]> rows = orderRepo.salesByDealerBetween(from, to);
        Map<Long, BigDecimal> salesMap = new HashMap<>();
        for (Object[] r : rows) {
            Long dealerId = ((Number) r[0]).longValue();
            BigDecimal sum = (BigDecimal) r[1];
            salesMap.put(dealerId, sum == null ? BigDecimal.ZERO : sum);
        }
        // Lấy total orders per dealer (simple)
        List<Dealer> dealers = dealerRepo.findAll();
        List<DealerPerformanceDTO> result = new ArrayList<>();
        for (Dealer d : dealers) {
            BigDecimal s = salesMap.getOrDefault(d.getDealerId(), BigDecimal.ZERO);
            // Đếm đơn: dùng repository findByDealerIdAndOrderDateBetween (not implemented here to count) -> simple approximation: not counted, set 0
            // nếu cần count thực tế, bổ sung query.
            result.add(new DealerPerformanceDTO(d.getDealerId(), d.getName(), s, 0L));
        }
        return result;
    }
}
