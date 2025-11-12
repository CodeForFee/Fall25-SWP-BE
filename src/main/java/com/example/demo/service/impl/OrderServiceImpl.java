package com.example.demo.service.impl;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderDetailResponseDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderDetail;
import com.example.demo.entity.Quote;
import com.example.demo.entity.QuoteDetail;
import com.example.demo.repository.OrderDetailRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.QuoteDetailRepository;
import com.example.demo.repository.QuoteRepository;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final QuoteDetailRepository quoteDetailRepository;
    private final QuoteRepository quoteRepository;

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
    public OrderResponseDTO createOrder(OrderDTO orderDTO) {
        try {
            List<QuoteDetail> quoteDetails = quoteDetailRepository.findByQuoteId(orderDTO.getQuoteId());
            if (quoteDetails.isEmpty()) {
                throw new RuntimeException("Không tìm thấy chi tiết báo giá với Quote ID: " + orderDTO.getQuoteId());
            }

            Quote quote = quoteRepository.findById(orderDTO.getQuoteId())
                    .orElseThrow(() -> new RuntimeException("Quote not found: " + orderDTO.getQuoteId()));

            // 🔥 FIX: Xử lý customerId có thể null
            Integer customerId = orderDTO.getCustomerId();
            if (customerId == null) {
                customerId = quote.getCustomerId(); // Có thể vẫn null
                log.warn("CustomerId is null in orderDTO, using quote customerId: {}", customerId);
            }

            // 🔥 VALIDATE: Nếu cả quote và order đều không có customerId
            if (customerId == null) {
                log.info("Creating order without customer - Quote ID: {}", orderDTO.getQuoteId());
                // Vẫn cho phép tạo order không có customer
            }

            Order order = new Order();
            order.setQuoteId(orderDTO.getQuoteId());
            order.setCustomerId(customerId); // Có thể là null
            order.setDealerId(orderDTO.getDealerId());
            order.setUserId(orderDTO.getUserId());
            order.setOrderDate(orderDTO.getOrderDate() != null ? orderDTO.getOrderDate() : LocalDate.now());
            order.setStatus(Order.OrderStatus.valueOf(orderDTO.getStatus().toUpperCase()));
            order.setPaymentMethod(Order.PaymentMethod.valueOf(orderDTO.getPaymentMethod().toUpperCase()));
            order.setNotes(orderDTO.getNotes());

            if (orderDTO.getPaymentPercentage() != null) {
                order.setPaymentPercentage(orderDTO.getPaymentPercentage());
            }

            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalDiscount = BigDecimal.ZERO;
            List<OrderDetail> orderDetails = new ArrayList<>();

            for (QuoteDetail quoteDetail : quoteDetails) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setVehicleId(quoteDetail.getVehicleId());
                orderDetail.setQuantity(quoteDetail.getQuantity());
                orderDetail.setUnitPrice(quoteDetail.getUnitPrice());

                BigDecimal promotionDiscount = quoteDetail.getPromotionDiscount();
                BigDecimal grossAmount = quoteDetail.getUnitPrice().multiply(BigDecimal.valueOf(quoteDetail.getQuantity()));
                BigDecimal discountAmount = BigDecimal.ZERO;
                BigDecimal netAmount = grossAmount;

                if (promotionDiscount != null && promotionDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discountPercent = promotionDiscount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    discountAmount = grossAmount.multiply(discountPercent).setScale(2, RoundingMode.HALF_UP);
                    netAmount = grossAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
                }

                orderDetail.setTotalAmount(netAmount);
                totalAmount = totalAmount.add(netAmount);
                totalDiscount = totalDiscount.add(discountAmount);
                orderDetails.add(orderDetail);
            }

            order.setTotalAmount(totalAmount);
            order.setTotalDiscount(totalDiscount);

            BigDecimal paidAmount = BigDecimal.ZERO;
            if (orderDTO.getPaymentPercentage() != null && orderDTO.getPaymentPercentage() > 0) {
                paidAmount = totalAmount.multiply(BigDecimal.valueOf(orderDTO.getPaymentPercentage()))
                        .divide(BigDecimal.valueOf(100));
            } else if (orderDTO.getPaidAmount() != null) {
                paidAmount = orderDTO.getPaidAmount();
            }

            BigDecimal remainingAmount = totalAmount.subtract(paidAmount);

            order.setPaidAmount(paidAmount);
            order.setRemainingAmount(remainingAmount);

            if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
                order.setPaymentStatus(Order.PaymentStatus.UNPAID);
            } else if (paidAmount.compareTo(totalAmount) == 0) {
                order.setPaymentStatus(Order.PaymentStatus.PAID);
            } else {
                order.setPaymentStatus(Order.PaymentStatus.PARTIALLY_PAID);
            }

            Order savedOrder = orderRepository.save(order);

            for (OrderDetail detail : orderDetails) {
                detail.setOrderId(savedOrder.getId());
            }
            orderDetailRepository.saveAll(orderDetails);

            log.info("Order created successfully - ID: {}, Quote ID: {}, Customer ID: {}",
                    savedOrder.getId(), orderDTO.getQuoteId(), customerId);

            return convertToResponseDTO(savedOrder);

        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
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

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        List<QuoteDetail> quoteDetails = quoteDetailRepository.findByQuoteId(orderDTO.getQuoteId());
        if (quoteDetails.isEmpty()) {
            throw new RuntimeException("Không tìm thấy chi tiết báo giá với Quote ID: " + orderDTO.getQuoteId());
        }

        orderDetailRepository.deleteByOrderId(id);
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (QuoteDetail quoteDetail : quoteDetails) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(id);
            orderDetail.setVehicleId(quoteDetail.getVehicleId());
            orderDetail.setQuantity(quoteDetail.getQuantity());
            orderDetail.setUnitPrice(quoteDetail.getUnitPrice());

            BigDecimal promotionDiscount = quoteDetail.getPromotionDiscount();
            BigDecimal grossAmount = quoteDetail.getUnitPrice().multiply(BigDecimal.valueOf(quoteDetail.getQuantity()));
            BigDecimal discountAmount = BigDecimal.ZERO;
            BigDecimal netAmount = grossAmount;

            if (promotionDiscount != null && promotionDiscount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discountPercent = promotionDiscount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                discountAmount = grossAmount.multiply(discountPercent).setScale(2, RoundingMode.HALF_UP);
                netAmount = grossAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
            }

            orderDetail.setTotalAmount(netAmount);
            totalAmount = totalAmount.add(netAmount);
            totalDiscount = totalDiscount.add(discountAmount);
            orderDetails.add(orderDetail);
        }

        orderDetailRepository.saveAll(orderDetails);
        existingOrder.setTotalAmount(totalAmount);
        existingOrder.setTotalDiscount(totalDiscount);

        BigDecimal paidAmount = orderDTO.getPaidAmount() != null ? orderDTO.getPaidAmount() : existingOrder.getPaidAmount();
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount);

        existingOrder.setPaidAmount(paidAmount);
        existingOrder.setRemainingAmount(remainingAmount);

        Order updatedOrder = orderRepository.save(existingOrder);
        return convertToResponseDTO(updatedOrder);
    }

    @Override
    @Transactional
    public void deleteOrder(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        orderDetailRepository.deleteByOrderId(id);
        orderRepository.delete(order);
    }

    @Override
    public List<OrderResponseDTO> getOrdersByCreatedByRole(String createdByRole) {
        return List.of();
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
        dto.setTotalDiscount(order.getTotalDiscount());
        dto.setPaidAmount(order.getPaidAmount());
        dto.setRemainingAmount(order.getRemainingAmount());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setNotes(order.getNotes());
        dto.setPaymentPercentage(order.getPaymentPercentage());
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        dto.setApprovalStatus(order.getApprovalStatus() != null ? order.getApprovalStatus().name() : null);
        dto.setApprovedBy(order.getApprovedBy());
        dto.setApprovedAt(order.getApprovedAt());
        dto.setApprovalNotes(order.getApprovalNotes());
        
        try {
            List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
            List<OrderDetailResponseDTO> detailDTOs = details.stream()
                    .map(this::convertToDetailResponseDTO)
                    .collect(Collectors.toList());
            dto.setOrderDetails(detailDTOs);
        } catch (Exception e) {
            log.warn("Could not fetch orderDetails for order {}: {}", order.getId(), e.getMessage());
            dto.setOrderDetails(new ArrayList<>());
        }

        return dto;
    }

    private OrderDetailResponseDTO convertToDetailResponseDTO(OrderDetail detail) {
        OrderDetailResponseDTO dto = new OrderDetailResponseDTO();
        dto.setId(detail.getId());
        dto.setOrderId(detail.getOrderId());
        dto.setVehicleId(detail.getVehicleId());
        dto.setQuantity(detail.getQuantity());
        dto.setUnitPrice(detail.getUnitPrice());
        dto.setTotalAmount(detail.getTotalAmount());
        return dto;
    }
}
