package com.example.demo.service;

import com.example.demo.entity.Quote;
import com.example.demo.entity.QuoteDetail;
import com.example.demo.entity.Customer;
import com.example.demo.entity.User;
import com.example.demo.repository.QuoteRepository;
import com.example.demo.repository.QuoteDetailRepository;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteDealerManagerService {

    private final QuoteRepository quoteRepository;
    private final QuoteDetailRepository quoteDetailRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final QuoteCalculationService quoteCalculationService;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    /**
     * Dealer Manager duyệt quote - TỰ ĐỘNG KIỂM TRA KHO LẠI KHI Ở TRẠNG THÁI INSUFFICIENT_INVENTORY
     */
    @Transactional
    public void approveQuoteByManager(Integer quoteId, Integer managerId, String notes) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerId));

        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        Customer customer = quote.getCustomer();

        // 🔥 SỬA LỖI: Kiểm tra điều kiện duyệt quote
        if (!quote.canBeApprovedByDealerManager() &&
                quote.getApprovalStatus() != Quote.QuoteApprovalStatus.INSUFFICIENT_INVENTORY) {
            throw new RuntimeException("Quote cannot be approved by dealer manager. Current approval status: "
                    + quote.getApprovalStatus() + ", Status: " + quote.getStatus());
        }

        // 🔥 TỰ ĐỘNG KIỂM TRA KHO LẠI
        boolean hasSufficientInventory = checkDealerInventoryForQuote(quoteId, customer.getDealerId());

        if (!hasSufficientInventory) {
            quote.setApprovalStatus(Quote.QuoteApprovalStatus.INSUFFICIENT_INVENTORY);
            quote.setApprovalNotes("Kho đại lý không đủ mẫu xe đang được đặt");
            quoteRepository.save(quote);
            throw new RuntimeException("Không thể duyệt quote: Kho đại lý không đủ mẫu xe đang được đặt");
        }

        // Tính toán lại và duyệt quote
        var calculationResult = quoteCalculationService.calculateQuoteTotal(quoteId);

        if (calculationResult.qualifiesForVip() && !customer.getIsVip()) {
            customer.setIsVip(true);
            customerRepository.save(customer);
        }

        // 🔥 DUYỆT THÀNH CÔNG
        quote.setApprovalStatus(Quote.QuoteApprovalStatus.APPROVED);
        quote.setStatus(Quote.QuoteStatus.ACCEPTED); // 🔥 QUAN TRỌNG: Cập nhật cả status
        quote.setApprovedBy(managerId);
        quote.setApprovedAt(LocalDateTime.now());
        quote.setApprovalNotes(notes);

        quote.setSubtotal(calculationResult.subtotal());
        quote.setVatAmount(calculationResult.vatAmount());
        quote.setDiscountAmount(calculationResult.discountAmount());
        quote.setFinalTotal(calculationResult.finalTotal());

        quoteRepository.save(quote);

        log.info("Quote {} approved by dealer manager {}", quoteId, managerId);
    }

    /**
     * Dealer Manager từ chối quote - CHO PHÉP TỪ CẢ INSUFFICIENT_INVENTORY
     */
    @Transactional
    public void rejectQuoteByManager(Integer quoteId, Integer managerId, String reason) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerId));

        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        // Kiểm tra quote thuộc dealer của manager
        Customer customer = quote.getCustomer();
        if (!customer.getDealerId().equals(manager.getDealerId())) {
            throw new RuntimeException("Quote does not belong to manager's dealer");
        }

        // 🔥 CHO PHÉP từ chối từ cả PENDING và INSUFFICIENT_INVENTORY
        if (quote.getApprovalStatus() != Quote.QuoteApprovalStatus.PENDING_DEALER_MANAGER_APPROVAL &&
                quote.getApprovalStatus() != Quote.QuoteApprovalStatus.INSUFFICIENT_INVENTORY) {
            throw new RuntimeException("Quote cannot be rejected in current status: " + quote.getApprovalStatus());
        }

        quote.setApprovalStatus(Quote.QuoteApprovalStatus.REJECTED);
        quote.setStatus(Quote.QuoteStatus.REJECTED);
        quote.setApprovedBy(managerId);
        quote.setApprovedAt(LocalDateTime.now());
        quote.setApprovalNotes(reason);
        quoteRepository.save(quote);

        auditLogService.log("QUOTE_REJECTED_BY_DEALER_MANAGER", "QUOTE", quoteId.toString(),
                Map.of("managerId", managerId, "reason", reason, "previousStatus", quote.getApprovalStatus()));

        log.info("Quote {} rejected by dealer manager {} from {} status",
                quoteId, managerId, quote.getApprovalStatus());
    }

    /**
     * Kiểm tra kho dealer có đủ hàng cho quote không
     */
    public boolean checkDealerInventoryForQuote(Integer quoteId, Integer dealerId) {
        List<QuoteDetail> quoteDetails = quoteDetailRepository.findByQuoteId(quoteId);

        if (quoteDetails.isEmpty()) {
            log.warn("No quote details found for quote: {}", quoteId);
            return false;
        }

        for (QuoteDetail detail : quoteDetails) {
            if (!inventoryService.checkDealerInventory(dealerId, detail.getVehicleId(), detail.getQuantity())) {
                log.warn("Dealer insufficient inventory for quote {} - Vehicle: {}, Required: {}, Dealer: {}",
                        quoteId, detail.getVehicleId(), detail.getQuantity(), dealerId);
                return false;
            }
        }

        log.info("Dealer inventory sufficient for quote {} - Dealer: {}", quoteId, dealerId);
        return true;
    }

    /**
     * Lấy danh sách quotes chờ Dealer Manager duyệt (bao gồm cả INSUFFICIENT_INVENTORY)
     */
    public List<Quote> getPendingQuotesForManager(Integer managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerId));

        return quoteRepository.findPendingAndInsufficientInventoryQuotesForDealerManager(manager.getDealerId());
    }

    /**
     * Lấy quotes đã approved sẵn sàng tạo order
     */
    public List<Quote> getApprovedQuotesReadyForOrder(Integer managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerId));

        return quoteRepository.findApprovedQuotesReadyForOrderByDealer(manager.getDealerId());
    }

    /**
     * Lấy quote theo ID
     */
    public Quote getQuoteById(Integer quoteId) {
        return quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));
    }
}