
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InspectionRecordRequest {

    private Long planId;

    private Long equipmentId;

    private LocalDate inspectionDate;

    private Integer result;

    private String abnormalDesc;

    private String photoUrl;

    private String inspector;

    private String remark;
}
