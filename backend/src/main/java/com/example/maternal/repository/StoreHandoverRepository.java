
package com.example.maternal.repository;

import com.example.maternal.entity.StoreHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreHandoverRepository extends JpaRepository<StoreHandover, Long> {

    Optional<StoreHandover> findByHandoverNo(String handoverNo);

    boolean existsByAreaIdAndHandoverDateAndHandoverType(Long areaId, LocalDate handoverDate, Integer handoverType);

    Optional<StoreHandover> findByAreaIdAndHandoverDateAndHandoverType(Long areaId, LocalDate handoverDate, Integer handoverType);

    @Query("SELECT h FROM StoreHandover h WHERE " +
            "(:startDate IS NULL OR h.handoverDate >= :startDate) AND " +
            "(:endDate IS NULL OR h.handoverDate <= :endDate) AND " +
            "(:areaId IS NULL OR h.areaId = :areaId) AND " +
            "(:handoverType IS NULL OR h.handoverType = :handoverType) AND " +
            "(:result IS NULL OR h.result = :result) " +
            "ORDER BY h.handoverDate DESC, h.handoverType ASC, h.id DESC")
    List<StoreHandover> findByFilter(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate,
                                     @Param("areaId") Long areaId,
                                     @Param("handoverType") Integer handoverType,
                                     @Param("result") Integer result);

    /**
     * 取某区域截至指定日期（含）最近一次已登记的交接，闭店与开店统一按时间先后比较，
     * 用于派生该区域设备是否可调配。
     */
    @Query("SELECT h FROM StoreHandover h WHERE h.areaId = :areaId AND h.handoverDate <= :date " +
            "ORDER BY h.handoverDate DESC, h.handoverType DESC, h.id DESC")
    List<StoreHandover> findLatestByAreaUpToDate(@Param("areaId") Long areaId, @Param("date") LocalDate date);
}
