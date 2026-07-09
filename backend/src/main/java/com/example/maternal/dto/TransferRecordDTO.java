
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;

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
}
