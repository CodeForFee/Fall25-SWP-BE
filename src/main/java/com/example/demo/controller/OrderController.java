package com.example.demo.controller;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.dto.QuoteResponseDTO;
import com.example.demo.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for order management")
@SecurityRequirement(name = "bearer-jwt")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Lấy tất cả đơn hàng")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy đơn hàng theo ID")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Integer id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Lấy đơn hàng theo ID khách hàng")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByCustomerId(@PathVariable Integer customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    @GetMapping("/dealer/{dealerId}")
    @Operation(summary = "Lấy đơn hàng theo ID đại lý")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByDealerId(@PathVariable Integer dealerId) {
        return ResponseEntity.ok(orderService.getOrdersByDealerId(dealerId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy đơn hàng theo ID người dùng")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/quote/{quoteId}")
    @Operation(summary = "Lấy đơn hàng theo ID báo giá")
    public ResponseEntity<OrderResponseDTO> getOrderByQuoteId(@PathVariable Integer quoteId) {
        return ResponseEntity.ok(orderService.getOrderByQuoteId(quoteId));
    }

    @GetMapping("/approved-quotes")
    @Operation(summary = "Lấy danh sách báo giá đã duyệt để tạo đơn hàng")
    public ResponseEntity<List<QuoteResponseDTO>> getApprovedQuotesForOrder() {
        return ResponseEntity.ok(orderService.getApprovedQuotesForOrder());
    }

    @PostMapping
    @Operation(summary = "Tạo đơn hàng mới")
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        return ResponseEntity.ok(orderService.createOrder(orderDTO));
    }

    @PostMapping("/create-from-quote/{quoteId}")
    @Operation(summary = "Tạo đơn hàng từ báo giá đã duyệt")
    public ResponseEntity<OrderResponseDTO> createOrderFromApprovedQuote(
            @PathVariable Integer quoteId,
            @RequestBody OrderDTO orderDTO) {
        return ResponseEntity.ok(orderService.createOrderFromApprovedQuote(quoteId, orderDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật đơn hàng")
    public ResponseEntity<OrderResponseDTO> updateOrder(@PathVariable Integer id, @RequestBody OrderDTO orderDTO) {
        return ResponseEntity.ok(orderService.updateOrder(id, orderDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa đơn hàng")
    public ResponseEntity<String> deleteOrder(@PathVariable Integer id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok("Đơn hàng đã được xóa thành công");
    }
}