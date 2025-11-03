package com.example.demo.service;

import com.example.demo.entity.Quote;
import com.example.demo.entity.QuoteDetail;
import com.example.demo.repository.QuoteRepository;
import com.example.demo.repository.QuoteDetailRepository;
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
public class QuoteApprovalService {

    private final QuoteRepository quoteRepository;
    private final QuoteDetailRepository quoteDetailRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    // Loại bỏ @Transactional để tránh nested transaction
    public void submitForEVMApproval(Integer quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        if (!quote.canBeSubmittedForApproval()) {
            throw new RuntimeException("Quote cannot be submitted for approval. Current status: " +
                    quote.getApprovalStatus() + ", " + quote.getStatus());
        }

        quote.setApprovalStatus(Quote.QuoteApprovalStatus.PENDING_EVM_APPROVAL);
        quoteRepository.save(quote);

        auditLogService.log("QUOTE_SUBMITTED_FOR_APPROVAL", "QUOTE", quoteId.toString(),
                Map.of("action", "SUBMITTED_FOR_EVM_APPROVAL"));

        log.info("Quote {} submitted for EVM approval by user {}", quoteId, quote.getUserId());
    }

    // Loại bỏ @Transactional để tránh nested transaction
    public void approveQuoteByEVM(Integer quoteId, Integer evmUserId, String notes) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        if (!quote.canBeApprovedByEVM()) {
            throw new RuntimeException("Quote cannot be approved by EVM. Current approval status: " + quote.getApprovalStatus());
        }
        boolean hasSufficientInventory = checkFactoryInventoryForQuote(quoteId);

        if (!hasSufficientInventory) {
            quote.setApprovalStatus(Quote.QuoteApprovalStatus.INSUFFICIENT_INVENTORY);
            quote.setApprovalNotes("Kho hãng không đủ mẫu xe đang được đặt");
            quoteRepository.save(quote);

            throw new RuntimeException("Không thể duyệt quote: Kho hãng không đủ mẫu xe đang được đặt");
        }

        quote.setApprovalStatus(Quote.QuoteApprovalStatus.APPROVED);
        quote.setStatus(Quote.QuoteStatus.ACCEPTED);
        quote.setApprovedBy(evmUserId);
        quote.setApprovedAt(LocalDateTime.now());
        quote.setApprovalNotes(notes);
        quoteRepository.save(quote);

        auditLogService.log("QUOTE_APPROVED_BY_EVM", "QUOTE", quoteId.toString(),
                Map.of("approvedBy", evmUserId, "notes", notes, "inventoryChecked", true));

        log.info("Quote {} approved by EVM user {} with inventory check", quoteId, evmUserId);
    }

    // Loại bỏ @Transactional để tránh nested transaction
    public void rejectQuoteByEVM(Integer quoteId, Integer evmUserId, String reason) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        quote.setApprovalStatus(Quote.QuoteApprovalStatus.REJECTED);
        quote.setStatus(Quote.QuoteStatus.REJECTED);
        quote.setApprovedBy(evmUserId);
        quote.setApprovedAt(LocalDateTime.now());
        quote.setApprovalNotes(reason);
        quoteRepository.save(quote);

        auditLogService.log("QUOTE_REJECTED_BY_EVM", "QUOTE", quoteId.toString(),
                Map.of("rejectedBy", evmUserId, "reason", reason));

        log.info("Quote {} rejected by EVM user {}", quoteId, evmUserId);
    }

    public boolean checkFactoryInventoryForQuote(Integer quoteId) {
        List<QuoteDetail> quoteDetails = quoteDetailRepository.findByQuoteId(quoteId);

        for (QuoteDetail detail : quoteDetails) {
            if (!inventoryService.checkFactoryInventory(detail.getVehicleId(), detail.getQuantity())) {
                log.warn("Factory insufficient inventory for quote {} - Vehicle: {}, Required: {}",
                        quoteId, detail.getVehicleId(), detail.getQuantity());
                return false;
            }
        }
        return true;
    }

    public List<Quote> getQuotesPendingEVMApproval() {
        return quoteRepository.findQuotesPendingEVMApproval();
    }

    public List<Quote> getApprovedQuotesReadyForOrder() {
        return quoteRepository.findApprovedQuotesReadyForOrder();
    }
}