
package com.example.maternal.service;

import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.dto.RepairStatusRequest;
import com.example.maternal.entity.RepairOrder;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionRecordRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.repository.SpotCheckRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 维修完成点「已恢复」前的复用前试机门禁：必须填写试机结论才能结单；
 * 列表支持按是否已试机筛选，是否已试机由服务端按落库结论统一计算。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepairOrderTrialGateTest {

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

    private RepairOrder repairingOrder(Long id, String trialResult) {
        RepairOrder order = new RepairOrder();
        order.setId(id);
        order.setRepairNo("RP-" + id);
        order.setEquipmentId(2000L + id);
        order.setAreaId(4L);
        order.setStatus(RepairOrderService.STATUS_REPAIRING);
        order.setRepairman("王师傅");
        order.setTrialResult(trialResult);
        return order;
    }

    private RepairStatusRequest restoreRequest(String trialResult) {
        RepairStatusRequest request = new RepairStatusRequest();
        request.setStatus(RepairOrderService.STATUS_RESTORED);
        request.setRepairNote("更换温控元件");
        request.setTrialResult(trialResult);
        return request;
    }

    @Test
    @DisplayName("未填写复用前试机结论不能结单恢复，并给出明确提示")
    void restore_withoutTrialResult_rejected() {
        RepairOrder order = repairingOrder(1L, null);
        when(repairOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> repairOrderService.updateStatus(1L, restoreRequest(null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("复用前试机结论")
                .hasMessageContaining("先试机");
        assertThatThrownBy(() -> repairOrderService.updateStatus(1L, restoreRequest("   ")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("复用前试机结论");

        // 状态不能被改成已恢复，也不能落库
        assertThat(order.getStatus()).isEqualTo(RepairOrderService.STATUS_REPAIRING);
        assertThat(order.getFinishTime()).isNull();
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("填写试机结论后允许结单：状态恢复并落库试机结论、试机时间，标记已试机")
    void restore_withTrialResult_succeeds() {
        RepairOrder order = repairingOrder(2L, null);
        when(repairOrderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(repairOrderRepository.save(any(RepairOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        RepairOrderDTO dto = repairOrderService.updateStatus(2L,
                restoreRequest("通电试机30分钟，水温稳定在45℃，功能正常，可恢复复用"));

        assertThat(dto.getStatus()).isEqualTo(RepairOrderService.STATUS_RESTORED);
        assertThat(dto.getTrialResult()).contains("水温稳定在45℃");
        assertThat(dto.getTrialDone()).isTrue();
        assertThat(dto.getTrialTime()).isNotNull();
        assertThat(dto.getFinishTime()).isNotNull();
        verify(repairOrderRepository).save(any(RepairOrder.class));
    }

    @Test
    @DisplayName("试机结论超过500字时拒绝结单")
    void restore_trialResultTooLong_rejected() {
        RepairOrder order = repairingOrder(3L, null);
        when(repairOrderRepository.findById(3L)).thenReturn(Optional.of(order));

        String tooLong = "试机".repeat(251);
        assertThat(tooLong.length()).isGreaterThan(500);
        assertThatThrownBy(() -> repairOrderService.updateStatus(3L, restoreRequest(tooLong)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("试机结论不能超过500字");

        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("按已试机筛选只返回已落库试机结论的报修单")
    void getRepairs_filterTrialDone_onlyReturnsTrialed() {
        RepairOrder trialed = repairingOrder(10L, "试机合格，可恢复使用");
        trialed.setStatus(RepairOrderService.STATUS_RESTORED);
        RepairOrder notTrialed = repairingOrder(11L, null);
        when(repairOrderRepository.findForDashboard(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(trialed, notTrialed));

        List<RepairOrderDTO> result = repairOrderService.getRepairs(null, null, null, null, null, null, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getTrialDone()).isTrue();
    }

    @Test
    @DisplayName("按未试机筛选只返回未落库试机结论的报修单（含维修中/待处理）")
    void getRepairs_filterNotTrialed_onlyReturnsUntrialed() {
        RepairOrder trialed = repairingOrder(10L, "试机合格，可恢复使用");
        trialed.setStatus(RepairOrderService.STATUS_RESTORED);
        RepairOrder notTrialed = repairingOrder(11L, null);
        when(repairOrderRepository.findForDashboard(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(trialed, notTrialed));

        List<RepairOrderDTO> result = repairOrderService.getRepairs(null, null, null, null, null, null, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(11L);
        assertThat(result.get(0).getTrialDone()).isFalse();
    }

    @Test
    @DisplayName("试机筛选条件非法时抛出明确提示")
    void getRepairs_invalidTrialStatus_rejected() {
        assertThatThrownBy(() -> repairOrderService.getRepairs(null, null, null, null, null, null, 9))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("试机筛选条件不合法");
    }
}
