
package com.example.maternal.dto;

import lombok.Data;

import java.util.List;

@Data
public class AreaDTO {
    
    private Long id;
    
    private String name;
    
    private String code;
    
    private Long parentId;
    
    private Integer level;
    
    private String path;
    
    private Integer status;
    
    private Integer sortOrder;
    
    private List<AreaDTO> children;
}
