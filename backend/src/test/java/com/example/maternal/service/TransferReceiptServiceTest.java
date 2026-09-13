
package com.example.maternal.service;

import com.example.maternal.dto.TransferReceiptRequest;
import com.example.maternal.dto.TransferRecordDTO;
import com.example.maternal.dto.TransferRequest;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.TransferRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferReceiptServiceTest {

    @Mock
    private TransferRecordRepository transferRecordRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private AreaService areaService;

    @InjectMocks
    private TransferRecordService transferRecordService;

    private static final long TO_AREA = 5L;
    private static final long CHILD_AREA = 51L;
    private static final long OTHER_AREA = 6L;

    private TransferRecord unsigned;
    private TransferRecord signed;

    @BeforeEach
    void setUp() {
        unsigned = transfer(1L, 10L, 4L, TO_AREA);
        signed = transfer(2L, 11L, 4L, CHILD_AREA);
        signed.setReceiver("赵值班");
        signed.setArrivalTime(LocalDateTime.of(2026, 9, 11, 10, 0));
        signed.setAppearanceIntact(true);

        lenient().when(areaRepository.findById(anyLong())).thenAnswer(inv -> Optional.of(area(inv.getArgument(0))));
        lenient().when(equipmentRepository.findById(anyLong())).thenAnswer(inv -> Optional.of(equipment(inv.getArgument(0))));
    }

    @Test
    @DisplayName("签收成功：签收人、到货时间、外观结论落库，DTO标记已签收，设备位置变更为目标区域")
    void sign_success() {
        when(transferRecordRepository.findById(1L)).thenReturn(Optional.of(unsigned));
        when(transferRecordRepository.save(any(TransferRecord.class))).thenAnswer(i -> i.getArgument(0));
        Equipment moving = equipment(10L);
        moving.setCurrentAreaId(4L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(moving));

        TransferRecordDTO dto = transferRecordService.signReceipt(1L, receiptRequest(
                "钱值班", LocalDateTime.of(2026, 9, 11, 9, 30), true));

        assertThat(dto.getReceiver()).isEqualTo("钱值班");
        assertThat(dto.getArrivalTime()).isEqualTo(LocalDateTime.of(2026, 9, 11, 9, 30));
        assertThat(dto.getAppearanceIntact()).isTrue();
        assertThat(dto.getSigned()).isTrue();
        // 签收完成才落位置：调出地 4 -> 目标母婴室 5
        assertThat(moving.getCurrentAreaId()).isEqualTo(TO_AREA);
        verify(equipmentRepository).save(moving);
    }

    @Test
    @DisplayName("重复签收被拒绝")
    void sign_duplicate_rejected() {
        when(transferRecordRepository.findById(2L)).thenReturn(Optional.of(signed));

        assertThatThrownBy(() -> transferRecordService.signReceipt(2L, receiptRequest(
                "钱值班", LocalDateTime.of(2026, 9, 11, 9, 30), true)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("重复签收");
        verify(transferRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("漏填签收人/到货时间/外观结论分别提示")
    void sign_missingFields_rejected() {
        when(transferRecordRepository.findById(1L)).thenReturn(Optional.of(unsigned));

        assertThatThrownBy(() -> transferRecordService.signReceipt(1L,
                receiptRequest("  ", LocalDateTime.now(), true)))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("签收人");

        assertThatThrownBy(() -> transferRecordService.signReceipt(1L,
                receiptRequest("钱值班", null, true)))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("到货时间");

        assertThatThrownBy(() -> transferRecordService.signReceipt(1L,
                receiptRequest("钱值班", LocalDateTime.now(), null)))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("外观是否完好");
    }

    @Test
    @DisplayName("到货时间早于调配日期被拒绝")
    void sign_arrivalBeforeTransferDate_rejected() {
        when(transferRecordRepository.findById(1L)).thenReturn(Optional.of(unsigned));

        assertThatThrownBy(() -> transferRecordService.signReceipt(1L, receiptRequest(
                "钱值班", LocalDateTime.of(2026, 9, 9, 9, 0), true)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("到货时间不能早于调配日期");
    }

    @Test
    @DisplayName("未签收前该设备不能再调出")
    void create_blockedWhenUnsignedExists() {
        when(repairOrderRepository.existsByEquipmentIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(transferRecordRepository.existsUnsignedByEquipmentId(10L)).thenReturn(true);

        TransferRequest request = new TransferRequest();
        request.setEquipmentId(10L);
        request.setToAreaId(TO_AREA);
        request.setTransferDate(LocalDate.of(2026, 9, 12));

        assertThatThrownBy(() -> transferRecordService.createTransfer(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("尚未到货签收");
        verify(transferRecordRepository, never()).save(any(TransferRecord.class));
    }

    @Test
    @DisplayName("已签收后允许再次调出")
    void create_allowedAfterSigned() {
        when(repairOrderRepository.existsByEquipmentIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(transferRecordRepository.existsUnsignedByEquipmentId(10L)).thenReturn(false);
        when(transferRecordRepository.save(any(TransferRecord.class)))
                .thenAnswer(i -> {
                    TransferRecord t = i.getArgument(0);
                    t.setId(99L);
                    t.setTransferNo("T-99");
                    return t;
                });

        TransferRequest request = new TransferRequest();
        request.setEquipmentId(10L);
        request.setToAreaId(TO_AREA);
        request.setTransferDate(LocalDate.of(2026, 9, 12));

        TransferRecordDTO dto = transferRecordService.createTransfer(request);
        assertThat(dto.getId()).isEqualTo(99L);
        assertThat(dto.getSigned()).isFalse();
    }

    @Test
    @DisplayName("发出未签收：设备当前位置保持在调出地，不落到目标区域")
    void create_unsignedKeepsEquipmentAtFromArea() {
        when(repairOrderRepository.existsByEquipmentIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(transferRecordRepository.existsUnsignedByEquipmentId(10L)).thenReturn(false);
        when(transferRecordRepository.save(any(TransferRecord.class)))
                .thenAnswer(i -> i.getArgument(0));
        Equipment staying = equipment(10L);
        staying.setCurrentAreaId(4L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(staying));

        TransferRequest request = new TransferRequest();
        request.setEquipmentId(10L);
        request.setToAreaId(TO_AREA);
        request.setTransferDate(LocalDate.of(2026, 9, 12));

        TransferRecordDTO dto = transferRecordService.createTransfer(request);
        assertThat(dto.getFromAreaId()).isEqualTo(4L);
        assertThat(dto.getToAreaId()).isEqualTo(TO_AREA);
        assertThat(dto.getSigned()).isFalse();
        assertThat(staying.getCurrentAreaId()).isEqualTo(4L);
        // 未签收不改位置：不允许对设备档案做任何保存
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

    @Test
    @DisplayName("取消未签收调配单：设备回到调出地（无已签收历史时恢复初始区域）")
    void cancel_unsignedReturnsToFromArea() {
        when(transferRecordRepository.findById(1L)).thenReturn(Optional.of(unsigned));
        when(transferRecordRepository.findFirstSignedByEquipmentIdOrderByCreatedAtDescIdDesc(10L))
                .thenReturn(Optional.empty());
        Equipment staying = equipment(10L);
        staying.setCurrentAreaId(4L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(staying));

        transferRecordService.cancelTransfer(1L);

        assertThat(unsigned.getStatus()).isEqualTo(0);
        // 设备初始区域为 4（调出地），取消后位置回到调出地
        assertThat(staying.getCurrentAreaId()).isEqualTo(4L);
        verify(equipmentRepository).save(staying);
    }

    @Test
    @DisplayName("取消已签收调配单：按最近一条剩余已签收单的目标区域重算位置")
    void cancel_signedRecomputesFromLatestSigned() {
        signed.setArrivalTime(LocalDateTime.of(2026, 9, 11, 10, 0));
        when(transferRecordRepository.findById(2L)).thenReturn(Optional.of(signed));
        TransferRecord earlierSigned = transfer(20L, 11L, 7L, 4L);
        earlierSigned.setArrivalTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        when(transferRecordRepository.findFirstSignedByEquipmentIdOrderByCreatedAtDescIdDesc(11L))
                .thenReturn(Optional.of(earlierSigned));
        Equipment moving = equipment(11L);
        moving.setCurrentAreaId(CHILD_AREA);
        when(equipmentRepository.findById(11L)).thenReturn(Optional.of(moving));

        transferRecordService.cancelTransfer(2L);

        assertThat(signed.getStatus()).isEqualTo(0);
        assertThat(moving.getCurrentAreaId()).isEqualTo(4L);
        verify(equipmentRepository).save(moving);
    }

    @Test
    @DisplayName("签收校验失败时不动设备位置")
    void sign_missingFields_keepsEquipmentUntouched() {
        when(transferRecordRepository.findById(1L)).thenReturn(Optional.of(unsigned));

        assertThatThrownBy(() -> transferRecordService.signReceipt(1L,
                receiptRequest("钱值班", null, true)))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("到货时间");

        verify(transferRecordRepository, never()).save(any(TransferRecord.class));
        verify(equipmentRepository, never()).findById(anyLong());
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

    @Test
    @DisplayName("签收台账按目标区域（含下级）过滤，已签收/未签收标记正确")
    void receipts_filterByToAreaWithChildren() {
        TransferRecord intoOther = transfer(3L, 12L, 4L, OTHER_AREA);
        when(transferRecordRepository.findActiveForReceipt(any(), any()))
                .thenReturn(List.of(unsigned, signed, intoOther));
        when(areaService.resolveScopeAreaIds(TO_AREA)).thenReturn(Set.of(TO_AREA, CHILD_AREA));

        List<TransferRecordDTO> rows = transferRecordService
                .getReceipts(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), TO_AREA);

        assertThat(rows).extracting(TransferRecordDTO::getId).containsExactlyInAnyOrder(1L, 2L);
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.getId()).isEqualTo(1L);
            assertThat(r.getSigned()).isFalse();
        });
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.getId()).isEqualTo(2L);
            assertThat(r.getSigned()).isTrue();
            assertThat(r.getAppearanceIntact()).isTrue();
            assertThat(r.getReceiver()).isEqualTo("赵值班");
        });
    }

    @Test
    @DisplayName("外观有破损且写清破损部位：破损部位随签收落库，DTO标记已签收")
    void sign_damagedWithPart_success() {
        when(transferRecordRepository.findById(1L)).thenReturn(Optional.of(unsigned));
        when(transferRecordRepository.save(any(TransferRecord.class))).thenAnswer(i -> i.getArgument(0));
        Equipment moving = equipment(10L);
        moving.setCurrentAreaId(4L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(moving));

        TransferRecordDTO dto = transferRecordService.signReceipt(1L, receiptRequest(
                "钱值班", LocalDateTime.of(2026, 9, 11, 9, 30), false, "瓶身左侧有裂纹"));

        assertThat(dto.getAppearanceIntact()).isFalse();
        assertThat(dto.getDamagePart()).isEqualTo("瓶身左侧有裂纹");
        assertThat(dto.getSigned()).isTrue();
        assertThat(moving.getCurrentAreaId()).isEqualTo(TO_AREA);
    }

    @Test
    @DisplayName("外观有破损但未写破损部位：拒绝签收且不落库、不动设备位置")
    void sign_damagedWithoutPart_rejected() {
        when(transferRecordRepository.findById(1L)).thenReturn(Optional.of(unsigned));

        assertThatThrownBy(() -> transferRecordService.signReceipt(1L, receiptRequest(
                "钱值班", LocalDateTime.of(2026, 9, 11, 9, 30), false, "   ")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("破损部位");

        verify(transferRecordRepository, never()).save(any(TransferRecord.class));
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

    @Test
    @DisplayName("破损部位超长（超过200字）被拒绝")
    void sign_damagePartTooLong_rejected() {
        when(transferRecordRepository.findById(1L)).thenReturn(Optional.of(unsigned));
        String tooLong = "裂".repeat(201);

        assertThatThrownBy(() -> transferRecordService.signReceipt(1L, receiptRequest(
                "钱值班", LocalDateTime.of(2026, 9, 11, 9, 30), false, tooLong)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("200");
        verify(transferRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("签收台账按外观筛选：待签收/完好/有破损口径正确，非法取值拦截提示")
    void receipts_filterByAppearance() {
        TransferRecord damaged = transfer(4L, 13L, 4L, TO_AREA);
        damaged.setReceiver("孙值班");
        damaged.setArrivalTime(LocalDateTime.of(2026, 9, 12, 9, 0));
        damaged.setAppearanceIntact(false);
        damaged.setDamagePart("底座一角凹陷");
        when(transferRecordRepository.findActiveForReceipt(any(), any()))
                .thenReturn(List.of(unsigned, signed, damaged));
        when(areaService.resolveScopeAreaIds(any())).thenReturn(null);

        List<TransferRecordDTO> unsignedRows = transferRecordService
                .getReceipts(null, null, null, "unsigned");
        assertThat(unsignedRows).extracting(TransferRecordDTO::getId).containsExactly(1L);

        List<TransferRecordDTO> intactRows = transferRecordService
                .getReceipts(null, null, null, "intact");
        assertThat(intactRows).extracting(TransferRecordDTO::getId).containsExactly(2L);

        List<TransferRecordDTO> damagedRows = transferRecordService
                .getReceipts(null, null, null, "damaged");
        assertThat(damagedRows).hasSize(1);
        assertThat(damagedRows.get(0).getDamagePart()).isEqualTo("底座一角凹陷");

        assertThatThrownBy(() -> transferRecordService.getReceipts(null, null, null, "bogus"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("外观筛选条件不合法");
    }

    private TransferReceiptRequest receiptRequest(String receiver, LocalDateTime time, Boolean intact) {
        return receiptRequest(receiver, time, intact, null);
    }

    private TransferReceiptRequest receiptRequest(String receiver, LocalDateTime time,
                                                  Boolean intact, String damagePart) {
        TransferReceiptRequest req = new TransferReceiptRequest();
        req.setReceiver(receiver);
        req.setArrivalTime(time);
        req.setAppearanceIntact(intact);
        req.setDamagePart(damagePart);
        return req;
    }

    private TransferRecord transfer(long id, long equipmentId, long from, long to) {
        TransferRecord t = new TransferRecord();
        t.setId(id);
        t.setTransferNo("T-" + id);
        t.setEquipmentId(equipmentId);
        t.setFromAreaId(from);
        t.setToAreaId(to);
        t.setStatus(1);
        t.setTransferDate(LocalDate.of(2026, 9, 10));
        return t;
    }

    private Area area(long id) {
        Area a = new Area();
        a.setId(id);
        a.setName("区域" + id);
        a.setCode("A" + id);
        a.setPath("/A" + id);
        a.setStatus(1);
        return a;
    }

    private Equipment equipment(long id) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setEquipmentNo("E-" + id);
        e.setEquipmentName("设备" + id);
        e.setInitialAreaId(4L);
        return e;
    }
}
