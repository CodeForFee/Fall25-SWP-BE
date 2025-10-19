package com.example.demo.service.impl;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.dto.OrderDetailResponseDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.OrderDetailRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    @Override
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponseDTO getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        return convertToResponseDTO(order);
    }

    @Override
    public List<OrderResponseDTO> getOrdersByCustomerId(Integer customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponseDTO> getOrdersByDealerId(Integer dealerId) {
        return orderRepository.findByDealerId(dealerId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponseDTO> getOrdersByUserId(Integer userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponseDTO getOrderByQuoteId(Integer quoteId) {
        Order order = orderRepository.findByQuoteId(quoteId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với Quote ID: " + quoteId));
        return convertToResponseDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderDTO orderDTO) {
        try {
            log.info("=== START CREATE ORDER ===");

            Order order = new Order();
            order.setQuoteId(orderDTO.getQuoteId());
            order.setCustomerId(orderDTO.getCustomerId());
            order.setDealerId(orderDTO.getDealerId());
            order.setUserId(orderDTO.getUserId());
            order.setOrderDate(orderDTO.getOrderDate() != null ? orderDTO.getOrderDate() : LocalDate.now());
            order.setStatus(Order.OrderStatus.valueOf(orderDTO.getStatus().toUpperCase()));
            order.setPaymentMethod(Order.PaymentMethod.valueOf(orderDTO.getPaymentMethod().toUpperCase()));
            order.setNotes(orderDTO.getNotes());

            // TÍNH TOÁN
            BigDecimal totalAmount = BigDecimal.ZERO;

            List<OrderDetail> orderDetails = null;
            if (orderDTO.getOrderDetails() != null && !orderDTO.getOrderDetails().isEmpty()) {
                orderDetails = new ArrayList<>();

                for (OrderDetailDTO detailDTO : orderDTO.getOrderDetails()) {
                    OrderDetail detail = new OrderDetail();
                    detail.setVehicleId(detailDTO.getVehicleId());
                    detail.setQuantity(detailDTO.getQuantity());
                    detail.setUnitPrice(detailDTO.getUnitPrice());

                    // TÍNH TOÁN
                    BigDecimal itemTotal = detailDTO.getUnitPrice()
                            .multiply(BigDecimal.valueOf(detailDTO.getQuantity()));

                    detail.setTotalAmount(itemTotal);
                    totalAmount = totalAmount.add(itemTotal);

                    orderDetails.add(detail);
                }

                order.setTotalAmount(totalAmount);
            } else {
                order.setTotalAmount(BigDecimal.ZERO);
            }

            // TÍNH TOÁN paidAmount và remainingAmount
            BigDecimal paidAmount = orderDTO.getPaidAmount() != null ?
                    orderDTO.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal remainingAmount = totalAmount.subtract(paidAmount);

            order.setPaidAmount(paidAmount);
            order.setRemainingAmount(remainingAmount);

            Order savedOrder = orderRepository.save(order);

            // Set orderId cho các detail và save
            if (!orderDetails.isEmpty()) { // SỬA ĐIỀU KIỆN Ở ĐÂY
                for (OrderDetail detail : orderDetails) {
                    detail.setOrderId(savedOrder.getId());
                }
                orderDetailRepository.saveAll(orderDetails);
            }

            log.info("=== ORDER CREATED SUCCESSFULLY - Total: {}, Paid: {}, Remaining: {} ===",
                    totalAmount, paidAmount, remainingAmount);
            return convertToResponseDTO(savedOrder);

        } catch (Exception e) {
            log.error("!!! ERROR IN CREATE ORDER !!!", e);
            throw new RuntimeException("Lỗi server khi tạo đơn hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrder(Integer id, OrderDTO orderDTO) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        existingOrder.setQuoteId(orderDTO.getQuoteId());
        existingOrder.setCustomerId(orderDTO.getCustomerId());
        existingOrder.setDealerId(orderDTO.getDealerId());
        existingOrder.setUserId(orderDTO.getUserId());
        existingOrder.setStatus(Order.OrderStatus.valueOf(orderDTO.getStatus().toUpperCase()));
        existingOrder.setPaymentMethod(Order.PaymentMethod.valueOf(orderDTO.getPaymentMethod().toUpperCase()));
        existingOrder.setNotes(orderDTO.getNotes());

        // TÍNH TOÁN LẠI totalAmount khi update
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Update order details
        if (orderDTO.getOrderDetails() != null && !orderDTO.getOrderDetails().isEmpty()) {
            // Delete existing details
            orderDetailRepository.deleteByOrderId(id);

            // Save new details và tính toán totalAmount
            List<OrderDetail> orderDetails = new ArrayList<>();
            for (OrderDetailDTO detailDTO : orderDTO.getOrderDetails()) {
                OrderDetail detail = new OrderDetail();
                detail.setOrderId(id);
                detail.setVehicleId(detailDTO.getVehicleId());
                detail.setQuantity(detailDTO.getQuantity());
                detail.setUnitPrice(detailDTO.getUnitPrice());

                // TÍNH TOÁN
                BigDecimal itemTotal = detailDTO.getUnitPrice()
                        .multiply(BigDecimal.valueOf(detailDTO.getQuantity()));

                detail.setTotalAmount(itemTotal);
                totalAmount = totalAmount.add(itemTotal);

                orderDetails.add(detail);
            }

            orderDetailRepository.saveAll(orderDetails);
            existingOrder.setTotalAmount(totalAmount);
        } else {
            existingOrder.setTotalAmount(BigDecimal.ZERO);
        }

        // TÍNH TOÁN
        BigDecimal paidAmount = orderDTO.getPaidAmount() != null ?
                orderDTO.getPaidAmount() : existingOrder.getPaidAmount();
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount);

        existingOrder.setPaidAmount(paidAmount);
        existingOrder.setRemainingAmount(remainingAmount);

        Order updatedOrder = orderRepository.save(existingOrder);
        log.info("=== ORDER UPDATED SUCCESSFULLY - Total: {}, Paid: {}, Remaining: {} ===",
                totalAmount, paidAmount, remainingAmount);
        return convertToResponseDTO(updatedOrder);
    }


    @Override
    @Transactional
    public void deleteOrder(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        // Delete order details first
        orderDetailRepository.deleteByOrderId(id);

        // Delete order
        orderRepository.delete(order);
        log.info("=== ORDER DELETED SUCCESSFULLY ===");
    }

    private OrderResponseDTO convertToResponseDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setQuoteId(order.getQuoteId());
        dto.setCustomerId(order.getCustomerId());
        dto.setDealerId(order.getDealerId());
        dto.setUserId(order.getUserId());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaidAmount(order.getPaidAmount());
        dto.setRemainingAmount(order.getRemainingAmount());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setNotes(order.getNotes());

        // Load order details
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        List<OrderDetailResponseDTO> detailDTOs = details.stream()
                .map(this::convertToDetailResponseDTO)
                .collect(Collectors.toList());
        dto.setOrderDetails(detailDTOs);

        return dto;
    }

    private OrderDetailResponseDTO convertToDetailResponseDTO(OrderDetail detail) {
        OrderDetailResponseDTO dto = new OrderDetailResponseDTO();
        dto.setId(detail.getId());
        dto.setOrderId(detail.getOrderId());
        dto.setVehicleId(detail.getVehicleId());
        dto.setQuantity(detail.getQuantity());
        dto.setUnitPrice(detail.getUnitPrice());
        dto.setTotalAmount(detail.getTotalAmount()); // Đã được tính tự động
        return dto;
    }
}