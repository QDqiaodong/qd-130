
package com.example.maternal.repository;

import com.example.maternal.entity.RepairOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {

    Optional<RepairOrder> findByRepairNo(String repairNo);

    Optional<RepairOrder> findByInspectionId(Long inspectionId);

    boolean existsByInspectionId(Long inspectionId);

    boolean existsByEquipmentIdAndStatusIn(Long equipmentId, Collection<Integer> statuses);

    Optional<RepairOrder> findFirstByEquipmentIdAndStatusInOrderByCreatedAtDescIdDesc(Long equipmentId, Collection<Integer> statuses);

    @Query("SELECT ro FROM RepairOrder ro WHERE " +
            "(:startTime IS NULL OR ro.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR ro.createdAt < :endTime) AND " +
            "(:areaId IS NULL OR ro.areaId = :areaId) AND " +
            "(:status IS NULL OR ro.status = :status) " +
            "ORDER BY ro.createdAt DESC, ro.id DESC")
    List<RepairOrder> findByFilter(@Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("areaId") Long areaId,
                                   @Param("status") Integer status);
}
