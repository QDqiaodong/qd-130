
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.InspectionDetailDTO;
import com.example.maternal.dto.InspectionRecordDTO;
import com.example.maternal.dto.InspectionRecordRequest;
import com.example.maternal.dto.InspectionReviewRequest;
import com.example.maternal.service.InspectionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionRecordController {

    private final InspectionRecordService inspectionRecordService;

    @GetMapping
    public ApiResponse<List<InspectionRecordDTO>> getInspections(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Integer result,
            @RequestParam(required = false) Integer repairStatus,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) Integer reviewStatus) {
        return ApiResponse.success(inspectionRecordService
                .getInspections(startDate, endDate, areaId, result, repairStatus, equipmentType, reviewStatus));
    }

    @GetMapping("/{id}")
    public ApiResponse<InspectionDetailDTO> getInspectionDetail(@PathVariable Long id) {
        InspectionDetailDTO detail = inspectionRecordService.getInspectionDetail(id);
        if (detail == null) {
            return ApiResponse.error(404, "巡检记录不存在");
        }
        return ApiResponse.success(detail);
    }

    @PostMapping
    public ApiResponse<InspectionRecordDTO> createInspection(@RequestBody InspectionRecordRequest request) {
        return ApiResponse.success("巡检记录提交成功", inspectionRecordService.createInspection(request));
    }

    /** 值班复核：对已标异常的巡检补录复核人、复核时间与是否属实 */
    @PostMapping("/{id}/review")
    public ApiResponse<InspectionRecordDTO> reviewInspection(@PathVariable Long id,
                                                             @RequestBody InspectionReviewRequest request) {
        return ApiResponse.success("复核提交成功", inspectionRecordService.reviewInspection(id, request));
    }
}
