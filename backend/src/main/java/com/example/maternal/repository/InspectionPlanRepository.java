
package com.example.maternal.repository;

import com.example.maternal.entity.InspectionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InspectionPlanRepository extends JpaRepository<InspectionPlan, Long> {

    Optional<InspectionPlan> findByPlanNo(String planNo);

    List<InspectionPlan> findByStatus(Integer status);

    List<InspectionPlan> findByEquipmentId(Long equipmentId);

    List<InspectionPlan> findByAreaId(Long areaId);

    boolean existsByPlanNo(String planNo);

    @Query("SELECT p FROM InspectionPlan p ORDER BY p.createdAt DESC, p.id DESC")
    List<InspectionPlan> findAllOrdered();
}
