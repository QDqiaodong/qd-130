
package com.example.maternal.service;

import com.example.maternal.dto.HealthDashboardDTO;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.InspectionPlan;
import com.example.maternal.entity.InspectionRecord;
import com.example.maternal.entity.RepairOrder;
import com.example.maternal.entity.TransferRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionPlanRepository;
import com.example.maternal.repository.InspectionRecordRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthDashboardServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private InspectionRecordRepository inspectionRecordRepository;
    @Mock
    private InspectionPlanRepository inspectionPlanRepository;
    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private TransferRecordRepository transferRecordRepository;

    @InjectMocks
    private HealthDashboardService healthDashboardService;

    private final LocalDate start = LocalDate.now().minusDays(29);
    private final LocalDate end = LocalDate.now();

    private Equipment equipment(long id, long areaId, String type) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setEquipmentNo("EQ-" + id);
        e.setEquipmentName(type);
        e.setEquipmentType(type);
        e.setCurrentAreaId(areaId);
        e.setInitialAreaId(areaId);
        return e;
    }

    private Area area(long id, String name, String path) {
        Area a = new Area();
        a.setId(id);
        a.setName(name);
        a.setCode("AREA-" + id);
        a.setPath(path);
        a.setStatus(1);
        return a;
    }

    @BeforeEach
    void setUp() {
        when(inspectionRecordRepository.findForDashboard(start, end, null, null, null))
                .thenReturn(List.of());
        when(repairOrderRepository.findForDashboard(null, null, null, null))
                .thenReturn(List.of());
        when(transferRecordRepository.findForDashboard(start, end, null, null))
                .thenReturn(List.of());
        when(inspectionPlanRepository.findByStatus(1)).thenReturn(List.of());
    }

    @Test
    @DisplayName("开始日期晚于结束日期时抛出明确提示")
    void getDashboard_startAfterEnd_rejected() {
        assertThatThrownBy(() -> healthDashboardService
                .getDashboard(end, start, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("开始日期不能晚于结束日期");
    }

    @Test
    @DisplayName("结束日期晚于今天时抛出明确提示")
    void getDashboard_endAfterToday_rejected() {
        assertThatThrownBy(() -> healthDashboardService
                .getDashboard(start, end.plusDays(1), null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("结束日期不能晚于今天");
    }

    @Test
    @DisplayName("日期范围超过366天时抛出明确提示")
    void getDashboard_rangeTooLong_rejected() {
        assertThatThrownBy(() -> healthDashboardService
                .getDashboard(end.minusDays(366), end, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("日期范围最长不能超过366天");
    }

    @Test
    @DisplayName("所选区域不存在时抛出明确提示")
    void getDashboard_areaNotFound_rejected() {
        when(areaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> healthDashboardService.getDashboard(start, end, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("所选区域不存在");
    }

    @Test
    @DisplayName("设备总数、待处理/维修中、巡检异常率、调配次数按同一批数据聚合，刷新后口径一致")
    void getDashboard_aggregatesMetricsConsistently() {
        Area area = area(4L, "A1母婴室", "/AREA-A/AREA-A1");
        when(areaRepository.findAll()).thenReturn(List.of(area));

        Equipment e1 = equipment(1L, 4L, "温奶器");
        Equipment e2 = equipment(2L, 4L, "护理台");
        when(equipmentRepository.findAll()).thenReturn(List.of(e1, e2));

        InspectionRecord normal = new InspectionRecord();
        normal.setEquipmentId(1L);
        normal.setAreaId(4L);
        normal.setResult(1);
        InspectionRecord abnormal = new InspectionRecord();
        abnormal.setEquipmentId(2L);
        abnormal.setAreaId(4L);
        abnormal.setResult(2);
        when(inspectionRecordRepository.findForDashboard(start, end, null, null, null))
                .thenReturn(List.of(normal, abnormal));

        RepairOrder pending = new RepairOrder();
        pending.setEquipmentId(1L);
        pending.setAreaId(4L);
        pending.setStatus(0);
        RepairOrder repairing = new RepairOrder();
        repairing.setEquipmentId(2L);
        repairing.setAreaId(4L);
        repairing.setStatus(1);
        RepairOrder restored = new RepairOrder();
        restored.setEquipmentId(1L);
        restored.setAreaId(4L);
        restored.setStatus(2);
        when(repairOrderRepository.findForDashboard(null, null, null, null))
                .thenReturn(List.of(pending, repairing, restored));

        TransferRecord t1 = new TransferRecord();
        t1.setEquipmentId(1L);
        t1.setFromAreaId(4L);
        t1.setToAreaId(5L);
        when(transferRecordRepository.findForDashboard(start, end, null, null))
                .thenReturn(List.of(t1));

        HealthDashboardDTO dashboard = healthDashboardService.getDashboard(start, end, null);

        assertThat(dashboard.getMetrics().getTotalEquipmentCount()).isEqualTo(2L);
        assertThat(dashboard.getMetrics().getPendingRepairCount()).isEqualTo(1L);
        assertThat(dashboard.getMetrics().getRepairingCount()).isEqualTo(1L);
        assertThat(dashboard.getMetrics().getInspectionCount()).isEqualTo(2L);
        assertThat(dashboard.getMetrics().getAbnormalCount()).isEqualTo(1L);
        assertThat(dashboard.getMetrics().getAbnormalRate()).isEqualTo(50.0);
        assertThat(dashboard.getMetrics().getTransferCount()).isEqualTo(1L);
        // 无启用计划时完成率不展示为虚假的 0%，而是 null（前端显示 —）
        assertThat(dashboard.getMetrics().getInspectionCompletionRate()).isNull();
        assertThat(dashboard.getMetrics().getInspectionPlannedCount()).isZero();

        // 区域排行行与总量指标一致
        assertThat(dashboard.getAreaRankings()).hasSize(1);
        assertThat(dashboard.getAreaRankings().get(0).getPendingRepairCount()).isEqualTo(1L);
        assertThat(dashboard.getAreaRankings().get(0).getRepairingCount()).isEqualTo(1L);
        assertThat(dashboard.getAreaRankings().get(0).getTransferOutCount()).isEqualTo(1L);
        assertThat(dashboard.getAreaRankings().get(0).getTransferInCount()).isZero();

        // 类型排行按设备总数降序
        assertThat(dashboard.getTypeRankings()).hasSize(2);
        assertThat(dashboard.getTypeRankings().get(0).getTotalEquipmentCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("巡检完成率按启用计划周期折算：每日计划×设备数，近30天应为30次")
    void getDashboard_completionRateBasedOnPlans() {
        Area area = area(4L, "A1母婴室", "/AREA-A/AREA-A1");
        when(areaRepository.findAll()).thenReturn(List.of(area));

        Equipment e1 = equipment(1L, 4L, "温奶器");
        when(equipmentRepository.findAll()).thenReturn(List.of(e1));

        InspectionPlan dailyPlan = new InspectionPlan();
        dailyPlan.setId(100L);
        dailyPlan.setAreaId(4L);
        dailyPlan.setCycleType(1);
        dailyPlan.setStatus(1);
        when(inspectionPlanRepository.findByStatus(1)).thenReturn(List.of(dailyPlan));

        InspectionRecord done = new InspectionRecord();
        done.setEquipmentId(1L);
        done.setAreaId(4L);
        done.setResult(1);
        when(inspectionRecordRepository.findForDashboard(start, end, null, null, null))
                .thenReturn(List.of(done));

        HealthDashboardDTO dashboard = healthDashboardService.getDashboard(start, end, null);

        assertThat(dashboard.getMetrics().getInspectionPlannedCount()).isEqualTo(30L);
        assertThat(dashboard.getMetrics().getInspectionCompletedCount()).isEqualTo(1L);
        assertThat(dashboard.getMetrics().getInspectionCompletionRate())
                .isEqualTo(Math.round(1 * 1000.0 / 30) / 10.0);
    }

    @Test
    @DisplayName("选择父区域时统计包含其下级区域设备")
    void getDashboard_parentAreaIncludesDescendants() {
        Area parent = area(1L, "商超A区", "/AREA-A");
        Area child = area(4L, "A1母婴室", "/AREA-A/AREA-A1");
        when(areaRepository.findAll()).thenReturn(List.of(parent, child));
        when(areaRepository.findById(1L)).thenReturn(Optional.of(parent));

        Equipment e1 = equipment(1L, 4L, "温奶器");
        when(equipmentRepository.findAll()).thenReturn(List.of(e1));

        HealthDashboardDTO dashboard = healthDashboardService.getDashboard(start, end, 1L);

        assertThat(dashboard.getAreaName()).isEqualTo("商超A区");
        assertThat(dashboard.getMetrics().getTotalEquipmentCount()).isEqualTo(1L);
        assertThat(dashboard.getAreaRankings()).hasSize(1);
        assertThat(dashboard.getAreaRankings().get(0).getAreaId()).isEqualTo(4L);
    }
}
