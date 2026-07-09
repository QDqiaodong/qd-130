
package com.example.maternal.repository;

import com.example.maternal.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    
    Optional<Equipment> findByEquipmentNo(String equipmentNo);
    
    List<Equipment> findByCurrentAreaId(Long currentAreaId);
    
    List<Equipment> findByInitialAreaId(Long initialAreaId);
    
    List<Equipment> findByEquipmentType(String equipmentType);
    
    List<Equipment> findByStatus(Integer status);
    
    boolean existsByEquipmentNo(String equipmentNo);
}
