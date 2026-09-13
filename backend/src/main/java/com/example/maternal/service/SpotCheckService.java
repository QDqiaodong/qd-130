
package com.example.maternal.service;

import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.dto.SpotCheckDetailDTO;
import com.example.maternal.dto.SpotCheckRecordDTO;
import com.example.maternal.dto.SpotCheckRecordRequest;
import com.example.maternal.dto.SpotCheckReviewerRequest;
import com.example.maternal.dto.TimelineItem;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.SpotCheckRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.repository.SpotCheckRecordRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpotCheckService {

    /** 仅温奶器可登记温度抽检 */
    public static final String EQUIPMENT_TYPE_WARMER = "温奶器";

    /** 合格水温区间（℃，闭区间）：与前端提示保持一致，结论统一由服务端按此口径判定 */
    public static final BigDecimal MIN_QUALIFIED_TEMP = new BigDecimal("40.00");
    public static final BigDecimal MAX_QUALIFIED_TEMP = new BigDecimal("50.00");

    /** 允许录入的水温物理范围，超过视为填错 */
    private static final BigDecimal MIN_TEMP_LIMIT = new BigDecimal("0.00");
    private static final BigDecimal MAX_TEMP_LIMIT = new BigDecimal("100.00");

    private final SpotCheckRecordRepository spotCheckRecordRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final RepairOrderService repairOrderService;
    private final AreaService areaService;

    /**
     * 登记抽检：抽检记录本身单独提交，保证一定落库；
     * 不合格自动报修走独立事务，若设备已有进行中的报修单等原因失败，仅在返回中提示，
     * 用户仍可在列表中看到不合格标记并手动「去补报修」。
     */
    public SpotCheckRecordDTO createSpotCheck(SpotCheckRecordRequest request) {
        if (request == null) {
            throw new RuntimeException("抽检内容不能为空");
        }
        if (request.getEquipmentId() == null) {
            throw new RuntimeException("抽检温奶器不能为空");
        }
        if (request.getCheckDate() == null) {
            throw new RuntimeException("抽检日期不能为空");
        }
        if (request.getTemperature() == null) {
            throw new RuntimeException("抽检温度不能为空");
        }
        if (request.getInspector() == null || request.getInspector().trim().isEmpty()) {
            throw new RuntimeException("抽检人不能为空");
        }
        if (request.getTemperature().compareTo(MIN_TEMP_LIMIT) < 0
                || request.getTemperature().compareTo(MAX_TEMP_LIMIT) > 0) {
            throw new RuntimeException("抽检温度不合法，请填写0~100℃之间的实测水温");
        }

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        if (!EQUIPMENT_TYPE_WARMER.equals(equipment.getEquipmentType())) {
            throw new RuntimeException("仅在用温奶器可登记温度抽检，所选设备类型为"
                    + (equipment.getEquipmentType() != null ? equipment.getEquipmentType() : "未知"));
        }
        if (equipment.getStatus() != null && equipment.getStatus() != 1) {
            throw new RuntimeException("该温奶器已停用，不能登记抽检");
        }

        if (spotCheckRecordRepository.existsByEquipmentIdAndCheckDate(
                request.getEquipmentId(), request.getCheckDate())) {
            throw new RuntimeException("该温奶器当日已存在抽检记录，请勿重复抽检");
        }

        boolean qualified = isQualified(request.getTemperature());

        SpotCheckRecord record = new SpotCheckRecord();
        record.setSpotCheckNo(CodeGenerator.generateSpotCheckNo());
        record.setEquipmentId(request.getEquipmentId());
        record.setAreaId(equipment.getCurrentAreaId());
        record.setCheckDate(request.getCheckDate());
        record.setTemperature(request.getTemperature());
        record.setQualified(qualified);
        record.setAbnormalDesc(buildAbnormalDesc(qualified, request.getTemperature(), request.getAbnormalDesc()));
        record.setPhotoUrl(request.getPhotoUrl());
        record.setInspector(request.getInspector().trim());
        record.setRemark(request.getRemark());

        SpotCheckRecord saved = spotCheckRecordRepository.save(record);
        SpotCheckRecordDTO dto = convertToDTO(saved);

        // 不合格且勾选了立即报修时自动创建报修单；自动报修失败不影响抽检结论，
        // 通过 repairMessage 提示用户在列表中手动「去补报修」
        if (!qualified && Boolean.TRUE.equals(request.getCreateRepair())) {
            try {
                RepairOrderDTO repair = repairOrderService.createRepairOrderFromSpotCheck(
                        saved.getId(), request.getInspector());
                dto.setRepairOrderId(repair.getId());
                dto.setRepairNo(repair.getRepairNo());
                dto.setRepairStatus(repair.getStatus());
            } catch (RuntimeException ex) {
                dto.setRepairMessage(ex.getMessage());
            }
        }

        return dto;
    }

    /**
     * @param reviewStatus 是否已补复核人筛选：1-已补复核人，0-未补复核人（不合格且复核人未填，
     *                     合格抽检无需复核不计入）；null 不过滤
     */
    public List<SpotCheckRecordDTO> getSpotChecks(LocalDate startDate, LocalDate endDate,
                                                  Long areaId, Boolean qualified, Integer repairStatus,
                                                  Integer reviewStatus) {
        if (reviewStatus != null && reviewStatus != 0 && reviewStatus != 1) {
            throw new RuntimeException("复核人筛选条件不合法，只能选择全部、已补复核人或未补复核人");
        }
        Set<Long> scopeAreaIds = areaService.resolveScopeAreaIds(areaId);
        List<SpotCheckRecord> records = spotCheckRecordRepository.findByFilter(
                startDate, endDate, areaId, qualified, repairStatus, reviewStatus);
        if (scopeAreaIds != null) {
            records = records.stream()
                    .filter(r -> scopeAreaIds.contains(r.getAreaId()))
                    .collect(Collectors.toList());
        }
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 补填当班复核人：仅不合格抽检需要复核人，补填后才允许转报修；已补填过的记录不允许重复提交。
     */
    @Transactional
    public SpotCheckRecordDTO updateReviewer(Long id, SpotCheckReviewerRequest request) {
        SpotCheckRecord record = spotCheckRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("抽检记录不存在"));

        if (Boolean.TRUE.equals(record.getQualified())) {
            throw new RuntimeException("合格抽检无需补填当班复核人");
        }
        if (record.getReviewer() != null && !record.getReviewer().trim().isEmpty()) {
            throw new RuntimeException("该抽检已补填当班复核人，请勿重复提交");
        }
        if (request == null || request.getReviewer() == null || request.getReviewer().trim().isEmpty()) {
            throw new RuntimeException("当班复核人不能为空");
        }
        String reviewer = request.getReviewer().trim();
        if (reviewer.length() > 50) {
            throw new RuntimeException("当班复核人姓名不能超过50字");
        }

        record.setReviewer(reviewer);
        record.setReviewTime(LocalDateTime.now());
        return convertToDTO(spotCheckRecordRepository.save(record));
    }

    public SpotCheckDetailDTO getSpotCheckDetail(Long id) {
        SpotCheckRecord record = spotCheckRecordRepository.findById(id).orElse(null);
        if (record == null) {
            return null;
        }

        SpotCheckDetailDTO detail = new SpotCheckDetailDTO();
        detail.setRecord(convertToDTO(record));

        List<TimelineItem> timeline = new ArrayList<>();
        timeline.add(new TimelineItem(record.getCreatedAt(), "温奶器抽检登记",
                "抽检单号 " + record.getSpotCheckNo()
                        + "，实测水温：" + record.getTemperature() + "℃，结论："
                        + (Boolean.TRUE.equals(record.getQualified()) ? "合格" : "不合格")
                        + (record.getInspector() != null ? "，抽检人：" + record.getInspector() : ""),
                "spotcheck"));

        if (record.getReviewer() != null && !record.getReviewer().trim().isEmpty()) {
            timeline.add(new TimelineItem(
                    record.getReviewTime() != null ? record.getReviewTime() : record.getUpdatedAt(),
                    "补填当班复核人",
                    "当班复核人：" + record.getReviewer() + "，复核后可转报修",
                    "review"));
        }

        repairOrderRepository.findBySpotCheckId(record.getId()).ifPresent(order -> {
            RepairOrderDTO repairDTO = repairOrderService.getRepairById(order.getId());
            detail.setRepairOrder(repairDTO);
            timeline.addAll(repairDTO.getTimeline().stream()
                    .filter(item -> !"spotcheck".equals(item.getType()))
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

    public boolean isQualified(BigDecimal temperature) {
        return temperature != null
                && temperature.compareTo(MIN_QUALIFIED_TEMP) >= 0
                && temperature.compareTo(MAX_QUALIFIED_TEMP) <= 0;
    }

    private String buildAbnormalDesc(boolean qualified, BigDecimal temperature, String customDesc) {
        if (qualified) {
            return null;
        }
        String prefix = "抽检温度" + temperature + "℃，不在合格区间"
                + MIN_QUALIFIED_TEMP + "~" + MAX_QUALIFIED_TEMP + "℃";
        if (customDesc != null && !customDesc.trim().isEmpty()) {
            String desc = prefix + "；" + customDesc.trim();
            return desc.length() > 500 ? desc.substring(0, 500) : desc;
        }
        return prefix;
    }

    private SpotCheckRecordDTO convertToDTO(SpotCheckRecord record) {
        SpotCheckRecordDTO dto = new SpotCheckRecordDTO();
        dto.setId(record.getId());
        dto.setSpotCheckNo(record.getSpotCheckNo());
        dto.setEquipmentId(record.getEquipmentId());
        dto.setAreaId(record.getAreaId());
        dto.setCheckDate(record.getCheckDate());
        dto.setTemperature(record.getTemperature());
        dto.setQualified(record.getQualified());
        dto.setAbnormalDesc(record.getAbnormalDesc());
        dto.setPhotoUrl(record.getPhotoUrl());
        dto.setInspector(record.getInspector());
        dto.setReviewer(record.getReviewer());
        dto.setReviewTime(record.getReviewTime());
        dto.setRemark(record.getRemark());
        dto.setCreatedAt(record.getCreatedAt());

        equipmentRepository.findById(record.getEquipmentId())
                .ifPresent(equipment -> {
                    dto.setEquipmentNo(equipment.getEquipmentNo());
                    dto.setEquipmentName(equipment.getEquipmentName());
                });

        areaRepository.findById(record.getAreaId())
                .ifPresent(area -> dto.setAreaName(area.getName()));

        repairOrderRepository.findBySpotCheckId(record.getId())
                .ifPresent(order -> {
                    dto.setRepairOrderId(order.getId());
                    dto.setRepairNo(order.getRepairNo());
                    dto.setRepairStatus(order.getStatus());
                });

        return dto;
    }
}
