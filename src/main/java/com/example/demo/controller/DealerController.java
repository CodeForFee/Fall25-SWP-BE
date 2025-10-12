package com.example.demo.controller;

import com.example.demo.dto.DealerDTO;
import com.example.demo.dto.DealerResponseDTO;
import com.example.demo.entity.DealerStatus;
import com.example.demo.service.DealerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dealers")
@CrossOrigin
@Tag(name = "Dealer Management", description = "APIs for dealer management")
@SecurityRequirement(name = "bearer-jwt")
public class DealerController {

    @Autowired
    private DealerService dealerService;

    @PostMapping
    @Operation(summary = "Tạo dealer mới")
    public DealerResponseDTO createDealer(@RequestBody DealerDTO dealerDTO) {
        return dealerService.createDealer(dealerDTO);
    }

    @GetMapping
    @Operation(summary = "Lấy tất cả dealers")
    public List<DealerResponseDTO> getAllDealers() {
        return dealerService.getAllDealers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy dealer theo ID")
    public DealerResponseDTO getDealerById(@PathVariable Integer id) {
        return dealerService.getDealerById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật dealer")
    public DealerResponseDTO updateDealer(@PathVariable Integer id, @RequestBody DealerDTO dealerDTO) {
        return dealerService.updateDealer(id, dealerDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa dealer")
    public String deleteDealer(@PathVariable Integer id) {
        dealerService.deleteDealer(id);
        return "Dealer deleted successfully";
    }

    // STATUS
    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái dealer")
    public DealerResponseDTO updateDealerStatus(@PathVariable Integer id, @RequestParam DealerStatus status) {
        return dealerService.updateDealerStatus(id, status);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Lấy dealers theo trạng thái")
    public List<DealerResponseDTO> getDealersByStatus(@PathVariable DealerStatus status) {
        return dealerService.getDealersByStatus(status);
    }

    // REGION
    @GetMapping("/region/{region}")
    @Operation(summary = "Lấy dealers theo khu vực")
    public List<DealerResponseDTO> getDealersByRegion(@PathVariable String region) {
        return dealerService.getDealersByRegion(region);
    }

    // SEARCH
    @GetMapping("/search/name")
    @Operation(summary = "Tìm kiếm dealers theo tên")
    public List<DealerResponseDTO> searchDealersByName(@RequestParam String name) {
        return dealerService.searchDealersByName(name);
    }

    @GetMapping("/search/representative")
    @Operation(summary = "Tìm kiếm dealers theo tên đại diện")
    public List<DealerResponseDTO> searchDealersByRepresentative(@RequestParam String representativeName) {
        return dealerService.searchDealersByRepresentative(representativeName);
    }
}