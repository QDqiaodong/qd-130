
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SupplyHandoverDTO {

    private Long id;

    private String handoverNo;

    private Long areaId;

    private String areaName;

    private LocalDate handoverDate;

    private String handoverPerson;

    private String receiver;

    private Integer wipesCount;

    private Integer diaperCount;

    /** 是否已交接：接班人确认后由服务端统一落库 */
    private Boolean handedOver;

    private LocalDateTime handoverTime;

    private String remark;

    private LocalDateTime createdAt;
}
