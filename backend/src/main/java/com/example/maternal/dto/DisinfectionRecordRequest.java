
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DisinfectionRecordRequest {

    /** 消毒的母婴室区域ID */
    private Long areaId;

    private LocalDate disinfectDate;

    /** 消毒人（值班人员） */
    private String operator;

    /** 消毒完成时间 */
    private LocalDateTime finishTime;

    /** 使用的消毒液 */
    private String disinfectant;

    /** 通风是否做完；闭环标记由服务端按此统一判定落库 */
    private Boolean ventilationDone;

    /** 未完成原因（通风未做完必填） */
    private String incompleteReason;

    private String remark;
}
