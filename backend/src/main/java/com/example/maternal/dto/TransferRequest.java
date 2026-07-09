
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TransferRequest {
    
    private Long equipmentId;
    
    private Long toAreaId;
    
    private LocalDate transferDate;
    
    private String reason;
    
    private String operator;
    
    private String remark;
}
