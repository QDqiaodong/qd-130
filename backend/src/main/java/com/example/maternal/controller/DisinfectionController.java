
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.DisinfectionRecordDTO;
import com.example.maternal.dto.DisinfectionRecordRequest;
import com.example.maternal.service.DisinfectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/disinfections")
@RequiredArgsConstructor
public class DisinfectionController {

    private final DisinfectionService disinfectionService;

    @GetMapping
    public ApiResponse<List<DisinfectionRecordDTO>> getDisinfections(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Boolean closedLoop) {
        return ApiResponse.success(disinfectionService
                .getDisinfections(startDate, endDate, areaId, closedLoop));
    }

    @PostMapping
    public ApiResponse<DisinfectionRecordDTO> createDisinfection(@RequestBody DisinfectionRecordRequest request) {
        return ApiResponse.success("消毒登记成功", disinfectionService.createDisinfection(request));
    }
}
