
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InspectionRecordDTO {

    private Long id;

    private String inspectionNo;

    private Long planId;

    private String planName;

    private Long equipmentId;

    private String equipmentNo;

    private String equipmentName;

    private Long areaId;

    private String areaName;

    private LocalDate inspectionDate;

    private Integer result;

    private String abnormalDesc;

    private String photoUrl;

    private String inspector;

    private String remark;

    private Long repairOrderId;

    private String repairNo;

    private Integer repairStatus;

    private LocalDateTime createdAt;
}
