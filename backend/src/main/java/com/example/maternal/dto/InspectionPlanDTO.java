
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InspectionPlanDTO {

    private Long id;

    private String planNo;

    private String planName;

    private Long equipmentId;

    private String equipmentNo;

    private String equipmentName;

    private Long areaId;

    private String areaName;

    private Integer cycleType;

    private String cycleTypeName;

    private LocalDate nextInspectionDate;

    private String inspector;

    private Integer status;

    private String remark;

    private LocalDateTime createdAt;
}
