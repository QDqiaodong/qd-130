
package com.example.maternal.repository;

import com.example.maternal.entity.RoomOpeningRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomOpeningRecordRepository extends JpaRepository<RoomOpeningRecord, Long> {

    /** 同一母婴室同一开放日是否已登记（重复登记闸门，DB 唯一约束兜底） */
    boolean existsByAreaIdAndOpenDate(Long areaId, LocalDate openDate);

    @Query("SELECT r FROM RoomOpeningRecord r WHERE " +
            "(:startDate IS NULL OR r.openDate >= :startDate) AND " +
            "(:endDate IS NULL OR r.openDate <= :endDate) AND " +
            "(:areaId IS NULL OR r.areaId = :areaId) " +
            "ORDER BY r.openDate DESC, r.id DESC")
    List<RoomOpeningRecord> findByFilter(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("areaId") Long areaId);
}
