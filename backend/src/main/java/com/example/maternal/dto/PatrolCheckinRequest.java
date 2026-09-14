
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PatrolCheckinRequest {

    /** 巡更的母婴室区域ID */
    private Long areaId;

    /** 巡更日期（夜间班次归属的日期） */
    private LocalDate patrolDate;

    /** 巡更班次：1-前夜班，2-后夜班 */
    private Integer shift;

    /** 巡更人（值班人员） */
    private String patrolPerson;

    /** 打卡时间 */
    private LocalDateTime checkinTime;

    private String remark;
}
