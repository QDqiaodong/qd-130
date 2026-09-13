
package com.example.maternal.service;

import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.RepairOrder;
import com.example.maternal.entity.SpotCheckRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionRecordRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.repository.SpotCheckRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotCheckRepairServiceTest {

    private static final Long SPOT_CHECK_ID = 7L;
    private static final Long EQUIPMENT_ID = 3L;
    private static final Long AREA_ID = 5L;

    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private InspectionRecordRepository inspectionRecordRepository;
    @Mock
    private SpotCheckRecordRepository spotCheckRecordRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private AreaService areaService;

    @InjectMocks
    private RepairOrderService repairOrderService;

    private SpotCheckRecord unqualifiedRecord() {
        SpotCheckRecord record = new SpotCheckRecord();
        record.setId(SPOT_CHECK_ID);
        record.setSpotCheckNo("SC202609120007");
        record.setEquipmentId(EQUIPMENT_ID);
        record.setAreaId(AREA_ID);
        record.setCheckDate(LocalDate.of(2026, 9, 12));
        record.setTemperature(new BigDecimal("55.20"));
        record.setQualified(false);
        record.setAbnormalDesc("抽检温度55.20℃，不在合格区间40.00~50.00℃");
        record.setInspector("赵值班");
        record.setReviewer("钱复核");
        return record;
    }

    @BeforeEach
    void setUp() {
        Equipment equipment = new Equipment();
        equipment.setId(EQUIPMENT_ID);
        equipment.setEquipmentNo("EQ-003");
        equipment.setEquipmentName("温奶器");
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));
    }

    @Test
    @DisplayName("不合格抽检可创建报修单，报修单写入抽检来源而非巡检来源")
    void createRepairFromSpotCheck_unqualified_succeeds() {
        when(spotCheckRecordRepository.findById(SPOT_CHECK_ID))
                .thenReturn(Optional.of(unqualifiedRecord()));
        when(repairOrderRepository.save(any(RepairOrder.class))).thenAnswer(invocation -> {
            RepairOrder order = invocation.getArgument(0);
            order.setId(70L);
            return order;
        });

        RepairOrderDTO dto = repairOrderService.createRepairOrderFromSpotCheck(SPOT_CHECK_ID, "赵值班");

        assertThat(dto.getSpotCheckId()).isEqualTo(SPOT_CHECK_ID);
        assertThat(dto.getInspectionId()).isNull();
        assertThat(dto.getStatus()).isEqualTo(RepairOrderService.STATUS_PENDING);
        assertThat(dto.getFaultDesc()).contains("55.20");
        verify(repairOrderRepository).save(any(RepairOrder.class));
        verify(inspectionRecordRepository, never()).findById(any());
    }

    @Test
    @DisplayName("合格抽检不能创建报修单")
    void createRepairFromSpotCheck_qualified_rejected() {
        SpotCheckRecord record = unqualifiedRecord();
        record.setQualified(true);
        when(spotCheckRecordRepository.findById(SPOT_CHECK_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> repairOrderService.createRepairOrderFromSpotCheck(SPOT_CHECK_ID, "赵值班"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("仅抽检结论为不合格的记录才能创建报修单");

        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("不合格抽检未补填当班复核人时不能转报修")
    void createRepairFromSpotCheck_missingReviewer_rejected() {
        SpotCheckRecord record = unqualifiedRecord();
        record.setReviewer(null);
        when(spotCheckRecordRepository.findById(SPOT_CHECK_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> repairOrderService.createRepairOrderFromSpotCheck(SPOT_CHECK_ID, "赵值班"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("尚未补填当班复核人");

        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("复核人仅填空白字符视为未补填，同样不能转报修")
    void createRepairFromSpotCheck_blankReviewer_rejected() {
        SpotCheckRecord record = unqualifiedRecord();
        record.setReviewer("   ");
        when(spotCheckRecordRepository.findById(SPOT_CHECK_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> repairOrderService.createRepairOrderFromSpotCheck(SPOT_CHECK_ID, "赵值班"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("尚未补填当班复核人");

        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("同一条抽检记录重复补报修被拒绝")
    void createRepairFromSpotCheck_duplicate_rejected() {
        when(spotCheckRecordRepository.findById(SPOT_CHECK_ID))
                .thenReturn(Optional.of(unqualifiedRecord()));
        when(repairOrderRepository.existsBySpotCheckId(SPOT_CHECK_ID)).thenReturn(true);

        assertThatThrownBy(() -> repairOrderService.createRepairOrderFromSpotCheck(SPOT_CHECK_ID, "赵值班"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已创建报修单，请勿重复提交");

        verify(repairOrderRepository, never()).save(any());
    }
}
