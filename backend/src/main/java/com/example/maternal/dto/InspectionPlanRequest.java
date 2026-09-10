
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InspectionPlanRequest {

    private String planName;

    private Long equipmentId;

    private Long areaId;

    private Integer cycleType;

    private LocalDate nextInspectionDate;

    private String inspector;

    private Integer status;

    private String remark;
}
