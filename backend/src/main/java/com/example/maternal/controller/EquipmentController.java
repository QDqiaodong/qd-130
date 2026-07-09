
package com.example.maternal.controller;

import com.example.maternal.dto.ApiResponse;
import com.example.maternal.dto.EquipmentDTO;
import com.example.maternal.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
public class EquipmentController {
    
    private final EquipmentService equipmentService;
    
    @GetMapping
    public ApiResponse<List<EquipmentDTO>> getAllEquipments() {
        return ApiResponse.success(equipmentService.getAllEquipments());
    }
    
    @GetMapping("/{id}")
    public ApiResponse<EquipmentDTO> getEquipmentById(@PathVariable Long id) {
        EquipmentDTO equipment = equipmentService.getEquipmentById(id);
        if (equipment == null) {
            return ApiResponse.error(404, "设备不存在");
        }
        return ApiResponse.success(equipment);
    }
    
    @GetMapping("/byNo/{equipmentNo}")
    public ApiResponse<EquipmentDTO> getEquipmentByNo(@PathVariable String equipmentNo) {
        EquipmentDTO equipment = equipmentService.getEquipmentByNo(equipmentNo);
        if (equipment == null) {
            return ApiResponse.error(404, "设备不存在");
        }
        return ApiResponse.success(equipment);
    }
    
    @GetMapping("/byArea/{areaId}")
    public ApiResponse<List<EquipmentDTO>> getEquipmentsByArea(@PathVariable Long areaId) {
        return ApiResponse.success(equipmentService.getEquipmentsByArea(areaId));
    }
    
    @GetMapping("/types")
    public ApiResponse<List<String>> getEquipmentTypes() {
        return ApiResponse.success(equipmentService.getEquipmentTypes());
    }
    
    @PostMapping
    public ApiResponse<EquipmentDTO> createEquipment(@RequestBody EquipmentDTO dto) {
        return ApiResponse.success("设备创建成功", equipmentService.createEquipment(dto));
    }
    
    @PutMapping("/{id}")
    public ApiResponse<EquipmentDTO> updateEquipment(@PathVariable Long id, @RequestBody EquipmentDTO dto) {
        EquipmentDTO equipment = equipmentService.updateEquipment(id, dto);
        if (equipment == null) {
            return ApiResponse.error(404, "设备不存在");
        }
        return ApiResponse.success("设备更新成功", equipment);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ApiResponse.success("设备删除成功", null);
    }
    
    @PutMapping("/{id}/bindArea")
    public ApiResponse<EquipmentDTO> bindArea(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long areaId = request.get("areaId");
        EquipmentDTO equipment = equipmentService.bindArea(id, areaId);
        if (equipment == null) {
            return ApiResponse.error(404, "设备不存在");
        }
        return ApiResponse.success("区域绑定成功", equipment);
    }
}
