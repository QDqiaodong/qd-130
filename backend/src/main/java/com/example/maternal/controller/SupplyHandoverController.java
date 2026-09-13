
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.SupplyHandoverDTO;
import com.example.maternal.dto.SupplyHandoverRequest;
import com.example.maternal.service.SupplyHandoverService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supply-handovers")
@RequiredArgsConstructor
public class SupplyHandoverController {

    private final SupplyHandoverService supplyHandoverService;

    @GetMapping
    public ApiResponse<List<SupplyHandoverDTO>> getHandovers(
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Boolean handedOver) {
        return ApiResponse.success(supplyHandoverService.getHandovers(areaId, handedOver));
    }

    @GetMapping("/{id}")
    public ApiResponse<SupplyHandoverDTO> getHandoverById(@PathVariable Long id) {
        SupplyHandoverDTO dto = supplyHandoverService.getHandoverById(id);
        if (dto == null) {
            return ApiResponse.error(404, "交接单不存在");
        }
        return ApiResponse.success(dto);
    }

    @PostMapping
    public ApiResponse<SupplyHandoverDTO> createHandover(@RequestBody SupplyHandoverRequest request) {
        return ApiResponse.success("交班登记成功", supplyHandoverService.createHandover(request));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<SupplyHandoverDTO> confirmHandover(@PathVariable Long id) {
        return ApiResponse.success("交接确认成功", supplyHandoverService.confirmHandover(id));
    }
}
