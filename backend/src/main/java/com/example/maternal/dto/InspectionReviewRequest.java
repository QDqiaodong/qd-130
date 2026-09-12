
package com.example.maternal.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 值班复核请求：对已标异常的巡检补做复核，结论为不属实时必须填写复核说明。
 */
@Data
public class InspectionReviewRequest {

    /** 复核人（值班人员） */
    private String reviewer;

    /** 复核时间 */
    private LocalDateTime reviewTime;

    /** 复核结论：true-属实，false-不属实 */
    private Boolean confirmed;

    /** 复核说明（不属实时必填） */
    private String reviewNote;
}
