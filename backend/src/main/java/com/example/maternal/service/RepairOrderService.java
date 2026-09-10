
package com.example.maternal.service;

import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.dto.RepairStatusRequest;
import com.example.maternal.dto.TimelineItem;
import com.example.maternal.entity.InspectionRecord;
import com.example.maternal.entity.RepairOrder;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionRecordRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairOrderService {

    private final RepairOrderRepository repairOrderRepository;
    private final InspectionRecordRepository inspectionRecordRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_REPAIRING = 1;
    public static final int STATUS_RESTORED = 2;

    @Transactional
    public RepairOrderDTO createRepairOrder(Long inspectionId, String reporter) {
        InspectionRecord record = inspectionRecordRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("巡检记录不存在"));

        if (record.getResult() == null || record.getResult() != 2) {
            throw new RuntimeException("仅巡检结果为异常的记录才能创建报修单");
        }

        if (repairOrderRepository.existsByInspectionId(inspectionId)) {
            throw new RuntimeException("该巡检记录已创建报修单，请勿重复提交");
        }

        if (hasActiveRepair(record.getEquipmentId())) {
            throw new RuntimeException("该设备存在未完成的报修单，请勿重复报修");
        }

        RepairOrder order = new RepairOrder();
        order.setRepairNo(CodeGenerator.generateRepairNo());
        order.setInspectionId(inspectionId);
        order.setEquipmentId(record.getEquipmentId());
        order.setAreaId(record.getAreaId());
        order.setFaultDesc(record.getAbnormalDesc());
        order.setPhotoUrl(record.getPhotoUrl());
        order.setStatus(STATUS_PENDING);
        order.setReporter(reporter != null && !reporter.isEmpty() ? reporter : record.getInspector());

        return convertToDTO(repairOrderRepository.save(order));
    }

    public List<RepairOrderDTO> getRepairs(LocalDate startDate, LocalDate endDate, Long areaId, Integer status) {
        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        return repairOrderRepository.findByFilter(startTime, endTime, areaId, status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public RepairOrderDTO getRepairById(Long id) {
        return repairOrderRepository.findById(id)
                .map(order -> {
                    RepairOrderDTO dto = convertToDTO(order);
                    dto.setTimeline(buildTimeline(order));
                    return dto;
                })
                .orElse(null);
    }

    @Transactional
    public RepairOrderDTO updateStatus(Long id, RepairStatusRequest request) {
        RepairOrder order = repairOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("报修单不存在"));

        Integer target = request.getStatus();
        if (target == null) {
            throw new RuntimeException("目标状态不能为空");
        }

        Integer current = order.getStatus();
        if (current.equals(target)) {
            throw new RuntimeException("报修单已处于" + statusName(target) + "状态，请勿重复提交");
        }

        if (current == STATUS_PENDING && target == STATUS_REPAIRING) {
            order.setStatus(STATUS_REPAIRING);
            order.setStartTime(LocalDateTime.now());
            order.setRepairman(request.getRepairman());
        } else if (current == STATUS_REPAIRING && target == STATUS_RESTORED) {
            order.setStatus(STATUS_RESTORED);
            order.setFinishTime(LocalDateTime.now());
            order.setRepairNote(request.getRepairNote());
            if (request.getRepairman() != null && !request.getRepairman().isEmpty()) {
                order.setRepairman(request.getRepairman());
            }
        } else {
            throw new RuntimeException("非法状态流转：报修单只能从" + statusName(current) + "按顺序流转，不允许变更为" + statusName(target));
        }

        return convertToDTO(repairOrderRepository.save(order));
    }

    public boolean hasActiveRepair(Long equipmentId) {
        return repairOrderRepository.existsByEquipmentIdAndStatusIn(equipmentId, List.of(STATUS_PENDING, STATUS_REPAIRING));
    }

    public Integer getActiveRepairStatus(Long equipmentId) {
        return repairOrderRepository
                .findFirstByEquipmentIdAndStatusInOrderByCreatedAtDescIdDesc(equipmentId, List.of(STATUS_PENDING, STATUS_REPAIRING))
                .map(RepairOrder::getStatus)
                .orElse(null);
    }

    public List<TimelineItem> buildTimeline(RepairOrder order) {
        List<TimelineItem> timeline = new ArrayList<>();
        inspectionRecordRepository.findById(order.getInspectionId()).ifPresent(record ->
                timeline.add(new TimelineItem(record.getCreatedAt(), "巡检异常",
                        "巡检单号 " + record.getInspectionNo() + "，结果：异常" +
                                (record.getAbnormalDesc() != null ? "，" + record.getAbnormalDesc() : ""), "inspection")));
        timeline.add(new TimelineItem(order.getCreatedAt(), "创建报修单",
                "报修单号 " + order.getRepairNo() + "，报修人：" + (order.getReporter() != null ? order.getReporter() : "-"), "repair"));
        if (order.getStartTime() != null) {
            timeline.add(new TimelineItem(order.getStartTime(), "开始维修",
                    "维修人：" + (order.getRepairman() != null ? order.getRepairman() : "-") + "，设备维修期间不可调配", "repair"));
        }
        if (order.getFinishTime() != null) {
            timeline.add(new TimelineItem(order.getFinishTime(), "维修完成，设备恢复",
                    (order.getRepairNote() != null ? order.getRepairNote() : "设备已恢复正常，可重新调配"), "repair"));
        }
        return timeline;
    }

    private String statusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case STATUS_PENDING -> "待处理";
            case STATUS_REPAIRING -> "维修中";
            case STATUS_RESTORED -> "已恢复";
            default -> "未知";
        };
    }

    private RepairOrderDTO convertToDTO(RepairOrder order) {
        RepairOrderDTO dto = new RepairOrderDTO();
        dto.setId(order.getId());
        dto.setRepairNo(order.getRepairNo());
        dto.setInspectionId(order.getInspectionId());
        dto.setEquipmentId(order.getEquipmentId());
        dto.setAreaId(order.getAreaId());
        dto.setFaultDesc(order.getFaultDesc());
        dto.setPhotoUrl(order.getPhotoUrl());
        dto.setStatus(order.getStatus());
        dto.setReporter(order.getReporter());
        dto.setRepairman(order.getRepairman());
        dto.setStartTime(order.getStartTime());
        dto.setFinishTime(order.getFinishTime());
        dto.setRepairNote(order.getRepairNote());
        dto.setCreatedAt(order.getCreatedAt());

        inspectionRecordRepository.findById(order.getInspectionId())
                .ifPresent(record -> dto.setInspectionNo(record.getInspectionNo()));

        equipmentRepository.findById(order.getEquipmentId())
                .ifPresent(equipment -> {
                    dto.setEquipmentNo(equipment.getEquipmentNo());
                    dto.setEquipmentName(equipment.getEquipmentName());
                });

        areaRepository.findById(order.getAreaId())
                .ifPresent(area -> dto.setAreaName(area.getName()));

        return dto;
    }
}
