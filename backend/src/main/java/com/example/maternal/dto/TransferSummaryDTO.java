
package com.example.maternal.dto;

import lombok.Data;

@Data
public class TransferSummaryDTO {
    
    private Long totalCount;
    
    private String period;
    
    private Long fromAreaId;
    
    private String fromAreaName;
    
    private Long toAreaId;
    
    private String toAreaName;
}
