
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PatrolBoardDTO {

    /** 本次看板的巡更日期（未传时由服务端按当天落库口径取值） */
    private LocalDate patrolDate;

    /** 本次看板筛选的班次：null-全部班次，1-前夜班，2-后夜班 */
    private Integer shift;

    /** 打卡记录 */
    private List<PatrolCheckinDTO> records;

    /** 漏打房间（与打卡记录同源同口径） */
    private List<PatrolMissedRoomDTO> missedRooms;
}
