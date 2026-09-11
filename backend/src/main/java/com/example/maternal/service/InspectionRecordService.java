
package com.example.maternal.service;

import com.example.maternal.dto.InspectionDetailDTO;
import com.example.maternal.dto.InspectionRecordDTO;
import com.example.maternal.dto.InspectionRecordRequest;
import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.dto.TimelineItem;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.InspectionPlan;
import com.example.maternal.entity.InspectionRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionPlanRepository;
import com.example.maternal.repository.InspectionRecordRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InspectionRecordService {

    private final InspectionRecordRepository inspectionRecordRepository;
    private final InspectionPlanRepository inspectionPlanRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final RepairOrderService repairOrderService;

    @Transactional
    public InspectionRecordDTO createInspection(InspectionRecordRequest request) {
        if (request.getEquipmentId() == null) {
            throw new RuntimeException("巡检设备不能为空");
        }

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        if (request.getInspectionDate() == null) {
            throw new RuntimeException("巡检日期不能为空");
        }

        if (request.getResult() == null || (request.getResult() != 1 && request.getResult() != 2)) {
            throw new RuntimeException("巡检结果不合法");
        }

        if (request.getResult() == 2 && (request.getAbnormalDesc() == null || request.getAbnormalDesc().isEmpty())) {
            throw new RuntimeException("巡检结果异常时必须填写异常描述");
        }

        if (inspectionRecordRepository.existsByEquipmentIdAndInspectionDate(request.getEquipmentId(), request.getInspectionDate())) {
            throw new RuntimeException("该设备当日已存在巡检记录，请勿重复提交");
        }

        InspectionPlan plan = null;
        if (request.getPlanId() != null) {
            plan = inspectionPlanRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("巡检计划不存在"));
            if (plan.getEquipmentId() != null) {
                // 按设备计划仍须保持设备匹配校验
                if (!plan.getEquipmentId().equals(request.getEquipmentId())) {
                    throw new RuntimeException("所选计划关联的设备与巡检设备不一致");
                }
            } else if (plan.getAreaId() != null) {
                // 按区域计划：设备调配出原区域后，原区域计划不得继续登记该设备
                if (!plan.getAreaId().equals(equipment.getCurrentAreaId())) {
                    String planAreaName = areaRepository.findById(plan.getAreaId())
                            .map(Area::getName).orElse("ID为" + plan.getAreaId() + "的区域");
                    String currentAreaName = areaRepository.findById(equipment.getCurrentAreaId())
                            .map(Area::getName).orElse("其他区域");
                    throw new RuntimeException("设备当前不在计划所属区域「" + planAreaName
                            + "」（当前区域：" + currentAreaName + "），不能使用该区域巡检计划登记，请选择设备当前区域的计划或不关联计划登记");
                }
            }
        }

        InspectionRecord record = new InspectionRecord();
        record.setInspectionNo(CodeGenerator.generateInspectionNo());
        record.setPlanId(request.getPlanId());
        record.setEquipmentId(request.getEquipmentId());
        record.setAreaId(equipment.getCurrentAreaId());
        record.setInspectionDate(request.getInspectionDate());
        record.setResult(request.getResult());
        record.setAbnormalDesc(request.getAbnormalDesc());
        record.setPhotoUrl(request.getPhotoUrl());
        record.setInspector(request.getInspector());
        record.setRemark(request.getRemark());

        InspectionRecord saved = inspectionRecordRepository.save(record);

        if (plan != null) {
            plan.setNextInspectionDate(nextDate(request.getInspectionDate(), plan.getCycleType()));
            inspectionPlanRepository.save(plan);
        }

        InspectionRecordDTO dto = convertToDTO(saved);

        if (request.getResult() == 2 && Boolean.TRUE.equals(request.getCreateRepair())) {
            RepairOrderDTO repair = repairOrderService.createRepairOrder(saved.getId(), request.getInspector());
            dto.setRepairOrderId(repair.getId());
            dto.setRepairNo(repair.getRepairNo());
            dto.setRepairStatus(repair.getStatus());
        }

        return dto;
    }

    public List<InspectionRecordDTO> getInspections(LocalDate startDate, LocalDate endDate, Long areaId,
                                                    Integer result, Integer repairStatus) {
        return inspectionRecordRepository.findByFilter(startDate, endDate, areaId, result, repairStatus).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public InspectionDetailDTO getInspectionDetail(Long id) {
        InspectionRecord record = inspectionRecordRepository.findById(id).orElse(null);
        if (record == null) {
            return null;
        }

        InspectionDetailDTO detail = new InspectionDetailDTO();
        detail.setRecord(convertToDTO(record));

        List<TimelineItem> timeline = new ArrayList<>();
        timeline.add(new TimelineItem(record.getCreatedAt(), "提交巡检记录",
                "巡检单号 " + record.getInspectionNo() + "，结果：" + (record.getResult() == 1 ? "正常" : "异常") +
                        (record.getInspector() != null ? "，巡检员：" + record.getInspector() : ""), "inspection"));

        repairOrderRepository.findByInspectionId(record.getId()).ifPresent(order -> {
            RepairOrderDTO repairDTO = repairOrderService.getRepairById(order.getId());
            detail.setRepairOrder(repairDTO);
            timeline.addAll(repairDTO.getTimeline().stream()
                    .filter(item -> !"inspection".equals(item.getType()))
                    .collect(Collectors.toList()));
        });

        timeline.sort((a, b) -> {
            if (a.getTime() == null && b.getTime() == null) return 0;
            if (a.getTime() == null) return 1;
            if (b.getTime() == null) return -1;
            return a.getTime().compareTo(b.getTime());
        });
        detail.setTimeline(timeline);
        return detail;
    }

    private LocalDate nextDate(LocalDate base, Integer cycleType) {
        if (cycleType == null) {
            return base;
        }
        return switch (cycleType) {
            case 1 -> base.plusDays(1);
            case 2 -> base.plusWeeks(1);
            case 3 -> base.plusMonths(1);
            default -> base;
        };
    }

    private InspectionRecordDTO convertToDTO(InspectionRecord record) {
        InspectionRecordDTO dto = new InspectionRecordDTO();
        dto.setId(record.getId());
        dto.setInspectionNo(record.getInspectionNo());
        dto.setPlanId(record.getPlanId());
        dto.setEquipmentId(record.getEquipmentId());
        dto.setAreaId(record.getAreaId());
        dto.setInspectionDate(record.getInspectionDate());
        dto.setResult(record.getResult());
        dto.setAbnormalDesc(record.getAbnormalDesc());
        dto.setPhotoUrl(record.getPhotoUrl());
        dto.setInspector(record.getInspector());
        dto.setRemark(record.getRemark());
        dto.setCreatedAt(record.getCreatedAt());

        if (record.getPlanId() != null) {
            inspectionPlanRepository.findById(record.getPlanId())
                    .ifPresent(plan -> dto.setPlanName(plan.getPlanName()));
        }

        equipmentRepository.findById(record.getEquipmentId())
                .ifPresent(equipment -> {
                    dto.setEquipmentNo(equipment.getEquipmentNo());
                    dto.setEquipmentName(equipment.getEquipmentName());
                });

        areaRepository.findById(record.getAreaId())
                .ifPresent(area -> dto.setAreaName(area.getName()));

        repairOrderRepository.findByInspectionId(record.getId())
                .ifPresent(order -> {
                    dto.setRepairOrderId(order.getId());
                    dto.setRepairNo(order.getRepairNo());
                    dto.setRepairStatus(order.getStatus());
                });

        return dto;
    }
}
