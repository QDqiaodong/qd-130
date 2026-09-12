
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transfer_no", nullable = false, unique = true, length = 50)
    private String transferNo;
    
    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;
    
    @Column(name = "from_area_id", nullable = false)
    private Long fromAreaId;
    
    @Column(name = "to_area_id", nullable = false)
    private Long toAreaId;
    
    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Column(name = "operator", length = 50)
    private String operator;
    
    @Column(name = "status")
    private Integer status;
    
    @Column(name = "remark", length = 500)
    private String remark;

    /** 到货签收人（值班人员），未签收时为空 */
    @Column(name = "receiver", length = 50)
    private String receiver;

    /** 到货签收时间 */
    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    /** 到货外观是否完好：true-完好，false-有破损，未签收时为空 */
    @Column(name = "appearance_intact")
    private Boolean appearanceIntact;

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
