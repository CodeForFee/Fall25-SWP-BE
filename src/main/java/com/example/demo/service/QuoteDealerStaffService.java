package com.example.demo.service;

import com.example.demo.entity.Quote;
import com.example.demo.entity.QuoteDetail;
import com.example.demo.entity.Customer;
import com.example.demo.repository.QuoteRepository;
import com.example.demo.repository.QuoteDetailRepository;
import com.example.demo.repository.CustomerRepository;
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
    private final QuoteCalculationService quoteCalculationService;
    private final AuditLogService auditLogService;

    public void submitToDealerManager(Integer quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        if (!quote.canBeSubmittedToDealerManager()) {
            throw new RuntimeException("Quote cannot be submitted to dealer manager. Current status: " +
                    quote.getApprovalStatus() + ", " + quote.getStatus());
        }

        quote.setApprovalStatus(Quote.QuoteApprovalStatus.PENDING_DEALER_MANAGER_APPROVAL);
        quoteRepository.save(quote);

        auditLogService.log("QUOTE_SUBMITTED_TO_DEALER_MANAGER", "QUOTE", quoteId.toString(),
                Map.of("action", "SUBMITTED_TO_DEALER_MANAGER"));

        log.info("Quote {} submitted to dealer manager by user {}", quoteId, quote.getUserId());
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