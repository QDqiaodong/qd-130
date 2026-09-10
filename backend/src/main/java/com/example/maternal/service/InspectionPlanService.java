
package com.example.maternal.service;

import com.example.maternal.dto.InspectionPlanDTO;
import com.example.maternal.dto.InspectionPlanRequest;
import com.example.maternal.entity.InspectionPlan;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionPlanRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InspectionPlanService {

    private final InspectionPlanRepository inspectionPlanRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;

    public List<InspectionPlanDTO> getAllPlans() {
        return inspectionPlanRepository.findAllOrdered().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
        applyRequest(plan, request);
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }

        return convertToDTO(inspectionPlanRepository.save(plan));
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
