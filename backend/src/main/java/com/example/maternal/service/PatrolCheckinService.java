
package com.example.maternal.service;

import com.example.maternal.dto.PatrolBoardDTO;
import com.example.maternal.dto.PatrolCheckinDTO;
import com.example.maternal.dto.PatrolCheckinRequest;
import com.example.maternal.dto.PatrolMissedRoomDTO;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.PatrolCheckin;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.PatrolCheckinRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatrolCheckinService {

    /** 前夜班（22:00-02:00） */
    public static final int SHIFT_FIRST = 1;
    /** 后夜班（02:00-06:00） */
    public static final int SHIFT_SECOND = 2;

    private static final Map<Integer, String> SHIFT_NAMES = Map.of(
            SHIFT_FIRST, "前夜班（22:00-02:00）",
            SHIFT_SECOND, "后夜班（02:00-06:00）");

    private final PatrolCheckinRepository patrolCheckinRepository;
    private final AreaRepository areaRepository;
    private final AreaService areaService;

    /**
     * 夜间巡更打卡：母婴室、巡更日期、班次、巡更人、打卡时间均必填；
     * 同一母婴室同一巡更日期同一班次只允许打一次卡，已打卡的房间不能再补同一班次，
     * 由服务端拦截并由 DB 唯一约束兜底。
     */
    public PatrolCheckinDTO createCheckin(PatrolCheckinRequest request) {
        if (request == null) {
            throw new RuntimeException("巡更打卡内容不能为空");
        }
        if (request.getAreaId() == null) {
            throw new RuntimeException("请选择巡更的母婴室");
        }
        if (request.getPatrolDate() == null) {
            throw new RuntimeException("巡更日期不能为空");
        }
        validateShift(request.getShift());
        if (request.getPatrolPerson() == null || request.getPatrolPerson().trim().isEmpty()) {
            throw new RuntimeException("巡更人不能为空");
        }
        if (request.getCheckinTime() == null) {
            throw new RuntimeException("打卡时间不能为空");
        }

        Area area = areaRepository.findById(request.getAreaId())
                .orElseThrow(() -> new RuntimeException("母婴室区域不存在"));
        if (area.getStatus() != null && area.getStatus() != 1) {
            throw new RuntimeException("该母婴室区域已停用，不能巡更打卡");
        }

        if (patrolCheckinRepository.existsByAreaIdAndPatrolDateAndShift(
                request.getAreaId(), request.getPatrolDate(), request.getShift())) {
            throw new RuntimeException("该母婴室当日" + shiftName(request.getShift())
                    + "已打卡，已打卡的房间不能再补同一班次");
        }

        PatrolCheckin record = new PatrolCheckin();
        record.setCheckinNo(CodeGenerator.generatePatrolCheckinNo());
        record.setAreaId(request.getAreaId());
        record.setPatrolDate(request.getPatrolDate());
        record.setShift(request.getShift());
        record.setPatrolPerson(request.getPatrolPerson().trim());
        record.setCheckinTime(request.getCheckinTime());
        record.setRemark(trimToNull(request.getRemark()));

        PatrolCheckin saved = patrolCheckinRepository.save(record);
        return convertToDTO(saved);
    }

    /**
     * 巡更看板：打卡记录与漏打房间同源同口径。
     * 漏打房间 = 筛选范围内在用母婴室中，所选巡更日期对应班次没有打卡记录的房间；
     * 班次选「全部」时按前夜班/后夜班分别列出漏打房间。
     */
    public PatrolBoardDTO getBoard(LocalDate patrolDate, Integer shift, Long areaId) {
        if (shift != null) {
            validateShift(shift);
        }
        LocalDate boardDate = patrolDate != null ? patrolDate : LocalDate.now();
        Set<Long> scopeAreaIds = areaService.resolveInUseScopeAreaIds(areaId);

        List<PatrolCheckin> records = patrolCheckinRepository.findByFilter(boardDate, shift).stream()
                .filter(r -> scopeAreaIds.contains(r.getAreaId()))
                .collect(Collectors.toList());

        PatrolBoardDTO board = new PatrolBoardDTO();
        board.setPatrolDate(boardDate);
        board.setShift(shift);
        board.setRecords(records.stream().map(this::convertToDTO).collect(Collectors.toList()));
        board.setMissedRooms(buildMissedRooms(boardDate, shift, scopeAreaIds));
        return board;
    }

    private List<PatrolMissedRoomDTO> buildMissedRooms(LocalDate patrolDate, Integer shift,
                                                       Set<Long> scopeAreaIds) {
        List<Integer> shiftsToCheck = shift != null
                ? List.of(shift)
                : List.of(SHIFT_FIRST, SHIFT_SECOND);

        Map<Long, Area> scopeAreas = areaRepository.findAllById(scopeAreaIds).stream()
                .collect(Collectors.toMap(Area::getId, Function.identity()));

        List<PatrolMissedRoomDTO> missed = new ArrayList<>();
        for (Integer s : shiftsToCheck) {
            Set<Long> checkedAreaIds = patrolCheckinRepository.findByFilter(patrolDate, s).stream()
                    .map(PatrolCheckin::getAreaId)
                    .collect(Collectors.toSet());
            scopeAreaIds.stream()
                    .filter(id -> !checkedAreaIds.contains(id))
                    .sorted()
                    .forEach(id -> {
                        PatrolMissedRoomDTO dto = new PatrolMissedRoomDTO();
                        dto.setAreaId(id);
                        Area area = scopeAreas.get(id);
                        dto.setAreaName(area != null ? area.getName() : null);
                        dto.setPatrolDate(patrolDate);
                        dto.setShift(s);
                        dto.setShiftName(shiftName(s));
                        missed.add(dto);
                    });
        }
        return missed;
    }

    private void validateShift(Integer shift) {
        if (shift == null) {
            throw new RuntimeException("请选择巡更班次");
        }
        if (!SHIFT_NAMES.containsKey(shift)) {
            throw new RuntimeException("巡更班次不合法，仅支持前夜班（22:00-02:00）和后夜班（02:00-06:00）");
        }
    }

    private String shiftName(Integer shift) {
        return SHIFT_NAMES.getOrDefault(shift, "未知班次");
    }

    private PatrolCheckinDTO convertToDTO(PatrolCheckin record) {
        PatrolCheckinDTO dto = new PatrolCheckinDTO();
        dto.setId(record.getId());
        dto.setCheckinNo(record.getCheckinNo());
        dto.setAreaId(record.getAreaId());
        dto.setPatrolDate(record.getPatrolDate());
        dto.setShift(record.getShift());
        dto.setShiftName(shiftName(record.getShift()));
        dto.setPatrolPerson(record.getPatrolPerson());
        dto.setCheckinTime(record.getCheckinTime());
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
