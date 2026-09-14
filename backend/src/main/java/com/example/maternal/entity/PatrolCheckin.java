
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patrol_checkin",
        uniqueConstraints = @UniqueConstraint(name = "uk_patrol_area_date_shift",
                columnNames = {"area_id", "patrol_date", "shift"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolCheckin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "checkin_no", nullable = false, unique = true, length = 50)
    private String checkinNo;

    /** 巡更的母婴室区域ID */
    @Column(name = "area_id", nullable = false)
    private Long areaId;

    /** 巡更日期（夜间班次归属的日期） */
    @Column(name = "patrol_date", nullable = false)
    private LocalDate patrolDate;

    /** 巡更班次：1-前夜班（22:00-02:00），2-后夜班（02:00-06:00） */
    @Column(name = "shift", nullable = false)
    private Integer shift;

    /** 巡更人（值班人员） */
    @Column(name = "patrol_person", nullable = false, length = 50)
    private String patrolPerson;

    /** 打卡时间 */
    @Column(name = "checkin_time", nullable = false)
    private LocalDateTime checkinTime;

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
