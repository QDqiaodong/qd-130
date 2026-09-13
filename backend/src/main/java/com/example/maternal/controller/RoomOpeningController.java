
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.RoomOpeningRecordDTO;
import com.example.maternal.dto.RoomOpeningRecordRequest;
import com.example.maternal.service.RoomOpeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/room-openings")
@RequiredArgsConstructor
public class RoomOpeningController {

    private final RoomOpeningService roomOpeningService;

    @GetMapping
    public ApiResponse<List<RoomOpeningRecordDTO>> getRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Boolean canEnter) {
        return ApiResponse.success(roomOpeningService.getRecords(startDate, endDate, areaId, canEnter));
    }

    @PostMapping
    public ApiResponse<RoomOpeningRecordDTO> createRecord(@RequestBody RoomOpeningRecordRequest request) {
        return ApiResponse.success("开放登记成功", roomOpeningService.createRecord(request));
    }
}
