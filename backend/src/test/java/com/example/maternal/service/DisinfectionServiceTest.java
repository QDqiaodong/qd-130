
package com.example.maternal.service;

import com.example.maternal.dto.DisinfectionRecordDTO;
import com.example.maternal.dto.DisinfectionRecordRequest;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.DisinfectionRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.DisinfectionRecordRepository;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisinfectionServiceTest {

    private static final Long AREA_PARENT_ID = 1L;
    private static final Long AREA_A1_ID = 4L;
    private static final Long AREA_A2_ID = 5L;
    private static final Long AREA_A3_DISABLED_ID = 8L;
    private static final Long AREA_B1_ID = 6L;
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 12);

    @Mock
    private DisinfectionRecordRepository disinfectionRecordRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private AreaService areaService;

    @InjectMocks
    private DisinfectionService disinfectionService;

    private Area room;

    @BeforeEach
    void setUp() {
        room = new Area();
        room.setId(AREA_A1_ID);
        room.setName("A1母婴室");
        room.setStatus(1);
        lenient().when(areaRepository.findById(AREA_A1_ID)).thenReturn(Optional.of(room));
    }

    private DisinfectionRecordRequest baseRequest() {
        DisinfectionRecordRequest request = new DisinfectionRecordRequest();
        request.setAreaId(AREA_A1_ID);
        request.setDisinfectDate(TODAY);
        request.setOperator("赵值班");
        request.setFinishTime(LocalDateTime.of(2026, 9, 12, 9, 30));
        request.setDisinfectant("84消毒液（1:100）");
        request.setVentilationDone(true);
        return request;
    }

    private DisinfectionRecord savedRecord(DisinfectionRecordRequest request, boolean closedLoop) {
        DisinfectionRecord record = new DisinfectionRecord();
        record.setId(1L);
        record.setDisinfectionNo("DS202609120001");
        record.setAreaId(request.getAreaId());
        record.setDisinfectDate(request.getDisinfectDate());
        record.setOperator(request.getOperator());
        record.setFinishTime(request.getFinishTime());
        record.setDisinfectant(request.getDisinfectant());
        record.setVentilationDone(request.getVentilationDone());
        record.setClosedLoop(closedLoop);
        record.setIncompleteReason(request.getIncompleteReason());
        return record;
    }

    private DisinfectionRecord record(Long id, Long areaId, LocalDate date, boolean closedLoop) {
        DisinfectionRecord record = new DisinfectionRecord();
        record.setId(id);
        record.setDisinfectionNo("DS" + date.toString().replace("-", "") + String.format("%04d", id));
        record.setAreaId(areaId);
        record.setDisinfectDate(date);
        record.setOperator("赵值班");
        record.setFinishTime(date.atTime(9, 30));
        record.setDisinfectant("84消毒液（1:100）");
        record.setVentilationDone(closedLoop);
        record.setClosedLoop(closedLoop);
        if (!closedLoop) {
            record.setIncompleteReason("排风扇故障");
        }
        return record;
    }

    @Test
    @DisplayName("通风做完登记成功，闭环标记落库为已闭环，当日不可再登记")
    void createDisinfection_ventilationDone_closedLoop() {
        DisinfectionRecordRequest request = baseRequest();
        when(disinfectionRecordRepository.save(any(DisinfectionRecord.class)))
                .thenAnswer(invocation -> savedRecord(request, true));
        when(disinfectionRecordRepository.existsByAreaIdAndDisinfectDateAndClosedLoopTrue(AREA_A1_ID, TODAY))
                .thenReturn(false, true);

        DisinfectionRecordDTO dto = disinfectionService.createDisinfection(request);

        ArgumentCaptor<DisinfectionRecord> captor = ArgumentCaptor.forClass(DisinfectionRecord.class);
        verify(disinfectionRecordRepository).save(captor.capture());
        DisinfectionRecord saved = captor.getValue();
        assertThat(saved.getClosedLoop()).isTrue();
        assertThat(saved.getIncompleteReason()).isNull();
        assertThat(dto.getClosedLoop()).isTrue();
        assertThat(dto.getCanRegisterAgain()).isFalse();
    }

    @Test
    @DisplayName("通风未做完不算当日闭环，未完成原因落库，当日可再登记")
    void createDisinfection_ventilationNotDone_notClosedLoop() {
        DisinfectionRecordRequest request = baseRequest();
        request.setVentilationDone(false);
        request.setIncompleteReason("排风扇故障未能通风");
        when(disinfectionRecordRepository.save(any(DisinfectionRecord.class)))
                .thenAnswer(invocation -> savedRecord(request, false));
        when(disinfectionRecordRepository.existsByAreaIdAndDisinfectDateAndClosedLoopTrue(AREA_A1_ID, TODAY))
                .thenReturn(false);

        DisinfectionRecordDTO dto = disinfectionService.createDisinfection(request);

        ArgumentCaptor<DisinfectionRecord> captor = ArgumentCaptor.forClass(DisinfectionRecord.class);
        verify(disinfectionRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getClosedLoop()).isFalse();
        assertThat(captor.getValue().getIncompleteReason()).isEqualTo("排风扇故障未能通风");
        assertThat(dto.getClosedLoop()).isFalse();
        assertThat(dto.getCanRegisterAgain()).isTrue();
    }

    @Test
    @DisplayName("通风未做完且漏填未完成原因时拒绝登记")
    void createDisinfection_ventilationNotDoneWithoutReason_rejected() {
        DisinfectionRecordRequest request = baseRequest();
        request.setVentilationDone(false);
        request.setIncompleteReason("  ");

        assertThatThrownBy(() -> disinfectionService.createDisinfection(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("未完成原因");
        verify(disinfectionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("漏填消毒人/完成时间/消毒液/通风结论时给出明确提示，不保存记录")
    void createDisinfection_missingFields_rejected() {
        DisinfectionRecordRequest noOperator = baseRequest();
        noOperator.setOperator("  ");
        assertThatThrownBy(() -> disinfectionService.createDisinfection(noOperator))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("消毒人不能为空");

        DisinfectionRecordRequest noFinishTime = baseRequest();
        noFinishTime.setFinishTime(null);
        assertThatThrownBy(() -> disinfectionService.createDisinfection(noFinishTime))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("完成时间不能为空");

        DisinfectionRecordRequest noDisinfectant = baseRequest();
        noDisinfectant.setDisinfectant(null);
        assertThatThrownBy(() -> disinfectionService.createDisinfection(noDisinfectant))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("消毒液不能为空");

        DisinfectionRecordRequest noVentilation = baseRequest();
        noVentilation.setVentilationDone(null);
        assertThatThrownBy(() -> disinfectionService.createDisinfection(noVentilation))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("通风是否做完");

        DisinfectionRecordRequest noDate = baseRequest();
        noDate.setDisinfectDate(null);
        assertThatThrownBy(() -> disinfectionService.createDisinfection(noDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("消毒日期不能为空");

        verify(disinfectionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("同一母婴室当天已闭环后再次登记被拒绝，提示不能重复闭环")
    void createDisinfection_duplicateClosedLoop_rejected() {
        when(disinfectionRecordRepository.existsByAreaIdAndDisinfectDateAndClosedLoopTrue(AREA_A1_ID, TODAY))
                .thenReturn(true);

        DisinfectionRecordRequest request = baseRequest();
        assertThatThrownBy(() -> disinfectionService.createDisinfection(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不能重复闭环");

        verify(disinfectionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("母婴室区域不存在或已停用时拒绝登记")
    void createDisinfection_areaMissingOrDisabled_rejected() {
        DisinfectionRecordRequest request = baseRequest();
        request.setAreaId(999L);
        when(areaRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> disinfectionService.createDisinfection(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("母婴室区域不存在");

        room.setStatus(0);
        DisinfectionRecordRequest disabled = baseRequest();
        assertThatThrownBy(() -> disinfectionService.createDisinfection(disabled))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已停用");

        verify(disinfectionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("选择父区域时带出下级在用母婴室，停用室和范围外区域不会混入；能否再登记与闭环标记同源")
    void getDisinfections_parentScopeIncludesActiveChildrenOnly() {
        DisinfectionRecord parentClosed = record(1L, AREA_PARENT_ID, TODAY, true);
        DisinfectionRecord a1Open = record(2L, AREA_A1_ID, TODAY, false);
        DisinfectionRecord a2Closed = record(3L, AREA_A2_ID, TODAY, true);
        DisinfectionRecord disabledOpen = record(4L, AREA_A3_DISABLED_ID, TODAY, false);
        DisinfectionRecord outsideClosed = record(5L, AREA_B1_ID, TODAY, true);

        when(areaService.resolveInUseScopeAreaIds(AREA_PARENT_ID))
                .thenReturn(Set.of(AREA_PARENT_ID, AREA_A1_ID, AREA_A2_ID));
        when(disinfectionRecordRepository.findByFilter(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(parentClosed, a1Open, a2Closed, disabledOpen, outsideClosed));
        when(disinfectionRecordRepository.findByFilter(isNull(), isNull(), isNull(), anyBoolean()))
                .thenReturn(List.of(parentClosed, a2Closed, outsideClosed));

        List<DisinfectionRecordDTO> result =
                disinfectionService.getDisinfections(null, null, AREA_PARENT_ID, null);

        verify(disinfectionRecordRepository)
                .findByFilter(isNull(), isNull(), isNull(), isNull());
        verify(disinfectionRecordRepository)
                .findByFilter(isNull(), isNull(), isNull(), anyBoolean());

        assertThat(result).extracting(DisinfectionRecordDTO::getAreaId)
                .containsExactly(AREA_PARENT_ID, AREA_A1_ID, AREA_A2_ID)
                .doesNotContain(AREA_A3_DISABLED_ID, AREA_B1_ID);
        assertThat(result).filteredOn(dto -> dto.getAreaId().equals(AREA_PARENT_ID))
                .singleElement()
                .extracting(DisinfectionRecordDTO::getCanRegisterAgain)
                .isEqualTo(false);
        assertThat(result).filteredOn(dto -> dto.getAreaId().equals(AREA_A1_ID))
                .singleElement()
                .extracting(DisinfectionRecordDTO::getClosedLoop, DisinfectionRecordDTO::getCanRegisterAgain)
                .containsExactly(false, true);
        assertThat(result).filteredOn(dto -> dto.getAreaId().equals(AREA_A2_ID))
                .singleElement()
                .extracting(DisinfectionRecordDTO::getClosedLoop, DisinfectionRecordDTO::getCanRegisterAgain)
                .containsExactly(true, false);
    }

    @Test
    @DisplayName("选择不存在区域时拒绝查询，避免漏筛成全部区域")
    void getDisinfections_missingScope_rejected() {
        when(areaService.resolveInUseScopeAreaIds(999L))
                .thenThrow(new RuntimeException("所选母婴室区域不存在，请刷新区域列表后重试"));

        assertThatThrownBy(() -> disinfectionService.getDisinfections(null, null, 999L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("区域不存在");
        verify(disinfectionRecordRepository, never()).findByFilter(any(), any(), any(), any());
    }
}
