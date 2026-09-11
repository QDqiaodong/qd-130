
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.HealthDashboardDTO;
import com.example.maternal.service.HealthDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class HealthDashboardController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final HealthDashboardService healthDashboardService;

    @GetMapping("/health")
    public ApiResponse<HealthDashboardDTO> getHealthDashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long areaId) {
        LocalDate start = parseDate(startDate, "开始日期");
        LocalDate end = parseDate(endDate, "结束日期");
        return ApiResponse.success(healthDashboardService.getDashboard(start, end, areaId));
    }

    private LocalDate parseDate(String value, String label) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new RuntimeException(label + "格式不合法，正确格式为 yyyy-MM-dd（例如 2026-09-01）");
        }
    }
}
