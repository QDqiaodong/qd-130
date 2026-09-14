
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PatrolCheckinDTO {

    private Long id;

    private String checkinNo;

    private Long areaId;

    private String areaName;

    private LocalDate patrolDate;

    /** 巡更班次：1-前夜班，2-后夜班 */
    private Integer shift;

    /** 班次名称（由服务端统一生成，刷新后口径一致） */
    private String shiftName;

    private String patrolPerson;

    private LocalDateTime checkinTime;

    private String remark;

    private LocalDateTime createdAt;
}
