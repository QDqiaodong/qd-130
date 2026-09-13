
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_opening_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_open_date",
                columnNames = {"area_id", "open_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomOpeningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_no", nullable = false, unique = true, length = 50)
    private String recordNo;

    /** 登记的母婴室区域ID */
    @Column(name = "area_id", nullable = false)
    private Long areaId;

    /** 开放日 */
    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    /** 当日开门时刻（临时关闭时为空） */
    @Column(name = "open_time")
    private LocalDateTime openTime;

    /** 当日关门时刻（临时关闭时为空） */
    @Column(name = "close_time")
    private LocalDateTime closeTime;

    /** 是否临时关闭：false-正常开放，true-临时关闭 */
    @Column(name = "temporarily_closed", nullable = false)
    private Boolean temporarilyClosed;

    /** 临时关闭原因（临时关闭必填） */
    @Column(name = "close_reason", length = 500)
    private String closeReason;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
