
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "repair_order",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_repair_inspection", columnNames = {"inspection_id"}),
                @UniqueConstraint(name = "uk_repair_spotcheck", columnNames = {"spot_check_id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repair_no", nullable = false, unique = true, length = 50)
    private String repairNo;

    /** 来源巡检记录ID（巡检异常报修时写入，温奶器抽检报修时为空） */
    @Column(name = "inspection_id")
    private Long inspectionId;

    /** 来源温奶器抽检记录ID（抽检不合格报修时写入） */
    @Column(name = "spot_check_id")
    private Long spotCheckId;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "fault_desc", length = 500)
    private String faultDesc;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "reporter", length = 50)
    private String reporter;

    @Column(name = "repairman", length = 50)
    private String repairman;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "finish_time")
    private LocalDateTime finishTime;

    @Column(name = "repair_note", length = 500)
    private String repairNote;

    @Column(name = "urge_note", length = 500)
    private String urgeNote;

    @Column(name = "urge_time")
    private LocalDateTime urgeTime;

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
