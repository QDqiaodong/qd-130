
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

    private Long spotCheckId;

    private String spotCheckNo;

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

    /** 复用前试机结论（维修完成恢复前必填） */
    private String trialResult;

    /** 复用前试机时间 */
    private LocalDateTime trialTime;

    /** 是否已试机：以是否落库试机结论为准，由服务端统一计算 */
    private Boolean trialDone;

    private String urgeNote;

    private LocalDateTime urgeTime;

    /** 已等待小时数（仅超时催办统计场景返回） */
    private Long waitedHours;

    private LocalDateTime createdAt;

    private List<TimelineItem> timeline;
}
