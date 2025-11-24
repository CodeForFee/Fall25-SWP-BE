package com.example.demo.repository;

import com.example.demo.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    // SỬA LẠI: Sử dụng @Query thay vì method name tự động
    @Query("SELECT i FROM Inventory i WHERE i.dealer.dealerId = :dealerId")
    List<Inventory> findByDealerId(@Param("dealerId") Integer dealerId);

    @Query("SELECT i.dealer.dealerId, SUM(i.availableQuantity) FROM Inventory i GROUP BY i.dealer.dealerId")
    List<Object[]> totalAvailableByDealer();

    // 👈 SỬA LẠI TẤT CẢ CÁC METHOD CÓ LIÊN QUAN ĐẾN dealerId
    @Query("SELECT i FROM Inventory i WHERE i.vehicle.id = :vehicleId AND i.inventoryType = :inventoryType")
    Optional<Inventory> findByVehicleIdAndInventoryType(
            @Param("vehicleId") Integer vehicleId,
            @Param("inventoryType") Inventory.InventoryType inventoryType);

    @Query("SELECT i FROM Inventory i WHERE i.inventoryType = :inventoryType")
    List<Inventory> findByInventoryType(@Param("inventoryType") Inventory.InventoryType inventoryType);

    @Query("SELECT i FROM Inventory i WHERE i.dealer.dealerId = :dealerId AND i.vehicle.id = :vehicleId AND i.inventoryType = :inventoryType")
    Optional<Inventory> findByDealerIdAndVehicleIdAndInventoryType(
            @Param("dealerId") Integer dealerId,
            @Param("vehicleId") Integer vehicleId,
            @Param("inventoryType") Inventory.InventoryType inventoryType);

    @Query("SELECT i FROM Inventory i WHERE i.dealer.dealerId = :dealerId AND i.inventoryType = :inventoryType")
    List<Inventory> findByDealerIdAndInventoryType(
            @Param("dealerId") Integer dealerId,
            @Param("inventoryType") Inventory.InventoryType inventoryType);

    // Kiểm tra tồn tại - SỬA LẠI
    @Query("SELECT COUNT(i) > 0 FROM Inventory i WHERE i.vehicle.id = :vehicleId AND i.inventoryType = :inventoryType")
    boolean existsByVehicleIdAndInventoryType(
            @Param("vehicleId") Integer vehicleId,
            @Param("inventoryType") Inventory.InventoryType inventoryType);

    @Query("SELECT COUNT(i) > 0 FROM Inventory i WHERE i.dealer.dealerId = :dealerId AND i.vehicle.id = :vehicleId AND i.inventoryType = :inventoryType")
    boolean existsByDealerIdAndVehicleIdAndInventoryType(
            @Param("dealerId") Integer dealerId,
            @Param("vehicleId") Integer vehicleId,
            @Param("inventoryType") Inventory.InventoryType inventoryType);

    // Lấy inventory theo vehicle và type
    @Query("SELECT i FROM Inventory i WHERE i.vehicle.id = :vehicleId AND i.inventoryType = :inventoryType ORDER BY i.availableQuantity DESC")
    List<Inventory> findByVehicleIdAndInventoryTypeOrderByAvailableQuantityDesc(
            @Param("vehicleId") Integer vehicleId,
            @Param("inventoryType") Inventory.InventoryType inventoryType);

    // Kiểm tra số lượng tồn kho
    @Query("SELECT i.availableQuantity FROM Inventory i WHERE i.vehicle.id = :vehicleId AND i.inventoryType = 'FACTORY'")
    Optional<Integer> getFactoryInventoryQuantity(@Param("vehicleId") Integer vehicleId);

    // 👈 THÊM METHOD MỚI: Lấy inventory theo dealer (cho factory inventory - dealer = null)
    @Query("SELECT i FROM Inventory i WHERE i.dealer IS NULL AND i.inventoryType = 'FACTORY'")
    List<Inventory> findFactoryInventory();

    // Lấy factory inventory cho vehicle cụ thể
    @Query("SELECT i FROM Inventory i WHERE i.vehicle.id = :vehicleId AND i.dealer IS NULL AND i.inventoryType = 'FACTORY'")
    Optional<Inventory> findFactoryInventoryByVehicleId(@Param("vehicleId") Integer vehicleId);

    List<Inventory> findByAvailableQuantityGreaterThanOrderByVehicleIdAsc(int availableQuantity);
}