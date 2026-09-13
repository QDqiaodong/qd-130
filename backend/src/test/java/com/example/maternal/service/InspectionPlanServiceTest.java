package com.example.maternal.service;

import com.example.maternal.dto.InspectionPlanDTO;
import com.example.maternal.dto.InspectionPlanRequest;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.InspectionPlan;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.InspectionPlanRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionPlanServiceTest {

    private static final Long AREA_A = 4L;
    private static final Long AREA_A_CHILD = 41L;
    private static final Long AREA_OTHER = 9L;
    private static final Long EQUIPMENT_IN_A = 10L;
    private static final Long EQUIPMENT_IN_OTHER = 11L;

    private static final LocalDate OLD_DATE = LocalDate.of(2026, 9, 20);
    private static final LocalDate NEW_DATE = LocalDate.of(2026, 10, 1);

    @Mock
    private InspectionPlanRepository inspectionPlanRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private AreaService areaService;

    @InjectMocks
    private InspectionPlanService inspectionPlanService;

    // ---------- 列表筛选 ----------

    @Test
    @DisplayName("按状态筛选只返回对应启用状态的计划")
    void list_filtersByStatus() {
        InspectionPlan enabled = plan(1L, 1, AREA_A, null);
        InspectionPlan disabled = plan(2L, 0, AREA_A, null);
        when(inspectionPlanRepository.findAllOrdered()).thenReturn(List.of(enabled, disabled));
        when(areaService.resolveScopeAreaIds(null)).thenReturn(null);

        List<InspectionPlanDTO> rows = inspectionPlanService.getAllPlans(null, 1);

        assertThat(rows).extracting(InspectionPlanDTO::getId).containsExactly(1L);
    }

    @Test
    @DisplayName("按母婴室筛选包含下级区域的区域计划与设备当前所在区域的设备计划")
    void list_filtersByAreaScope() {
        InspectionPlan areaPlanInScope = plan(1L, 1, AREA_A, null);
        InspectionPlan childAreaPlan = plan(2L, 1, AREA_A_CHILD, null);
        InspectionPlan equipmentPlanInScope = plan(3L, 1, null, EQUIPMENT_IN_A);
        InspectionPlan equipmentPlanOutOfScope = plan(4L, 1, null, EQUIPMENT_IN_OTHER);
        InspectionPlan areaPlanOutOfScope = plan(5L, 1, AREA_OTHER, null);
        when(inspectionPlanRepository.findAllOrdered())
                .thenReturn(List.of(areaPlanInScope, childAreaPlan, equipmentPlanInScope,
                        equipmentPlanOutOfScope, areaPlanOutOfScope));
        when(areaService.resolveScopeAreaIds(AREA_A)).thenReturn(Set.of(AREA_A, AREA_A_CHILD));
        when(equipmentRepository.findAll()).thenReturn(List.of(
                equipment(EQUIPMENT_IN_A, AREA_A), equipment(EQUIPMENT_IN_OTHER, AREA_OTHER)));

        List<InspectionPlanDTO> rows = inspectionPlanService.getAllPlans(AREA_A, null);

        assertThat(rows).extracting(InspectionPlanDTO::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("不带筛选条件时返回全部计划")
    void list_noFilterReturnsAll() {
        when(inspectionPlanRepository.findAllOrdered())
                .thenReturn(List.of(plan(1L, 1, AREA_A, null), plan(2L, 0, null, EQUIPMENT_IN_OTHER)));
        when(areaService.resolveScopeAreaIds(null)).thenReturn(null);

        List<InspectionPlanDTO> rows = inspectionPlanService.getAllPlans(null, null);

        assertThat(rows).hasSize(2);
    }

    // ---------- 停用计划下次巡检日期保护 ----------

    @Test
    @DisplayName("已停用计划修改下次巡检日期被拒绝")
    void update_disabledPlanCannotChangeNextInspectionDate() {
        InspectionPlan disabled = plan(1L, 0, AREA_A, null);
        disabled.setNextInspectionDate(OLD_DATE);
        when(inspectionPlanRepository.findById(1L)).thenReturn(Optional.of(disabled));
        when(areaRepository.existsById(AREA_A)).thenReturn(true);

        InspectionPlanRequest request = baseRequest();
        request.setNextInspectionDate(NEW_DATE);

        assertThatThrownBy(() -> inspectionPlanService.updatePlan(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已停用")
                .hasMessageContaining("下次巡检日期");

        verify(inspectionPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("已停用计划日期未变时仍可修改其他字段")
    void update_disabledPlanKeepsDateCanEditOthers() {
        InspectionPlan disabled = plan(1L, 0, AREA_A, null);
        disabled.setNextInspectionDate(OLD_DATE);
        when(inspectionPlanRepository.findById(1L)).thenReturn(Optional.of(disabled));
        when(areaRepository.existsById(AREA_A)).thenReturn(true);
        when(inspectionPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InspectionPlanRequest request = baseRequest();
        request.setNextInspectionDate(OLD_DATE);
        request.setInspector("李四");

        InspectionPlanDTO dto = inspectionPlanService.updatePlan(1L, request);

        assertThat(dto.getInspector()).isEqualTo("李四");
        assertThat(dto.getNextInspectionDate()).isEqualTo(OLD_DATE);
        assertThat(dto.getStatus()).isEqualTo(0);
    }

    @Test
    @DisplayName("已停用计划同请求重新启用时允许修改下次巡检日期")
    void update_disabledPlanReenabledCanChangeDate() {
        InspectionPlan disabled = plan(1L, 0, AREA_A, null);
        disabled.setNextInspectionDate(OLD_DATE);
        when(inspectionPlanRepository.findById(1L)).thenReturn(Optional.of(disabled));
        when(areaRepository.existsById(AREA_A)).thenReturn(true);
        when(inspectionPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InspectionPlanRequest request = baseRequest();
        request.setNextInspectionDate(NEW_DATE);
        request.setStatus(1);

        InspectionPlanDTO dto = inspectionPlanService.updatePlan(1L, request);

        assertThat(dto.getNextInspectionDate()).isEqualTo(NEW_DATE);
        assertThat(dto.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("启用中的计划允许修改下次巡检日期")
    void update_enabledPlanCanChangeDate() {
        InspectionPlan enabled = plan(1L, 1, AREA_A, null);
        enabled.setNextInspectionDate(OLD_DATE);
        when(inspectionPlanRepository.findById(1L)).thenReturn(Optional.of(enabled));
        when(areaRepository.existsById(AREA_A)).thenReturn(true);
        when(inspectionPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InspectionPlanRequest request = baseRequest();
        request.setNextInspectionDate(NEW_DATE);

        InspectionPlanDTO dto = inspectionPlanService.updatePlan(1L, request);

        assertThat(dto.getNextInspectionDate()).isEqualTo(NEW_DATE);
    }

    // ---------- 测试数据 ----------

    private InspectionPlan plan(Long id, Integer status, Long areaId, Long equipmentId) {
        InspectionPlan plan = new InspectionPlan();
        plan.setId(id);
        plan.setPlanNo("PLAN-" + id);
        plan.setPlanName("计划" + id);
        plan.setAreaId(areaId);
        plan.setEquipmentId(equipmentId);
        plan.setCycleType(2);
        plan.setStatus(status);
        return plan;
    }

    private Equipment equipment(Long id, Long currentAreaId) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setEquipmentNo("EQ-" + id);
        equipment.setEquipmentName("设备" + id);
        equipment.setCurrentAreaId(currentAreaId);
        return equipment;
    }

    private InspectionPlanRequest baseRequest() {
        InspectionPlanRequest request = new InspectionPlanRequest();
        request.setPlanName("计划");
        request.setAreaId(AREA_A);
        request.setCycleType(2);
        request.setNextInspectionDate(OLD_DATE);
        request.setInspector("张三");
        return request;
    }
}
