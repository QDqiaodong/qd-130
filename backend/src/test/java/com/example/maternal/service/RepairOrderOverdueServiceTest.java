package com.example.maternal.service;

import com.example.maternal.dto.OverdueRepairsDTO;
import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.dto.RepairUrgeRequest;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.RepairOrder;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionRecordRepository;
import com.example.maternal.repository.RepairOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepairOrderOverdueServiceTest {

    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private InspectionRecordRepository inspectionRecordRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private AreaService areaService;

    @InjectMocks
    private RepairOrderService repairOrderService;

    private RepairOrder pendingOrder(long id, long areaId, LocalDateTime createdAt) {
        RepairOrder order = new RepairOrder();
        order.setId(id);
        order.setRepairNo("RP-" + id);
        order.setInspectionId(1000L + id);
        order.setEquipmentId(2000L + id);
        order.setAreaId(areaId);
        order.setStatus(RepairOrderService.STATUS_PENDING);
        order.setCreatedAt(createdAt);
        return order;
    }

    private Area area(long id, String name) {
        Area a = new Area();
        a.setId(id);
        a.setName(name);
        a.setCode("AREA-" + id);
        a.setPath("/AREA-" + id);
        a.setStatus(1);
        return a;
    }

    @Test
    @DisplayName("等待小时数为空或不大于0时抛出明确提示")
    void getOverdueRepairs_invalidHours_rejected() {
        assertThatThrownBy(() -> repairOrderService.getOverdueRepairs(null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("约定等待小时数必须大于0");
        assertThatThrownBy(() -> repairOrderService.getOverdueRepairs(0, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("约定等待小时数必须大于0");
        assertThatThrownBy(() -> repairOrderService.getOverdueRepairs(-3, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("约定等待小时数必须大于0");
    }

    @Test
    @DisplayName("等待小时数超过上限时抛出明确提示")
    void getOverdueRepairs_hoursTooLarge_rejected() {
        assertThatThrownBy(() -> repairOrderService
                .getOverdueRepairs(RepairOrderService.MAX_WAIT_HOURS + 1, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("约定等待小时数不能超过");
    }

    @Test
    @DisplayName("所选区域不存在时抛出明确提示")
    void getOverdueRepairs_areaNotFound_rejected() {
        when(areaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> repairOrderService.getOverdueRepairs(24, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("所选区域不存在");
    }

    @Test
    @DisplayName("超时件数与清单来自同一批结果，刷新后行数与件数一致")
    void getOverdueRepairs_countMatchesListRows() {
        LocalDateTime now = LocalDateTime.now();
        RepairOrder inScope1 = pendingOrder(1L, 4L, now.minusHours(50));
        RepairOrder inScope2 = pendingOrder(2L, 4L, now.minusHours(30));
        RepairOrder outOfScope = pendingOrder(3L, 5L, now.minusHours(60));
        when(repairOrderRepository.findOverduePending(any()))
                .thenReturn(List.of(inScope1, inScope2, outOfScope));

        Area parent = area(1L, "商超A区");
        when(areaRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(areaService.resolveScopeAreaIds(1L)).thenReturn(Set.of(4L));

        OverdueRepairsDTO result = repairOrderService.getOverdueRepairs(24, 1L);

        assertThat(result.getWaitHours()).isEqualTo(24);
        assertThat(result.getAreaName()).isEqualTo("商超A区");
        // 只包含区域内已超时待处理单，件数恒等于清单行数
        assertThat(result.getOrders()).hasSize(2);
        assertThat(result.getOverdueCount()).isEqualTo(result.getOrders().size());
        assertThat(result.getOrders()).allMatch(o -> o.getStatus() == 0);
        assertThat(result.getOrders()).allMatch(o -> o.getWaitedHours() != null && o.getWaitedHours() >= 24);
        assertThat(result.getOrders().get(0).getWaitedHours())
                .isGreaterThanOrEqualTo(result.getOrders().get(1).getWaitedHours());
    }

    @Test
    @DisplayName("没有超时单时返回0件与空清单")
    void getOverdueRepairs_noOverdue_returnsEmpty() {
        when(repairOrderRepository.findOverduePending(any())).thenReturn(List.of());
        when(areaService.resolveScopeAreaIds(anyLong())).thenReturn(null);

        OverdueRepairsDTO result = repairOrderService.getOverdueRepairs(24, null);

        assertThat(result.getOverdueCount()).isZero();
        assertThat(result.getOrders()).isEmpty();
    }

    @Test
    @DisplayName("催办成功：改跟进人并留下催办说明与催办时间，状态保持待处理")
    void urgeRepair_success_updatesFollowUpAndNote() {
        RepairOrder order = pendingOrder(1L, 4L, LocalDateTime.now().minusHours(30));
        order.setRepairman("张三");
        when(repairOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(repairOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RepairUrgeRequest request = new RepairUrgeRequest();
        request.setFollowUpPerson("李四");
        request.setUrgeNote("已超时，请尽快跟进处理");
        RepairOrderDTO dto = repairOrderService.urgeRepair(1L, request);

        assertThat(dto.getRepairman()).isEqualTo("李四");
        assertThat(dto.getUrgeNote()).isEqualTo("已超时，请尽快跟进处理");
        assertThat(dto.getUrgeTime()).isNotNull();
        assertThat(dto.getStatus()).isEqualTo(RepairOrderService.STATUS_PENDING);
    }

    @Test
    @DisplayName("催办时跟进人留空则保留原跟进人")
    void urgeRepair_blankFollowUpPerson_keepsOriginal() {
        RepairOrder order = pendingOrder(1L, 4L, LocalDateTime.now().minusHours(30));
        order.setRepairman("张三");
        when(repairOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(repairOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RepairUrgeRequest request = new RepairUrgeRequest();
        request.setFollowUpPerson("  ");
        request.setUrgeNote("再次催办");
        RepairOrderDTO dto = repairOrderService.urgeRepair(1L, request);

        assertThat(dto.getRepairman()).isEqualTo("张三");
    }

    @Test
    @DisplayName("催办说明为空时抛出明确提示")
    void urgeRepair_blankNote_rejected() {
        RepairOrder order = pendingOrder(1L, 4L, LocalDateTime.now().minusHours(30));
        when(repairOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        RepairUrgeRequest request = new RepairUrgeRequest();
        request.setFollowUpPerson("李四");
        request.setUrgeNote("  ");
        assertThatThrownBy(() -> repairOrderService.urgeRepair(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("催办说明不能为空");
    }

    @Test
    @DisplayName("非待处理单不允许催办")
    void urgeRepair_notPending_rejected() {
        RepairOrder order = pendingOrder(1L, 4L, LocalDateTime.now().minusHours(30));
        order.setStatus(RepairOrderService.STATUS_REPAIRING);
        when(repairOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        RepairUrgeRequest request = new RepairUrgeRequest();
        request.setUrgeNote("催办");
        assertThatThrownBy(() -> repairOrderService.urgeRepair(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("仅待处理的报修单才能催办");
    }

    @Test
    @DisplayName("催办的报修单不存在时抛出明确提示")
    void urgeRepair_orderNotFound_rejected() {
        when(repairOrderRepository.findById(99L)).thenReturn(Optional.empty());

        RepairUrgeRequest request = new RepairUrgeRequest();
        request.setUrgeNote("催办");
        assertThatThrownBy(() -> repairOrderService.urgeRepair(99L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("报修单不存在");
    }
}
