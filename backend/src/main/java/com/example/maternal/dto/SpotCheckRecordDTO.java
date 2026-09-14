
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

    /** 本次抽检所用体温枪编号 */
    private String thermometerNo;

    private String abnormalDesc;

    private String photoUrl;

    private String inspector;

    /** 当班复核人：不合格抽检补填后才允许转报修 */
    private String reviewer;

    private LocalDateTime reviewTime;

    private String remark;

    private Long repairOrderId;

    private String repairNo;

    private Integer repairStatus;

    /** 抽检保存成功但自动报修未完成时的提示（如设备已有进行中的报修单） */
    private String repairMessage;

    private LocalDateTime createdAt;
}
