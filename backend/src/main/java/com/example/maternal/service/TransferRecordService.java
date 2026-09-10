
package com.example.maternal.service;

import com.example.maternal.dto.TransferRecordDTO;
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

    @Transactional
    public TransferRecordDTO createTransfer(TransferRequest request) {
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        if (repairOrderRepository.existsByEquipmentIdAndStatusIn(request.getEquipmentId(), List.of(0, 1))) {
            throw new RuntimeException("设备维修中，不可调配，待维修恢复后方可重新调配");
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
        
        TransferRecord saved = transferRecordRepository.save(record);
        
        equipment.setCurrentAreaId(request.getToAreaId());
        equipmentRepository.save(equipment);
        
        return convertToDTO(saved);
    }
    
    public List<TransferRecordDTO> getAllTransfers() {
        return transferRecordRepository.findAllOrdered().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<TransferRecordDTO> getTransfersByDateRange(LocalDate startDate, LocalDate endDate) {
        return transferRecordRepository.findByDateRange(startDate, endDate).stream()
                .map(this::convertToDTO)
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
        Long count = transferRecordRepository.countByDateRange(startDate, endDate);
        TransferSummaryDTO summary = new TransferSummaryDTO();
        summary.setTotalCount(count);
        summary.setPeriod(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 至 " + endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        return summary;
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

        // 按该设备剩余有效调配记录恢复当前位置：取最近一条有效调配的目标区域，无有效记录则恢复初始区域
        Equipment equipment = equipmentRepository.findById(record.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        Long restoredAreaId = transferRecordRepository
                .findFirstByEquipmentIdAndStatusOrderByCreatedAtDescIdDesc(record.getEquipmentId(), 1)
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
