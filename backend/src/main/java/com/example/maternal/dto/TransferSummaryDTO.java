
package com.example.maternal.dto;

import lombok.Data;

@Data
public class TransferSummaryDTO {

    private Long totalCount;

    private String period;

    /**
     * 调出本区域（含下级）的有效调配数。
     */
    private Long outboundCount;

    /**
     * 调入本区域（含下级）的有效调配数；区域内互相调配同时计入调出与调入。
     */
    private Long inboundCount;
}
