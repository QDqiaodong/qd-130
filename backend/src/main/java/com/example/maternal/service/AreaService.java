
package com.example.maternal.service;

import com.example.maternal.dto.AreaDTO;
import com.example.maternal.entity.Area;
import com.example.maternal.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;

    /**
     * 解析区域筛选范围：选中父区域时包含其全部下级区域，选中不存在的区域时返回 null。
     */
    public Set<Long> resolveScopeAreaIds(Long areaId) {
        if (areaId == null) {
            return null;
        }
        Area selected = areaRepository.findById(areaId).orElse(null);
        if (selected == null) {
            return null;
        }
        String prefix = selected.getPath() == null ? null : selected.getPath() + "/";
        return areaRepository.findAll().stream()
                .filter(a -> a.getId().equals(areaId)
                        || (prefix != null && a.getPath() != null && a.getPath().startsWith(prefix)))
                .map(Area::getId)
                .collect(Collectors.toSet());
    }

    public List<AreaDTO> getAllAreas() {
        return areaRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<AreaDTO> getAreaTree() {
        List<Area> allAreas = areaRepository.findAllByStatus(1);
        Map<Long, List<Area>> childrenMap = allAreas.stream()
                .filter(area -> area.getParentId() != null)
                .collect(Collectors.groupingBy(Area::getParentId));
        
        List<AreaDTO> tree = new ArrayList<>();
        for (Area area : allAreas) {
            if (area.getParentId() == null) {
                AreaDTO dto = convertToDTO(area);
                buildTree(dto, childrenMap);
                tree.add(dto);
            }
        }
        return tree;
    }
    
    private void buildTree(AreaDTO parent, Map<Long, List<Area>> childrenMap) {
        List<Area> children = childrenMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            List<AreaDTO> childDTOs = children.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            childDTOs.forEach(child -> buildTree(child, childrenMap));
            parent.setChildren(childDTOs);
        }
    }
    
    public AreaDTO getAreaById(Long id) {
        return areaRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }
    
    public AreaDTO createArea(AreaDTO dto) {
        Area area = convertToEntity(dto);
        if (area.getParentId() != null) {
            Area parent = areaRepository.findById(area.getParentId()).orElse(null);
            if (parent != null) {
                area.setLevel(parent.getLevel() + 1);
                area.setPath(parent.getPath() + "/" + area.getCode());
            }
        } else {
            area.setLevel(1);
            area.setPath("/" + area.getCode());
        }
        Area saved = areaRepository.save(area);
        return convertToDTO(saved);
    }
    
    public AreaDTO updateArea(Long id, AreaDTO dto) {
        return areaRepository.findById(id)
                .map(area -> {
                    area.setName(dto.getName());
                    area.setCode(dto.getCode());
                    area.setParentId(dto.getParentId());
                    area.setStatus(dto.getStatus());
                    area.setSortOrder(dto.getSortOrder());
                    if (area.getParentId() != null) {
                        Area parent = areaRepository.findById(area.getParentId()).orElse(null);
                        if (parent != null) {
                            area.setLevel(parent.getLevel() + 1);
                            area.setPath(parent.getPath() + "/" + area.getCode());
                        }
                    } else {
                        area.setLevel(1);
                        area.setPath("/" + area.getCode());
                    }
                    return convertToDTO(areaRepository.save(area));
                })
                .orElse(null);
    }
    
    public void deleteArea(Long id) {
        areaRepository.deleteById(id);
    }
    
    private AreaDTO convertToDTO(Area area) {
        AreaDTO dto = new AreaDTO();
        dto.setId(area.getId());
        dto.setName(area.getName());
        dto.setCode(area.getCode());
        dto.setParentId(area.getParentId());
        dto.setLevel(area.getLevel());
        dto.setPath(area.getPath());
        dto.setStatus(area.getStatus());
        dto.setSortOrder(area.getSortOrder());
        return dto;
    }
    
    private Area convertToEntity(AreaDTO dto) {
        Area area = new Area();
        area.setName(dto.getName());
        area.setCode(dto.getCode());
        area.setParentId(dto.getParentId());
        area.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        area.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        return area;
    }
}
