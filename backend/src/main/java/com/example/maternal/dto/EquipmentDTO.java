
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

    private Integer repairStatus;

    /**
     * 是否还能再调走：服务端按维修闸门 + 未签收在途调配闸门统一计算。
     * 刷新后档案区域、签收状态、可否再调同口径，前端按钮据此禁用。
     */
    private Boolean transferable;
}
