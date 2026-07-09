
package com.example.maternal.dto;

import lombok.Data;

@Data
public class EquipmentDTO {
    
    private Long id;
    
    private String equipmentNo;
    
    private String equipmentName;
    
    private String equipmentType;
    
    private String model;
    
    private String brand;
    
    private String imageUrl;
    
    private Long currentAreaId;
    
    private Long initialAreaId;
    
    private Integer status;
    
    private String remark;
    
    private String currentAreaName;
    
    private String initialAreaName;
}
