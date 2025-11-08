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

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteDealerStaffService {

    private final QuoteRepository quoteRepository;
    private final QuoteDetailRepository quoteDetailRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final QuoteCalculationService quoteCalculationService;
    private final AuditLogService auditLogService;

    /**
     * 🔥 STAFF GỬI QUOTE CỦA CHÍNH MÌNH CHO MANAGER
     */
    public void submitToDealerManager(Integer quoteId, Integer staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found: " + staffId));

        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        // 🔥 CHỈ KIỂM TRA: Staff gửi quote của chính mình
        if (!quote.canBeSubmittedToDealerManager(staff)) {
            throw new RuntimeException("Staff can only submit their own quotes to manager");
        }

        quote.setApprovalStatus(Quote.QuoteApprovalStatus.PENDING_DEALER_MANAGER_APPROVAL);
        quote.setCurrentApproverRole("DEALER_MANAGER");
        quoteRepository.save(quote);

        auditLogService.log("QUOTE_SUBMITTED_TO_DEALER_MANAGER", "QUOTE", quoteId.toString(),
                Map.of("staffId", staffId, "dealerId", quote.getDealerId()));

        log.info("Staff {} submitted quote {} to dealer manager", staffId, quoteId);
    }

    public List<Quote> getQuotesByStaff(Integer staffId) {
        return quoteRepository.findByUserId(staffId);
    }

    public List<Quote> getDraftQuotesByStaff(Integer staffId) {
        return quoteRepository.findByUserIdAndApprovalStatus(staffId, Quote.QuoteApprovalStatus.DRAFT);
    }

    public Quote getQuoteById(Integer quoteId) {
        return quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));
    }

    public List<QuoteDetail> getQuoteDetails(Integer quoteId) {
        return quoteDetailRepository.findByQuoteId(quoteId);
    }
}