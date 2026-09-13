
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RoomOpeningRecordDTO {

    private Long id;

    private String recordNo;

    private Long areaId;

    private String areaName;

    private LocalDate openDate;

    private LocalDateTime openTime;

    private LocalDateTime closeTime;

    private Boolean temporarilyClosed;

    private String closeReason;

    private String remark;

    /** 此刻能否进入：由服务端按当前时间与开关门时刻统一判定，刷新后与服务端口径一致 */
    private Boolean canEnterNow;

    /**
     * 此刻状态码：OPEN-开放中可进；BEFORE_OPEN-未到开门时间；AFTER_CLOSE-已过关门时间；
     * TEMP_CLOSED-当日临时关闭；NOT_TODAY-非当日登记，仅供回看
     */
    private String enterStatusCode;

    private LocalDateTime createdAt;
}
