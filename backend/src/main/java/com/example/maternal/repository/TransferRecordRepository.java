
package com.example.maternal.repository;

import com.example.maternal.entity.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {
    
    List<TransferRecord> findByTransferDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<TransferRecord> findByTransferDateBetweenOrderByTransferDateDesc(LocalDate startDate, LocalDate endDate);
    
    List<TransferRecord> findByEquipmentId(Long equipmentId);
    
    List<TransferRecord> findByFromAreaId(Long fromAreaId);
    
    List<TransferRecord> findByToAreaId(Long toAreaId);
    
    @Query("SELECT t FROM TransferRecord t WHERE t.transferDate >= :startDate AND t.transferDate <= :endDate AND t.status = 1 ORDER BY t.transferDate DESC")
    List<TransferRecord> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(t) FROM TransferRecord t WHERE t.transferDate >= :startDate AND t.transferDate <= :endDate AND t.status = 1")
    Long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM TransferRecord t ORDER BY t.createdAt DESC, t.id DESC")
    List<TransferRecord> findAllOrdered();

    Optional<TransferRecord> findFirstByEquipmentIdAndStatusOrderByCreatedAtDescIdDesc(Long equipmentId, Integer status);
}
