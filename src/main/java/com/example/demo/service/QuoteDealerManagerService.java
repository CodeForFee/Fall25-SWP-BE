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
     * 🔥 MANAGER DUYỆT QUOTE CỦA STAFF CÙNG DEALER
     */
    @Transactional
    public void approveQuoteByManager(Integer quoteId, Integer managerId, String notes) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerId));

        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        // 🔥 CHỈ KIỂM TRA: Manager cùng dealer duyệt quote của staff
        if (!quote.canBeApprovedByDealerManager(manager)) {
            throw new RuntimeException("Manager can only approve quotes from staff in the same dealer");
        }

        // 🔥 KIỂM TRA KHO DEALER
        boolean hasSufficientInventory = checkDealerInventoryForQuote(quoteId, quote.getDealerId());

        if (!hasSufficientInventory) {
            quote.setApprovalStatus(Quote.QuoteApprovalStatus.INSUFFICIENT_INVENTORY);
            quote.setApprovalNotes("Kho đại lý không đủ mẫu xe đang được đặt");
            quoteRepository.save(quote);
            throw new RuntimeException("Không thể duyệt quote: Kho đại lý không đủ mẫu xe đang được đặt");
        }

        // 🔥 TÍNH TOÁN VÀ DUYỆT QUOTE
        var calculationResult = quoteCalculationService.calculateQuoteTotal(quoteId);

        if (calculationResult.qualifiesForVip() && !quote.getCustomer().getIsVip()) {
            Customer customer = quote.getCustomer();
            customer.setIsVip(true);
            customerRepository.save(customer);
        }

        quote.setApprovalStatus(Quote.QuoteApprovalStatus.APPROVED);
        quote.setStatus(Quote.QuoteStatus.ACCEPTED);
        quote.setCurrentApproverRole(null); // 🔥 HOÀN THÀNH PHÊ DUYỆT
        quote.setApprovedBy(managerId);
        quote.setApprovedAt(LocalDateTime.now());
        quote.setApprovalNotes(notes);

        quote.setSubtotal(calculationResult.subtotal());
        quote.setVatAmount(calculationResult.vatAmount());
        quote.setDiscountAmount(calculationResult.discountAmount());
        quote.setFinalTotal(calculationResult.finalTotal());

        quoteRepository.save(quote);

        auditLogService.log("QUOTE_APPROVED_BY_DEALER_MANAGER", "QUOTE", quoteId.toString(),
                Map.of("managerId", managerId, "dealerId", quote.getDealerId(), "notes", notes));

        log.info("Manager {} approved quote {} from staff {}", managerId, quoteId, quote.getUserId());
    }

    /**
     * 🔥 MANAGER TỪ CHỐI QUOTE CỦA STAFF CÙNG DEALER
     */
    @Transactional
    public void rejectQuoteByManager(Integer quoteId, Integer managerId, String reason) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerId));

        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        // 🔥 CHỈ KIỂM TRA: Manager cùng dealer từ chối quote của staff
        if (!quote.canBeApprovedByDealerManager(manager)) {
            throw new RuntimeException("Manager can only reject quotes from staff in the same dealer");
        }

        quote.setApprovalStatus(Quote.QuoteApprovalStatus.REJECTED);
        quote.setStatus(Quote.QuoteStatus.REJECTED);
        quote.setCurrentApproverRole(null);
        quote.setApprovedBy(managerId);
        quote.setApprovedAt(LocalDateTime.now());
        quote.setApprovalNotes(reason);
        quoteRepository.save(quote);

        auditLogService.log("QUOTE_REJECTED_BY_DEALER_MANAGER", "QUOTE", quoteId.toString(),
                Map.of("managerId", managerId, "reason", reason));

        log.info("Manager {} rejected quote {} from staff {}", managerId, quoteId, quote.getUserId());
    }

    /**
     * 🔥 KIỂM TRA KHO DEALER CÓ ĐỦ HÀNG CHO QUOTE KHÔNG
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
     * 🔥 LẤY DANH SÁCH QUOTES CHỜ DEALER MANAGER DUYỆT (BAO GỒM CẢ INSUFFICIENT_INVENTORY)
     */
    public List<Quote> getPendingQuotesForManager(Integer managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerId));

        return quoteRepository.findPendingAndInsufficientInventoryQuotesForDealerManager(manager.getDealerId());
    }

    /**
     * 🔥 LẤY QUOTES ĐÃ APPROVED SẴN SÀNG TẠO ORDER
     */
    public List<Quote> getApprovedQuotesReadyForOrder(Integer managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerId));

        return quoteRepository.findApprovedQuotesReadyForOrderByDealer(manager.getDealerId());
    }

    /**
     * 🔥 LẤY QUOTE THEO ID
     */
    public Quote getQuoteById(Integer quoteId) {
        return quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));
    }
}