
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

    Optional<RepairOrder> findBySpotCheckId(Long spotCheckId);

    boolean existsByInspectionId(Long inspectionId);

    boolean existsBySpotCheckId(Long spotCheckId);

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

    @Query("SELECT ro FROM RepairOrder ro WHERE " +
            "(:startTime IS NULL OR ro.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR ro.createdAt < :endTime) AND " +
            "(:areaId IS NULL OR ro.areaId = :areaId) AND " +
            "(:equipmentType IS NULL OR EXISTS (SELECT 1 FROM Equipment e WHERE e.id = ro.equipmentId AND e.equipmentType = :equipmentType)) " +
            "ORDER BY ro.createdAt DESC, ro.id DESC")
    List<RepairOrder> findForDashboard(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       @Param("areaId") Long areaId,
                                       @Param("equipmentType") String equipmentType);

    /** 仍停在待处理且报修时间早于截止时间的超时单，按等待时长从长到短排序 */
    @Query("SELECT ro FROM RepairOrder ro WHERE ro.status = 0 AND ro.createdAt <= :cutoff " +
            "ORDER BY ro.createdAt ASC, ro.id ASC")
    List<RepairOrder> findOverduePending(@Param("cutoff") LocalDateTime cutoff);
}
