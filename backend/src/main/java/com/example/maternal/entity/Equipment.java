
package com.example.maternal.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "equipment_no", nullable = false, unique = true, length = 50)
    private String equipmentNo;
    
    @Column(name = "equipment_name", nullable = false, length = 100)
    private String equipmentName;
    
    @Column(name = "equipment_type", nullable = false, length = 50)
    private String equipmentType;
    
    @Column(name = "model", length = 100)
    private String model;
    
    @Column(name = "brand", length = 100)
    private String brand;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @Column(name = "current_area_id", nullable = false)
    private Long currentAreaId;
    
    @Column(name = "initial_area_id", nullable = false)
    private Long initialAreaId;
    
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
