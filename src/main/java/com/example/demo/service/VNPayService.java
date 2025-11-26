package com.example.demo.service;

import com.example.demo.dto.VNPayPaymentRequestDTO;
import com.example.demo.dto.VNPayPaymentResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${vnpay.url}")
    private String vnpayUrl;
    @Value("${vnpay.tmn-code}")
    private String vnpayTmnCode;
    @Value("${vnpay.secret-key}")
    private String vnpaySecretKey;
    @Value("${vnpay.return-url}")
    private String vnpayReturnUrl;
    @Value("${vnpay.return-url.success:http://localhost:5173/payment-result?status=success}")
    private String vnpayReturnUrlSuccess;
    @Value("${vnpay.return-url.fail:http://localhost:5173/payment-result?status=fail}")
    private String vnpayReturnUrlFail;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Transactional
    public VNPayPaymentResponseDTO createPayment(VNPayPaymentRequestDTO paymentRequest, HttpServletRequest request, BigDecimal paidAmount) {
        try {
            log.info("Creating VNPay payment for order: {}", paymentRequest.getOrderId());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + paymentRequest.getOrderId()));

            if (!order.canProcessPayment()) {
                throw new RuntimeException("Order cannot process payment");
            }
            
            if (!isOrderApproved(order)) {
                throw new RuntimeException("Order must be approved before creating VNPay payment. Current status: " +
                        order.getStatus() + ", Approval status: " + order.getApprovalStatus());
            }

            List<Payment> existingPayments = paymentRepository.findByOrderId(order.getId());
            if (!existingPayments.isEmpty()) {
                boolean hasSuccessfulCashPayment = existingPayments.stream()
                        .anyMatch(p -> p.getPaymentMethod() == Payment.PaymentMethod.CASH && p.isSuccessful());

                if (hasSuccessfulCashPayment) {
                    throw new RuntimeException("Order already has successful CASH payment. Cannot create VNPay payment.");
                }
                boolean hasPendingVNPayPayment = existingPayments.stream()
                        .anyMatch(p -> p.getPaymentMethod() == Payment.PaymentMethod.VNPAY && p.isPending());

                if (hasPendingVNPayPayment) {
                    throw new RuntimeException("Order already has pending VNPay payment.");
                }
                boolean hasSuccessfulVNPayPayment = existingPayments.stream()
                        .anyMatch(p -> p.getPaymentMethod() == Payment.PaymentMethod.VNPAY && p.isSuccessful());

                if (hasSuccessfulVNPayPayment) {
                    throw new RuntimeException("Order already has successful VNPay payment.");
                }
            }
            if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Paid amount is invalid: " + paidAmount);
            }

            Payment payment = Payment.createVNPayPayment(order, paidAmount);
            payment = paymentRepository.save(payment);

            log.info("Payment created - ID: {}, TxnRef: {}, Paid Amount: {}, Status: {}",
                    payment.getId(), payment.getVnpayTxnRef(), paidAmount, payment.getStatus());

            String paymentUrl = createVNPayPaymentUrl(payment, order, request, paidAmount);

            VNPayPaymentResponseDTO response = new VNPayPaymentResponseDTO();
            response.setCode("00");
            response.setMessage("Success");
            response.setPaymentUrl(paymentUrl);
            response.setTransactionId(payment.getVnpayTxnRef());

            log.info("VNPay payment created successfully for order: {}", order.getId());
            return response;

        } catch (Exception e) {
            log.error("Error creating VNPay payment: {}", e.getMessage(), e);

            VNPayPaymentResponseDTO response = new VNPayPaymentResponseDTO();
            response.setCode("99");
            response.setMessage("Error: " + e.getMessage());
            return response;
        }
    }


    private boolean isOrderApproved(Order order) {
        boolean isApprovalStatusValid = order.getApprovalStatus() == Order.OrderApprovalStatus.APPROVED;
        boolean isOrderStatusValid = order.getStatus() == Order.OrderStatus.APPROVED ||
                order.getStatus() == Order.OrderStatus.COMPLETED;

        log.info("Order approval check - ID: {}, ApprovalStatus: {}, OrderStatus: {}, Result: {}",
                order.getId(), order.getApprovalStatus(), order.getStatus(),
                isApprovalStatusValid && isOrderStatusValid);

        return isApprovalStatusValid && isOrderStatusValid;
    }

    private String createVNPayPaymentUrl(Payment payment, Order order, HttpServletRequest request, BigDecimal paidAmount) {
        try {
            log.info("Creating VNPay URL for TxnRef: {}, Paid Amount: {}", payment.getVnpayTxnRef(), paidAmount);
            Map<String, String> vnpParams = new TreeMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", vnpayTmnCode);


            long amount = paidAmount.multiply(new BigDecimal(100)).longValue();
            vnpParams.put("vnp_Amount", String.valueOf(amount));

            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", payment.getVnpayTxnRef());
            vnpParams.put("vnp_OrderInfo", "Payment for order " + order.getId() + " - Amount: " + paidAmount);
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", vnpayReturnUrl);
            vnpParams.put("vnp_IpAddr", getRealClientIpAddress(request));

            ZonedDateTime nowVietnam = ZonedDateTime.now(VIETNAM_ZONE);
            String createDate = nowVietnam.format(VNPAY_DATE_FORMATTER);
            vnpParams.put("vnp_CreateDate", createDate);
            String expireDate = nowVietnam.plusMinutes(15).format(VNPAY_DATE_FORMATTER);
            vnpParams.put("vnp_ExpireDate", expireDate);

            log.info("VNPay Parameters - Paid Amount: {}, Create Date: {}, Expire Date: {}", paidAmount, createDate, expireDate);

            StringBuilder hashData = new StringBuilder();
            vnpParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                }
            });

            String hashDataStr = hashData.toString();
            String vnpSecureHash = hmacSHA512(vnpaySecretKey, hashDataStr);
            String finalUrl = vnpayUrl + "?" + hashDataStr + "&vnp_SecureHash=" + vnpSecureHash;

            log.info("Final VNPay URL created successfully with paid amount: {}", paidAmount);
            return finalUrl;

        } catch (Exception e) {
            log.error("Error creating VNPay URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create VNPay URL: " + e.getMessage(), e);
        }
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac.init(secretKeySpec);
        byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String getRealClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    public boolean validateResponse(Map<String, String> params) {
        try {
            log.info("Validating VNPay response");

            String vnpSecureHash = params.get("vnp_SecureHash");
            if (vnpSecureHash == null) {
                log.error("Missing vnp_SecureHash in response");
                return false;
            }

            Map<String, String> hashParams = new TreeMap<>(params);
            hashParams.remove("vnp_SecureHash");
            hashParams.remove("vnp_SecureHashType");

            StringBuilder hashData = new StringBuilder();
            Iterator<Map.Entry<String, String>> iterator = hashParams.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(fieldName)
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                }
            }

            String hashDataStr = hashData.toString();
            log.info("Validation Hash Data: {}", hashDataStr);

            String calculatedHash = hmacSHA512(vnpaySecretKey, hashDataStr);
            boolean isValid = vnpSecureHash.equalsIgnoreCase(calculatedHash);

            log.info("Signature validation: {}", isValid ? "VALID" : "INVALID");
            log.info("Received Hash: {}, Calculated Hash: {}", vnpSecureHash, calculatedHash);

            if (!isValid) {
                log.error("Hash mismatch - Check encoding and parameter order");
                log.error("Params received: {}", params);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating VNPay signature: {}", e.getMessage(), e);
            return false;
        }
    }

    public Map<String, String> handleVNPayReturn(Map<String, String> params) {
        try {
            log.info("Handling VNPay return with params: {}", params);

            boolean isValid = validateResponse(params);
            String vnpResponseCode = params.get("vnp_ResponseCode");
            String vnpTxnRef = params.get("vnp_TxnRef");

            Map<String, String> result = new HashMap<>();

            if (isValid && "00".equals(vnpResponseCode)) {
                Payment payment = processVNPayReturn(params);
                log.info("Payment successful, TxnRef: {}", vnpTxnRef);

                result.put("status", "success");
                result.put("redirectUrl", vnpayReturnUrlSuccess + "&transactionId=" + vnpTxnRef);
                result.put("transactionId", vnpTxnRef);
                result.put("message", "Payment successful");

            } else {
                // Payment failed
                log.warn("Payment failed or invalid signature. Response code: {}", vnpResponseCode);

                result.put("status", "fail");
                result.put("redirectUrl", vnpayReturnUrlFail + "&transactionId=" + vnpTxnRef + "&errorCode=" + vnpResponseCode);
                result.put("transactionId", vnpTxnRef);
                result.put("errorCode", vnpResponseCode);
                result.put("message", "Payment failed");
            }

            return result;

        } catch (Exception e) {
            log.error("Error handling VNPay return: {}", e.getMessage(), e);

            Map<String, String> result = new HashMap<>();
            result.put("status", "error");
            result.put("redirectUrl", vnpayReturnUrlFail + "&error=processing_error");
            result.put("message", "Payment processing error: " + e.getMessage());
            return result;
        }
    }


    public Payment processVNPayReturn(Map<String, String> params) {
        try {
            log.info("PROCESSING VNPay RETURN");

            if (!validateResponse(params)) {
                throw new RuntimeException("Invalid signature");
            }

            String vnpTxnRef = params.get("vnp_TxnRef");
            String vnpResponseCode = params.get("vnp_ResponseCode");
            String vnpTransactionNo = params.get("vnp_TransactionNo");

            log.info("Processing payment: TxnRef={}, ResponseCode={}", vnpTxnRef, vnpResponseCode);

            Payment payment = paymentRepository.findByVnpayTxnRef(vnpTxnRef)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + vnpTxnRef));

            log.info("Current payment status: {}", payment.getStatus());

            Integer paymentId = payment.getId();
            Integer orderId = payment.getOrderId();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            payment.setVnpayTransactionNo(vnpTransactionNo);
            payment.setVnpayBankCode(params.get("vnp_BankCode"));
            payment.setVnpayCardType(params.get("vnp_CardType"));
            payment.setVnpayResponseCode(vnpResponseCode);

            String vnpPayDate = params.get("vnp_PayDate");
            if (vnpPayDate != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                payment.setVnpayPayDate(LocalDateTime.parse(vnpPayDate, formatter));
            }

            if ("00".equals(vnpResponseCode)) {
                payment.markAsCompleted(vnpTransactionNo);
                payment.setNotes("Payment successful via VNPay");
                log.info("Payment completed: {} - Status changed from PENDING to COMPLETED", paymentId);

                // Xử lý order status
                if (order.getRemainingAmount() != null && order.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
                    order.setStatus(Order.OrderStatus.COMPLETED);
                    order.setPaymentStatus(Order.PaymentStatus.PAID);
                    orderRepository.save(order);
                    log.info("Order {} - remainingAmount = 0 -> status set to COMPLETED", orderId);
                } else {
                    log.info("Order {} - remainingAmount = {} -> keep status APPROVED",
                            orderId, order.getRemainingAmount());
                }

            } else {
                payment.markAsFailed();
                payment.setNotes("Payment failed. Error code: " + vnpResponseCode);
                log.warn("Payment failed: {}", vnpResponseCode);
            }

            payment = paymentRepository.save(payment);
            log.info("FINAL PAYMENT STATUS: {}", payment.getStatus());
            log.info("PROCESSING COMPLETED");
            return payment;

        } catch (Exception e) {
            log.error("ERROR PROCESSING PAYMENT: {}", e.getMessage(), e);
            throw new RuntimeException("Payment processing error: " + e.getMessage(), e);
        }
    }

    public String testCreatePaymentUrl() {
        try {
            Map<String, String> testParams = new TreeMap<>();
            testParams.put("vnp_Version", "2.1.0");
            testParams.put("vnp_Command", "pay");
            testParams.put("vnp_TmnCode", vnpayTmnCode);
            testParams.put("vnp_Amount", "1000000");
            testParams.put("vnp_CurrCode", "VND");
            testParams.put("vnp_TxnRef", "TEST" + System.currentTimeMillis());
            testParams.put("vnp_OrderInfo", "Test payment");
            testParams.put("vnp_OrderType", "other");
            testParams.put("vnp_Locale", "vn");
            testParams.put("vnp_ReturnUrl", vnpayReturnUrl); // VẪN DÙNG RETURN URL CŨ
            testParams.put("vnp_IpAddr", "127.0.0.1");

            // Sử dụng múi giờ Việt Nam
            ZonedDateTime nowVietnam = ZonedDateTime.now(VIETNAM_ZONE);
            testParams.put("vnp_CreateDate", nowVietnam.format(VNPAY_DATE_FORMATTER));
            testParams.put("vnp_ExpireDate", nowVietnam.plusMinutes(15).format(VNPAY_DATE_FORMATTER));

            StringBuilder hashData = new StringBuilder();
            testParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                }
            });

            String hashDataStr = hashData.toString();
            log.info("Test Hash Data: {}", hashDataStr);

            String signature = hmacSHA512(vnpaySecretKey, hashDataStr);

            log.info("Test Signature: {}", signature);

            String finalUrl = vnpayUrl + "?" + hashDataStr + "&vnp_SecureHash=" + signature;

            return "Test URL: " + finalUrl;

        } catch (Exception e) {
            return "Test Error: " + e.getMessage();
        }
    }

    public Map<String, String> getCurrentTimeInfo() {
        Map<String, String> timeInfo = new HashMap<>();
        timeInfo.put("Vietnam Time", ZonedDateTime.now(VIETNAM_ZONE).format(VNPAY_DATE_FORMATTER));
        timeInfo.put("UTC Time", LocalDateTime.now(ZoneId.of("UTC")).format(VNPAY_DATE_FORMATTER));
        timeInfo.put("System Time", LocalDateTime.now().format(VNPAY_DATE_FORMATTER));
        timeInfo.put("System Timezone", ZoneId.systemDefault().toString());
        return timeInfo;
    }
}