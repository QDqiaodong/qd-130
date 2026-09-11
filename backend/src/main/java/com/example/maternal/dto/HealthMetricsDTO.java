
package com.example.maternal.dto;

import lombok.Data;

@Data
public class HealthMetricsDTO {

    private Long totalEquipmentCount;

    private Long repairingCount;

    private Long inspectionCompletedCount;

    private Long inspectionPlannedCount;

    private Double inspectionCompletionRate;

    private Long inspectionCount;

    private Long abnormalCount;

    private Double abnormalRate;

    private Long pendingRepairCount;

    private Long transferCount;
}
