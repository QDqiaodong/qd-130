
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class HealthDashboardDTO {

    private LocalDate startDate;

    private LocalDate endDate;

    private Long areaId;

    private String areaName;

    private HealthMetricsDTO metrics;

    private List<HealthAreaRankDTO> areaRankings;

    private List<HealthTypeRankDTO> typeRankings;
}
