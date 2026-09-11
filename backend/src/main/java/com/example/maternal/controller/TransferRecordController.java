
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.TransferRecordDTO;
import com.example.maternal.dto.TransferRequest;
import com.example.maternal.dto.TransferSummaryDTO;
import com.example.maternal.service.TransferRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferRecordController {
    
    private final TransferRecordService transferRecordService;
    
    @GetMapping
    public ApiResponse<List<TransferRecordDTO>> getAllTransfers() {
        return ApiResponse.success(transferRecordService.getAllTransfers());
    }
    
    @GetMapping("/filter")
    public ApiResponse<List<TransferRecordDTO>> getTransfersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) String equipmentType) {
        return ApiResponse.success(transferRecordService
                .getTransfersByDateRange(startDate, endDate, areaId, equipmentType));
    }
    
    @GetMapping("/{id}")
    public ApiResponse<TransferRecordDTO> getTransferById(@PathVariable Long id) {
        TransferRecordDTO record = transferRecordService.getTransferById(id);
        if (record == null) {
            return ApiResponse.error(404, "调配记录不存在");
        }
        return ApiResponse.success(record);
    }
    
    @GetMapping("/byNo/{transferNo}")
    public ApiResponse<TransferRecordDTO> getTransferByNo(@PathVariable String transferNo) {
        TransferRecordDTO record = transferRecordService.getTransferByNo(transferNo);
        if (record == null) {
            return ApiResponse.error(404, "调配记录不存在");
        }
        return ApiResponse.success(record);
    }
    
    @GetMapping("/summary")
    public ApiResponse<TransferSummaryDTO> getTransferSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(transferRecordService.getTransferSummary(startDate, endDate));
    }
    
    @PostMapping
    public ApiResponse<TransferRecordDTO> createTransfer(@RequestBody TransferRequest request) {
        return ApiResponse.success("调配登记成功", transferRecordService.createTransfer(request));
    }
    
    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancelTransfer(@PathVariable Long id) {
        transferRecordService.cancelTransfer(id);
        return ApiResponse.success("调配已取消", null);
    }
}
