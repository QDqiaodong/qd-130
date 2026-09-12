
package com.example.maternal.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SpotCheckRecordDTO {

    private Long id;

    private String spotCheckNo;

    private Long equipmentId;

    private String equipmentNo;

    private String equipmentName;

    private Long areaId;

    private String areaName;

    private LocalDate checkDate;

    private BigDecimal temperature;

    private Boolean qualified;

    private String abnormalDesc;

    private String photoUrl;

    private String inspector;

    private String remark;

    private Long repairOrderId;

    private String repairNo;

    private Integer repairStatus;

    /** 抽检保存成功但自动报修未完成时的提示（如设备已有进行中的报修单） */
    private String repairMessage;

    private LocalDateTime createdAt;
}
