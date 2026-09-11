
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StoreHandoverRequest {

    private Long areaId;

    private LocalDate handoverDate;

    /**
     * 交接类型：1-开店交接，2-闭店交接
     */
    private Integer handoverType;

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
}
