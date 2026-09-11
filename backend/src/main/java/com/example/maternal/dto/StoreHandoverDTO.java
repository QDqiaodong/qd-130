
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StoreHandoverDTO {

    private Long id;

    private String handoverNo;

    private Long areaId;

    private String areaName;

    private LocalDate handoverDate;

    /**
     * 交接类型：1-开店交接，2-闭店交接
     */
    private Integer handoverType;

    private String handoverTypeName;

    /**
     * 温奶器是否在位：0-否，1-是
     */
    private Integer warmerInPlace;

    /**
     * 护理台是否擦净：0-否，1-是
     */
    private Integer careTableClean;

    private String keyReceiver;

    private String abnormalNote;

    private String handoverPerson;

    private String remark;

    /**
     * 交接结论：0-不通过，1-通过
     */
    private Integer result;

    /**
     * 该区域设备当前是否可调配（由最近一次闭店/开店交接结论派生）
     */
    private Boolean equipmentTransferable;

    private LocalDateTime createdAt;
}
