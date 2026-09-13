
package com.example.maternal.dto;

import lombok.Data;

@Data
public class RepairStatusRequest {

    private Integer status;

    private String repairman;

    private String repairNote;

    /** 复用前试机结论：维修中 → 已恢复（结单）时必填 */
    private String trialResult;
}
