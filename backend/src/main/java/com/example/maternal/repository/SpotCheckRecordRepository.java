
package com.example.maternal.repository;

import com.example.maternal.entity.SpotCheckRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpotCheckRecordRepository extends JpaRepository<SpotCheckRecord, Long> {

    Optional<SpotCheckRecord> findBySpotCheckNo(String spotCheckNo);

    boolean existsByEquipmentIdAndCheckDate(Long equipmentId, LocalDate checkDate);

    @Query("SELECT s FROM SpotCheckRecord s LEFT JOIN RepairOrder ro ON ro.spotCheckId = s.id WHERE " +
            "(:startDate IS NULL OR s.checkDate >= :startDate) AND " +
            "(:endDate IS NULL OR s.checkDate <= :endDate) AND " +
            "(:areaId IS NULL OR s.areaId = :areaId) AND " +
            "(:qualified IS NULL OR s.qualified = :qualified) AND " +
            "(:repairStatus IS NULL OR " +
            " ((:repairStatus = -1 AND ro.id IS NULL) OR (:repairStatus <> -1 AND ro.status = :repairStatus))) AND " +
            "(:reviewStatus IS NULL OR " +
            " ((:reviewStatus = 1 AND s.reviewer IS NOT NULL AND s.reviewer <> '') OR " +
            "  (:reviewStatus = 0 AND s.qualified = false AND (s.reviewer IS NULL OR s.reviewer = '')))) AND " +
            "(:thermometerNo IS NULL OR s.thermometerNo = :thermometerNo) " +
            "ORDER BY s.checkDate DESC, s.id DESC")
    List<SpotCheckRecord> findByFilter(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("areaId") Long areaId,
                                      @Param("qualified") Boolean qualified,
                                      @Param("repairStatus") Integer repairStatus,
                                      @Param("reviewStatus") Integer reviewStatus,
                                      @Param("thermometerNo") String thermometerNo);
}
