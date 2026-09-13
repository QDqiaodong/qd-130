
package com.example.maternal.service;

import com.example.maternal.dto.DisinfectionRecordDTO;
import com.example.maternal.dto.DisinfectionRecordRequest;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.DisinfectionRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.DisinfectionRecordRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisinfectionService {

    private final DisinfectionRecordRepository disinfectionRecordRepository;
    private final AreaRepository areaRepository;
    private final AreaService areaService;

    /**
     * 登记当日消毒：闭环标记由服务端按「通风是否做完」统一判定落库；
     * 同一母婴室当天已闭环的记录存在时，禁止再次登记（不能重复闭环）。
     */
    public DisinfectionRecordDTO createDisinfection(DisinfectionRecordRequest request) {
        if (request == null) {
            throw new RuntimeException("消毒登记内容不能为空");
        }
        if (request.getAreaId() == null) {
            throw new RuntimeException("请选择消毒的母婴室");
        }
        if (request.getDisinfectDate() == null) {
            throw new RuntimeException("消毒日期不能为空");
        }
        if (request.getOperator() == null || request.getOperator().trim().isEmpty()) {
            throw new RuntimeException("消毒人不能为空");
        }
        if (request.getFinishTime() == null) {
            throw new RuntimeException("完成时间不能为空");
        }
        if (request.getDisinfectant() == null || request.getDisinfectant().trim().isEmpty()) {
            throw new RuntimeException("消毒液不能为空");
        }
        if (request.getVentilationDone() == null) {
            throw new RuntimeException("请选择通风是否做完");
        }

        Area area = areaRepository.findById(request.getAreaId())
                .orElseThrow(() -> new RuntimeException("母婴室区域不存在"));
        if (area.getStatus() != null && area.getStatus() != 1) {
            throw new RuntimeException("该母婴室区域已停用，不能登记消毒");
        }

        // 通风没做完不能算当日闭环
        boolean closedLoop = Boolean.TRUE.equals(request.getVentilationDone());
        if (!closedLoop && (request.getIncompleteReason() == null
                || request.getIncompleteReason().trim().isEmpty())) {
            throw new RuntimeException("通风未做完时必须填写未完成原因");
        }

        // 同一母婴室当天不能重复闭环：已闭环后当日不可再登记
        if (disinfectionRecordRepository.existsByAreaIdAndDisinfectDateAndClosedLoopTrue(
                request.getAreaId(), request.getDisinfectDate())) {
            throw new RuntimeException("该母婴室当日已完成消毒闭环，同一母婴室当天不能重复闭环");
        }

        DisinfectionRecord record = new DisinfectionRecord();
        record.setDisinfectionNo(CodeGenerator.generateDisinfectionNo());
        record.setAreaId(request.getAreaId());
        record.setDisinfectDate(request.getDisinfectDate());
        record.setOperator(request.getOperator().trim());
        record.setFinishTime(request.getFinishTime());
        record.setDisinfectant(request.getDisinfectant().trim());
        record.setVentilationDone(request.getVentilationDone());
        record.setClosedLoop(closedLoop);
        record.setIncompleteReason(closedLoop ? null : request.getIncompleteReason().trim());
        record.setRemark(request.getRemark());

        DisinfectionRecord saved = disinfectionRecordRepository.save(record);
        return convertToDTO(saved, null);
    }

    public List<DisinfectionRecordDTO> getDisinfections(LocalDate startDate, LocalDate endDate,
                                                        Long areaId, Boolean closedLoop) {
        Set<Long> scopeAreaIds = areaService.resolveInUseScopeAreaIds(areaId);

        // 先按日期/闭环状态取候选记录，再由服务端按「父区域 + 下级在用母婴室」统一过滤。
        // 不能把 areaId 直接下传为精确匹配，否则选择父区域时会漏掉 A1/A2 等下级室。
        List<DisinfectionRecord> records = disinfectionRecordRepository.findByFilter(
                startDate, endDate, null, closedLoop).stream()
                .filter(r -> scopeAreaIds.contains(r.getAreaId()))
                .collect(Collectors.toList());

        // 能否再登记只看当前筛选范围和日期内的闭环记录，确保刷新后与闭环标记同源、口径一致。
        Set<String> closedKeys = disinfectionRecordRepository
                .findByFilter(startDate, endDate, null, true).stream()
                .filter(r -> scopeAreaIds.contains(r.getAreaId()))
                .map(r -> closedKey(r.getAreaId(), r.getDisinfectDate()))
                .collect(Collectors.toSet());

        return records.stream()
                .map(r -> convertToDTO(r, closedKeys))
                .collect(Collectors.toList());
    }

    private String closedKey(Long areaId, LocalDate disinfectDate) {
        return areaId + "_" + disinfectDate;
    }

    private DisinfectionRecordDTO convertToDTO(DisinfectionRecord record, Set<String> closedKeys) {
        DisinfectionRecordDTO dto = new DisinfectionRecordDTO();
        dto.setId(record.getId());
        dto.setDisinfectionNo(record.getDisinfectionNo());
        dto.setAreaId(record.getAreaId());
        dto.setDisinfectDate(record.getDisinfectDate());
        dto.setOperator(record.getOperator());
        dto.setFinishTime(record.getFinishTime());
        dto.setDisinfectant(record.getDisinfectant());
        dto.setVentilationDone(record.getVentilationDone());
        dto.setClosedLoop(record.getClosedLoop());
        dto.setIncompleteReason(record.getIncompleteReason());
        dto.setRemark(record.getRemark());
        dto.setCreatedAt(record.getCreatedAt());

        areaRepository.findById(record.getAreaId())
                .ifPresent(area -> dto.setAreaName(area.getName()));

        boolean closedExists = closedKeys != null
                ? closedKeys.contains(closedKey(record.getAreaId(), record.getDisinfectDate()))
                : disinfectionRecordRepository.existsByAreaIdAndDisinfectDateAndClosedLoopTrue(
                        record.getAreaId(), record.getDisinfectDate());
        dto.setCanRegisterAgain(!closedExists);

        return dto;
    }
}
