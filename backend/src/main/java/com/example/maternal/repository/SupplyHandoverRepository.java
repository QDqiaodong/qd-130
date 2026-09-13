
package com.example.maternal.repository;

import com.example.maternal.entity.SupplyHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplyHandoverRepository extends JpaRepository<SupplyHandover, Long> {

    @Query("SELECT h FROM SupplyHandover h WHERE " +
            "(:areaId IS NULL OR h.areaId = :areaId) AND " +
            "(:handedOver IS NULL OR h.handedOver = :handedOver) " +
            "ORDER BY h.handoverDate DESC, h.id DESC")
    List<SupplyHandover> findByFilter(@Param("areaId") Long areaId,
                                      @Param("handedOver") Boolean handedOver);
}
