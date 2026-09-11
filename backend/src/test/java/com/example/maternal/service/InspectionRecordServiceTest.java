
package com.example.maternal.service;

import com.example.maternal.dto.InspectionRecordDTO;
import com.example.maternal.dto.InspectionRecordRequest;
import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.InspectionPlan;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionRecordServiceTest {

    private static final Long EQUIPMENT_ID = 10L;
    private static final Long AREA_A_ID = 1L;
    private static final Long AREA_B_ID = 2L;
    private static final Long AREA_PLAN_ID = 100L;
    private static final Long EQUIPMENT_PLAN_ID = 200L;

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

    @InjectMocks
    private InspectionRecordService inspectionRecordService;

    private Equipment equipment;

    @BeforeEach
    void setUp() {
        equipment = new Equipment();
        equipment.setId(EQUIPMENT_ID);
        equipment.setEquipmentNo("EQ001");
        equipment.setEquipmentName("温奶器");
        equipment.setCurrentAreaId(AREA_A_ID);
        equipment.setInitialAreaId(AREA_A_ID);

        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));
    }

    private InspectionRecordRequest baseRequest() {
        InspectionRecordRequest request = new InspectionRecordRequest();
        request.setEquipmentId(EQUIPMENT_ID);
        request.setInspectionDate(LocalDate.of(2026, 9, 11));
        request.setResult(1);
        request.setInspector("张三");
        return request;
    }

    private InspectionPlan areaPlan(Long areaId) {
        InspectionPlan plan = new InspectionPlan();
        plan.setId(AREA_PLAN_ID);
        plan.setPlanNo("PL-AREA");
        plan.setPlanName("一楼母婴室区域巡检");
        plan.setEquipmentId(null);
        plan.setAreaId(areaId);
        plan.setCycleType(1);
        plan.setStatus(1);
        return plan;
    }

    private InspectionPlan equipmentPlan(Long equipmentId) {
        InspectionPlan plan = new InspectionPlan();
        plan.setId(EQUIPMENT_PLAN_ID);
        plan.setPlanNo("PL-EQ");
        plan.setPlanName("温奶器专项巡检");
        plan.setEquipmentId(equipmentId);
        plan.setAreaId(null);
        plan.setCycleType(2);
        plan.setStatus(1);
        return plan;
    }

    private Area area(Long id, String name) {
        Area area = new Area();
        area.setId(id);
        area.setName(name);
        area.setCode("AREA-" + id);
        area.setLevel(1);
        return area;
    }

    @Test
    @DisplayName("区域计划：设备仍在计划所属区域时可以正常登记")
    void createInspection_areaPlan_deviceStillInArea_succeeds() {
        InspectionRecordRequest request = baseRequest();
        request.setPlanId(AREA_PLAN_ID);
        when(inspectionPlanRepository.findById(AREA_PLAN_ID)).thenReturn(Optional.of(areaPlan(AREA_A_ID)));
        when(inspectionRecordRepository.save(any(InspectionRecord.class))).thenAnswer(invocation -> {
            InspectionRecord record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });

        InspectionRecordDTO dto = inspectionRecordService.createInspection(request);

        assertThat(dto).isNotNull();
        ArgumentCaptor<InspectionRecord> captor = ArgumentCaptor.forClass(InspectionRecord.class);
        verify(inspectionRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getAreaId()).isEqualTo(AREA_A_ID);
        verify(inspectionPlanRepository).save(any(InspectionPlan.class));
        verify(repairOrderService, never()).createRepairOrder(anyLong(), any());
    }

    @Test
    @DisplayName("区域计划：设备调配到其他区域后，原区域计划不得继续登记并返回明确提示")
    void createInspection_areaPlan_deviceTransferredOut_rejected() {
        equipment.setCurrentAreaId(AREA_B_ID);
        when(areaRepository.findById(AREA_A_ID)).thenReturn(Optional.of(area(AREA_A_ID, "一楼母婴室")));
        when(areaRepository.findById(AREA_B_ID)).thenReturn(Optional.of(area(AREA_B_ID, "二楼母婴室")));

        InspectionRecordRequest request = baseRequest();
        request.setPlanId(AREA_PLAN_ID);
        when(inspectionPlanRepository.findById(AREA_PLAN_ID)).thenReturn(Optional.of(areaPlan(AREA_A_ID)));

        assertThatThrownBy(() -> inspectionRecordService.createInspection(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("设备当前不在计划所属区域")
                .hasMessageContaining("一楼母婴室")
                .hasMessageContaining("二楼母婴室");

        verify(inspectionRecordRepository, never()).save(any());
        verify(inspectionPlanRepository, never()).save(any(InspectionPlan.class));
        verify(repairOrderService, never()).createRepairOrder(anyLong(), any());
    }

    @Test
    @DisplayName("区域计划：设备调配到新区域后，使用新区域的计划登记应成功，记录写入当前区域")
    void createInspection_newAreaPlanAfterTransfer_succeeds() {
        equipment.setCurrentAreaId(AREA_B_ID);

        InspectionRecordRequest request = baseRequest();
        request.setPlanId(AREA_PLAN_ID);
        when(inspectionPlanRepository.findById(AREA_PLAN_ID)).thenReturn(Optional.of(areaPlan(AREA_B_ID)));
        when(inspectionRecordRepository.save(any(InspectionRecord.class))).thenAnswer(invocation -> {
            InspectionRecord record = invocation.getArgument(0);
            record.setId(2L);
            return record;
        });

        InspectionRecordDTO dto = inspectionRecordService.createInspection(request);

        assertThat(dto).isNotNull();
        ArgumentCaptor<InspectionRecord> captor = ArgumentCaptor.forClass(InspectionRecord.class);
        verify(inspectionRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getAreaId()).isEqualTo(AREA_B_ID);
        verify(inspectionPlanRepository).save(any(InspectionPlan.class));
    }

    @Test
    @DisplayName("区域变更后不关联计划直接登记仍应成功")
    void createInspection_withoutPlanAfterTransfer_succeeds() {
        equipment.setCurrentAreaId(AREA_B_ID);

        InspectionRecordRequest request = baseRequest();
        when(inspectionRecordRepository.save(any(InspectionRecord.class))).thenAnswer(invocation -> {
            InspectionRecord record = invocation.getArgument(0);
            record.setId(3L);
            return record;
        });

        InspectionRecordDTO dto = inspectionRecordService.createInspection(request);

        assertThat(dto).isNotNull();
        ArgumentCaptor<InspectionRecord> captor = ArgumentCaptor.forClass(InspectionRecord.class);
        verify(inspectionRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getPlanId()).isNull();
        assertThat(captor.getValue().getAreaId()).isEqualTo(AREA_B_ID);
        verify(inspectionPlanRepository, never()).save(any(InspectionPlan.class));
    }

    @Test
    @DisplayName("设备计划：计划关联设备与巡检设备一致时登记成功（保持设备匹配校验）")
    void createInspection_equipmentPlan_matchingDevice_succeeds() {
        InspectionRecordRequest request = baseRequest();
        request.setPlanId(EQUIPMENT_PLAN_ID);
        when(inspectionPlanRepository.findById(EQUIPMENT_PLAN_ID)).thenReturn(Optional.of(equipmentPlan(EQUIPMENT_ID)));
        when(inspectionRecordRepository.save(any(InspectionRecord.class))).thenAnswer(invocation -> {
            InspectionRecord record = invocation.getArgument(0);
            record.setId(4L);
            return record;
        });

        InspectionRecordDTO dto = inspectionRecordService.createInspection(request);

        assertThat(dto).isNotNull();
        verify(inspectionPlanRepository).save(any(InspectionPlan.class));
    }

    @Test
    @DisplayName("设备计划：计划关联设备与巡检设备不一致时仍被拒绝，且与设备当前区域无关")
    void createInspection_equipmentPlan_mismatchedDevice_rejected() {
        // 设备已调配到其他区域，设备计划不校验区域，只校验设备匹配
        equipment.setCurrentAreaId(AREA_B_ID);

        InspectionRecordRequest request = baseRequest();
        request.setPlanId(EQUIPMENT_PLAN_ID);
        when(inspectionPlanRepository.findById(EQUIPMENT_PLAN_ID)).thenReturn(Optional.of(equipmentPlan(99L)));

        assertThatThrownBy(() -> inspectionRecordService.createInspection(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("所选计划关联的设备与巡检设备不一致");

        verify(inspectionRecordRepository, never()).save(any());
        verify(inspectionPlanRepository, never()).save(any(InspectionPlan.class));
        verify(areaRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("重复登记校验不受区域校验影响：设备当日已有记录仍按重复提交拒绝")
    void createInspection_duplicateSameDay_rejectedBeforePlanCheck() {
        when(inspectionRecordRepository.existsByEquipmentIdAndInspectionDate(eq(EQUIPMENT_ID),
                eq(LocalDate.of(2026, 9, 11)))).thenReturn(true);

        InspectionRecordRequest request = baseRequest();
        request.setPlanId(AREA_PLAN_ID);

        assertThatThrownBy(() -> inspectionRecordService.createInspection(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("该设备当日已存在巡检记录，请勿重复提交");

        verify(inspectionPlanRepository, never()).findById(anyLong());
        verify(inspectionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("异常自动报修流程不受区域校验影响：区域计划下异常巡检仍可自动创建报修单")
    void createInspection_areaPlan_abnormalWithAutoRepair_stillCreatesRepairOrder() {
        InspectionRecordRequest request = baseRequest();
        request.setPlanId(AREA_PLAN_ID);
        request.setResult(2);
        request.setAbnormalDesc("设备无法加热");
        request.setCreateRepair(true);

        when(inspectionPlanRepository.findById(AREA_PLAN_ID)).thenReturn(Optional.of(areaPlan(AREA_A_ID)));
        when(inspectionRecordRepository.save(any(InspectionRecord.class))).thenAnswer(invocation -> {
            InspectionRecord record = invocation.getArgument(0);
            record.setId(5L);
            return record;
        });
        RepairOrderDTO repairOrderDTO = new RepairOrderDTO();
        repairOrderDTO.setId(50L);
        repairOrderDTO.setRepairNo("RP202609110001");
        repairOrderDTO.setStatus(RepairOrderService.STATUS_PENDING);
        when(repairOrderService.createRepairOrder(eq(5L), eq("张三"))).thenReturn(repairOrderDTO);

        InspectionRecordDTO dto = inspectionRecordService.createInspection(request);

        assertThat(dto.getRepairOrderId()).isEqualTo(50L);
        assertThat(dto.getRepairNo()).isEqualTo("RP202609110001");
        assertThat(dto.getRepairStatus()).isEqualTo(RepairOrderService.STATUS_PENDING);
        verify(repairOrderService).createRepairOrder(eq(5L), eq("张三"));
    }
}
