package com.example.maternal.dto;

import lombok.Data;

import java.util.List;

/**
 * 超时催办统计结果：件数与清单来自同一次查询，保证刷新后两者一致。
 */
@Data
public class OverdueRepairsDTO {

    /** 约定等待小时数（回显统计口径） */
    private Integer waitHours;

    private Long areaId;

    private String areaName;

    /** 超时催办件数，恒等于 orders 的行数 */
    private Long overdueCount;

    /** 已超时的待处理报修单清单 */
    private List<RepairOrderDTO> orders;
}
