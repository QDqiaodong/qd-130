
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "spot_check_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_spotcheck_equipment_date",
                columnNames = {"equipment_id", "check_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpotCheckRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_check_no", nullable = false, unique = true, length = 50)
    private String spotCheckNo;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    /** 抽检时设备所在母婴室区域ID（按设备当前区域落库） */
    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    /** 实测水温（℃），保留两位小数 */
    @Column(name = "temperature", nullable = false, precision = 5, scale = 2)
    private BigDecimal temperature;

    /** 抽检结论：true-合格，false-不合格（按温度区间服务端判定落库） */
    @Column(name = "qualified", nullable = false)
    private Boolean qualified;

    @Column(name = "abnormal_desc", length = 500)
    private String abnormalDesc;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "inspector", length = 50)
    private String inspector;

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
