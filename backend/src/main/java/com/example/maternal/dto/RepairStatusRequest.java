
package com.example.maternal.dto;

import lombok.Data;

@Data
public class RepairStatusRequest {

    private Integer status;

    private String repairman;

    private String repairNote;
}
