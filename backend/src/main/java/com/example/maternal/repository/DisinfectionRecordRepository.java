
package com.example.maternal.repository;

import com.example.maternal.entity.DisinfectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisinfectionRecordRepository extends JpaRepository<DisinfectionRecord, Long> {

    /** 同一母婴室当天是否已存在闭环记录（重复闭环闸门） */
    boolean existsByAreaIdAndDisinfectDateAndClosedLoopTrue(Long areaId, LocalDate disinfectDate);

    @Query("SELECT d FROM DisinfectionRecord d WHERE " +
            "(:startDate IS NULL OR d.disinfectDate >= :startDate) AND " +
            "(:endDate IS NULL OR d.disinfectDate <= :endDate) AND " +
            "(:areaId IS NULL OR d.areaId = :areaId) AND " +
            "(:closedLoop IS NULL OR d.closedLoop = :closedLoop) " +
            "ORDER BY d.disinfectDate DESC, d.id DESC")
    List<DisinfectionRecord> findByFilter(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("areaId") Long areaId,
                                          @Param("closedLoop") Boolean closedLoop);
}
