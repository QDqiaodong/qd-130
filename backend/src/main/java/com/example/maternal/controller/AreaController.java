
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.AreaDTO;
import com.example.maternal.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {
    
    private final AreaService areaService;
    
    @GetMapping
    public ApiResponse<List<AreaDTO>> getAllAreas() {
        return ApiResponse.success(areaService.getAllAreas());
    }
    
    @GetMapping("/tree")
    public ApiResponse<List<AreaDTO>> getAreaTree() {
        return ApiResponse.success(areaService.getAreaTree());
    }
    
    @GetMapping("/{id}")
    public ApiResponse<AreaDTO> getAreaById(@PathVariable Long id) {
        AreaDTO area = areaService.getAreaById(id);
        if (area == null) {
            return ApiResponse.error(404, "区域不存在");
        }
        return ApiResponse.success(area);
    }
    
    @PostMapping
    public ApiResponse<AreaDTO> createArea(@RequestBody AreaDTO dto) {
        return ApiResponse.success("区域创建成功", areaService.createArea(dto));
    }
    
    @PutMapping("/{id}")
    public ApiResponse<AreaDTO> updateArea(@PathVariable Long id, @RequestBody AreaDTO dto) {
        AreaDTO area = areaService.updateArea(id, dto);
        if (area == null) {
            return ApiResponse.error(404, "区域不存在");
        }
        return ApiResponse.success("区域更新成功", area);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteArea(@PathVariable Long id) {
        areaService.deleteArea(id);
        return ApiResponse.success("区域删除成功", null);
    }
}
