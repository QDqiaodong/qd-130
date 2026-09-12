
package com.example.maternal.service;

import com.example.maternal.dto.OverdueRepairsDTO;
import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.dto.RepairStatusRequest;
import com.example.maternal.dto.RepairUrgeRequest;
import com.example.maternal.dto.TimelineItem;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.InspectionRecord;
import com.example.maternal.entity.RepairOrder;
import com.example.maternal.entity.SpotCheckRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionRecordRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.repository.SpotCheckRecordRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairOrderService {

    private final RepairOrderRepository repairOrderRepository;
    private final InspectionRecordRepository inspectionRecordRepository;
    private final SpotCheckRecordRepository spotCheckRecordRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final AreaService areaService;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_REPAIRING = 1;
    public static final int STATUS_RESTORED = 2;

    /** 约定等待小时数上限（约一年），防止误填超大值 */
    public static final int MAX_WAIT_HOURS = 8760;

    /**
     * 超时催办统计：仍停在待处理且等待时长达到区域约定小时数的报修单。
     * 件数与清单来自同一批过滤结果，保证刷新后件数与清单行数一致。
     */
    public OverdueRepairsDTO getOverdueRepairs(Integer waitHours, Long areaId) {
        if (waitHours == null || waitHours <= 0) {
            throw new RuntimeException("约定等待小时数必须大于0");
        }
        if (waitHours > MAX_WAIT_HOURS) {
            throw new RuntimeException("约定等待小时数不能超过" + MAX_WAIT_HOURS + "（约一年）");
        }

        Area selectedArea = null;
        if (areaId != null) {
            selectedArea = areaRepository.findById(areaId)
                    .orElseThrow(() -> new RuntimeException("所选区域不存在"));
        }
        Set<Long> scopeAreaIds = areaService.resolveScopeAreaIds(areaId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(waitHours);
        List<RepairOrderDTO> orders = repairOrderRepository.findOverduePending(cutoff).stream()
                .filter(order -> scopeAreaIds == null || scopeAreaIds.contains(order.getAreaId()))
                .map(order -> {
                    RepairOrderDTO dto = convertToDTO(order);
                    dto.setWaitedHours(Math.max(0L, ChronoUnit.HOURS.between(order.getCreatedAt(), now)));
                    return dto;
                })
                .collect(Collectors.toList());

        OverdueRepairsDTO result = new OverdueRepairsDTO();
        result.setWaitHours(waitHours);
        result.setAreaId(areaId);
        result.setAreaName(selectedArea != null ? selectedArea.getName() : null);
        result.setOrders(orders);
        result.setOverdueCount((long) orders.size());
        return result;
    }

    /**
     * 催办：仅允许对待处理单操作，可改跟进人并留下催办说明（不改变报修单状态）。
     */
    @Transactional
    public RepairOrderDTO urgeRepair(Long id, RepairUrgeRequest request) {
        RepairOrder order = repairOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("报修单不存在"));

        if (order.getStatus() == null || order.getStatus() != STATUS_PENDING) {
            throw new RuntimeException("仅待处理的报修单才能催办，当前状态为" + statusName(order.getStatus()));
        }
        if (request.getUrgeNote() == null || request.getUrgeNote().trim().isEmpty()) {
            throw new RuntimeException("催办说明不能为空");
        }

        if (request.getFollowUpPerson() != null && !request.getFollowUpPerson().trim().isEmpty()) {
            order.setRepairman(request.getFollowUpPerson().trim());
        }
        order.setUrgeNote(request.getUrgeNote().trim());
        order.setUrgeTime(LocalDateTime.now());

        return convertToDTO(repairOrderRepository.save(order));
    }

    @Transactional
    public RepairOrderDTO createRepairOrder(Long inspectionId, String reporter) {
        InspectionRecord record = inspectionRecordRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("巡检记录不存在"));

        if (record.getResult() == null || record.getResult() != 2) {
            throw new RuntimeException("仅巡检结果为异常的记录才能创建报修单");
        }

        // 异常巡检须先由值班复核：属实才允许转报修，不属实或未复核都不能报修
        if (record.getReviewResult() == null) {
            throw new RuntimeException("该异常巡检尚未复核，需值班复核属实后才能转报修");
        }
        if (record.getReviewResult() != InspectionRecordService.REVIEW_RESULT_CONFIRMED) {
            throw new RuntimeException("复核结论为不属实，该巡检记录不能转报修");
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

    /**
     * 温奶器抽检不合格一键补报修：抽检结论必须为不合格，同一抽检记录/设备不得重复创建进行中的报修单。
     */
    @Transactional
    public RepairOrderDTO createRepairOrderFromSpotCheck(Long spotCheckId, String reporter) {
        SpotCheckRecord record = spotCheckRecordRepository.findById(spotCheckId)
                .orElseThrow(() -> new RuntimeException("抽检记录不存在"));

        if (Boolean.TRUE.equals(record.getQualified())) {
            throw new RuntimeException("仅抽检结论为不合格的记录才能创建报修单");
        }

        if (repairOrderRepository.existsBySpotCheckId(spotCheckId)) {
            throw new RuntimeException("该抽检记录已创建报修单，请勿重复提交");
        }

        if (hasActiveRepair(record.getEquipmentId())) {
            throw new RuntimeException("该设备存在未完成的报修单，请勿重复报修");
        }

        RepairOrder order = new RepairOrder();
        order.setRepairNo(CodeGenerator.generateRepairNo());
        order.setSpotCheckId(spotCheckId);
        order.setEquipmentId(record.getEquipmentId());
        order.setAreaId(record.getAreaId());
        order.setFaultDesc(record.getAbnormalDesc() != null && !record.getAbnormalDesc().isEmpty()
                ? record.getAbnormalDesc()
                : "温奶器抽检温度不合格，实测" + record.getTemperature() + "℃");
        order.setPhotoUrl(record.getPhotoUrl());
        order.setStatus(STATUS_PENDING);
        order.setReporter(reporter != null && !reporter.isEmpty() ? reporter : record.getInspector());

        return convertToDTO(repairOrderRepository.save(order));
    }

    public List<RepairOrderDTO> getRepairs(LocalDate startDate, LocalDate endDate, Long areaId, Integer status) {
        return getRepairs(startDate, endDate, areaId, status, null);
    }

    public List<RepairOrderDTO> getRepairs(LocalDate startDate, LocalDate endDate, Long areaId,
                                           Integer status, String equipmentType) {
        return getRepairs(startDate, endDate, areaId, status, equipmentType, null);
    }

    /**
     * @param equipmentCurrentAreaId 看板下钻使用：按设备「当前所在区域」匹配报修单，
     *                               与看板当前维修数/待处理数口径一致；为 null 时回退到报修单记录区域
     */
    public List<RepairOrderDTO> getRepairs(LocalDate startDate, LocalDate endDate, Long areaId,
                                           Integer status, String equipmentType,
                                           Long equipmentCurrentAreaId) {
        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        java.util.Set<Long> recordScopeAreaIds = areaService.resolveScopeAreaIds(areaId);
        java.util.Set<Long> currentScopeAreaIds = areaService.resolveScopeAreaIds(equipmentCurrentAreaId);
        return repairOrderRepository.findForDashboard(startTime, endTime, null, equipmentType).stream()
                .filter(order -> status == null || status.equals(order.getStatus()))
                .filter(order -> {
                    if (currentScopeAreaIds != null) {
                        Equipment equipment = equipmentRepository.findById(order.getEquipmentId()).orElse(null);
                        Long resolvedAreaId = equipment != null ? equipment.getCurrentAreaId() : order.getAreaId();
                        return currentScopeAreaIds.contains(resolvedAreaId);
                    }
                    return recordScopeAreaIds == null || recordScopeAreaIds.contains(order.getAreaId());
                })
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
        if (order.getInspectionId() != null) {
            inspectionRecordRepository.findById(order.getInspectionId()).ifPresent(record ->
                    timeline.add(new TimelineItem(record.getCreatedAt(), "巡检异常",
                            "巡检单号 " + record.getInspectionNo() + "，结果：异常" +
                                    (record.getAbnormalDesc() != null ? "，" + record.getAbnormalDesc() : ""), "inspection")));
        }
        if (order.getSpotCheckId() != null) {
            spotCheckRecordRepository.findById(order.getSpotCheckId()).ifPresent(record ->
                    timeline.add(new TimelineItem(record.getCreatedAt(), "温奶器抽检不合格",
                            "抽检单号 " + record.getSpotCheckNo() + "，实测水温：" + record.getTemperature() + "℃" +
                                    (record.getAbnormalDesc() != null ? "，" + record.getAbnormalDesc() : ""), "spotcheck")));
        }
        timeline.add(new TimelineItem(order.getCreatedAt(), "创建报修单",
                "报修单号 " + order.getRepairNo() + "，报修人：" + (order.getReporter() != null ? order.getReporter() : "-"), "repair"));
        if (order.getUrgeTime() != null) {
            timeline.add(new TimelineItem(order.getUrgeTime(), "超时催办",
                    "跟进人：" + (order.getRepairman() != null ? order.getRepairman() : "-")
                            + "，催办说明：" + (order.getUrgeNote() != null ? order.getUrgeNote() : "-"), "urge"));
        }
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
        dto.setSpotCheckId(order.getSpotCheckId());
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
        dto.setUrgeNote(order.getUrgeNote());
        dto.setUrgeTime(order.getUrgeTime());
        dto.setCreatedAt(order.getCreatedAt());

        if (order.getInspectionId() != null) {
            inspectionRecordRepository.findById(order.getInspectionId())
                    .ifPresent(record -> dto.setInspectionNo(record.getInspectionNo()));
        }

        if (order.getSpotCheckId() != null) {
            spotCheckRecordRepository.findById(order.getSpotCheckId())
                    .ifPresent(record -> dto.setSpotCheckNo(record.getSpotCheckNo()));
        }

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
