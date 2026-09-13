
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RoomOpeningRecordRequest {

    /** 登记的母婴室区域ID */
    private Long areaId;

    /** 开放日 */
    private LocalDate openDate;

    /** 当日开门时刻（正常开放必填） */
    private LocalDateTime openTime;

    /** 当日关门时刻（正常开放必填） */
    private LocalDateTime closeTime;

    /** 是否临时关闭；false-正常开放，true-临时关闭 */
    private Boolean temporarilyClosed;

    /** 临时关闭原因（临时关闭必填） */
    private String closeReason;

    private String remark;
}
