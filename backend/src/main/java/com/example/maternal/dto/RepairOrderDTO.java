
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RepairOrderDTO {

    private Long id;

    private String repairNo;

    private Long inspectionId;

    private String inspectionNo;

    private Long equipmentId;

    private String equipmentNo;

    private String equipmentName;

    private Long areaId;

    private String areaName;

    private String faultDesc;

    private String photoUrl;

    private Integer status;

    private String reporter;

    private String repairman;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private String repairNote;

    private LocalDateTime createdAt;

    private List<TimelineItem> timeline;
}
