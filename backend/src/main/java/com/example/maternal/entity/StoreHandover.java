
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "store_handover",
        uniqueConstraints = @UniqueConstraint(name = "uk_handover_area_date_type",
                columnNames = {"area_id", "handover_date", "handover_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "handover_no", nullable = false, unique = true, length = 50)
    private String handoverNo;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "handover_date", nullable = false)
    private LocalDate handoverDate;

    /**
     * 交接类型：1-开店交接，2-闭店交接
     */
    @Column(name = "handover_type", nullable = false)
    private Integer handoverType;

    /**
     * 温奶器是否在位：0-否，1-是
     */
    @Column(name = "warmer_in_place", nullable = false)
    private Integer warmerInPlace;

    /**
     * 护理台是否擦净：0-否，1-是
     */
    @Column(name = "care_table_clean", nullable = false)
    private Integer careTableClean;

    @Column(name = "key_receiver", length = 50)
    private String keyReceiver;

    @Column(name = "abnormal_note", length = 500)
    private String abnormalNote;

    @Column(name = "handover_person", length = 50)
    private String handoverPerson;

    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 交接结论：0-不通过，1-通过
     */
    @Column(name = "result", nullable = false)
    private Integer result;

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
