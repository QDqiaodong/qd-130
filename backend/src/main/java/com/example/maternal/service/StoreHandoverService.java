
package com.example.maternal.service;

import com.example.maternal.dto.StoreHandoverDTO;
import com.example.maternal.dto.StoreHandoverRequest;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.StoreHandover;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.StoreHandoverRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreHandoverService {

    public static final int TYPE_OPEN = 1;
    public static final int TYPE_CLOSE = 2;

    public static final int RESULT_FAIL = 0;
    public static final int RESULT_PASS = 1;

    private final StoreHandoverRepository storeHandoverRepository;
    private final AreaRepository areaRepository;
    private final AreaService areaService;

    @Transactional
    public StoreHandoverDTO createHandover(StoreHandoverRequest request) {
        if (request.getAreaId() == null) {
            throw new RuntimeException("交接区域不能为空");
        }
        Area area = areaRepository.findById(request.getAreaId())
                .orElseThrow(() -> new RuntimeException("交接区域不存在"));

        if (request.getHandoverDate() == null) {
            throw new RuntimeException("交接日期不能为空");
        }
        if (request.getHandoverType() == null
                || (request.getHandoverType() != TYPE_OPEN && request.getHandoverType() != TYPE_CLOSE)) {
            throw new RuntimeException("交接类型不合法，须为开店交接或闭店交接");
        }
        if (request.getWarmerInPlace() == null
                || (request.getWarmerInPlace() != 0 && request.getWarmerInPlace() != 1)) {
            throw new RuntimeException("请确认温奶器是否在位");
        }
        if (request.getCareTableClean() == null
                || (request.getCareTableClean() != 0 && request.getCareTableClean() != 1)) {
            throw new RuntimeException("请确认护理台是否擦净");
        }
        if (request.getHandoverPerson() == null || request.getHandoverPerson().trim().isEmpty()) {
            throw new RuntimeException("交接人不能为空");
        }
        if (request.getKeyReceiver() == null || request.getKeyReceiver().trim().isEmpty()) {
            throw new RuntimeException(request.getHandoverType() == TYPE_CLOSE
                    ? "请填写钥匙交给谁" : "请填写钥匙接收人");
        }

        if (storeHandoverRepository.existsByAreaIdAndHandoverDateAndHandoverType(
                request.getAreaId(), request.getHandoverDate(), request.getHandoverType())) {
            throw new RuntimeException("区域「" + area.getName() + "」" + request.getHandoverDate()
                    + (request.getHandoverType() == TYPE_OPEN ? "开店" : "闭店")
                    + "交接已登记，请勿重复提交");
        }

        // 交接结论由检查项服务端统一计算：温奶器在位且护理台擦净才算通过
        int result = (request.getWarmerInPlace() == 1 && request.getCareTableClean() == 1)
                ? RESULT_PASS : RESULT_FAIL;

        if (result == RESULT_FAIL
                && (request.getAbnormalNote() == null || request.getAbnormalNote().trim().isEmpty())) {
            throw new RuntimeException("温奶器不在位或护理台未擦净时，必须填写异常说明");
        }

        StoreHandover record = new StoreHandover();
        record.setHandoverNo(CodeGenerator.generateHandoverNo());
        record.setAreaId(request.getAreaId());
        record.setHandoverDate(request.getHandoverDate());
        record.setHandoverType(request.getHandoverType());
        record.setWarmerInPlace(request.getWarmerInPlace());
        record.setCareTableClean(request.getCareTableClean());
        record.setKeyReceiver(request.getKeyReceiver().trim());
        record.setAbnormalNote(trimToNull(request.getAbnormalNote()));
        record.setHandoverPerson(request.getHandoverPerson().trim());
        record.setRemark(trimToNull(request.getRemark()));
        record.setResult(result);

        StoreHandover saved = storeHandoverRepository.save(record);
        return convertToDTO(saved, null);
    }

    public List<StoreHandoverDTO> getHandovers(LocalDate startDate, LocalDate endDate, Long areaId,
                                               Integer handoverType, Integer result) {
        Set<Long> scopeAreaIds = areaService.resolveScopeAreaIds(areaId);
        List<StoreHandover> records = storeHandoverRepository
                .findByFilter(startDate, endDate, null, handoverType, result);
        if (scopeAreaIds != null) {
            records = records.stream()
                    .filter(h -> scopeAreaIds.contains(h.getAreaId()))
                    .collect(Collectors.toList());
        }

        // 同一批列表中每个区域只计算一次当前可调配状态，保证列表与设备调配口径一致
        Map<Long, Boolean> transferableCache = new HashMap<>();
        return records.stream()
                .map(h -> convertToDTO(h, transferableCache))
                .collect(Collectors.toList());
    }

    public StoreHandoverDTO getHandoverById(Long id) {
        return storeHandoverRepository.findById(id)
                .map(h -> convertToDTO(h, null))
                .orElse(null);
    }

    /**
     * 设备可否调出（以当天为止最近一次交接派生，单一数据源）：
     * 无交接记录默认可调配；最近一次为闭店交接（含未完成）则锁定；
     * 最近一次为开店交接且通过才恢复；开店交接不通过仍锁定。
     */
    public boolean isAreaTransferable(Long areaId) {
        return latestHandoverAllowsTransfer(areaId, LocalDate.now());
    }

    private boolean latestHandoverAllowsTransfer(Long areaId, LocalDate date) {
        if (areaId == null) {
            return true;
        }
        List<StoreHandover> latest = storeHandoverRepository.findLatestByAreaUpToDate(areaId, date);
        if (latest.isEmpty()) {
            return true;
        }
        StoreHandover last = latest.get(0);
        if (last.getHandoverType() != null && last.getHandoverType() == TYPE_OPEN) {
            return last.getResult() != null && last.getResult() == RESULT_PASS;
        }
        return false;
    }

    private StoreHandoverDTO convertToDTO(StoreHandover record, Map<Long, Boolean> transferableCache) {
        StoreHandoverDTO dto = new StoreHandoverDTO();
        dto.setId(record.getId());
        dto.setHandoverNo(record.getHandoverNo());
        dto.setAreaId(record.getAreaId());
        dto.setHandoverDate(record.getHandoverDate());
        dto.setHandoverType(record.getHandoverType());
        dto.setHandoverTypeName(record.getHandoverType() != null && record.getHandoverType() == TYPE_OPEN
                ? "开店交接" : "闭店交接");
        dto.setWarmerInPlace(record.getWarmerInPlace());
        dto.setCareTableClean(record.getCareTableClean());
        dto.setKeyReceiver(record.getKeyReceiver());
        dto.setAbnormalNote(record.getAbnormalNote());
        dto.setHandoverPerson(record.getHandoverPerson());
        dto.setRemark(record.getRemark());
        dto.setResult(record.getResult());
        dto.setCreatedAt(record.getCreatedAt());

        areaRepository.findById(record.getAreaId())
                .ifPresent(area -> dto.setAreaName(area.getName()));

        Boolean transferable;
        if (transferableCache != null) {
            transferable = transferableCache.computeIfAbsent(record.getAreaId(),
                    id -> latestHandoverAllowsTransfer(id, LocalDate.now()));
        } else {
            transferable = isAreaTransferable(record.getAreaId());
        }
        dto.setEquipmentTransferable(transferable);
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
