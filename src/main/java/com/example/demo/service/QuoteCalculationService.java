package com.example.demo.service;

import com.example.demo.entity.Quote;
import com.example.demo.entity.QuoteDetail;
import com.example.demo.entity.Customer;
import com.example.demo.repository.QuoteDetailRepository;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteCalculationService {

    private final QuoteDetailRepository quoteDetailRepository;
    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;

    private static final BigDecimal VAT_RATE = new BigDecimal("0.10");
    private static final BigDecimal VIP_DISCOUNT_RATE = new BigDecimal("0.05");
    private static final BigDecimal VIP_THRESHOLD = new BigDecimal("5000000000");

    public QuoteCalculationResult calculateQuoteTotal(Integer quoteId) {
        List<QuoteDetail> quoteDetails = quoteDetailRepository.findByQuoteId(quoteId);
        if (quoteDetails == null || quoteDetails.isEmpty()) {
            log.error("No quote details found for quote: {}", quoteId);
            throw new RuntimeException("Không thể tính toán quote: Chưa có chi tiết xe nào trong báo giá. Vui lòng thêm xe vào báo giá trước.");
        }

        for (QuoteDetail detail : quoteDetails) {
            if (detail.getVehicleId() == null) {
                throw new RuntimeException("Vehicle ID is missing in quote details");
            }
            if (detail.getQuantity() == null || detail.getQuantity() <= 0) {
                throw new RuntimeException("Invalid quantity for vehicle: " + detail.getVehicleId());
            }
        }

        BigDecimal subtotal = quoteDetails.stream()
                .map(detail -> {
                    BigDecimal unitPrice = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
                    Integer quantity = detail.getQuantity() != null ? detail.getQuantity() : 0;
                    return unitPrice.multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        Customer customer = customerRepository.findById(quote.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + quote.getCustomerId()));

        boolean qualifiesForVip = subtotal.compareTo(VIP_THRESHOLD) >= 0;
        BigDecimal vatAmount = subtotal.multiply(VAT_RATE);
        BigDecimal totalAfterVat = subtotal.add(vatAmount);

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalTotal = totalAfterVat;

        if (customer.getIsVip()) {
            discountAmount = totalAfterVat.multiply(VIP_DISCOUNT_RATE);
            finalTotal = totalAfterVat.subtract(discountAmount);
        }

        log.info("Quote calculation completed - Quote: {}, Subtotal: {}, VAT: {}, Discount: {}, Final: {}, VIP Qualified: {}",
                quoteId, subtotal, vatAmount, discountAmount, finalTotal, qualifiesForVip);

        return new QuoteCalculationResult(
                subtotal,
                vatAmount,
                VAT_RATE,
                discountAmount,
                customer.getIsVip() ? VIP_DISCOUNT_RATE : BigDecimal.ZERO,
                finalTotal,
                qualifiesForVip,
                customer.getIsVip(),
                customer.getId()
        );
    }

    public record QuoteCalculationResult(
            BigDecimal subtotal,
            BigDecimal vatAmount,
            BigDecimal vatRate,
            BigDecimal discountAmount,
            BigDecimal discountRate,
            BigDecimal finalTotal,
            boolean qualifiesForVip,
            boolean isVipCustomer,
            Integer customerId
    ) {}
}