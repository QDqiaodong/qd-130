package com.example.maternal.service;

import com.example.maternal.dto.EquipmentDTO;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.repository.TransferRecordRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String EQUIPMENT_TYPE_CACHE_KEY = "equipment:types";
    private static final long CACHE_TTL_SECONDS = 7200;
    
    public List<EquipmentDTO> getAllEquipments() {
        return equipmentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public EquipmentDTO getEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }
    
    public EquipmentDTO getEquipmentByNo(String equipmentNo) {
        return equipmentRepository.findByEquipmentNo(equipmentNo)
                .map(this::convertToDTO)
                .orElse(null);
    }
    
    public EquipmentDTO createEquipment(EquipmentDTO dto) {
        Equipment equipment = convertToEntity(dto);
        if (equipment.getEquipmentNo() == null || equipment.getEquipmentNo().isEmpty()) {
            equipment.setEquipmentNo(CodeGenerator.generateEquipmentNo());
        }
        Equipment saved = equipmentRepository.save(equipment);
        invalidateEquipmentTypeCache();
        return convertToDTO(saved);
    }
    
    public EquipmentDTO updateEquipment(Long id, EquipmentDTO dto) {
        return equipmentRepository.findById(id)
                .map(equipment -> {
                    equipment.setEquipmentName(dto.getEquipmentName());
                    equipment.setEquipmentType(dto.getEquipmentType());
                    equipment.setModel(dto.getModel());
                    equipment.setBrand(dto.getBrand());
                    equipment.setImageUrl(dto.getImageUrl());
                    equipment.setStatus(dto.getStatus());
                    equipment.setRemark(dto.getRemark());
                    Equipment saved = equipmentRepository.save(equipment);
                    invalidateEquipmentTypeCache();
                    return convertToDTO(saved);
                })
                .orElse(null);
    }
    
    public void deleteEquipment(Long id) {
        equipmentRepository.deleteById(id);
        invalidateEquipmentTypeCache();
    }
    
    public List<EquipmentDTO> getEquipmentsByArea(Long areaId) {
        return equipmentRepository.findByCurrentAreaId(areaId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<String> getEquipmentTypes() {
        Object cached = redisTemplate.opsForValue().get(EQUIPMENT_TYPE_CACHE_KEY);
        if (cached != null) {
            return (List<String>) cached;
        }
        List<String> types = equipmentRepository.findAll().stream()
                .map(Equipment::getEquipmentType)
                .distinct()
                .collect(Collectors.toList());
        redisTemplate.opsForValue().set(EQUIPMENT_TYPE_CACHE_KEY, types, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return types;
    }
    
    public EquipmentDTO bindArea(Long equipmentId, Long areaId) {
        if (repairOrderRepository.existsByEquipmentIdAndStatusIn(equipmentId, List.of(0, 1))) {
            throw new RuntimeException("设备维修中，不可调配，待维修恢复后方可重新调配");
        }
        if (transferRecordRepository.existsUnsignedByEquipmentId(equipmentId)) {
            throw new RuntimeException("该设备上一张调配单尚未到货签收，签收完成后目标区域才能再次调出");
        }
        return equipmentRepository.findById(equipmentId)
                .map(equipment -> {
                    equipment.setCurrentAreaId(areaId);
                    Equipment saved = equipmentRepository.save(equipment);
                    return convertToDTO(saved);
                })
                .orElse(null);
    }
    
    private void invalidateEquipmentTypeCache() {
        redisTemplate.delete(EQUIPMENT_TYPE_CACHE_KEY);
    }
    
    private EquipmentDTO convertToDTO(Equipment equipment) {
        EquipmentDTO dto = new EquipmentDTO();
        dto.setId(equipment.getId());
        dto.setEquipmentNo(equipment.getEquipmentNo());
        dto.setEquipmentName(equipment.getEquipmentName());
        dto.setEquipmentType(equipment.getEquipmentType());
        dto.setModel(equipment.getModel());
        dto.setBrand(equipment.getBrand());
        dto.setImageUrl(equipment.getImageUrl());
        dto.setCurrentAreaId(equipment.getCurrentAreaId());
        dto.setInitialAreaId(equipment.getInitialAreaId());
        dto.setStatus(equipment.getStatus());
        dto.setRemark(equipment.getRemark());
        
        areaRepository.findById(equipment.getCurrentAreaId())
                .ifPresent(area -> dto.setCurrentAreaName(area.getName()));
        areaRepository.findById(equipment.getInitialAreaId())
                .ifPresent(area -> dto.setInitialAreaName(area.getName()));

        repairOrderRepository
                .findFirstByEquipmentIdAndStatusInOrderByCreatedAtDescIdDesc(equipment.getId(), List.of(0, 1))
                .ifPresent(order -> dto.setRepairStatus(order.getStatus()));

        // 能否再调走与「调配登记」服务端闸门同一口径：维修中 或 上一张调配单未到货签收 均不可调
        boolean inRepair = dto.getRepairStatus() != null;
        dto.setTransferable(!inRepair
                && !transferRecordRepository.existsUnsignedByEquipmentId(equipment.getId()));

        return dto;
    }
    
    private Equipment convertToEntity(EquipmentDTO dto) {
        Equipment equipment = new Equipment();
        equipment.setEquipmentNo(dto.getEquipmentNo());
        equipment.setEquipmentName(dto.getEquipmentName());
        equipment.setEquipmentType(dto.getEquipmentType());
        equipment.setModel(dto.getModel());
        equipment.setBrand(dto.getBrand());
        equipment.setImageUrl(dto.getImageUrl());
        equipment.setCurrentAreaId(dto.getCurrentAreaId());
        equipment.setInitialAreaId(dto.getInitialAreaId());
        equipment.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        equipment.setRemark(dto.getRemark());
        return equipment;
    }
}