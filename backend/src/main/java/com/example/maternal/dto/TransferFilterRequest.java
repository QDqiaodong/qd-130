
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TransferFilterRequest {
    
    private LocalDate startDate;
    
    private LocalDate endDate;
}
