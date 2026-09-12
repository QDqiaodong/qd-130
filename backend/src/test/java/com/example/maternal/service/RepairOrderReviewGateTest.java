
package com.example.maternal.service;

import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.entity.InspectionRecord;
import com.example.maternal.entity.RepairOrder;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 巡检异常转报修的复核门禁：属实才允许转报修，未复核或不属实都不能报修。
 */
@ExtendWith(MockitoExtension.class)
class RepairOrderReviewGateTest {

    private static final Long INSPECTION_ID = 9L;
    private static final Long EQUIPMENT_ID = 8L;
    private static final Long AREA_ID = 7L;

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

    private InspectionRecord abnormalRecord;

    @BeforeEach
    void setUp() {
        abnormalRecord = new InspectionRecord();
        abnormalRecord.setId(INSPECTION_ID);
        abnormalRecord.setInspectionNo("IN202609110001");
        abnormalRecord.setEquipmentId(EQUIPMENT_ID);
        abnormalRecord.setAreaId(AREA_ID);
        abnormalRecord.setInspectionDate(LocalDate.of(2026, 9, 11));
        abnormalRecord.setResult(2);
        abnormalRecord.setAbnormalDesc("护理台软包破损，海绵外露");
        abnormalRecord.setInspector("钱工");
    }

    @Test
    @DisplayName("异常巡检未复核时不能创建报修单，提示需先复核属实")
    void createRepairOrder_notReviewed_rejected() {
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(abnormalRecord));

        assertThatThrownBy(() -> repairOrderService.createRepairOrder(INSPECTION_ID, "钱工"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("尚未复核")
                .hasMessageContaining("复核属实后才能转报修");

        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("复核结论为不属实时不能创建报修单")
    void createRepairOrder_reviewRejected_rejected() {
        abnormalRecord.setReviewer("赵值班");
        abnormalRecord.setReviewResult(InspectionRecordService.REVIEW_RESULT_REJECTED);
        abnormalRecord.setReviewNote("现场复测为误判");
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(abnormalRecord));

        assertThatThrownBy(() -> repairOrderService.createRepairOrder(INSPECTION_ID, "钱工"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("复核结论为不属实")
                .hasMessageContaining("不能转报修");

        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("复核属实后才允许转报修，报修单正常创建")
    void createRepairOrder_reviewConfirmed_succeeds() {
        abnormalRecord.setReviewer("赵值班");
        abnormalRecord.setReviewResult(InspectionRecordService.REVIEW_RESULT_CONFIRMED);
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(abnormalRecord));
        when(repairOrderRepository.save(any(RepairOrder.class))).thenAnswer(invocation -> {
            RepairOrder order = invocation.getArgument(0);
            order.setId(90L);
            return order;
        });

        RepairOrderDTO dto = repairOrderService.createRepairOrder(INSPECTION_ID, "钱工");

        assertThat(dto.getInspectionId()).isEqualTo(INSPECTION_ID);
        assertThat(dto.getStatus()).isEqualTo(RepairOrderService.STATUS_PENDING);
        verify(repairOrderRepository).save(any(RepairOrder.class));
    }

    @Test
    @DisplayName("复核属实的记录重复创建报修单仍被拒绝")
    void createRepairOrder_confirmedButDuplicate_rejected() {
        abnormalRecord.setReviewer("赵值班");
        abnormalRecord.setReviewResult(InspectionRecordService.REVIEW_RESULT_CONFIRMED);
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(abnormalRecord));
        when(repairOrderRepository.existsByInspectionId(INSPECTION_ID)).thenReturn(true);

        assertThatThrownBy(() -> repairOrderService.createRepairOrder(INSPECTION_ID, "钱工"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已创建报修单，请勿重复提交");

        verify(repairOrderRepository, never()).save(any());
    }
}
