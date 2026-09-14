
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.PatrolBoardDTO;
import com.example.maternal.dto.PatrolCheckinDTO;
import com.example.maternal.dto.PatrolCheckinRequest;
import com.example.maternal.service.PatrolCheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/patrol-checkins")
@RequiredArgsConstructor
public class PatrolCheckinController {

    private final PatrolCheckinService patrolCheckinService;

    @GetMapping
    public ApiResponse<PatrolBoardDTO> getBoard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate patrolDate,
            @RequestParam(required = false) Integer shift,
            @RequestParam(required = false) Long areaId) {
        return ApiResponse.success(patrolCheckinService.getBoard(patrolDate, shift, areaId));
    }

    @PostMapping
    public ApiResponse<PatrolCheckinDTO> createCheckin(@RequestBody PatrolCheckinRequest request) {
        return ApiResponse.success("巡更打卡成功", patrolCheckinService.createCheckin(request));
    }
}
