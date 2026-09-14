
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PatrolMissedRoomDTO {

    private Long areaId;

    private String areaName;

    private LocalDate patrolDate;

    /** 漏打的班次：1-前夜班，2-后夜班 */
    private Integer shift;

    /** 漏打班次名称（由服务端统一生成） */
    private String shiftName;
}
