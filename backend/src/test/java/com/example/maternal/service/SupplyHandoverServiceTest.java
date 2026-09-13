
package com.example.maternal.service;

import com.example.maternal.dto.SupplyHandoverDTO;
import com.example.maternal.dto.SupplyHandoverRequest;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.SupplyHandover;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.SupplyHandoverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class SupplyHandoverServiceTest {

    private static final Long AREA_PARENT_ID = 1L;
    private static final Long AREA_A1_ID = 4L;
    private static final Long AREA_A2_ID = 5L;
    private static final Long AREA_B1_ID = 6L;
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 13);

    @Mock
    private SupplyHandoverRepository supplyHandoverRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private AreaService areaService;

    @InjectMocks
    private SupplyHandoverService supplyHandoverService;

    private Area room;

    @BeforeEach
    void setUp() {
        room = new Area();
        room.setId(AREA_A1_ID);
        room.setName("A1母婴室");
        room.setStatus(1);
        lenient().when(areaRepository.findById(AREA_A1_ID)).thenReturn(Optional.of(room));
    }

    private SupplyHandoverRequest baseRequest() {
        SupplyHandoverRequest request = new SupplyHandoverRequest();
        request.setAreaId(AREA_A1_ID);
        request.setHandoverDate(TODAY);
        request.setHandoverPerson("赵值班");
        request.setReceiver("钱值班");
        request.setWipesCount(12);
        request.setDiaperCount(30);
        return request;
    }

    private SupplyHandover record(Long id, Long areaId, boolean handedOver) {
        SupplyHandover record = new SupplyHandover();
        record.setId(id);
        record.setHandoverNo("SH20260913" + String.format("%04d", id));
        record.setAreaId(areaId);
        record.setHandoverDate(TODAY);
        record.setHandoverPerson("赵值班");
        record.setReceiver("钱值班");
        record.setWipesCount(12);
        record.setDiaperCount(30);
        record.setHandedOver(handedOver);
        return record;
    }

    @Test
    @DisplayName("盘点件数齐全登记成功，落库为未交接")
    void createHandover_success_pendingHandover() {
        SupplyHandoverRequest request = baseRequest();
        when(supplyHandoverRepository.save(any(SupplyHandover.class)))
                .thenAnswer(invocation -> {
                    SupplyHandover saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        SupplyHandoverDTO dto = supplyHandoverService.createHandover(request);

        ArgumentCaptor<SupplyHandover> captor = ArgumentCaptor.forClass(SupplyHandover.class);
        verify(supplyHandoverRepository).save(captor.capture());
        SupplyHandover saved = captor.getValue();
        assertThat(saved.getHandedOver()).isFalse();
        assertThat(saved.getHandoverTime()).isNull();
        assertThat(saved.getWipesCount()).isEqualTo(12);
        assertThat(saved.getDiaperCount()).isEqualTo(30);
        assertThat(dto.getHandedOver()).isFalse();
        assertThat(dto.getAreaName()).isEqualTo("A1母婴室");
    }

    @Test
    @DisplayName("没写接班人或两类件数时拒绝登记并给出明确提示")
    void createHandover_missingReceiverOrCounts_rejected() {
        SupplyHandoverRequest noReceiver = baseRequest();
        noReceiver.setReceiver("  ");
        assertThatThrownBy(() -> supplyHandoverService.createHandover(noReceiver))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("接班人");

        SupplyHandoverRequest noWipes = baseRequest();
        noWipes.setWipesCount(null);
        assertThatThrownBy(() -> supplyHandoverService.createHandover(noWipes))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("湿巾件数");

        SupplyHandoverRequest noDiaper = baseRequest();
        noDiaper.setDiaperCount(null);
        assertThatThrownBy(() -> supplyHandoverService.createHandover(noDiaper))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("纸尿裤件数");

        SupplyHandoverRequest negative = baseRequest();
        negative.setWipesCount(-1);
        assertThatThrownBy(() -> supplyHandoverService.createHandover(negative))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不能为负数");

        SupplyHandoverRequest noPerson = baseRequest();
        noPerson.setHandoverPerson(null);
        assertThatThrownBy(() -> supplyHandoverService.createHandover(noPerson))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("交班人不能为空");

        verify(supplyHandoverRepository, never()).save(any());
    }

    @Test
    @DisplayName("母婴室区域不存在或已停用时拒绝登记")
    void createHandover_areaMissingOrDisabled_rejected() {
        SupplyHandoverRequest request = baseRequest();
        request.setAreaId(999L);
        when(areaRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> supplyHandoverService.createHandover(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("母婴室区域不存在");

        room.setStatus(0);
        SupplyHandoverRequest disabled = baseRequest();
        assertThatThrownBy(() -> supplyHandoverService.createHandover(disabled))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已停用");

        verify(supplyHandoverRepository, never()).save(any());
    }

    @Test
    @DisplayName("确认交接后落库已交接并记录交接时间，重复确认被拦截")
    void confirmHandover_success_thenDuplicateRejected() {
        SupplyHandover pending = record(1L, AREA_A1_ID, false);
        when(supplyHandoverRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(supplyHandoverRepository.save(any(SupplyHandover.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupplyHandoverDTO dto = supplyHandoverService.confirmHandover(1L);

        assertThat(dto.getHandedOver()).isTrue();
        assertThat(dto.getHandoverTime()).isNotNull();

        assertThatThrownBy(() -> supplyHandoverService.confirmHandover(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("请勿重复操作");
    }

    @Test
    @DisplayName("确认不存在的交接单时给出明确提示")
    void confirmHandover_missingRecord_rejected() {
        when(supplyHandoverRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplyHandoverService.confirmHandover(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("交接单不存在");
        verify(supplyHandoverRepository, never()).save(any());
    }

    @Test
    @DisplayName("清单按母婴室（含下级在用区域）和是否已交接筛选，范围外记录不混入")
    void getHandovers_scopeAndStatusFilter() {
        SupplyHandover a1Pending = record(1L, AREA_A1_ID, false);
        SupplyHandover a2Done = record(2L, AREA_A2_ID, true);
        SupplyHandover outsidePending = record(3L, AREA_B1_ID, false);

        when(areaService.resolveInUseScopeAreaIds(AREA_PARENT_ID))
                .thenReturn(Set.of(AREA_PARENT_ID, AREA_A1_ID, AREA_A2_ID));
        when(supplyHandoverRepository.findByFilter(isNull(), isNull()))
                .thenReturn(List.of(a1Pending, a2Done, outsidePending));

        List<SupplyHandoverDTO> all = supplyHandoverService.getHandovers(AREA_PARENT_ID, null);
        assertThat(all).extracting(SupplyHandoverDTO::getAreaId)
                .containsExactly(AREA_A1_ID, AREA_A2_ID)
                .doesNotContain(AREA_B1_ID);

        when(supplyHandoverRepository.findByFilter(isNull(), eq(false)))
                .thenReturn(List.of(a1Pending, outsidePending));
        List<SupplyHandoverDTO> pendingOnly = supplyHandoverService.getHandovers(AREA_PARENT_ID, false);
        assertThat(pendingOnly).extracting(SupplyHandoverDTO::getAreaId)
                .containsExactly(AREA_A1_ID);
    }

    @Test
    @DisplayName("选择不存在区域时拒绝查询，避免漏筛成全部区域")
    void getHandovers_missingScope_rejected() {
        when(areaService.resolveInUseScopeAreaIds(999L))
                .thenThrow(new RuntimeException("所选母婴室区域不存在，请刷新区域列表后重试"));

        assertThatThrownBy(() -> supplyHandoverService.getHandovers(999L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("区域不存在");
        verify(supplyHandoverRepository, never()).findByFilter(any(), any());
    }
}
