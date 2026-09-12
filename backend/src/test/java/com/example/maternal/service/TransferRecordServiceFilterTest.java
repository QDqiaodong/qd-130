
package com.example.maternal.service;

import com.example.maternal.dto.TransferRecordDTO;
import com.example.maternal.dto.TransferSummaryDTO;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.TransferRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.repository.TransferRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferRecordServiceFilterTest {

    @Mock
    private TransferRecordRepository transferRecordRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private AreaService areaService;

    @InjectMocks
    private TransferRecordService transferRecordService;

    private static final LocalDate START = LocalDate.now().minusDays(6);
    private static final LocalDate END = LocalDate.now();

    private static final long PARENT = 1L;
    private static final long AREA_A = 4L;
    private static final long AREA_B = 5L;
    private static final long OTHER = 9L;

    // 调出A、调入A、区域内A->子区域、与A无关 各一单
    private TransferRecord outFromA;
    private TransferRecord intoA;
    private TransferRecord innerWithinScope;
    private TransferRecord unrelated;

    @BeforeEach
    void setUp() {
        outFromA = transfer(1L, 10L, AREA_A, AREA_B);
        intoA = transfer(2L, 11L, AREA_B, AREA_A);
        // 子区域 41 属于 A 的下级（与看板父区域含下级口径一致）
        innerWithinScope = transfer(3L, 12L, AREA_A, 41L);
        unrelated = transfer(4L, 13L, AREA_B, OTHER);

        when(transferRecordRepository.findForDashboard(any(), any(), isNull(), any()))
                .thenReturn(List.of(outFromA, intoA, innerWithinScope, unrelated));

        when(areaRepository.findById(anyLong())).thenAnswer(inv -> Optional.of(area(inv.getArgument(0))));
        when(equipmentRepository.findById(anyLong())).thenAnswer(inv -> Optional.of(equipment(inv.getArgument(0))));
    }

    @Test
    @DisplayName("按区域筛选明细同时包含调出与调入，不漏调入单")
    void list_includesOutboundAndInbound() {
        when(areaService.resolveScopeAreaIds(AREA_A)).thenReturn(Set.of(AREA_A, 41L));

        List<TransferRecordDTO> rows = transferRecordService
                .getTransfersByDateRange(START, END, AREA_A, null);

        // 调出A(1) + 调入A(2) + A与下级子区域互调(3)，排除无关单(4)
        assertThat(rows).extracting(TransferRecordDTO::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("汇总总量与明细条数一致，调出/调入分列且计入调入")
    void summary_matchesRowsAndBreaksDownDirection() {
        Set<Long> scope = Set.of(AREA_A, 41L);
        when(areaService.resolveScopeAreaIds(AREA_A)).thenReturn(scope);

        TransferSummaryDTO summary = transferRecordService
                .getTransferSummary(START, END, AREA_A, null);

        List<TransferRecordDTO> rows = transferRecordService
                .getTransfersByDateRange(START, END, AREA_A, null);

        assertThat(summary.getTotalCount()).isEqualTo((long) rows.size());
        assertThat(summary.getTotalCount()).isEqualTo(3L);
        // 调出：单1(A->B)、单3(A->41)；调入：单2(B->A)、单3(41在scope内)
        assertThat(summary.getOutboundCount()).isEqualTo(2L);
        assertThat(summary.getInboundCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("不选区域时全部周期内有效调配计入，总量=明细条数")
    void noScope_countsAll() {
        when(areaService.resolveScopeAreaIds(null)).thenReturn(null);

        TransferSummaryDTO summary = transferRecordService
                .getTransferSummary(START, END, null, null);
        List<TransferRecordDTO> rows = transferRecordService
                .getTransfersByDateRange(START, END, null, null);

        assertThat(summary.getTotalCount()).isEqualTo(4L);
        assertThat(rows).hasSize(4);
        assertThat(summary.getOutboundCount()).isEqualTo(4L);
        assertThat(summary.getInboundCount()).isEqualTo(4L);
    }

    private TransferRecord transfer(long id, long equipmentId, long from, long to) {
        TransferRecord t = new TransferRecord();
        t.setId(id);
        t.setTransferNo("T-" + id);
        t.setEquipmentId(equipmentId);
        t.setFromAreaId(from);
        t.setToAreaId(to);
        t.setStatus(1);
        t.setTransferDate(LocalDate.now());
        return t;
    }

    private Area area(long id) {
        Area a = new Area();
        a.setId(id);
        a.setName("区域" + id);
        a.setCode("A" + id);
        a.setPath("/A" + id);
        a.setStatus(1);
        return a;
    }

    private Equipment equipment(long id) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setEquipmentNo("E-" + id);
        e.setEquipmentName("设备" + id);
        return e;
    }
}
