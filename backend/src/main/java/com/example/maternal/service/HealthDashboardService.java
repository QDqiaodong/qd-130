
package com.example.maternal.service;

import com.example.maternal.dto.HealthAreaRankDTO;
import com.example.maternal.dto.HealthDashboardDTO;
import com.example.maternal.dto.HealthMetricsDTO;
import com.example.maternal.dto.HealthTypeRankDTO;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.InspectionPlan;
import com.example.maternal.entity.InspectionRecord;
import com.example.maternal.entity.RepairOrder;
import com.example.maternal.entity.TransferRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionPlanRepository;
import com.example.maternal.repository.InspectionRecordRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.repository.TransferRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthDashboardService {

    private static final int REPAIR_STATUS_PENDING = 0;
    private static final int REPAIR_STATUS_REPAIRING = 1;
    private static final long MAX_RANGE_DAYS = 366L;

    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final InspectionRecordRepository inspectionRecordRepository;
    private final InspectionPlanRepository inspectionPlanRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final TransferRecordRepository transferRecordRepository;

    public HealthDashboardDTO getDashboard(LocalDate startDate, LocalDate endDate, Long areaId) {
        LocalDate today = LocalDate.now();
        if (startDate == null && endDate == null) {
            endDate = today;
            startDate = today.minusDays(29);
        } else if (startDate == null || endDate == null) {
            throw new RuntimeException("开始日期和结束日期必须同时填写");
        }
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_RANGE_DAYS) {
            throw new RuntimeException("日期范围最长不能超过366天");
        }
        if (endDate.isAfter(today)) {
            throw new RuntimeException("结束日期不能晚于今天");
        }

        Area selectedArea = null;
        Set<Long> scopeAreaIds = null;
        if (areaId != null) {
            selectedArea = areaRepository.findById(areaId)
                    .orElseThrow(() -> new RuntimeException("所选区域不存在"));
            scopeAreaIds = areaRepository.findAll().stream()
                    .filter(a -> isSameOrDescendant(a, selectedArea))
                    .map(Area::getId)
                    .collect(Collectors.toSet());
        }

        List<Equipment> equipmentList = equipmentRepository.findAll().stream()
                .filter(e -> scopeAreaIds == null || scopeAreaIds.contains(e.getCurrentAreaId()))
                .collect(Collectors.toList());

        List<InspectionRecord> inspections = inspectionRecordRepository
                .findForDashboard(startDate, endDate, null, null, null).stream()
                .filter(r -> scopeAreaIds == null || scopeAreaIds.contains(r.getAreaId()))
                .collect(Collectors.toList());

        Map<Long, Equipment> scopeEquipmentMap = equipmentMap(equipmentList);
        List<RepairOrder> activeRepairs = repairOrderRepository
                .findForDashboard(null, null, null, null).stream()
                .filter(o -> o.getStatus() != null
                        && (o.getStatus() == REPAIR_STATUS_PENDING || o.getStatus() == REPAIR_STATUS_REPAIRING))
                .filter(o -> {
                    Equipment e = scopeEquipmentMap.get(o.getEquipmentId());
                    return e != null && (scopeAreaIds == null || scopeAreaIds.contains(e.getCurrentAreaId()));
                })
                .collect(Collectors.toList());

        List<TransferRecord> transfers = transferRecordRepository
                .findForDashboard(startDate, endDate, null, null).stream()
                .filter(t -> scopeAreaIds == null
                        || scopeAreaIds.contains(t.getFromAreaId())
                        || scopeAreaIds.contains(t.getToAreaId()))
                .collect(Collectors.toList());

        Set<Long> scopeEquipmentIds = equipmentList.stream()
                .map(Equipment::getId)
                .collect(Collectors.toSet());
        List<InspectionPlan> plans = inspectionPlanRepository.findByStatus(1).stream()
                .filter(p -> p.getAreaId() == null || scopeAreaIds == null || scopeAreaIds.contains(p.getAreaId()))
                .filter(p -> p.getEquipmentId() == null || scopeEquipmentIds.contains(p.getEquipmentId()))
                .collect(Collectors.toList());

        Map<Long, Area> areaMap = areaRepository.findAll().stream()
                .collect(Collectors.toMap(Area::getId, a -> a));

        HealthDashboardDTO dashboard = new HealthDashboardDTO();
        dashboard.setStartDate(startDate);
        dashboard.setEndDate(endDate);
        dashboard.setAreaId(areaId);
        dashboard.setAreaName(selectedArea != null ? selectedArea.getName() : null);
        dashboard.setMetrics(buildMetrics(equipmentList, inspections, activeRepairs, transfers, plans,
                startDate, endDate));
        dashboard.setAreaRankings(buildAreaRankings(equipmentList, inspections, activeRepairs, transfers,
                plans, areaMap, startDate, endDate));
        dashboard.setTypeRankings(buildTypeRankings(equipmentList, inspections, activeRepairs, transfers,
                plans, startDate, endDate));
        return dashboard;
    }

    private Map<Long, Equipment> equipmentMap(List<Equipment> equipmentList) {
        return equipmentList.stream().collect(Collectors.toMap(Equipment::getId, e -> e));
    }

    private HealthMetricsDTO buildMetrics(List<Equipment> equipmentList,
                                          List<InspectionRecord> inspections,
                                          List<RepairOrder> activeRepairs,
                                          List<TransferRecord> transfers,
                                          List<InspectionPlan> plans,
                                          LocalDate startDate,
                                          LocalDate endDate) {
        long total = equipmentList.size();
        long repairing = activeRepairs.stream()
                .filter(o -> o.getStatus() == REPAIR_STATUS_REPAIRING).count();
        long pending = activeRepairs.stream()
                .filter(o -> o.getStatus() == REPAIR_STATUS_PENDING).count();
        long inspectionTotal = inspections.size();
        long abnormal = inspections.stream().filter(r -> r.getResult() != null && r.getResult() == 2).count();
        long planned = countPlannedSlots(plans, equipmentList, startDate, endDate);

        HealthMetricsDTO metrics = new HealthMetricsDTO();
        metrics.setTotalEquipmentCount(total);
        metrics.setRepairingCount(repairing);
        metrics.setInspectionCount(inspectionTotal);
        metrics.setInspectionCompletedCount(inspectionTotal);
        metrics.setAbnormalCount(abnormal);
        metrics.setInspectionPlannedCount(planned);
        metrics.setInspectionCompletionRate(rate(inspectionTotal, planned));
        metrics.setAbnormalRate(rate(abnormal, inspectionTotal));
        metrics.setPendingRepairCount(pending);
        metrics.setTransferCount((long) transfers.size());
        return metrics;
    }

    private List<HealthAreaRankDTO> buildAreaRankings(List<Equipment> equipmentList,
                                                      List<InspectionRecord> inspections,
                                                      List<RepairOrder> activeRepairs,
                                                      List<TransferRecord> transfers,
                                                      List<InspectionPlan> plans,
                                                      Map<Long, Area> areaMap,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        // 只统计直接挂有设备的区域（母婴室叶子区域），避免父区域与子区域重复计数
        Map<Long, List<Equipment>> equipmentByArea = equipmentList.stream()
                .collect(Collectors.groupingBy(Equipment::getCurrentAreaId));
        Map<Long, Equipment> scopeEquipmentMap = equipmentMap(equipmentList);

        List<HealthAreaRankDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<Equipment>> entry : equipmentByArea.entrySet()) {
            Long areaId = entry.getKey();
            Set<Long> areaEquipmentIds = entry.getValue().stream()
                    .map(Equipment::getId).collect(Collectors.toSet());

            List<InspectionRecord> areaInspections = inspections.stream()
                    .filter(r -> areaId.equals(r.getAreaId()))
                    .collect(Collectors.toList());
            long areaAbnormal = areaInspections.stream()
                    .filter(r -> r.getResult() != null && r.getResult() == 2).count();

            List<RepairOrder> areaRepairs = activeRepairs.stream()
                    .filter(o -> areaId.equals(resolvedRepairAreaId(o, scopeEquipmentMap)))
                    .collect(Collectors.toList());
            long areaRepairing = areaRepairs.stream()
                    .filter(o -> o.getStatus() == REPAIR_STATUS_REPAIRING).count();
            long areaPending = areaRepairs.stream()
                    .filter(o -> o.getStatus() == REPAIR_STATUS_PENDING).count();

            long transferOut = transfers.stream().filter(t -> areaId.equals(t.getFromAreaId())).count();
            long transferIn = transfers.stream().filter(t -> areaId.equals(t.getToAreaId())).count();

            List<InspectionPlan> areaPlans = plans.stream()
                    .filter(p -> areaId.equals(p.getAreaId())
                            || (p.getEquipmentId() != null && areaEquipmentIds.contains(p.getEquipmentId())))
                    .collect(Collectors.toList());
            long planned = countPlannedSlots(areaPlans, entry.getValue(), startDate, endDate);

            HealthAreaRankDTO dto = new HealthAreaRankDTO();
            dto.setAreaId(areaId);
            dto.setAreaName(areaMap.containsKey(areaId) ? areaMap.get(areaId).getName() : "未知区域");
            dto.setTotalEquipmentCount((long) entry.getValue().size());
            dto.setRepairingCount(areaRepairing);
            dto.setPendingRepairCount(areaPending);
            dto.setInspectionCompletedCount((long) areaInspections.size());
            dto.setInspectionPlannedCount(planned);
            dto.setInspectionCompletionRate(rate(areaInspections.size(), planned));
            dto.setAbnormalCount(areaAbnormal);
            dto.setAbnormalRate(rate(areaAbnormal, areaInspections.size()));
            dto.setTransferOutCount(transferOut);
            dto.setTransferInCount(transferIn);
            dto.setTransferCount(transferOut + transferIn);
            result.add(dto);
        }

        result.sort(Comparator.comparing(HealthAreaRankDTO::getTotalEquipmentCount).reversed()
                .thenComparing(HealthAreaRankDTO::getAreaId));
        return result;
    }

    private List<HealthTypeRankDTO> buildTypeRankings(List<Equipment> equipmentList,
                                                      List<InspectionRecord> inspections,
                                                      List<RepairOrder> activeRepairs,
                                                      List<TransferRecord> transfers,
                                                      List<InspectionPlan> plans,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        Map<String, List<Equipment>> equipmentByType = equipmentList.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEquipmentType() != null ? e.getEquipmentType() : "未分类",
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<HealthTypeRankDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<Equipment>> entry : equipmentByType.entrySet()) {
            String type = entry.getKey();
            Set<Long> typeEquipmentIds = entry.getValue().stream()
                    .map(Equipment::getId).collect(Collectors.toSet());

            List<InspectionRecord> typeInspections = inspections.stream()
                    .filter(r -> typeEquipmentIds.contains(r.getEquipmentId()))
                    .collect(Collectors.toList());
            long typeAbnormal = typeInspections.stream()
                    .filter(r -> r.getResult() != null && r.getResult() == 2).count();

            List<RepairOrder> typeRepairs = activeRepairs.stream()
                    .filter(o -> typeEquipmentIds.contains(o.getEquipmentId()))
                    .collect(Collectors.toList());
            long typeRepairing = typeRepairs.stream()
                    .filter(o -> o.getStatus() == REPAIR_STATUS_REPAIRING).count();
            long typePending = typeRepairs.stream()
                    .filter(o -> o.getStatus() == REPAIR_STATUS_PENDING).count();

            long typeTransfers = transfers.stream()
                    .filter(t -> typeEquipmentIds.contains(t.getEquipmentId())).count();

            List<InspectionPlan> typePlans = plans.stream()
                    .filter(p -> p.getEquipmentId() != null && typeEquipmentIds.contains(p.getEquipmentId()))
                    .collect(Collectors.toList());
            long planned = countPlannedSlots(typePlans, entry.getValue(), startDate, endDate);

            HealthTypeRankDTO dto = new HealthTypeRankDTO();
            dto.setEquipmentType(type);
            dto.setTotalEquipmentCount((long) entry.getValue().size());
            dto.setRepairingCount(typeRepairing);
            dto.setPendingRepairCount(typePending);
            dto.setInspectionCompletedCount((long) typeInspections.size());
            dto.setInspectionPlannedCount(planned);
            dto.setInspectionCompletionRate(rate(typeInspections.size(), planned));
            dto.setAbnormalCount(typeAbnormal);
            dto.setAbnormalRate(rate(typeAbnormal, typeInspections.size()));
            dto.setTransferCount(typeTransfers);
            result.add(dto);
        }

        result.sort(Comparator.comparing(HealthTypeRankDTO::getTotalEquipmentCount).reversed()
                .thenComparing(HealthTypeRankDTO::getEquipmentType));
        return result;
    }

    /**
     * 统计周期内计划应巡检次数：
     * 按设备计划按周期折算应巡检次数；按区域计划按周期乘以区域内在册设备数。
     */
    private long countPlannedSlots(List<InspectionPlan> plans, List<Equipment> scopeEquipments,
                                   LocalDate startDate, LocalDate endDate) {
        long rangeDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        Map<Long, Long> areaDeviceCount = new HashMap<>();
        long slots = 0;
        for (InspectionPlan plan : plans) {
            long cycles = cyclesInRange(plan.getCycleType(), rangeDays);
            if (plan.getEquipmentId() != null) {
                slots += cycles;
            } else if (plan.getAreaId() != null) {
                long deviceCount = areaDeviceCount.computeIfAbsent(plan.getAreaId(),
                        aid -> scopeEquipments.stream()
                                .filter(e -> aid.equals(e.getCurrentAreaId()))
                                .count());
                slots += cycles * deviceCount;
            }
        }
        return slots;
    }

    private long cyclesInRange(Integer cycleType, long rangeDays) {
        if (cycleType == null) {
            return 0;
        }
        return switch (cycleType) {
            case 1 -> rangeDays;
            case 2 -> (rangeDays + 6) / 7;
            case 3 -> (rangeDays + 29) / 30;
            default -> 0;
        };
    }

    private boolean isSameOrDescendant(Area area, Area parent) {
        if (area.getId().equals(parent.getId())) {
            return true;
        }
        String parentPath = parent.getPath();
        return parentPath != null && area.getPath() != null && area.getPath().startsWith(parentPath + "/");
    }

    /**
     * 报修单归属区域：优先按设备当前所在区域统计（与设备总数口径一致），
     * 设备不存在时回退到报修单记录区域。
     */
    private Long resolvedRepairAreaId(RepairOrder order, Map<Long, Equipment> equipmentMap) {
        Equipment equipment = equipmentMap.get(order.getEquipmentId());
        return equipment != null ? equipment.getCurrentAreaId() : order.getAreaId();
    }

    private Double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return null;
        }
        return Math.round(numerator * 1000.0 / denominator) / 10.0;
    }
}
