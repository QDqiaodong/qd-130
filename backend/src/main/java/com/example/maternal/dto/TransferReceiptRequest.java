
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 到货签收请求：值班对已发出的调配做到货签收，填写签收人、到货时间和外观是否完好。
 * 签收结论由服务端校验后统一落库，未签收前目标区域不能再把这台设备调走。
 */
@Data
public class TransferReceiptRequest {

    /** 签收人（值班人员） */
    private String receiver;

    /** 到货时间 */
    private LocalDateTime arrivalTime;

    /** 外观是否完好：true-完好，false-有破损 */
    private Boolean appearanceIntact;
}
