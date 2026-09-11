
package com.example.maternal.dto;

import lombok.Data;

@Data
public class HealthTypeRankDTO {

    private String equipmentType;

    private Long totalEquipmentCount;

    private Long repairingCount;

    private Long inspectionCompletedCount;

    private Long inspectionPlannedCount;

    private Double inspectionCompletionRate;

    private Long abnormalCount;

    private Double abnormalRate;

    private Long pendingRepairCount;

    private Long transferCount;
}
