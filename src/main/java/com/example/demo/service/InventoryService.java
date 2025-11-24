package com.example.demo.service;

import com.example.demo.dto.InventoryGroupResponseDTO;
import com.example.demo.dto.VehicleInventoryDetailDTO;
import com.example.demo.entity.Inventory;
import com.example.demo.entity.Vehicle;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.DealerRepository;
import com.example.demo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<InventoryGroupResponseDTO> getDealerInventory(Integer dealerId) {
        List<Inventory> inventories = inventoryRepository.findByDealerIdAndInventoryType(dealerId, Inventory.InventoryType.DEALER);

        // Lọc chỉ những inventory còn availableQuantity > 0
        List<Inventory> availableInventories = inventories.stream()
                .filter(inv -> inv.getAvailableQuantity() > 0)
                .collect(Collectors.toList());

        // Nhóm theo modelName
        Map<String, List<Inventory>> groupedByModel = availableInventories.stream()
                .collect(Collectors.groupingBy(inv -> inv.getVehicle().getModelName()));

        // Chuyển đổi sang DTO
        return groupedByModel.entrySet().stream()
                .map(entry -> {
                    List<Inventory> modelInventories = entry.getValue();
                    Inventory firstInventory = modelInventories.get(0);
                    Vehicle firstVehicle = firstInventory.getVehicle();

                    InventoryGroupResponseDTO groupDTO = new InventoryGroupResponseDTO();

                    // Thông tin chung từ vehicle
                    groupDTO.setModelName(firstVehicle.getModelName());
                    groupDTO.setBrand(firstVehicle.getBrand());
                    groupDTO.setYearOfManufacture(firstVehicle.getYearOfManufacture());
                    groupDTO.setListedPrice(firstVehicle.getListedPrice()); // Đã sửa - giữ nguyên BigDecimal
                    groupDTO.setBatteryCapacity(firstVehicle.getBatteryCapacity());
                    groupDTO.setStatus(firstVehicle.getStatus());

                    // Thông tin vehicle type
                    if (firstVehicle.getVehicleType() != null) {
                        groupDTO.setVehicleType(firstVehicle.getVehicleType().getTypeName());
                    }

                    // Tính tổng số lượng
                    Integer totalAvailable = modelInventories.stream()
                            .mapToInt(Inventory::getAvailableQuantity)
                            .sum();
                    Integer totalReserved = modelInventories.stream()
                            .mapToInt(Inventory::getReservedQuantity)
                            .sum();

                    groupDTO.setTotalAvailableQuantity(totalAvailable);
                    groupDTO.setTotalReservedQuantity(totalReserved);
                    groupDTO.setInventoryType(firstInventory.getInventoryType().toString());
                    groupDTO.setLastUpdated(firstInventory.getLastUpdated());

                    // Danh sách các xe cùng model
                    List<VehicleInventoryDetailDTO> vehicleDetails = modelInventories.stream()
                            .map(inv -> {
                                VehicleInventoryDetailDTO detail = new VehicleInventoryDetailDTO();
                                detail.setVehicleId(inv.getVehicle().getId());
                                detail.setVin(inv.getVehicle().getVin());
                                detail.setEngineNumber(inv.getVehicle().getEngineNumber());
                                detail.setStatus(inv.getVehicle().getStatus());
                                detail.setInventoryId(inv.getId());
                                detail.setAvailableQuantity(inv.getAvailableQuantity());
                                detail.setReservedQuantity(inv.getReservedQuantity());
                                return detail;
                            })
                            .collect(Collectors.toList());

                    groupDTO.setVehicles(vehicleDetails);
                    return groupDTO;
                })
                .sorted(Comparator.comparing(InventoryGroupResponseDTO::getModelName))
                .collect(Collectors.toList());
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

    public Map<String, Integer> getDealerInventorySummary(Integer dealerId) {
        List<Inventory> inventories = inventoryRepository.findByDealerIdAndInventoryType(dealerId, Inventory.InventoryType.DEALER);

        return inventories.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getVehicle().getModelName(),
                        Collectors.summingInt(Inventory::getAvailableQuantity)
                ));
    }


    public List<Map<String, Object>> getDealerInventoryDetails(Integer dealerId) {
        List<Inventory> inventories = inventoryRepository.findByDealerIdAndInventoryType(dealerId, Inventory.InventoryType.DEALER);

        return inventories.stream()
                .map(inventory -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", inventory.getId());
                    detail.put("availableQuantity", inventory.getAvailableQuantity());
                    detail.put("reservedQuantity", inventory.getReservedQuantity());
                    detail.put("inventoryType", inventory.getInventoryType());
                    detail.put("lastUpdated", inventory.getLastUpdated());

                    // Dealer info
                    if (inventory.getDealer() != null) {
                        Map<String, Object> dealerMap = new HashMap<>();
                        dealerMap.put("dealerId", inventory.getDealer().getDealerId());
                        dealerMap.put("name", inventory.getDealer().getName());
                        dealerMap.put("region", inventory.getDealer().getRegion());
                        dealerMap.put("status", inventory.getDealer().getStatus());
                        detail.put("dealer", dealerMap);
                    }

                    // Vehicle info với VIN và engineNumber
                    if (inventory.getVehicle() != null) {
                        Map<String, Object> vehicleMap = new HashMap<>();
                        vehicleMap.put("id", inventory.getVehicle().getId());
                        vehicleMap.put("modelName", inventory.getVehicle().getModelName());
                        vehicleMap.put("brand", inventory.getVehicle().getBrand());
                        vehicleMap.put("yearOfManufacture", inventory.getVehicle().getYearOfManufacture());
                        vehicleMap.put("status", inventory.getVehicle().getStatus());
                        vehicleMap.put("listedPrice", inventory.getVehicle().getListedPrice());
                        vehicleMap.put("batteryCapacity", inventory.getVehicle().getBatteryCapacity());
                        vehicleMap.put("vin", inventory.getVehicle().getVin());
                        vehicleMap.put("engineNumber", inventory.getVehicle().getEngineNumber());
                        detail.put("vehicle", vehicleMap);
                    }

                    return detail;
                })
                .collect(Collectors.toList());
    }


}
