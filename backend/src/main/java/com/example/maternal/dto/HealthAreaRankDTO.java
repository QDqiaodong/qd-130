
package com.example.maternal.dto;

import lombok.Data;

@Data
public class HealthAreaRankDTO {

    private Long areaId;

    private String areaName;

    private Long totalEquipmentCount;

    private Long repairingCount;

    private Long inspectionCompletedCount;

    private Long inspectionPlannedCount;

    private Double inspectionCompletionRate;

    private Long abnormalCount;

    private Double abnormalRate;

    private Long pendingRepairCount;

    private Long transferOutCount;

    private Long transferInCount;

    private Long transferCount;
}
