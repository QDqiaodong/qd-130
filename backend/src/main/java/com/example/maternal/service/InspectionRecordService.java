
package com.example.maternal.service;

import com.example.maternal.dto.InspectionDetailDTO;
import com.example.maternal.dto.InspectionRecordDTO;
import com.example.maternal.dto.InspectionRecordRequest;
import com.example.maternal.dto.InspectionReviewRequest;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InspectionRecordService {

    /** 复核结论：1-属实，2-不属实 */
    public static final int REVIEW_RESULT_CONFIRMED = 1;
    public static final int REVIEW_RESULT_REJECTED = 2;

    /** 复核状态筛选值：-1 表示待复核（异常且尚未复核） */
    public static final int REVIEW_STATUS_PENDING = -1;

    private final InspectionRecordRepository inspectionRecordRepository;
    private final InspectionPlanRepository inspectionPlanRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final RepairOrderService repairOrderService;
    private final AreaService areaService;

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

        // 异常巡检不再随登记直接报修：需值班复核属实后才允许转报修
        return convertToDTO(saved);
    }

    /**
     * 值班复核：仅已标异常且尚未复核的巡检可复核；结论不属实时必须填写说明。
     * 复核结论落库后不再允许修改，属实才允许转报修，不属实不能再报修。
     */
    @Transactional
    public InspectionRecordDTO reviewInspection(Long id, InspectionReviewRequest request) {
        InspectionRecord record = inspectionRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("巡检记录不存在"));

        if (record.getResult() == null || record.getResult() != 2) {
            throw new RuntimeException("仅巡检结果为异常的记录需要复核");
        }
        if (record.getReviewResult() != null) {
            throw new RuntimeException("该巡检记录已完成复核，请勿重复复核");
        }
        if (request.getReviewer() == null || request.getReviewer().trim().isEmpty()) {
            throw new RuntimeException("复核人不能为空");
        }
        if (request.getReviewTime() == null) {
            throw new RuntimeException("复核时间不能为空");
        }
        if (request.getConfirmed() == null) {
            throw new RuntimeException("请选择复核结论（是否属实）");
        }
        if (Boolean.FALSE.equals(request.getConfirmed())
                && (request.getReviewNote() == null || request.getReviewNote().trim().isEmpty())) {
            throw new RuntimeException("复核结论为不属实时必须填写复核说明");
        }

        record.setReviewer(request.getReviewer().trim());
        record.setReviewTime(request.getReviewTime());
        record.setReviewResult(Boolean.TRUE.equals(request.getConfirmed())
                ? REVIEW_RESULT_CONFIRMED : REVIEW_RESULT_REJECTED);
        record.setReviewNote(request.getReviewNote() != null && !request.getReviewNote().trim().isEmpty()
                ? request.getReviewNote().trim() : null);

        return convertToDTO(inspectionRecordRepository.save(record));
    }

    public List<InspectionRecordDTO> getInspections(LocalDate startDate, LocalDate endDate, Long areaId,
                                                    Integer result, Integer repairStatus) {
        return getInspections(startDate, endDate, areaId, result, repairStatus, null);
    }

    public List<InspectionRecordDTO> getInspections(LocalDate startDate, LocalDate endDate, Long areaId,
                                                    Integer result, Integer repairStatus, String equipmentType) {
        return getInspections(startDate, endDate, areaId, result, repairStatus, equipmentType, null);
    }

    /**
     * @param reviewStatus 复核状态：-1 待复核（异常且未复核），1 属实，2 不属实；null 不过滤
     */
    public List<InspectionRecordDTO> getInspections(LocalDate startDate, LocalDate endDate, Long areaId,
                                                    Integer result, Integer repairStatus, String equipmentType,
                                                    Integer reviewStatus) {
        Set<Long> scopeAreaIds = areaService.resolveScopeAreaIds(areaId);
        List<InspectionRecord> records;
        if (repairStatus == null && equipmentType != null) {
            records = inspectionRecordRepository.findForDashboard(startDate, endDate, null, result, equipmentType);
        } else {
            records = inspectionRecordRepository.findByFilter(startDate, endDate, null, result, repairStatus);
            if (equipmentType != null) {
                Set<Long> typeEquipmentIds = equipmentRepository.findAll().stream()
                        .filter(e -> equipmentType.equals(e.getEquipmentType()))
                        .map(Equipment::getId)
                        .collect(Collectors.toSet());
                records = records.stream()
                        .filter(r -> typeEquipmentIds.contains(r.getEquipmentId()))
                        .collect(Collectors.toList());
            }
        }
        if (scopeAreaIds != null) {
            records = records.stream()
                    .filter(r -> scopeAreaIds.contains(r.getAreaId()))
                    .collect(Collectors.toList());
        }
        if (reviewStatus != null) {
            records = records.stream()
                    .filter(r -> matchesReviewStatus(r, reviewStatus))
                    .collect(Collectors.toList());
        }
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private boolean matchesReviewStatus(InspectionRecord record, Integer reviewStatus) {
        if (reviewStatus == REVIEW_STATUS_PENDING) {
            return record.getResult() != null && record.getResult() == 2 && record.getReviewResult() == null;
        }
        return Objects.equals(record.getReviewResult(), reviewStatus);
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

        if (record.getReviewResult() != null) {
            timeline.add(new TimelineItem(record.getReviewTime(), "值班复核",
                    "复核人：" + (record.getReviewer() != null ? record.getReviewer() : "-")
                            + "，结论：" + (record.getReviewResult() == REVIEW_RESULT_CONFIRMED ? "属实" : "不属实")
                            + (record.getReviewNote() != null && !record.getReviewNote().isEmpty()
                                    ? "，说明：" + record.getReviewNote() : "")
                            + (record.getReviewResult() == REVIEW_RESULT_CONFIRMED ? "，可转报修" : "，不可转报修"),
                    "review"));
        }

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
        dto.setReviewer(record.getReviewer());
        dto.setReviewTime(record.getReviewTime());
        dto.setReviewResult(record.getReviewResult());
        dto.setReviewNote(record.getReviewNote());
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

        // 可否报修与复核结论同源计算，保证列表、详情与刷新后的口径一致
        dto.setCanRepair(record.getResult() != null && record.getResult() == 2
                && record.getReviewResult() != null && record.getReviewResult() == REVIEW_RESULT_CONFIRMED
                && dto.getRepairOrderId() == null);

        return dto;
    }
}
