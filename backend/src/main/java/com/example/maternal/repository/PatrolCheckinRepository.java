
package com.example.maternal.repository;

import com.example.maternal.entity.PatrolCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PatrolCheckinRepository extends JpaRepository<PatrolCheckin, Long> {

    /** 同一母婴室同一巡更日期同一班次是否已打卡（重复打卡闸门，DB 唯一约束兜底） */
    boolean existsByAreaIdAndPatrolDateAndShift(Long areaId, LocalDate patrolDate, Integer shift);

    @Query("SELECT p FROM PatrolCheckin p WHERE " +
            "(:patrolDate IS NULL OR p.patrolDate = :patrolDate) AND " +
            "(:shift IS NULL OR p.shift = :shift) " +
            "ORDER BY p.patrolDate DESC, p.shift ASC, p.id DESC")
    List<PatrolCheckin> findByFilter(@Param("patrolDate") LocalDate patrolDate,
                                     @Param("shift") Integer shift);
}
