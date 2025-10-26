package com.example.demo.service.impl;

import com.example.demo.dto.DealerDebtRiskDTO;
import com.example.demo.dto.InventorySpeedDTO;
import com.example.demo.entity.Dealer;
import com.example.demo.repository.DealerRepository;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.OrderDetailRepository;
import com.example.demo.service.DealerAggregateService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;

@Service
public class DealerAggregateServiceIMPL implements DealerAggregateService {

    private final OrderRepository orderRepo;
    private final OrderDetailRepository orderDetailRepo;
    private final InventoryRepository inventoryRepo;
    private final DealerRepository dealerRepo;

    public DealerAggregateServiceIMPL(OrderRepository orderRepo,
                                      OrderDetailRepository orderDetailRepo,
                                      InventoryRepository inventoryRepo,
                                      DealerRepository dealerRepo) {
        this.orderRepo = orderRepo;
        this.orderDetailRepo = orderDetailRepo;
        this.inventoryRepo = inventoryRepo;
        this.dealerRepo = dealerRepo;
    }

    @Override
    public List<DealerDebtRiskDTO> aggregateDebtRisk(LocalDate from, LocalDate to) {
        // Logic đơn giản:
        // - Tính remaining_amount tổng của orders trong khoảng => outstanding
        // - RiskLevel: > 50000 => HIGH, >20000 => MEDIUM, else LOW  (diagnostic rule)
        List<DealerDebtRiskDTO> result = new ArrayList<>();
        List<Dealer> dealers = dealerRepo.findAll();
        for (Dealer d : dealers) {
            List<com.example.demo.entity.Order> orders = orderRepo.findByDealerIdAndOrderDateBetween(d.getDealerId().longValue(), from, to);
            BigDecimal outstanding = orders.stream()
                    .map(o -> o.getRemainingAmount() == null ? BigDecimal.ZERO : o.getRemainingAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String risk;
            if (outstanding.compareTo(new BigDecimal("50000")) > 0) risk = "HIGH";
            else if (outstanding.compareTo(new BigDecimal("20000")) > 0) risk = "MEDIUM";
            else risk = "LOW";
            result.add(new DealerDebtRiskDTO(d.getDealerId().longValue(), d.getName(), outstanding, risk));
        }
        return result;
    }

    @Override
    public List<?> salesDataByDealer(LocalDate from, LocalDate to) {
        // Đơn giản trả list Object[] từ orderRepo.salesByDealerBetween
        return orderRepo.salesByDealerBetween(from, to);
    }

    @Override
    public List<InventorySpeedDTO> inventoryAndSellSpeed(LocalDate from, LocalDate to) {
        // Tính sold in period by vehicle, map to dealer inventories
        List<Object[]> soldByVehicle = orderDetailRepo.totalSoldByVehicleBetween(from, to);
        Map<Long, Integer> soldMap = new HashMap<>();
        for (Object[] r : soldByVehicle) {
            Long vehicleId = ((Number) r[0]).longValue();
            Integer qty = ((Number) r[1]).intValue();
            soldMap.put(vehicleId, qty);
        }
        // Tổng theo dealer: inventoryRepo.totalAvailableByDealer
        List<Object[]> availByDealer = inventoryRepo.totalAvailableByDealer();
        Map<Long, Integer> availMap = new HashMap<>();
        for (Object[] r : availByDealer) {
            Long dealerId = ((Number) r[0]).longValue();
            Integer qty = ((Number) r[1]).intValue();
            availMap.put(dealerId, qty);
        }
        List<InventorySpeedDTO> result = new ArrayList<>();
        List<com.example.demo.entity.Dealer> dealers = dealerRepo.findAll();
        for (Dealer d : dealers) {
            Integer available = availMap.getOrDefault(d.getDealerId(), 0);
            // soldLastPeriod: sum of sold for vehicles that dealer holds — approximation: use soldMap total (global) as proxy
            Integer sold = soldMap.values().stream().mapToInt(Integer::intValue).sum();
            // compute sellThroughRate:
            double denom = available + sold;
            BigDecimal rate = denom == 0 ? BigDecimal.ZERO :
                    BigDecimal.valueOf((double)sold / denom);
            result.add(new InventorySpeedDTO(d.getDealerId(), d.getName(), available, sold, rate));
        }
        return result;
    }
}
