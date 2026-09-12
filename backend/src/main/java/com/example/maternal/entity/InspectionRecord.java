
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspection_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_inspection_equipment_date", columnNames = {"equipment_id", "inspection_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inspection_no", nullable = false, unique = true, length = 50)
    private String inspectionNo;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Column(name = "result", nullable = false)
    private Integer result;

    @Column(name = "abnormal_desc", length = 500)
    private String abnormalDesc;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "inspector", length = 50)
    private String inspector;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "reviewer", length = 50)
    private String reviewer;

    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    @Column(name = "review_result")
    private Integer reviewResult;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

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
