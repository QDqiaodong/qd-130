
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DisinfectionRecordDTO {

    private Long id;

    private String disinfectionNo;

    private Long areaId;

    private String areaName;

    private LocalDate disinfectDate;

    private String operator;

    private LocalDateTime finishTime;

    private String disinfectant;

    private Boolean ventilationDone;

    /** 闭环标记：仅通风做完才算当日闭环，服务端统一落库 */
    private Boolean closedLoop;

    private String incompleteReason;

    private String remark;

    /** 该母婴室当日能否再登记（已闭环则不可再登记），与闭环标记同源计算 */
    private Boolean canRegisterAgain;

    private LocalDateTime createdAt;
}
