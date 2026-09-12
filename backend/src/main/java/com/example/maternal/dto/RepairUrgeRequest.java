package com.example.maternal.dto;

import lombok.Data;

@Data
public class RepairUrgeRequest {

    /** 跟进人（为空则不修改原跟进人） */
    private String followUpPerson;

    /** 催办说明（必填，留痕） */
    private String urgeNote;
}
