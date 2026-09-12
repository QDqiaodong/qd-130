
package com.example.maternal.dto;

import lombok.Data;

import java.util.List;

@Data
public class SpotCheckDetailDTO {

    private SpotCheckRecordDTO record;

    private RepairOrderDTO repairOrder;

    private List<TimelineItem> timeline;
}
