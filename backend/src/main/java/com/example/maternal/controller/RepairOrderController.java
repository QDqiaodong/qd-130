
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.OverdueRepairsDTO;
import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.dto.RepairStatusRequest;
import com.example.maternal.dto.RepairUrgeRequest;
import com.example.maternal.service.RepairOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repairs")
@RequiredArgsConstructor
public class RepairOrderController {

    private final RepairOrderService repairOrderService;

    @GetMapping
    public ApiResponse<List<RepairOrderDTO>> getRepairs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) Long equipmentCurrentAreaId) {
        return ApiResponse.success(repairOrderService
                .getRepairs(startDate, endDate, areaId, status, equipmentType, equipmentCurrentAreaId));
    }

    @GetMapping("/{id}")
    public ApiResponse<RepairOrderDTO> getRepairById(@PathVariable Long id) {
        RepairOrderDTO repair = repairOrderService.getRepairById(id);
        if (repair == null) {
            return ApiResponse.error(404, "报修单不存在");
        }
        return ApiResponse.success(repair);
    }

    @PostMapping
    public ApiResponse<RepairOrderDTO> createRepair(@RequestBody Map<String, Object> request) {
        Long inspectionId = request.get("inspectionId") != null
                ? Long.valueOf(request.get("inspectionId").toString()) : null;
        Long spotCheckId = request.get("spotCheckId") != null
                ? Long.valueOf(request.get("spotCheckId").toString()) : null;
        String reporter = request.get("reporter") != null ? request.get("reporter").toString() : null;
        if (inspectionId == null && spotCheckId == null) {
            return ApiResponse.error(400, "来源记录ID不能为空（inspectionId 或 spotCheckId 至少传一个）");
        }
        if (spotCheckId != null) {
            return ApiResponse.success("报修单创建成功",
                    repairOrderService.createRepairOrderFromSpotCheck(spotCheckId, reporter));
        }
        return ApiResponse.success("报修单创建成功", repairOrderService.createRepairOrder(inspectionId, reporter));
    }

    @GetMapping("/overdue")
    public ApiResponse<OverdueRepairsDTO> getOverdueRepairs(
            @RequestParam(required = false) Integer waitHours,
            @RequestParam(required = false) Long areaId) {
        return ApiResponse.success(repairOrderService.getOverdueRepairs(waitHours, areaId));
    }

    @PutMapping("/{id}/urge")
    public ApiResponse<RepairOrderDTO> urgeRepair(@PathVariable Long id, @RequestBody RepairUrgeRequest request) {
        return ApiResponse.success("催办成功，已更新跟进人和催办说明", repairOrderService.urgeRepair(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<RepairOrderDTO> updateStatus(@PathVariable Long id, @RequestBody RepairStatusRequest request) {
        return ApiResponse.success("报修单状态更新成功", repairOrderService.updateStatus(id, request));
    }
}
