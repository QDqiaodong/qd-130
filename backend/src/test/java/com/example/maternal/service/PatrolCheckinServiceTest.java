
package com.example.maternal.service;

import com.example.maternal.dto.PatrolBoardDTO;
import com.example.maternal.dto.PatrolCheckinDTO;
import com.example.maternal.dto.PatrolCheckinRequest;
import com.example.maternal.dto.PatrolMissedRoomDTO;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.PatrolCheckin;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.PatrolCheckinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatrolCheckinServiceTest {

    private static final Long AREA_PARENT_ID = 1L;
    private static final Long AREA_A1_ID = 4L;
    private static final Long AREA_A2_ID = 5L;
    private static final Long AREA_B1_ID = 6L;
    private static final LocalDate PATROL_DATE = LocalDate.of(2026, 9, 13);

    @Mock
    private PatrolCheckinRepository patrolCheckinRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private AreaService areaService;

    @InjectMocks
    private PatrolCheckinService patrolCheckinService;

    private Area room;

    @BeforeEach
    void setUp() {
        room = new Area();
        room.setId(AREA_A1_ID);
        room.setName("A1母婴室");
        room.setStatus(1);
        lenient().when(areaRepository.findById(AREA_A1_ID)).thenReturn(Optional.of(room));
    }

    private PatrolCheckinRequest baseRequest() {
        PatrolCheckinRequest request = new PatrolCheckinRequest();
        request.setAreaId(AREA_A1_ID);
        request.setPatrolDate(PATROL_DATE);
        request.setShift(PatrolCheckinService.SHIFT_FIRST);
        request.setPatrolPerson("赵值班");
        request.setCheckinTime(LocalDateTime.of(2026, 9, 13, 22, 10));
        return request;
    }

    private PatrolCheckin record(Long id, Long areaId, Integer shift) {
        PatrolCheckin record = new PatrolCheckin();
        record.setId(id);
        record.setCheckinNo("PC20260913" + String.format("%04d", id));
        record.setAreaId(areaId);
        record.setPatrolDate(PATROL_DATE);
        record.setShift(shift);
        record.setPatrolPerson("赵值班");
        record.setCheckinTime(LocalDateTime.of(2026, 9, 13, 22, 10));
        return record;
    }

    private Area areaOf(Long id, String name) {
        Area area = new Area();
        area.setId(id);
        area.setName(name);
        area.setStatus(1);
        return area;
    }

    @Test
    @DisplayName("必填项齐全打卡成功，班次名称由服务端落库口径生成")
    void createCheckin_success() {
        PatrolCheckinRequest request = baseRequest();
        when(patrolCheckinRepository.existsByAreaIdAndPatrolDateAndShift(
                AREA_A1_ID, PATROL_DATE, PatrolCheckinService.SHIFT_FIRST)).thenReturn(false);
        when(patrolCheckinRepository.save(any(PatrolCheckin.class)))
                .thenAnswer(invocation -> {
                    PatrolCheckin saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        PatrolCheckinDTO dto = patrolCheckinService.createCheckin(request);

        ArgumentCaptor<PatrolCheckin> captor = ArgumentCaptor.forClass(PatrolCheckin.class);
        verify(patrolCheckinRepository).save(captor.capture());
        PatrolCheckin saved = captor.getValue();
        assertThat(saved.getShift()).isEqualTo(PatrolCheckinService.SHIFT_FIRST);
        assertThat(saved.getPatrolPerson()).isEqualTo("赵值班");
        assertThat(saved.getCheckinNo()).startsWith("PC");
        assertThat(dto.getShiftName()).isEqualTo("前夜班（22:00-02:00）");
        assertThat(dto.getAreaName()).isEqualTo("A1母婴室");
    }

    @Test
    @DisplayName("漏填母婴室/日期/班次/巡更人/打卡时间时拒绝打卡并给出明确提示")
    void createCheckin_missingFields_rejected() {
        PatrolCheckinRequest noArea = baseRequest();
        noArea.setAreaId(null);
        assertThatThrownBy(() -> patrolCheckinService.createCheckin(noArea))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("请选择巡更的母婴室");

        PatrolCheckinRequest noDate = baseRequest();
        noDate.setPatrolDate(null);
        assertThatThrownBy(() -> patrolCheckinService.createCheckin(noDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("巡更日期不能为空");

        PatrolCheckinRequest noShift = baseRequest();
        noShift.setShift(null);
        assertThatThrownBy(() -> patrolCheckinService.createCheckin(noShift))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("请选择巡更班次");

        PatrolCheckinRequest badShift = baseRequest();
        badShift.setShift(9);
        assertThatThrownBy(() -> patrolCheckinService.createCheckin(badShift))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("巡更班次不合法");

        PatrolCheckinRequest noPerson = baseRequest();
        noPerson.setPatrolPerson("  ");
        assertThatThrownBy(() -> patrolCheckinService.createCheckin(noPerson))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("巡更人不能为空");

        PatrolCheckinRequest noTime = baseRequest();
        noTime.setCheckinTime(null);
        assertThatThrownBy(() -> patrolCheckinService.createCheckin(noTime))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("打卡时间不能为空");

        verify(patrolCheckinRepository, never()).save(any());
    }

    @Test
    @DisplayName("母婴室区域不存在或已停用时拒绝打卡")
    void createCheckin_areaMissingOrDisabled_rejected() {
        PatrolCheckinRequest request = baseRequest();
        request.setAreaId(999L);
        when(areaRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> patrolCheckinService.createCheckin(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("母婴室区域不存在");

        room.setStatus(0);
        PatrolCheckinRequest disabled = baseRequest();
        assertThatThrownBy(() -> patrolCheckinService.createCheckin(disabled))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已停用");

        verify(patrolCheckinRepository, never()).save(any());
    }

    @Test
    @DisplayName("同一母婴室同一班次已打卡时不能再补同一班次")
    void createCheckin_duplicateSameShift_rejected() {
        PatrolCheckinRequest request = baseRequest();
        when(patrolCheckinRepository.existsByAreaIdAndPatrolDateAndShift(
                AREA_A1_ID, PATROL_DATE, PatrolCheckinService.SHIFT_FIRST)).thenReturn(true);

        assertThatThrownBy(() -> patrolCheckinService.createCheckin(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已打卡")
                .hasMessageContaining("不能再补同一班次");

        verify(patrolCheckinRepository, never()).save(any());
    }

    @Test
    @DisplayName("同一母婴室不同班次互不影响，可分别打卡")
    void createCheckin_differentShift_allowed() {
        PatrolCheckinRequest request = baseRequest();
        request.setShift(PatrolCheckinService.SHIFT_SECOND);
        request.setCheckinTime(LocalDateTime.of(2026, 9, 14, 2, 15));
        when(patrolCheckinRepository.existsByAreaIdAndPatrolDateAndShift(
                AREA_A1_ID, PATROL_DATE, PatrolCheckinService.SHIFT_SECOND)).thenReturn(false);
        when(patrolCheckinRepository.save(any(PatrolCheckin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatrolCheckinDTO dto = patrolCheckinService.createCheckin(request);

        assertThat(dto.getShift()).isEqualTo(PatrolCheckinService.SHIFT_SECOND);
        assertThat(dto.getShiftName()).isEqualTo("后夜班（02:00-06:00）");
        verify(patrolCheckinRepository).save(any(PatrolCheckin.class));
    }

    @Test
    @DisplayName("看板按班次筛选打卡记录，漏打房间单独列出且范围外记录不混入")
    void getBoard_shiftFilterAndMissedRooms() {
        PatrolCheckin a1Shift1 = record(1L, AREA_A1_ID, PatrolCheckinService.SHIFT_FIRST);
        PatrolCheckin outsideShift1 = record(2L, AREA_B1_ID, PatrolCheckinService.SHIFT_FIRST);

        Set<Long> scope = Set.of(AREA_A1_ID, AREA_A2_ID);
        when(areaService.resolveInUseScopeAreaIds(AREA_PARENT_ID)).thenReturn(scope);
        when(areaRepository.findAllById(scope)).thenReturn(List.of(
                areaOf(AREA_A1_ID, "A1母婴室"), areaOf(AREA_A2_ID, "A2母婴室")));
        when(patrolCheckinRepository.findByFilter(eq(PATROL_DATE), eq(PatrolCheckinService.SHIFT_FIRST)))
                .thenReturn(List.of(a1Shift1, outsideShift1));

        PatrolBoardDTO board = patrolCheckinService.getBoard(
                PATROL_DATE, PatrolCheckinService.SHIFT_FIRST, AREA_PARENT_ID);

        assertThat(board.getPatrolDate()).isEqualTo(PATROL_DATE);
        assertThat(board.getShift()).isEqualTo(PatrolCheckinService.SHIFT_FIRST);
        assertThat(board.getRecords()).extracting(PatrolCheckinDTO::getAreaId)
                .containsExactly(AREA_A1_ID)
                .doesNotContain(AREA_B1_ID);
        // A1 前夜班已打卡，A2 漏打；范围外的 B1 不出现在漏打列表
        assertThat(board.getMissedRooms()).extracting(PatrolMissedRoomDTO::getAreaId)
                .containsExactly(AREA_A2_ID);
        assertThat(board.getMissedRooms().get(0).getShift()).isEqualTo(PatrolCheckinService.SHIFT_FIRST);
        assertThat(board.getMissedRooms().get(0).getShiftName()).isEqualTo("前夜班（22:00-02:00）");
    }

    @Test
    @DisplayName("班次选全部时漏打房间按前后夜班分别列出")
    void getBoard_allShifts_missedPerShift() {
        PatrolCheckin a1Shift1 = record(1L, AREA_A1_ID, PatrolCheckinService.SHIFT_FIRST);
        PatrolCheckin a1Shift2 = record(2L, AREA_A1_ID, PatrolCheckinService.SHIFT_SECOND);

        Set<Long> scope = Set.of(AREA_A1_ID, AREA_A2_ID);
        when(areaService.resolveInUseScopeAreaIds(null)).thenReturn(scope);
        when(areaRepository.findAllById(scope)).thenReturn(List.of(
                areaOf(AREA_A1_ID, "A1母婴室"), areaOf(AREA_A2_ID, "A2母婴室")));
        when(patrolCheckinRepository.findByFilter(eq(PATROL_DATE), isNull()))
                .thenReturn(List.of(a1Shift1, a1Shift2));
        when(patrolCheckinRepository.findByFilter(eq(PATROL_DATE), eq(PatrolCheckinService.SHIFT_FIRST)))
                .thenReturn(List.of(a1Shift1));
        when(patrolCheckinRepository.findByFilter(eq(PATROL_DATE), eq(PatrolCheckinService.SHIFT_SECOND)))
                .thenReturn(List.of(a1Shift2));

        PatrolBoardDTO board = patrolCheckinService.getBoard(PATROL_DATE, null, null);

        assertThat(board.getRecords()).hasSize(2);
        // A1 两个班次都已打卡；A2 前夜班、后夜班均漏打，分两行列出
        assertThat(board.getMissedRooms()).extracting(PatrolMissedRoomDTO::getAreaId)
                .containsExactly(AREA_A2_ID, AREA_A2_ID);
        assertThat(board.getMissedRooms()).extracting(PatrolMissedRoomDTO::getShift)
                .containsExactly(PatrolCheckinService.SHIFT_FIRST, PatrolCheckinService.SHIFT_SECOND);
    }

    @Test
    @DisplayName("全部房间都打卡后漏打列表为空")
    void getBoard_noMissedRooms() {
        PatrolCheckin a1Shift1 = record(1L, AREA_A1_ID, PatrolCheckinService.SHIFT_FIRST);
        PatrolCheckin a2Shift1 = record(2L, AREA_A2_ID, PatrolCheckinService.SHIFT_FIRST);

        Set<Long> scope = Set.of(AREA_A1_ID, AREA_A2_ID);
        when(areaService.resolveInUseScopeAreaIds(null)).thenReturn(scope);
        when(areaRepository.findAllById(scope)).thenReturn(List.of(
                areaOf(AREA_A1_ID, "A1母婴室"), areaOf(AREA_A2_ID, "A2母婴室")));
        when(patrolCheckinRepository.findByFilter(eq(PATROL_DATE), eq(PatrolCheckinService.SHIFT_FIRST)))
                .thenReturn(List.of(a1Shift1, a2Shift1));

        PatrolBoardDTO board = patrolCheckinService.getBoard(
                PATROL_DATE, PatrolCheckinService.SHIFT_FIRST, null);

        assertThat(board.getRecords()).hasSize(2);
        assertThat(board.getMissedRooms()).isEmpty();
    }

    @Test
    @DisplayName("筛选班次不合法或所选区域不存在时拒绝查询")
    void getBoard_invalidShiftOrMissingScope_rejected() {
        assertThatThrownBy(() -> patrolCheckinService.getBoard(PATROL_DATE, 9, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("巡更班次不合法");

        when(areaService.resolveInUseScopeAreaIds(999L))
                .thenThrow(new RuntimeException("所选母婴室区域不存在，请刷新区域列表后重试"));
        assertThatThrownBy(() -> patrolCheckinService.getBoard(PATROL_DATE, null, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("区域不存在");

        verify(patrolCheckinRepository, never()).findByFilter(any(), any());
    }
}
