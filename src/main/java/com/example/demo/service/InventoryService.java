package com.example.demo.service;

import com.example.demo.entity.Inventory;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.DealerRepository;
import com.example.demo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final DealerRepository dealerRepository;
    private final VehicleRepository vehicleRepository;


    @Transactional
    public Inventory createFactoryInventory(Integer vehicleId, Integer initialQuantity) {
        var vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));

        if (inventoryRepository.findFactoryInventoryByVehicleId(vehicleId).isPresent()) {
            throw new RuntimeException("Factory inventory already exists for vehicle: " + vehicleId);
        }

        Inventory factoryInventory = Inventory.builder()
                .dealer(null)
                .vehicle(vehicle)
                .availableQuantity(initialQuantity)
                .reservedQuantity(0)
                .inventoryType(Inventory.InventoryType.FACTORY)
                .lastUpdated(LocalDateTime.now())
                .build();

        return inventoryRepository.save(factoryInventory);
    }

    @Transactional
    public Inventory createDealerInventory(Integer dealerId, Integer vehicleId, Integer quantity) {
        var dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer not found: " + dealerId));
        var vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));
        var existingInventory = inventoryRepository.findByDealerIdAndVehicleIdAndInventoryType(
                dealerId, vehicleId, Inventory.InventoryType.DEALER);

        if (existingInventory.isPresent()) {
            throw new RuntimeException("Vehicle already exists in dealer inventory: " + vehicleId);
        }

        Inventory dealerInventory = Inventory.builder()
                .dealer(dealer)
                .vehicle(vehicle)
                .availableQuantity(1)
                .reservedQuantity(0)
                .inventoryType(Inventory.InventoryType.DEALER)
                .lastUpdated(LocalDateTime.now())
                .build();

        log.info("Added unique vehicle to dealer inventory - Dealer: {}, Vehicle: {}",
                dealerId, vehicleId);

        return inventoryRepository.save(dealerInventory);
    }


    public boolean checkFactoryInventory(Integer vehicleId, Integer requiredQuantity) {
        return inventoryRepository.findFactoryInventoryByVehicleId(vehicleId)
                .map(inventory -> inventory.hasSufficientQuantity(requiredQuantity))
                .orElse(false);
    }


    public boolean checkDealerInventory(Integer dealerId, Integer vehicleId, Integer requiredQuantity) {
        return inventoryRepository.findByDealerIdAndVehicleIdAndInventoryType(
                        dealerId, vehicleId, Inventory.InventoryType.DEALER)
                .map(inventory -> inventory.hasSufficientQuantity(requiredQuantity))
                .orElse(false);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deductFactoryInventory(Integer vehicleId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Invalid quantity: " + quantity);
        }

        Inventory factoryInventory = inventoryRepository.findFactoryInventoryByVehicleId(vehicleId)
                .orElseThrow(() -> new RuntimeException("Factory inventory not found for vehicle: " + vehicleId));

        if (!factoryInventory.hasSufficientQuantity(quantity)) {
            throw new RuntimeException("Factory insufficient inventory for vehicle: " + vehicleId +
                    ". Available: " + factoryInventory.getAvailableQuantity() + ", Requested: " + quantity);
        }

        factoryInventory.setAvailableQuantity(factoryInventory.getAvailableQuantity() - quantity);
        factoryInventory.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(factoryInventory);

        log.info("Deducted factory inventory - Vehicle: {}, Quantity: {}, Remaining: {}",
                vehicleId, quantity, factoryInventory.getAvailableQuantity());
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void transferFactoryToDealer(Integer dealerId, Integer vehicleId, Integer quantity) {
        log.info("Starting inventory transfer - Dealer: {}, Vehicle: {}, Quantity: {}",
                dealerId, vehicleId, quantity);

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Invalid transfer quantity: " + quantity);
        }

        Inventory factoryInventory = inventoryRepository.findFactoryInventoryByVehicleId(vehicleId)
                .orElseThrow(() -> new RuntimeException("Factory inventory not found for vehicle: " + vehicleId));

        if (!factoryInventory.hasSufficientQuantity(quantity)) {
            throw new RuntimeException("Factory insufficient inventory for vehicle: " + vehicleId +
                    ". Available: " + factoryInventory.getAvailableQuantity() + ", Requested: " + quantity);
        }

        factoryInventory.setAvailableQuantity(factoryInventory.getAvailableQuantity() - quantity);
        factoryInventory.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(factoryInventory);

        log.info("Deducted factory inventory - Vehicle: {}, Quantity: {}, Remaining: {}",
                vehicleId, quantity, factoryInventory.getAvailableQuantity());

        Inventory dealerInventory = getOrCreateDealerInventoryInternal(dealerId, vehicleId);

        dealerInventory.setAvailableQuantity(dealerInventory.getAvailableQuantity() + quantity);
        dealerInventory.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(dealerInventory);

        log.info("Successfully transferred inventory - Dealer: {}, Vehicle: {}, Quantity: {}, New Dealer Stock: {}",
                dealerId, vehicleId, quantity, dealerInventory.getAvailableQuantity());
    }

    private Inventory getOrCreateDealerInventoryInternal(Integer dealerId, Integer vehicleId) {

        var existingInventory = inventoryRepository.findByDealerIdAndVehicleIdAndInventoryType(
                dealerId, vehicleId, Inventory.InventoryType.DEALER);

        if (existingInventory.isPresent()) {
            return existingInventory.get();
        }

        var dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer not found: " + dealerId));
        var vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));

        Inventory newDealerInventory = Inventory.builder()
                .dealer(dealer)
                .vehicle(vehicle)
                .availableQuantity(0)
                .reservedQuantity(0)
                .inventoryType(Inventory.InventoryType.DEALER)
                .lastUpdated(LocalDateTime.now())
                .build();

        return inventoryRepository.save(newDealerInventory);
    }

    public List<Inventory> getFactoryInventory() {
        return inventoryRepository.findFactoryInventory();
    }

    public List<Inventory> getDealerInventory(Integer dealerId) {
        return inventoryRepository.findByDealerIdAndInventoryType(dealerId, Inventory.InventoryType.DEALER);
    }

    public Integer getFactoryInventoryQuantity(Integer vehicleId) {
        return inventoryRepository.getFactoryInventoryQuantity(vehicleId).orElse(0);
    }

    public List<Inventory> getInventoryByDealer(Integer dealerId) {
        return inventoryRepository.findByDealerId(dealerId);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deductDealerInventory(Integer dealerId, Integer vehicleId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Invalid quantity: " + quantity);
        }

        Inventory dealerInventory = inventoryRepository.findByDealerIdAndVehicleIdAndInventoryType(
                        dealerId, vehicleId, Inventory.InventoryType.DEALER)
                .orElseThrow(() -> new RuntimeException("Dealer inventory not found for vehicle: " + vehicleId));

        if (!dealerInventory.hasSufficientQuantity(quantity)) {
            throw new RuntimeException("Dealer insufficient inventory for vehicle: " + vehicleId +
                    ". Available: " + dealerInventory.getAvailableQuantity() + ", Requested: " + quantity);
        }

        dealerInventory.setAvailableQuantity(dealerInventory.getAvailableQuantity() - quantity);
        dealerInventory.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(dealerInventory);

        log.info("Deducted dealer inventory - Dealer: {}, Vehicle: {}, Quantity: {}, Remaining: {}",
                dealerId, vehicleId, quantity, dealerInventory.getAvailableQuantity());
    }
}