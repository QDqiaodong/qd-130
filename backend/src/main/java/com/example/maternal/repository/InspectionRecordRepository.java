
package com.example.maternal.repository;

import com.example.maternal.entity.InspectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InspectionRecordRepository extends JpaRepository<InspectionRecord, Long> {

    Optional<InspectionRecord> findByInspectionNo(String inspectionNo);

    boolean existsByEquipmentIdAndInspectionDate(Long equipmentId, LocalDate inspectionDate);

    List<InspectionRecord> findByEquipmentIdOrderByInspectionDateDescIdDesc(Long equipmentId);

    List<InspectionRecord> findByPlanId(Long planId);

    @Query("SELECT r FROM InspectionRecord r LEFT JOIN RepairOrder ro ON ro.inspectionId = r.id WHERE " +
            "(:startDate IS NULL OR r.inspectionDate >= :startDate) AND " +
            "(:endDate IS NULL OR r.inspectionDate <= :endDate) AND " +
            "(:areaId IS NULL OR r.areaId = :areaId) AND " +
            "(:result IS NULL OR r.result = :result) AND " +
            "(:repairStatus IS NULL OR (:repairStatus = -1 AND ro.id IS NULL) OR (:repairStatus <> -1 AND ro.status = :repairStatus)) " +
            "ORDER BY r.inspectionDate DESC, r.id DESC")
    List<InspectionRecord> findByFilter(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("areaId") Long areaId,
                                        @Param("result") Integer result,
                                        @Param("repairStatus") Integer repairStatus);

    @Query("SELECT r FROM InspectionRecord r WHERE " +
            "(:startDate IS NULL OR r.inspectionDate >= :startDate) AND " +
            "(:endDate IS NULL OR r.inspectionDate <= :endDate) AND " +
            "(:areaId IS NULL OR r.areaId = :areaId) AND " +
            "(:result IS NULL OR r.result = :result) AND " +
            "(:equipmentType IS NULL OR EXISTS (SELECT 1 FROM Equipment e WHERE e.id = r.equipmentId AND e.equipmentType = :equipmentType)) " +
            "ORDER BY r.inspectionDate DESC, r.id DESC")
    List<InspectionRecord> findForDashboard(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("areaId") Long areaId,
                                            @Param("result") Integer result,
                                            @Param("equipmentType") String equipmentType);
}
