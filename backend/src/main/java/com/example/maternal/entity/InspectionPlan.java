
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspection_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_no", nullable = false, unique = true, length = 50)
    private String planNo;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "cycle_type", nullable = false)
    private Integer cycleType;

    @Column(name = "next_inspection_date")
    private LocalDate nextInspectionDate;

    @Column(name = "inspector", length = 50)
    private String inspector;

    @Column(name = "status")
    private Integer status;

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
