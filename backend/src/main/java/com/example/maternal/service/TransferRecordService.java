
package com.example.maternal.service;

import com.example.maternal.dto.TransferRecordDTO;
import com.example.maternal.dto.TransferReceiptRequest;
import com.example.maternal.dto.TransferRequest;
import com.example.maternal.dto.TransferSummaryDTO;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.TransferRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
import com.example.maternal.repository.RepairOrderRepository;
import com.example.maternal.repository.TransferRecordRepository;
import com.example.maternal.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferRecordService {

    private final TransferRecordRepository transferRecordRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final AreaService areaService;

    @Transactional
    public TransferRecordDTO createTransfer(TransferRequest request) {
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        if (repairOrderRepository.existsByEquipmentIdAndStatusIn(request.getEquipmentId(), List.of(0, 1))) {
            throw new RuntimeException("设备维修中，不可调配，待维修恢复后方可重新调配");
        }

        // 到货签收闸门：上一张调配单未签收前，该设备不能再被调出（口径以服务端落库为准）
        if (transferRecordRepository.existsUnsignedByEquipmentId(request.getEquipmentId())) {
            throw new RuntimeException("该设备上一张调配单尚未到货签收，签收完成后目标区域才能再次调出");
        }

        Area toArea = areaRepository.findById(request.getToAreaId())
                .orElseThrow(() -> new RuntimeException("目标区域不存在"));
        
        TransferRecord record = new TransferRecord();
        record.setTransferNo(CodeGenerator.generateTransferNo());
        record.setEquipmentId(request.getEquipmentId());
        record.setFromAreaId(equipment.getCurrentAreaId());
        record.setToAreaId(request.getToAreaId());
        record.setTransferDate(request.getTransferDate());
        record.setReason(request.getReason());
        record.setOperator(request.getOperator());
        record.setStatus(1);
        record.setRemark(request.getRemark());
        
        // 未到货签收前设备当前位置保持在调出地：只登记调出单，不改 equipment.currentAreaId，
        // 避免一线按目标区域找货、取消后位置也回不去；位置在签收完成时才变更为目标区域。
        TransferRecord saved = transferRecordRepository.save(record);

        return convertToDTO(saved);
    }
    
    public List<TransferRecordDTO> getAllTransfers() {
        return transferRecordRepository.findAllOrdered().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<TransferRecordDTO> getTransfersByDateRange(LocalDate startDate, LocalDate endDate) {
        return getTransfersByDateRange(startDate, endDate, null, null);
    }

    public List<TransferRecordDTO> getTransfersByDateRange(LocalDate startDate, LocalDate endDate,
                                                           Long areaId, String equipmentType) {
        return findScopedTransfers(startDate, endDate, areaId, equipmentType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 与健康看板「调配次数」同一口径取数：周期内有效（status=1）调配，
     * 区域维度同时计入「调出本区域」和「调入本区域」，选中父区域时含全部下级区域。
     * 明细列表与汇总条数共用本方法，保证卡片数字 = 汇总 = 明细条数。
     */
    private List<TransferRecord> findScopedTransfers(LocalDate startDate, LocalDate endDate,
                                                     Long areaId, String equipmentType) {
        java.util.Set<Long> scopeAreaIds = areaService.resolveScopeAreaIds(areaId);
        return transferRecordRepository.findForDashboard(startDate, endDate, null, equipmentType).stream()
                .filter(t -> scopeAreaIds == null
                        || scopeAreaIds.contains(t.getFromAreaId())
                        || scopeAreaIds.contains(t.getToAreaId()))
                .collect(Collectors.toList());
    }
    
    public TransferRecordDTO getTransferById(Long id) {
        return transferRecordRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }
    
    public TransferRecordDTO getTransferByNo(String transferNo) {
        return transferRecordRepository.findAll().stream()
                .filter(r -> r.getTransferNo().equals(transferNo))
                .findFirst()
                .map(this::convertToDTO)
                .orElse(null);
    }
    
    public TransferSummaryDTO getTransferSummary(LocalDate startDate, LocalDate endDate) {
        return getTransferSummary(startDate, endDate, null, null);
    }

    /**
     * 汇总与明细共用 {@link #findScopedTransfers}，区域维度同时统计调出与调入，
     * 保证健康看板卡片数字、汇总总量、明细条数完全一致。
     */
    public TransferSummaryDTO getTransferSummary(LocalDate startDate, LocalDate endDate,
                                                 Long areaId, String equipmentType) {
        List<TransferRecord> scoped = findScopedTransfers(startDate, endDate, areaId, equipmentType);
        java.util.Set<Long> scopeAreaIds = areaService.resolveScopeAreaIds(areaId);
        long outbound = scoped.stream()
                .filter(t -> scopeAreaIds == null || scopeAreaIds.contains(t.getFromAreaId()))
                .count();
        long inbound = scoped.stream()
                .filter(t -> scopeAreaIds == null || scopeAreaIds.contains(t.getToAreaId()))
                .count();

        TransferSummaryDTO summary = new TransferSummaryDTO();
        summary.setTotalCount((long) scoped.size());
        summary.setOutboundCount(outbound);
        summary.setInboundCount(inbound);
        summary.setPeriod(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " 至 " + endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        return summary;
    }
    
    /**
     * 到货签收：仅已发出（status=1）且尚未签收的调配单可签收。
     * 签收人、到货时间、外观是否完好缺一不可；签收后不可重复签收。
     */
    @Transactional
    public TransferRecordDTO signReceipt(Long id, TransferReceiptRequest request) {
        TransferRecord record = transferRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("调配记录不存在"));

        if (record.getStatus() == null || record.getStatus() != 1) {
            throw new RuntimeException("该调配记录已取消，不能签收");
        }
        if (record.getArrivalTime() != null) {
            throw new RuntimeException("该调配单已完成到货签收，请勿重复签收");
        }
        if (request == null || request.getReceiver() == null || request.getReceiver().trim().isEmpty()) {
            throw new RuntimeException("签收人不能为空");
        }
        if (request.getArrivalTime() == null) {
            throw new RuntimeException("到货时间不能为空");
        }
        if (request.getAppearanceIntact() == null) {
            throw new RuntimeException("请选择外观是否完好");
        }
        // 外观有破损必须写清破损部位，否则不允许确认签收（口径由服务端统一校验落库）
        if (Boolean.FALSE.equals(request.getAppearanceIntact())
                && (request.getDamagePart() == null || request.getDamagePart().trim().isEmpty())) {
            throw new RuntimeException("外观有破损时必须写清破损部位后才能确认签收");
        }
        if (request.getDamagePart() != null && request.getDamagePart().trim().length() > 200) {
            throw new RuntimeException("破损部位描述不能超过200字");
        }
        if (request.getArrivalTime().toLocalDate().isBefore(record.getTransferDate())) {
            throw new RuntimeException("到货时间不能早于调配日期");
        }

        record.setReceiver(request.getReceiver().trim());
        record.setArrivalTime(request.getArrivalTime());
        record.setAppearanceIntact(request.getAppearanceIntact());
        // 仅外观有破损才落破损部位，外观完好一律清空，避免前端串值
        record.setDamagePart(Boolean.FALSE.equals(request.getAppearanceIntact())
                ? request.getDamagePart().trim() : null);
        TransferRecord saved = transferRecordRepository.save(record);

        // 签收完成才落位置：设备当前区域从调出地变更为目标区域，与签收状态同事务落库
        Equipment equipment = equipmentRepository.findById(record.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("设备不存在"));
        equipment.setCurrentAreaId(record.getToAreaId());
        equipmentRepository.save(equipment);

        return convertToDTO(saved);
    }

    /**
     * 到货签收台账：可按调配日区间和目标区域筛选（选父区域含全部下级），
     * 并可叠加外观筛选：unsigned-待签收（未填外观）、intact-外观完好、damaged-外观有破损。
     * 签收标记随 DTO 一并返回，刷新后与「还能不能调出」保持同一口径。
     */
    public List<TransferRecordDTO> getReceipts(LocalDate startDate, LocalDate endDate, Long toAreaId) {
        return getReceipts(startDate, endDate, toAreaId, null);
    }

    public List<TransferRecordDTO> getReceipts(LocalDate startDate, LocalDate endDate,
                                               Long toAreaId, String appearance) {
        java.util.Set<Long> scopeAreaIds = areaService.resolveScopeAreaIds(toAreaId);
        return transferRecordRepository.findActiveForReceipt(startDate, endDate).stream()
                .filter(t -> scopeAreaIds == null || scopeAreaIds.contains(t.getToAreaId()))
                .filter(t -> matchAppearance(t, appearance))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 外观筛选口径全部依据服务端落库字段：未签收（arrivalTime 为空）→待签收；
     * 已签收再按 appearanceIntact 区分完好/有破损。非法取值直接拦截并提示。
     */
    private boolean matchAppearance(TransferRecord t, String appearance) {
        if (appearance == null || appearance.trim().isEmpty()) {
            return true;
        }
        switch (appearance.trim()) {
            case "unsigned":
                return t.getArrivalTime() == null;
            case "intact":
                return t.getArrivalTime() != null && Boolean.TRUE.equals(t.getAppearanceIntact());
            case "damaged":
                return t.getArrivalTime() != null && Boolean.FALSE.equals(t.getAppearanceIntact());
            default:
                throw new RuntimeException("外观筛选条件不合法，请选择待签收、外观完好或外观有破损");
        }
    }

    @Transactional
    public void cancelTransfer(Long id) {
        TransferRecord record = transferRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("调配记录不存在"));

        if (record.getStatus() == null || record.getStatus() != 1) {
            throw new RuntimeException("该调配记录已取消，请勿重复取消");
        }

        record.setStatus(0);
        transferRecordRepository.save(record);

        // 取消回到调出地：按该设备剩余「已签收」有效调配重新计算当前位置，
        // 取最近一条已签收单的目标区域；本单未签收时设备本就仍在调出地，无已签收单则恢复初始区域。
        Equipment equipment = equipmentRepository.findById(record.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        Long restoredAreaId = transferRecordRepository
                .findFirstSignedByEquipmentIdOrderByCreatedAtDescIdDesc(record.getEquipmentId())
                .map(TransferRecord::getToAreaId)
                .orElse(equipment.getInitialAreaId());
        equipment.setCurrentAreaId(restoredAreaId);
        equipmentRepository.save(equipment);
    }
    
    private TransferRecordDTO convertToDTO(TransferRecord record) {
        TransferRecordDTO dto = new TransferRecordDTO();
        dto.setId(record.getId());
        dto.setTransferNo(record.getTransferNo());
        dto.setEquipmentId(record.getEquipmentId());
        dto.setFromAreaId(record.getFromAreaId());
        dto.setToAreaId(record.getToAreaId());
        dto.setTransferDate(record.getTransferDate());
        dto.setReason(record.getReason());
        dto.setOperator(record.getOperator());
        dto.setStatus(record.getStatus());
        dto.setRemark(record.getRemark());
        dto.setReceiver(record.getReceiver());
        dto.setArrivalTime(record.getArrivalTime());
        dto.setAppearanceIntact(record.getAppearanceIntact());
        dto.setDamagePart(record.getDamagePart());
        // 签收标记服务端统一计算：有到货签收时间即视为已签收，列表/详情/调出闸门共用此口径
        dto.setSigned(record.getArrivalTime() != null);
        
        equipmentRepository.findById(record.getEquipmentId())
                .ifPresent(e -> {
                    dto.setEquipmentNo(e.getEquipmentNo());
                    dto.setEquipmentName(e.getEquipmentName());
                });
        
        areaRepository.findById(record.getFromAreaId())
                .ifPresent(a -> dto.setFromAreaName(a.getName()));
        
        areaRepository.findById(record.getToAreaId())
                .ifPresent(a -> dto.setToAreaName(a.getName()));
        
        return dto;
    }
}
