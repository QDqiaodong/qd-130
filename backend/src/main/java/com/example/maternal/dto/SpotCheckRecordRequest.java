
package com.example.maternal.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SpotCheckRecordRequest {

    private Long equipmentId;

    private LocalDate checkDate;

    /** 实测水温（℃），抽检结论由温度区间服务端判定 */
    private BigDecimal temperature;

    /** 本次抽检所用体温枪编号：必填，空编号服务端当场拦截，不允许留空后沿用上一台记录 */
    private String thermometerNo;

    private String abnormalDesc;

    private String photoUrl;

    private String inspector;

    private String remark;

    /** 不合格时是否同时创建报修单 */
    private Boolean createRepair;
}
