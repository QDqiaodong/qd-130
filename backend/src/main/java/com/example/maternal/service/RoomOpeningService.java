
package com.example.maternal.service;

import com.example.maternal.dto.RoomOpeningRecordDTO;
import com.example.maternal.dto.RoomOpeningRecordRequest;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.RoomOpeningRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.RoomOpeningRecordRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomOpeningService {

    private final RoomOpeningRecordRepository roomOpeningRecordRepository;
    private final AreaRepository areaRepository;
    private final AreaService areaService;

    /**
     * 登记开放日开关门时刻：正常开放必须填写开门/关门时刻；
     * 临时关闭必须填写关闭原因，且不登记开关门时刻。
     * 同一母婴室同一开放日只允许登记一条，由服务端拦截并由 DB 唯一约束兜底。
     */
    public RoomOpeningRecordDTO createRecord(RoomOpeningRecordRequest request) {
        if (request == null) {
            throw new RuntimeException("开放登记内容不能为空");
        }
        if (request.getAreaId() == null) {
            throw new RuntimeException("请选择登记的母婴室");
        }
        if (request.getOpenDate() == null) {
            throw new RuntimeException("开放日期不能为空");
        }
        if (request.getTemporarilyClosed() == null) {
            throw new RuntimeException("请选择当日正常开放还是临时关闭");
        }

        Area area = areaRepository.findById(request.getAreaId())
                .orElseThrow(() -> new RuntimeException("母婴室区域不存在"));
        if (area.getStatus() != null && area.getStatus() != 1) {
            throw new RuntimeException("该母婴室区域已停用，不能登记开关门时刻");
        }

        if (roomOpeningRecordRepository.existsByAreaIdAndOpenDate(
                request.getAreaId(), request.getOpenDate())) {
            throw new RuntimeException("该母婴室当日已登记开关门时刻，同一开放日不能重复登记");
        }

        RoomOpeningRecord record = new RoomOpeningRecord();
        record.setRecordNo(CodeGenerator.generateOpeningNo());
        record.setAreaId(request.getAreaId());
        record.setOpenDate(request.getOpenDate());
        record.setTemporarilyClosed(request.getTemporarilyClosed());

        if (Boolean.TRUE.equals(request.getTemporarilyClosed())) {
            // 临时关闭必须写原因；当日不登记开关门时刻
            if (request.getCloseReason() == null || request.getCloseReason().trim().isEmpty()) {
                throw new RuntimeException("临时关闭时必须填写关闭原因");
            }
            record.setOpenTime(null);
            record.setCloseTime(null);
            record.setCloseReason(request.getCloseReason().trim());
        } else {
            // 正常开放必须填写开门与关门时刻
            if (request.getOpenTime() == null) {
                throw new RuntimeException("开门时刻不能为空");
            }
            if (request.getCloseTime() == null) {
                throw new RuntimeException("关门时刻不能为空");
            }
            LocalDate openDay = request.getOpenDate();
            if (!request.getOpenTime().toLocalDate().equals(openDay)) {
                throw new RuntimeException("开门时刻必须与开放日期为同一天");
            }
            if (!request.getCloseTime().toLocalDate().equals(openDay)) {
                throw new RuntimeException("关门时刻必须与开放日期为同一天");
            }
            if (!request.getCloseTime().isAfter(request.getOpenTime())) {
                throw new RuntimeException("关门时刻必须晚于开门时刻");
            }
            record.setOpenTime(request.getOpenTime());
            record.setCloseTime(request.getCloseTime());
            record.setCloseReason(null);
        }
        record.setRemark(request.getRemark());

        RoomOpeningRecord saved = roomOpeningRecordRepository.save(record);
        return buildDTO(saved, LocalDateTime.now());
    }

    /**
     * 按开放日区间和母婴室（选父区域含全部下级在用母婴室）查询登记；
     * canEnter 为是否只看此刻可进，可进状态由服务端按当前时间统一判定，避免前端口径漂移。
     */
    public List<RoomOpeningRecordDTO> getRecords(LocalDate startDate, LocalDate endDate,
                                                 Long areaId, Boolean canEnter) {
        Set<Long> scopeAreaIds = areaService.resolveInUseScopeAreaIds(areaId);
        LocalDateTime now = LocalDateTime.now();

        List<RoomOpeningRecordDTO> result = roomOpeningRecordRepository
                .findByFilter(startDate, endDate, null).stream()
                .filter(r -> scopeAreaIds.contains(r.getAreaId()))
                .map(r -> buildDTO(r, now))
                .collect(Collectors.toList());

        if (canEnter != null) {
            result = result.stream()
                    .filter(dto -> canEnter.equals(Boolean.TRUE.equals(dto.getCanEnterNow())))
                    .collect(Collectors.toList());
        }
        return result;
    }

    /**
     * 此刻能否进入（仅对当日记录有意义）：
     * 临时关闭 → 不可进；未到开门 → 不可进；已过关门 → 不可进；处于开放区间 → 可进；非当日记录仅供回看，不可进。
     */
    RoomOpeningRecordDTO buildDTO(RoomOpeningRecord record, LocalDateTime now) {
        RoomOpeningRecordDTO dto = new RoomOpeningRecordDTO();
        dto.setId(record.getId());
        dto.setRecordNo(record.getRecordNo());
        dto.setAreaId(record.getAreaId());
        dto.setOpenDate(record.getOpenDate());
        dto.setOpenTime(record.getOpenTime());
        dto.setCloseTime(record.getCloseTime());
        dto.setTemporarilyClosed(record.getTemporarilyClosed());
        dto.setCloseReason(record.getCloseReason());
        dto.setRemark(record.getRemark());
        dto.setCreatedAt(record.getCreatedAt());

        areaRepository.findById(record.getAreaId())
                .ifPresent(area -> dto.setAreaName(area.getName()));

        boolean isToday = now.toLocalDate().equals(record.getOpenDate());
        boolean canEnter;
        String statusCode;
        if (!isToday) {
            canEnter = false;
            statusCode = "NOT_TODAY";
        } else if (Boolean.TRUE.equals(record.getTemporarilyClosed())) {
            canEnter = false;
            statusCode = "TEMP_CLOSED";
        } else if (record.getOpenTime() != null && now.isBefore(record.getOpenTime())) {
            canEnter = false;
            statusCode = "BEFORE_OPEN";
        } else if (record.getCloseTime() != null && !now.isBefore(record.getCloseTime())) {
            canEnter = false;
            statusCode = "AFTER_CLOSE";
        } else {
            canEnter = true;
            statusCode = "OPEN";
        }
        dto.setCanEnterNow(canEnter);
        dto.setEnterStatusCode(statusCode);
        return dto;
    }
}
