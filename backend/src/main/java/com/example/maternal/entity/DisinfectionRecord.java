
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "disinfection_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisinfectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disinfection_no", nullable = false, unique = true, length = 50)
    private String disinfectionNo;

    /** 消毒的母婴室区域ID */
    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "disinfect_date", nullable = false)
    private LocalDate disinfectDate;

    /** 消毒人（值班人员） */
    @Column(name = "operator", nullable = false, length = 50)
    private String operator;

    /** 消毒完成时间 */
    @Column(name = "finish_time", nullable = false)
    private LocalDateTime finishTime;

    /** 使用的消毒液 */
    @Column(name = "disinfectant", nullable = false, length = 100)
    private String disinfectant;

    /** 通风是否做完：true-已做完，false-未做完 */
    @Column(name = "ventilation_done", nullable = false)
    private Boolean ventilationDone;

    /** 当日是否闭环：仅通风做完才算闭环，由服务端统一判定落库 */
    @Column(name = "closed_loop", nullable = false)
    private Boolean closedLoop;

    /** 未完成原因（通风未做完必填） */
    @Column(name = "incomplete_reason", length = 500)
    private String incompleteReason;

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
