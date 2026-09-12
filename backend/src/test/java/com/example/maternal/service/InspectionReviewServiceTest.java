
package com.example.maternal.service;

import com.example.maternal.dto.InspectionRecordDTO;
import com.example.maternal.dto.InspectionReviewRequest;
import com.example.maternal.entity.InspectionRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionPlanRepository;
import com.example.maternal.repository.InspectionRecordRepository;
import com.example.maternal.repository.RepairOrderRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionReviewServiceTest {

    private static final Long INSPECTION_ID = 9L;

    @Mock
    private InspectionRecordRepository inspectionRecordRepository;
    @Mock
    private InspectionPlanRepository inspectionPlanRepository;
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
    private InspectionRecordService inspectionRecordService;

    private InspectionRecord abnormalRecord;

    @BeforeEach
    void setUp() {
        abnormalRecord = new InspectionRecord();
        abnormalRecord.setId(INSPECTION_ID);
        abnormalRecord.setInspectionNo("IN202609110001");
        abnormalRecord.setEquipmentId(8L);
        abnormalRecord.setAreaId(7L);
        abnormalRecord.setInspectionDate(LocalDate.of(2026, 9, 11));
        abnormalRecord.setResult(2);
        abnormalRecord.setAbnormalDesc("护理台软包破损，海绵外露");
        abnormalRecord.setInspector("钱工");
    }

    private InspectionReviewRequest baseRequest() {
        InspectionReviewRequest request = new InspectionReviewRequest();
        request.setReviewer("赵值班");
        request.setReviewTime(LocalDateTime.of(2026, 9, 11, 16, 30));
        request.setConfirmed(true);
        return request;
    }

    @Test
    @DisplayName("复核属实：落库复核人、复核时间与结论，记录允许转报修")
    void reviewInspection_confirmed_allowsRepair() {
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(abnormalRecord));
        when(inspectionRecordRepository.save(any(InspectionRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InspectionRecordDTO dto = inspectionRecordService.reviewInspection(INSPECTION_ID, baseRequest());

        ArgumentCaptor<InspectionRecord> captor = ArgumentCaptor.forClass(InspectionRecord.class);
        verify(inspectionRecordRepository).save(captor.capture());
        InspectionRecord saved = captor.getValue();
        assertThat(saved.getReviewer()).isEqualTo("赵值班");
        assertThat(saved.getReviewTime()).isEqualTo(LocalDateTime.of(2026, 9, 11, 16, 30));
        assertThat(saved.getReviewResult()).isEqualTo(InspectionRecordService.REVIEW_RESULT_CONFIRMED);
        assertThat(dto.getReviewResult()).isEqualTo(InspectionRecordService.REVIEW_RESULT_CONFIRMED);
        assertThat(dto.getCanRepair()).isTrue();
    }

    @Test
    @DisplayName("复核不属实：必须写说明，落库后不能再报修")
    void reviewInspection_rejected_requiresNoteAndBlocksRepair() {
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(abnormalRecord));
        when(inspectionRecordRepository.save(any(InspectionRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InspectionReviewRequest request = baseRequest();
        request.setConfirmed(false);
        request.setReviewNote("现场复测软包完好，为巡检误判");

        InspectionRecordDTO dto = inspectionRecordService.reviewInspection(INSPECTION_ID, request);

        ArgumentCaptor<InspectionRecord> captor = ArgumentCaptor.forClass(InspectionRecord.class);
        verify(inspectionRecordRepository).save(captor.capture());
        InspectionRecord saved = captor.getValue();
        assertThat(saved.getReviewResult()).isEqualTo(InspectionRecordService.REVIEW_RESULT_REJECTED);
        assertThat(saved.getReviewNote()).isEqualTo("现场复测软包完好，为巡检误判");
        assertThat(dto.getCanRepair()).isFalse();
    }

    @Test
    @DisplayName("复核不属实但未填说明时被拒绝，提示必须填写复核说明")
    void reviewInspection_rejectedWithoutNote_rejected() {
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(abnormalRecord));

        InspectionReviewRequest request = baseRequest();
        request.setConfirmed(false);
        request.setReviewNote("  ");

        assertThatThrownBy(() -> inspectionRecordService.reviewInspection(INSPECTION_ID, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("复核结论为不属实时必须填写复核说明");

        verify(inspectionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("漏填复核人、复核时间或复核结论时给出明确提示，不保存复核")
    void reviewInspection_missingFields_rejected() {
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(abnormalRecord));

        InspectionReviewRequest noReviewer = baseRequest();
        noReviewer.setReviewer("  ");
        assertThatThrownBy(() -> inspectionRecordService.reviewInspection(INSPECTION_ID, noReviewer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("复核人不能为空");

        InspectionReviewRequest noTime = baseRequest();
        noTime.setReviewTime(null);
        assertThatThrownBy(() -> inspectionRecordService.reviewInspection(INSPECTION_ID, noTime))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("复核时间不能为空");

        InspectionReviewRequest noConclusion = baseRequest();
        noConclusion.setConfirmed(null);
        assertThatThrownBy(() -> inspectionRecordService.reviewInspection(INSPECTION_ID, noConclusion))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("请选择复核结论");

        verify(inspectionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("已复核的记录再次复核被拒绝，提示请勿重复复核")
    void reviewInspection_duplicate_rejected() {
        abnormalRecord.setReviewer("钱值班");
        abnormalRecord.setReviewTime(LocalDateTime.of(2026, 9, 11, 15, 0));
        abnormalRecord.setReviewResult(InspectionRecordService.REVIEW_RESULT_CONFIRMED);
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(abnormalRecord));

        assertThatThrownBy(() -> inspectionRecordService.reviewInspection(INSPECTION_ID, baseRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("请勿重复复核");

        verify(inspectionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("正常巡检记录不需要复核，复核请求被拒绝")
    void reviewInspection_normalRecord_rejected() {
        abnormalRecord.setResult(1);
        abnormalRecord.setAbnormalDesc(null);
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(abnormalRecord));

        assertThatThrownBy(() -> inspectionRecordService.reviewInspection(INSPECTION_ID, baseRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("仅巡检结果为异常的记录需要复核");

        verify(inspectionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("复核不存在的巡检记录时提示记录不存在")
    void reviewInspection_recordNotFound_rejected() {
        when(inspectionRecordRepository.findById(INSPECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inspectionRecordService.reviewInspection(INSPECTION_ID, baseRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("巡检记录不存在");

        verify(inspectionRecordRepository, never()).save(any());
    }
}
