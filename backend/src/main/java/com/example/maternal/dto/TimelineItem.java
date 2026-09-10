
package com.example.maternal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimelineItem {

    private LocalDateTime time;

    private String title;

    private String description;

    private String type;
}
