
package com.example.maternal.service;

import com.example.maternal.dto.InspectionPlanDTO;
import com.example.maternal.dto.InspectionPlanRequest;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.InspectionPlan;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionPlanRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InspectionPlanService {

    private final InspectionPlanRepository inspectionPlanRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final AreaService areaService;

    public List<InspectionPlanDTO> getAllPlans() {
        return getAllPlans(null, null);
    }

    /**
     * 按母婴室区域与启用状态筛选计划：区域含下级区域；
     * 区域类计划按计划区域归属，设备类计划按设备当前所在区域归属。
     */
    public List<InspectionPlanDTO> getAllPlans(Long areaId, Integer status) {
        Set<Long> scopeAreaIds = areaService.resolveScopeAreaIds(areaId);
        Map<Long, Long> equipmentAreaMap = new HashMap<>();
        if (scopeAreaIds != null) {
            equipmentRepository.findAll()
                    .forEach(e -> equipmentAreaMap.put(e.getId(), e.getCurrentAreaId()));
        }
        Map<Long, Long> finalEquipmentAreaMap = equipmentAreaMap;
        return inspectionPlanRepository.findAllOrdered().stream()
                .filter(plan -> status == null || status.equals(plan.getStatus()))
                .filter(plan -> scopeAreaIds == null || inScope(plan, scopeAreaIds, finalEquipmentAreaMap))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private boolean inScope(InspectionPlan plan, Set<Long> scopeAreaIds, Map<Long, Long> equipmentAreaMap) {
        if (plan.getAreaId() != null) {
            return scopeAreaIds.contains(plan.getAreaId());
        }
        Long equipmentAreaId = plan.getEquipmentId() == null ? null : equipmentAreaMap.get(plan.getEquipmentId());
        return equipmentAreaId != null && scopeAreaIds.contains(equipmentAreaId);
    }

    public InspectionPlanDTO getPlanById(Long id) {
        return inspectionPlanRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional
    public InspectionPlanDTO createPlan(InspectionPlanRequest request) {
        validateScope(request);

        InspectionPlan plan = new InspectionPlan();
        plan.setPlanNo(CodeGenerator.generatePlanNo());
        applyRequest(plan, request);
        plan.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        return convertToDTO(inspectionPlanRepository.save(plan));
    }

    @Transactional
    public InspectionPlanDTO updatePlan(Long id, InspectionPlanRequest request) {
        InspectionPlan plan = inspectionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("巡检计划不存在"));

        validateScope(request);
        validateNextInspectionDateEditable(plan, request);
        applyRequest(plan, request);
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }

        return convertToDTO(inspectionPlanRepository.save(plan));
    }

    /**
     * 已停用且本次请求未重新启用的计划，不允许修改下次巡检日期。
     */
    private void validateNextInspectionDateEditable(InspectionPlan plan, InspectionPlanRequest request) {
        boolean staysDisabled = Integer.valueOf(0).equals(plan.getStatus())
                && !Integer.valueOf(1).equals(request.getStatus());
        if (staysDisabled && !Objects.equals(plan.getNextInspectionDate(), request.getNextInspectionDate())) {
            throw new RuntimeException("计划已停用，不能修改下次巡检日期，请先启用计划");
        }
    }

    @Transactional
    public InspectionPlanDTO updateStatus(Long id, Integer status) {
        InspectionPlan plan = inspectionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("巡检计划不存在"));

        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("计划状态不合法");
        }
        if (status.equals(plan.getStatus())) {
            throw new RuntimeException("计划已处于" + (status == 1 ? "启用" : "停用") + "状态，请勿重复提交");
        }

        plan.setStatus(status);
        return convertToDTO(inspectionPlanRepository.save(plan));
    }

    private void validateScope(InspectionPlanRequest request) {
        boolean hasEquipment = request.getEquipmentId() != null;
        boolean hasArea = request.getAreaId() != null;
        if (hasEquipment == hasArea) {
            throw new RuntimeException("巡检计划必须且只能关联一个设备或一个区域");
        }
        if (hasEquipment && !equipmentRepository.existsById(request.getEquipmentId())) {
            throw new RuntimeException("关联设备不存在");
        }
        if (hasArea && !areaRepository.existsById(request.getAreaId())) {
            throw new RuntimeException("关联区域不存在");
        }
        if (request.getCycleType() == null || request.getCycleType() < 1 || request.getCycleType() > 3) {
            throw new RuntimeException("巡检周期不合法");
        }
    }

    private void applyRequest(InspectionPlan plan, InspectionPlanRequest request) {
        plan.setPlanName(request.getPlanName());
        plan.setEquipmentId(request.getEquipmentId());
        plan.setAreaId(request.getAreaId());
        plan.setCycleType(request.getCycleType());
        plan.setNextInspectionDate(request.getNextInspectionDate());
        plan.setInspector(request.getInspector());
        plan.setRemark(request.getRemark());
    }

    private InspectionPlanDTO convertToDTO(InspectionPlan plan) {
        InspectionPlanDTO dto = new InspectionPlanDTO();
        dto.setId(plan.getId());
        dto.setPlanNo(plan.getPlanNo());
        dto.setPlanName(plan.getPlanName());
        dto.setEquipmentId(plan.getEquipmentId());
        dto.setAreaId(plan.getAreaId());
        dto.setCycleType(plan.getCycleType());
        dto.setCycleTypeName(cycleTypeName(plan.getCycleType()));
        dto.setNextInspectionDate(plan.getNextInspectionDate());
        dto.setInspector(plan.getInspector());
        dto.setStatus(plan.getStatus());
        dto.setRemark(plan.getRemark());
        dto.setCreatedAt(plan.getCreatedAt());

        if (plan.getEquipmentId() != null) {
            equipmentRepository.findById(plan.getEquipmentId()).ifPresent(equipment -> {
                dto.setEquipmentNo(equipment.getEquipmentNo());
                dto.setEquipmentName(equipment.getEquipmentName());
            });
        }
        if (plan.getAreaId() != null) {
            areaRepository.findById(plan.getAreaId())
                    .ifPresent(area -> dto.setAreaName(area.getName()));
        }

        return dto;
    }

    private String cycleTypeName(Integer cycleType) {
        if (cycleType == null) {
            return "-";
        }
        return switch (cycleType) {
            case 1 -> "每日";
            case 2 -> "每周";
            case 3 -> "每月";
            default -> "-";
        };
    }
}
