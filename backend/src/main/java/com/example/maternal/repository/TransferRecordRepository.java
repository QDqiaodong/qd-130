
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

    @Query("SELECT t FROM TransferRecord t WHERE t.transferDate >= :startDate AND t.transferDate <= :endDate AND t.status = 1 AND " +
            "(:areaId IS NULL OR t.fromAreaId = :areaId OR t.toAreaId = :areaId) AND " +
            "(:equipmentType IS NULL OR EXISTS (SELECT 1 FROM Equipment e WHERE e.id = t.equipmentId AND e.equipmentType = :equipmentType)) " +
            "ORDER BY t.transferDate DESC, t.id DESC")
    List<TransferRecord> findForDashboard(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("areaId") Long areaId,
                                          @Param("equipmentType") String equipmentType);

    @Query("SELECT COUNT(t) FROM TransferRecord t WHERE t.transferDate >= :startDate AND t.transferDate <= :endDate AND t.status = 1")
    Long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM TransferRecord t ORDER BY t.createdAt DESC, t.id DESC")
    List<TransferRecord> findAllOrdered();

    /**
     * 设备最近一条「有效且已到货签收」的调配单。位置口径：
     * 设备当前位置 = 最近已签收有效单的目标区域；无则为初始区域（在途/取消均不落位置）。
     */
    @Query("SELECT t FROM TransferRecord t WHERE t.equipmentId = :equipmentId " +
            "AND t.status = 1 AND t.arrivalTime IS NOT NULL " +
            "ORDER BY t.createdAt DESC, t.id DESC")
    Optional<TransferRecord> findFirstSignedByEquipmentIdOrderByCreatedAtDescIdDesc(@Param("equipmentId") Long equipmentId);

    /**
     * 设备是否存在「已发出但未到货签收」的有效调配：未签收前目标区域不能再把这台设备调走。
     */
    @Query("SELECT COUNT(t) > 0 FROM TransferRecord t " +
            "WHERE t.equipmentId = :equipmentId AND t.status = 1 AND t.arrivalTime IS NULL")
    boolean existsUnsignedByEquipmentId(@Param("equipmentId") Long equipmentId);

    /**
     * 到货签收台账：仅有效（status=1）调配，支持按调配日区间过滤，按调配日倒序、id倒序。
     */
    @Query("SELECT t FROM TransferRecord t WHERE t.status = 1 " +
            "AND (:startDate IS NULL OR t.transferDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.transferDate <= :endDate) " +
            "ORDER BY t.transferDate DESC, t.id DESC")
    List<TransferRecord> findActiveForReceipt(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
}
