
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TransferRecordDTO {
    
    private Long id;
    
    private String transferNo;
    
    private Long equipmentId;
    
    private String equipmentNo;
    
    private String equipmentName;
    
    private Long fromAreaId;
    
    private String fromAreaName;
    
    private Long toAreaId;
    
    private String toAreaName;
    
    private LocalDate transferDate;
    
    private String reason;
    
    private String operator;
    
    private Integer status;

    private String remark;

    /** 到货签收人（值班人员） */
    private String receiver;

    /** 到货签收时间 */
    private LocalDateTime arrivalTime;

    /** 到货外观是否完好 */
    private Boolean appearanceIntact;

    /** 外观破损部位：仅外观有破损签收时有值 */
    private String damagePart;

    /** 是否已到货签收：由服务端按签收字段统一计算，列表、详情与调出闸门同口径 */
    private Boolean signed;
}
