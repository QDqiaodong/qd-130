
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "supply_handover")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplyHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "handover_no", nullable = false, unique = true, length = 50)
    private String handoverNo;

    /** 交班的母婴室区域ID */
    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "handover_date", nullable = false)
    private LocalDate handoverDate;

    /** 交班人（值班人员） */
    @Column(name = "handover_person", nullable = false, length = 50)
    private String handoverPerson;

    /** 接班人 */
    @Column(name = "receiver", nullable = false, length = 50)
    private String receiver;

    /** 湿巾盘点件数 */
    @Column(name = "wipes_count", nullable = false)
    private Integer wipesCount;

    /** 纸尿裤盘点件数 */
    @Column(name = "diaper_count", nullable = false)
    private Integer diaperCount;

    /** 是否已交接：false-未交接，true-已交接（接班人确认后由服务端落库） */
    @Column(name = "handed_over", nullable = false)
    private Boolean handedOver;

    /** 交接确认时间 */
    @Column(name = "handover_time")
    private LocalDateTime handoverTime;

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
