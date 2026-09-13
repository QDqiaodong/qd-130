
package com.example.maternal.service;

import com.example.maternal.dto.RepairOrderDTO;
import com.example.maternal.dto.SpotCheckRecordDTO;
import com.example.maternal.dto.SpotCheckRecordRequest;
import com.example.maternal.entity.Area;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotCheckServiceTest {

    private static final Long EQUIPMENT_ID = 10L;
    private static final Long AREA_A_ID = 1L;
    private static final Long AREA_B_ID = 2L;

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

    private Equipment warmer;

    @BeforeEach
    void setUp() {
        warmer = new Equipment();
        warmer.setId(EQUIPMENT_ID);
        warmer.setEquipmentNo("EQ-001");
        warmer.setEquipmentName("温奶器");
        warmer.setEquipmentType(SpotCheckService.EQUIPMENT_TYPE_WARMER);
        warmer.setCurrentAreaId(AREA_A_ID);
        warmer.setStatus(1);

        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(warmer));
    }

    private SpotCheckRecordRequest baseRequest(BigDecimal temperature) {
        SpotCheckRecordRequest request = new SpotCheckRecordRequest();
        request.setEquipmentId(EQUIPMENT_ID);
        request.setCheckDate(LocalDate.of(2026, 9, 12));
        request.setTemperature(temperature);
        request.setInspector("赵值班");
        return request;
    }

    @Test
    @DisplayName("温度在40~50℃闭区间内判定为合格，正常登记不触发报修")
    void createSpotCheck_qualifiedTemperature_succeedsWithoutRepair() {
        SpotCheckRecordRequest request = baseRequest(new BigDecimal("45.5"));
        when(spotCheckRecordRepository.save(any(SpotCheckRecord.class))).thenAnswer(invocation -> {
            SpotCheckRecord record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });

        SpotCheckRecordDTO dto = spotCheckService.createSpotCheck(request);

        ArgumentCaptor<SpotCheckRecord> captor = ArgumentCaptor.forClass(SpotCheckRecord.class);
        verify(spotCheckRecordRepository).save(captor.capture());
        SpotCheckRecord saved = captor.getValue();
        assertThat(saved.getQualified()).isTrue();
        assertThat(saved.getAreaId()).isEqualTo(AREA_A_ID);
        assertThat(saved.getAbnormalDesc()).isNull();
        assertThat(dto.getQualified()).isTrue();
        verify(repairOrderService, never()).createRepairOrderFromSpotCheck(anyLong(), any());
    }

    @Test
    @DisplayName("边界值40.00℃与50.00℃均判定为合格")
    void createSpotCheck_boundaryTemperatures_qualified() {
        assertThat(spotCheckService.isQualified(new BigDecimal("40.00"))).isTrue();
        assertThat(spotCheckService.isQualified(new BigDecimal("50.00"))).isTrue();
        assertThat(spotCheckService.isQualified(new BigDecimal("39.99"))).isFalse();
        assertThat(spotCheckService.isQualified(new BigDecimal("50.01"))).isFalse();
    }

    @Test
    @DisplayName("温度不合格时结论落为不合格、生成不合格说明，勾选报修则自动创建报修单")
    void createSpotCheck_tooHot_createsRepair() {
        SpotCheckRecordRequest request = baseRequest(new BigDecimal("55.20"));
        request.setAbnormalDesc("水温偏高，疑似温控失灵");
        request.setCreateRepair(true);
        when(spotCheckRecordRepository.save(any(SpotCheckRecord.class))).thenAnswer(invocation -> {
            SpotCheckRecord record = invocation.getArgument(0);
            record.setId(5L);
            return record;
        });
        RepairOrderDTO repairOrderDTO = new RepairOrderDTO();
        repairOrderDTO.setId(50L);
        repairOrderDTO.setRepairNo("RP202609120001");
        repairOrderDTO.setStatus(RepairOrderService.STATUS_PENDING);
        when(repairOrderService.createRepairOrderFromSpotCheck(5L, "赵值班")).thenReturn(repairOrderDTO);

        SpotCheckRecordDTO dto = spotCheckService.createSpotCheck(request);

        ArgumentCaptor<SpotCheckRecord> captor = ArgumentCaptor.forClass(SpotCheckRecord.class);
        verify(spotCheckRecordRepository).save(captor.capture());
        SpotCheckRecord saved = captor.getValue();
        assertThat(saved.getQualified()).isFalse();
        assertThat(saved.getAbnormalDesc()).contains("55.20").contains("40.00~50.00").contains("温控失灵");
        assertThat(dto.getRepairOrderId()).isEqualTo(50L);
        assertThat(dto.getRepairStatus()).isEqualTo(RepairOrderService.STATUS_PENDING);
        verify(repairOrderService).createRepairOrderFromSpotCheck(5L, "赵值班");
    }

    @Test
    @DisplayName("自动报修失败（设备已有进行中报修单）时抽检仍保存成功，仅返回提示")
    void createSpotCheck_autoRepairFails_recordStillSavedWithMessage() {
        SpotCheckRecordRequest request = baseRequest(new BigDecimal("36.00"));
        request.setCreateRepair(true);
        when(spotCheckRecordRepository.save(any(SpotCheckRecord.class))).thenAnswer(invocation -> {
            SpotCheckRecord record = invocation.getArgument(0);
            record.setId(6L);
            return record;
        });
        when(repairOrderService.createRepairOrderFromSpotCheck(6L, "赵值班"))
                .thenThrow(new RuntimeException("该设备存在未完成的报修单，请勿重复报修"));

        SpotCheckRecordDTO dto = spotCheckService.createSpotCheck(request);

        assertThat(dto.getId()).isEqualTo(6L);
        assertThat(dto.getQualified()).isFalse();
        assertThat(dto.getRepairOrderId()).isNull();
        assertThat(dto.getRepairMessage()).contains("未完成的报修单");
        verify(spotCheckRecordRepository).save(any(SpotCheckRecord.class));
    }

    @Test
    @DisplayName("漏填抽检温度/抽检人/日期时给出明确提示，不保存记录")
    void createSpotCheck_missingFields_rejected() {
        SpotCheckRecordRequest noTemp = baseRequest(null);
        assertThatThrownBy(() -> spotCheckService.createSpotCheck(noTemp))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("抽检温度不能为空");

        SpotCheckRecordRequest noInspector = baseRequest(new BigDecimal("45.0"));
        noInspector.setInspector("  ");
        assertThatThrownBy(() -> spotCheckService.createSpotCheck(noInspector))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("抽检人不能为空");

        SpotCheckRecordRequest noDate = baseRequest(new BigDecimal("45.0"));
        noDate.setCheckDate(null);
        assertThatThrownBy(() -> spotCheckService.createSpotCheck(noDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("抽检日期不能为空");

        verify(spotCheckRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("温度超出0~100℃物理范围视为填错并拒绝")
    void createSpotCheck_temperatureOutOfPhysicalRange_rejected() {
        SpotCheckRecordRequest request = baseRequest(new BigDecimal("150"));
        assertThatThrownBy(() -> spotCheckService.createSpotCheck(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("抽检温度不合法");
        verify(spotCheckRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("非温奶器设备或已停用温奶器不能登记抽检")
    void createSpotCheck_notWarmerOrDisabled_rejected() {
        warmer.setEquipmentType("护理台");
        SpotCheckRecordRequest request = baseRequest(new BigDecimal("45.0"));
        assertThatThrownBy(() -> spotCheckService.createSpotCheck(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("仅在用温奶器可登记温度抽检");

        warmer.setEquipmentType(SpotCheckService.EQUIPMENT_TYPE_WARMER);
        warmer.setStatus(0);
        assertThatThrownBy(() -> spotCheckService.createSpotCheck(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已停用");

        verify(spotCheckRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("同一温奶器当日重复抽检被拒绝，提示请勿重复抽检")
    void createSpotCheck_duplicateSameDay_rejected() {
        when(spotCheckRecordRepository.existsByEquipmentIdAndCheckDate(EQUIPMENT_ID,
                LocalDate.of(2026, 9, 12))).thenReturn(true);

        SpotCheckRecordRequest request = baseRequest(new BigDecimal("45.0"));
        assertThatThrownBy(() -> spotCheckService.createSpotCheck(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("当日已存在抽检记录，请勿重复抽检");

        verify(spotCheckRecordRepository, never()).save(any());
        verify(repairOrderService, never()).createRepairOrderFromSpotCheck(anyLong(), any());
    }

    @Test
    @DisplayName("按母婴室区域过滤：选择父区域时包含下级区域，返回结果再按范围收敛")
    void getSpotChecks_areaFilter_appliesScope() {
        when(areaService.resolveScopeAreaIds(AREA_A_ID))
                .thenReturn(java.util.Set.of(AREA_A_ID, 4L, 5L));
        when(spotCheckRecordRepository.findByFilter(any(), any(), any(), any(), any(), any()))
                .thenReturn(java.util.List.of());

        var result = spotCheckService.getSpotChecks(null, null, AREA_A_ID, null, null, null);

        assertThat(result).isEmpty();
        verify(areaService).resolveScopeAreaIds(AREA_A_ID);
    }

    @Test
    @DisplayName("复核人筛选条件只接受0或1，非法值给出明确提示")
    void getSpotChecks_invalidReviewStatus_rejected() {
        assertThatThrownBy(() -> spotCheckService.getSpotChecks(null, null, null, null, null, 2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("复核人筛选条件不合法");
    }

    @Test
    @DisplayName("复核人筛选条件透传到查询：已补复核人按1过滤")
    void getSpotChecks_reviewStatusPassedThrough() {
        when(spotCheckRecordRepository.findByFilter(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(java.util.List.of());

        var result = spotCheckService.getSpotChecks(null, null, null, null, null, 1);

        assertThat(result).isEmpty();
        verify(spotCheckRecordRepository).findByFilter(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(1));
    }
}
