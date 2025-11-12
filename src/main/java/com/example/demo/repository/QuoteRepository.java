package com.example.demo.repository;

import com.example.demo.entity.Quote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Integer> {

    List<Quote> findByCustomerId(Integer customerId);

    List<Quote> findByUserId(Integer userId);

    List<Quote> findByStatus(Quote.QuoteStatus status);

    List<Quote> findByApprovalStatus(Quote.QuoteApprovalStatus approvalStatus);

    List<Quote> findByUserIdAndApprovalStatus(Integer userId, Quote.QuoteApprovalStatus approvalStatus);

    @Query("SELECT q FROM Quote q WHERE q.validUntil < :currentDate AND q.status = 'SENT'")
    List<Quote> findExpiredQuotes(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT q FROM Quote q WHERE q.customerId = :customerId AND q.status = 'ACCEPTED'")
    List<Quote> findAcceptedQuotesByCustomer(@Param("customerId") Integer customerId);

    // Quotes chờ EVM duyệt
    @Query("SELECT q FROM Quote q WHERE q.approvalStatus = 'PENDING_EVM_APPROVAL'")
    List<Quote> findQuotesPendingEVMApproval();

    // Quotes đã được EVM duyệt
    @Query("SELECT q FROM Quote q WHERE q.approvalStatus = 'APPROVED' AND q.status = 'ACCEPTED'")
    List<Quote> findApprovedQuotesReadyForOrder();

    @Query("SELECT q FROM Quote q WHERE q.approvalStatus = 'PENDING_DEALER_MANAGER_APPROVAL' " +
            "AND q.customer.dealerId = :dealerId")
    List<Quote> findPendingQuotesForDealerManager(@Param("dealerId") Integer dealerId);

    @Query("SELECT q FROM Quote q WHERE q.approvalStatus = com.example.demo.entity.Quote.QuoteApprovalStatus.APPROVED " +
            "AND q.status = com.example.demo.entity.Quote.QuoteStatus.ACCEPTED " +
            "AND q.customer.dealerId = :dealerId")
    List<Quote> findApprovedQuotesReadyForOrderByDealer(@Param("dealerId") Integer dealerId);

    /**
     * Lấy quotes chờ duyệt và quotes bị INSUFFICIENT_INVENTORY
     */
    @Query("SELECT q FROM Quote q WHERE q.customer.dealerId = :dealerId " +
            "AND q.approvalStatus IN (com.example.demo.entity.Quote.QuoteApprovalStatus.PENDING_DEALER_MANAGER_APPROVAL, " +
            "com.example.demo.entity.Quote.QuoteApprovalStatus.INSUFFICIENT_INVENTORY)")
    List<Quote> findPendingAndInsufficientInventoryQuotesForDealerManager(@Param("dealerId") Integer dealerId);

    /**
     * Lấy quote với pessimistic lock để tránh race condition
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Quote q WHERE q.id = :quoteId")
    Optional<Quote> findByIdWithLock(@Param("quoteId") Integer quoteId);

    List<Quote> findByCustomerIdIsNull();

}