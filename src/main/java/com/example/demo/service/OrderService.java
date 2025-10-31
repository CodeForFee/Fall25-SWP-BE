package com.example.demo.service;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.dto.QuoteResponseDTO;

import java.util.List;

public interface OrderService {
    List<OrderResponseDTO> getAllOrders();
    OrderResponseDTO getOrderById(Integer id);
    List<OrderResponseDTO> getOrdersByCustomerId(Integer customerId);
    List<OrderResponseDTO> getOrdersByDealerId(Integer dealerId);
    List<OrderResponseDTO> getOrdersByUserId(Integer userId);
    OrderResponseDTO getOrderByQuoteId(Integer quoteId);
    List<QuoteResponseDTO> getApprovedQuotesForOrder();
    OrderResponseDTO createOrder(OrderDTO orderDTO);
    OrderResponseDTO createOrderFromApprovedQuote(Integer quoteId, OrderDTO orderDTO);
    OrderResponseDTO updateOrder(Integer id, OrderDTO orderDTO);
    void deleteOrder(Integer id);
}