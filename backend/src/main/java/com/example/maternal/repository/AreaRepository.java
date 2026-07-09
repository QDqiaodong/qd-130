
package com.example.maternal.repository;

import com.example.maternal.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {
    
    Optional<Area> findByCode(String code);
    
    List<Area> findByParentId(Long parentId);
    
    List<Area> findByParentIdOrderBySortOrderAsc(Long parentId);
    
    List<Area> findByLevel(Integer level);
    
    List<Area> findByStatus(Integer status);
    
    List<Area> findAllByStatus(Integer status);
    
    boolean existsByCode(String code);
}
