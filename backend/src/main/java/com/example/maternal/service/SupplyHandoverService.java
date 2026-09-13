
package com.example.maternal.service;

import com.example.maternal.dto.SupplyHandoverDTO;
import com.example.maternal.dto.SupplyHandoverRequest;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.SupplyHandover;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.SupplyHandoverRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplyHandoverService {

    private final SupplyHandoverRepository supplyHandoverRepository;
    private final AreaRepository areaRepository;
    private final AreaService areaService;

    /**
     * 登记值班交班单：交班人、接班人和湿巾/纸尿裤两类件数都必须盘点填写，
     * 登记后状态为未交接，待接班人确认后才算交出班。
     */
    @Transactional
    public SupplyHandoverDTO createHandover(SupplyHandoverRequest request) {
        if (request == null) {
            throw new RuntimeException("交班登记内容不能为空");
        }
        if (request.getAreaId() == null) {
            throw new RuntimeException("请选择交班的母婴室");
        }
        if (request.getHandoverDate() == null) {
            throw new RuntimeException("交班日期不能为空");
        }
        if (request.getHandoverPerson() == null || request.getHandoverPerson().trim().isEmpty()) {
            throw new RuntimeException("交班人不能为空");
        }
        if (request.getReceiver() == null || request.getReceiver().trim().isEmpty()) {
            throw new RuntimeException("接班人不能为空，没写接班人不能交出班");
        }
        validateCount(request.getWipesCount(), "湿巾件数");
        validateCount(request.getDiaperCount(), "纸尿裤件数");

        Area area = areaRepository.findById(request.getAreaId())
                .orElseThrow(() -> new RuntimeException("母婴室区域不存在"));
        if (area.getStatus() != null && area.getStatus() != 1) {
            throw new RuntimeException("该母婴室区域已停用，不能登记交班");
        }

        SupplyHandover record = new SupplyHandover();
        record.setHandoverNo(CodeGenerator.generateSupplyHandoverNo());
        record.setAreaId(request.getAreaId());
        record.setHandoverDate(request.getHandoverDate());
        record.setHandoverPerson(request.getHandoverPerson().trim());
        record.setReceiver(request.getReceiver().trim());
        record.setWipesCount(request.getWipesCount());
        record.setDiaperCount(request.getDiaperCount());
        record.setHandedOver(false);
        record.setRemark(trimToNull(request.getRemark()));

        SupplyHandover saved = supplyHandoverRepository.save(record);
        return convertToDTO(saved);
    }

    /**
     * 接班人确认交接：确认后该单才算交出班，重复确认会被拦截。
     */
    @Transactional
    public SupplyHandoverDTO confirmHandover(Long id) {
        if (id == null) {
            throw new RuntimeException("交接单ID不能为空");
        }
        SupplyHandover record = supplyHandoverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("交接单不存在，请刷新清单后重试"));
        if (Boolean.TRUE.equals(record.getHandedOver())) {
            throw new RuntimeException("交接单「" + record.getHandoverNo() + "」已确认交接，请勿重复操作");
        }
        record.setHandedOver(true);
        record.setHandoverTime(LocalDateTime.now());
        SupplyHandover saved = supplyHandoverRepository.save(record);
        return convertToDTO(saved);
    }

    /**
     * 交接清单：按母婴室（选父区域含全部下级在用母婴室）和是否已交接筛选，
     * 筛选口径由服务端统一过滤，保证刷新后结果一致。
     */
    public List<SupplyHandoverDTO> getHandovers(Long areaId, Boolean handedOver) {
        Set<Long> scopeAreaIds = areaService.resolveInUseScopeAreaIds(areaId);
        return supplyHandoverRepository.findByFilter(null, handedOver).stream()
                .filter(h -> scopeAreaIds.contains(h.getAreaId()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public SupplyHandoverDTO getHandoverById(Long id) {
        return supplyHandoverRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    private void validateCount(Integer count, String label) {
        if (count == null) {
            throw new RuntimeException(label + "不能为空，请盘点后填写");
        }
        if (count < 0) {
            throw new RuntimeException(label + "不能为负数");
        }
    }

    private SupplyHandoverDTO convertToDTO(SupplyHandover record) {
        SupplyHandoverDTO dto = new SupplyHandoverDTO();
        dto.setId(record.getId());
        dto.setHandoverNo(record.getHandoverNo());
        dto.setAreaId(record.getAreaId());
        dto.setHandoverDate(record.getHandoverDate());
        dto.setHandoverPerson(record.getHandoverPerson());
        dto.setReceiver(record.getReceiver());
        dto.setWipesCount(record.getWipesCount());
        dto.setDiaperCount(record.getDiaperCount());
        dto.setHandedOver(record.getHandedOver());
        dto.setHandoverTime(record.getHandoverTime());
        dto.setRemark(record.getRemark());
        dto.setCreatedAt(record.getCreatedAt());

        areaRepository.findById(record.getAreaId())
                .ifPresent(area -> dto.setAreaName(area.getName()));
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
