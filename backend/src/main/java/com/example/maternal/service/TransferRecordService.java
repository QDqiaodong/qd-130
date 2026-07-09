
package com.example.maternal.service;

import com.example.maternal.dto.TransferRecordDTO;
import com.example.maternal.dto.TransferRequest;
import com.example.maternal.dto.TransferSummaryDTO;
import com.example.maternal.entity.Area;
import com.example.maternal.entity.Equipment;
import com.example.maternal.entity.TransferRecord;
import com.example.maternal.repository.AreaRepository;
import com.example.maternal.repository.EquipmentRepository;
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
    
    @Transactional
    public TransferRecordDTO createTransfer(TransferRequest request) {
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("设备不存在"));
        
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
        return transferRecordRepository.findAllActive().stream()
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
        transferRecordRepository.findById(id)
                .ifPresent(record -> {
                    record.setStatus(0);
                    transferRecordRepository.save(record);
                });
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
