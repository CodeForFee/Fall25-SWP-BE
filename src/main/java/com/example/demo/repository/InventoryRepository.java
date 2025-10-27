package com.example.demo.repository;

import com.example.demo.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByDealer_DealerId(Integer dealerId);

    @Query("SELECT i.dealer.dealerId, SUM(i.availableQuantity) FROM Inventory i GROUP BY i.dealer.dealerId")
    List<Object[]> totalAvailableByDealer();
}