
package com.example.maternal.service;

import com.example.maternal.entity.Area;
import com.example.maternal.repository.AreaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock
    private AreaRepository areaRepository;

    @InjectMocks
    private AreaService areaService;

    private Area area(Long id, String name, String code, Long parentId, String path, Integer status) {
        Area area = new Area();
        area.setId(id);
        area.setName(name);
        area.setCode(code);
        area.setParentId(parentId);
        area.setLevel(parentId == null ? 1 : 2);
        area.setPath(path);
        area.setStatus(status);
        return area;
    }

    @Test
    void resolveInUseScopeAreaIds_parentIncludesActiveSelfAndChildrenButExcludesDisabledRoom() {
        Area parent = area(1L, "商超A区", "AREA-A", null, "/AREA-A", 1);
        Area a1 = area(4L, "A1母婴室", "AREA-A1", 1L, "/AREA-A/AREA-A1", 1);
        Area a2 = area(5L, "A2母婴室", "AREA-A2", 1L, "/AREA-A/AREA-A2", 1);
        Area disabled = area(8L, "A3停用室", "AREA-A3", 1L, "/AREA-A/AREA-A3", 0);
        Area b1 = area(6L, "B1母婴室", "AREA-B1", 2L, "/AREA-B/AREA-B1", 1);

        when(areaRepository.findAllByStatus(1)).thenReturn(List.of(parent, a1, a2, b1));
        when(areaRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(areaRepository.findAll()).thenReturn(List.of(parent, a1, a2, disabled, b1));

        Set<Long> scope = areaService.resolveInUseScopeAreaIds(1L);

        assertThat(scope).containsExactlyInAnyOrder(1L, 4L, 5L);
        assertThat(scope).doesNotContain(8L, 6L);
    }

    @Test
    void resolveInUseScopeAreaIds_withoutSelectedAreaReturnsAllActiveAreas() {
        Area parent = area(1L, "商超A区", "AREA-A", null, "/AREA-A", 1);
        Area activeRoom = area(4L, "A1母婴室", "AREA-A1", 1L, "/AREA-A/AREA-A1", 1);
        Area disabledRoom = area(8L, "A3停用室", "AREA-A3", 1L, "/AREA-A/AREA-A3", 0);
        when(areaRepository.findAllByStatus(1)).thenReturn(List.of(parent, activeRoom));

        Set<Long> scope = areaService.resolveInUseScopeAreaIds(null);

        assertThat(scope).containsExactlyInAnyOrder(1L, 4L);
        assertThat(scope).doesNotContain(disabledRoom.getId());
        verify(areaRepository, never()).findAll();
    }

    @Test
    void resolveInUseScopeAreaIds_missingAreaThrowsClearError() {
        when(areaRepository.findAllByStatus(1)).thenReturn(List.of());
        when(areaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> areaService.resolveInUseScopeAreaIds(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("所选母婴室区域不存在");
    }
}
