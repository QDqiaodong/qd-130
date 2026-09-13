
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SupplyHandoverRequest {

    /** 交班的母婴室区域ID */
    private Long areaId;

    private LocalDate handoverDate;

    /** 交班人（值班人员） */
    private String handoverPerson;

    /** 接班人 */
    private String receiver;

    /** 湿巾盘点件数 */
    private Integer wipesCount;

    /** 纸尿裤盘点件数 */
    private Integer diaperCount;

    private String remark;
}
