
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.dto.SpotCheckDetailDTO;
import com.example.maternal.dto.SpotCheckRecordDTO;
import com.example.maternal.dto.SpotCheckRecordRequest;
import com.example.maternal.dto.SpotCheckReviewerRequest;
import com.example.maternal.service.RepairOrderService;
import com.example.maternal.service.SpotCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/spot-checks")
@RequiredArgsConstructor
public class SpotCheckController {

    private final SpotCheckService spotCheckService;
    private final RepairOrderService repairOrderService;

    @GetMapping
    public ApiResponse<List<SpotCheckRecordDTO>> getSpotChecks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Boolean qualified,
            @RequestParam(required = false) Integer repairStatus,
            @RequestParam(required = false) Integer reviewStatus,
            @RequestParam(required = false) String thermometerNo) {
        return ApiResponse.success(spotCheckService
                .getSpotChecks(startDate, endDate, areaId, qualified, repairStatus, reviewStatus, thermometerNo));
    }

    @GetMapping("/{id}")
    public ApiResponse<SpotCheckDetailDTO> getSpotCheckDetail(@PathVariable Long id) {
        SpotCheckDetailDTO detail = spotCheckService.getSpotCheckDetail(id);
        if (detail == null) {
            return ApiResponse.error(404, "抽检记录不存在");
        }
        return ApiResponse.success(detail);
    }

    @PostMapping
    public ApiResponse<SpotCheckRecordDTO> createSpotCheck(@RequestBody SpotCheckRecordRequest request) {
        return ApiResponse.success("抽检记录登记成功", spotCheckService.createSpotCheck(request));
    }

    /** 不合格抽检补填当班复核人，补填后才允许转报修 */
    @PutMapping("/{id}/reviewer")
    public ApiResponse<SpotCheckRecordDTO> updateReviewer(@PathVariable Long id,
                                                          @RequestBody SpotCheckReviewerRequest request) {
        return ApiResponse.success("当班复核人补填成功", spotCheckService.updateReviewer(id, request));
    }

    /** 不合格抽检一键补报修 */
    @PostMapping("/{id}/repair")
    public ApiResponse<RepairOrderDTO> createRepair(@PathVariable Long id,
                                                     @RequestBody(required = false) java.util.Map<String, Object> body) {
        String reporter = body != null && body.get("reporter") != null ? body.get("reporter").toString() : null;
        return ApiResponse.success("报修单创建成功",
                repairOrderService.createRepairOrderFromSpotCheck(id, reporter));
    }
}
