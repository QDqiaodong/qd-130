
package com.example.maternal.service;

import com.example.maternal.dto.SpotCheckRecordDTO;
import com.example.maternal.dto.SpotCheckReviewerRequest;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.SpotCheckRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.repository.SpotCheckRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class SpotCheckReviewerServiceTest {

    private static final Long SPOT_CHECK_ID = 7L;
    private static final Long EQUIPMENT_ID = 3L;
    private static final Long AREA_ID = 5L;

    @Mock
    private SpotCheckRecordRepository spotCheckRecordRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private RepairOrderService repairOrderService;
    @Mock
    private AreaService areaService;

    @InjectMocks
    private SpotCheckService spotCheckService;

    private SpotCheckRecord unqualifiedRecord;

    @BeforeEach
    void setUp() {
        unqualifiedRecord = new SpotCheckRecord();
        unqualifiedRecord.setId(SPOT_CHECK_ID);
        unqualifiedRecord.setSpotCheckNo("SC202609120007");
        unqualifiedRecord.setEquipmentId(EQUIPMENT_ID);
        unqualifiedRecord.setAreaId(AREA_ID);
        unqualifiedRecord.setCheckDate(LocalDate.of(2026, 9, 12));
        unqualifiedRecord.setTemperature(new BigDecimal("55.20"));
        unqualifiedRecord.setQualified(false);
        unqualifiedRecord.setInspector("赵值班");

        Equipment equipment = new Equipment();
        equipment.setId(EQUIPMENT_ID);
        equipment.setEquipmentNo("EQ-003");
        equipment.setEquipmentName("温奶器");
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));
    }

    private SpotCheckReviewerRequest request(String reviewer) {
        SpotCheckReviewerRequest request = new SpotCheckReviewerRequest();
        request.setReviewer(reviewer);
        return request;
    }

    @Test
    @DisplayName("不合格抽检补填当班复核人成功，复核人与复核时间落库")
    void updateReviewer_unqualified_succeeds() {
        when(spotCheckRecordRepository.findById(SPOT_CHECK_ID))
                .thenReturn(Optional.of(unqualifiedRecord));
        when(spotCheckRecordRepository.save(any(SpotCheckRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SpotCheckRecordDTO dto = spotCheckService.updateReviewer(SPOT_CHECK_ID, request(" 钱复核 "));

        ArgumentCaptor<SpotCheckRecord> captor = ArgumentCaptor.forClass(SpotCheckRecord.class);
        verify(spotCheckRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getReviewer()).isEqualTo("钱复核");
        assertThat(captor.getValue().getReviewTime()).isNotNull();
        assertThat(dto.getReviewer()).isEqualTo("钱复核");
        assertThat(dto.getReviewTime()).isNotNull();
    }

    @Test
    @DisplayName("漏填当班复核人（未填或仅空白）时给出明确提示，不保存")
    void updateReviewer_missingReviewer_rejected() {
        when(spotCheckRecordRepository.findById(SPOT_CHECK_ID))
                .thenReturn(Optional.of(unqualifiedRecord));

        assertThatThrownBy(() -> spotCheckService.updateReviewer(SPOT_CHECK_ID, request(null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("当班复核人不能为空");
        assertThatThrownBy(() -> spotCheckService.updateReviewer(SPOT_CHECK_ID, request("   ")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("当班复核人不能为空");
        assertThatThrownBy(() -> spotCheckService.updateReviewer(SPOT_CHECK_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("当班复核人不能为空");

        verify(spotCheckRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("合格抽检无需补填复核人，补填被拒绝")
    void updateReviewer_qualified_rejected() {
        unqualifiedRecord.setQualified(true);
        when(spotCheckRecordRepository.findById(SPOT_CHECK_ID))
                .thenReturn(Optional.of(unqualifiedRecord));

        assertThatThrownBy(() -> spotCheckService.updateReviewer(SPOT_CHECK_ID, request("钱复核")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("合格抽检无需补填当班复核人");

        verify(spotCheckRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("已补填过复核人的抽检记录不允许重复提交")
    void updateReviewer_alreadyReviewed_rejected() {
        unqualifiedRecord.setReviewer("钱复核");
        when(spotCheckRecordRepository.findById(SPOT_CHECK_ID))
                .thenReturn(Optional.of(unqualifiedRecord));

        assertThatThrownBy(() -> spotCheckService.updateReviewer(SPOT_CHECK_ID, request("孙复核")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("请勿重复提交");

        verify(spotCheckRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("抽检记录不存在时补填复核人给出明确提示")
    void updateReviewer_recordNotFound_rejected() {
        when(spotCheckRecordRepository.findById(SPOT_CHECK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spotCheckService.updateReviewer(SPOT_CHECK_ID, request("钱复核")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("抽检记录不存在");

        verify(spotCheckRecordRepository, never()).save(any());
    }
}
