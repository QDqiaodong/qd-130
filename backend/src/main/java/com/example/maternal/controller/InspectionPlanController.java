
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.InspectionPlanDTO;
import com.example.maternal.dto.InspectionPlanRequest;
import com.example.maternal.service.InspectionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inspection-plans")
@RequiredArgsConstructor
public class InspectionPlanController {

    private final InspectionPlanService inspectionPlanService;

    @GetMapping
    public ApiResponse<List<InspectionPlanDTO>> getAllPlans(
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(inspectionPlanService.getAllPlans(areaId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<InspectionPlanDTO> getPlanById(@PathVariable Long id) {
        InspectionPlanDTO plan = inspectionPlanService.getPlanById(id);
        if (plan == null) {
            return ApiResponse.error(404, "巡检计划不存在");
        }
        return ApiResponse.success(plan);
    }

    @PostMapping
    public ApiResponse<InspectionPlanDTO> createPlan(@RequestBody InspectionPlanRequest request) {
        return ApiResponse.success("巡检计划创建成功", inspectionPlanService.createPlan(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<InspectionPlanDTO> updatePlan(@PathVariable Long id, @RequestBody InspectionPlanRequest request) {
        return ApiResponse.success("巡检计划更新成功", inspectionPlanService.updatePlan(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<InspectionPlanDTO> updatePlanStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        return ApiResponse.success("计划状态更新成功", inspectionPlanService.updateStatus(id, request.get("status")));
    }
}
