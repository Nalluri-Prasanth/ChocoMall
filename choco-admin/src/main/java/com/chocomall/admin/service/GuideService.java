package com.chocomall.admin.service;

import com.chocomall.admin.dto.OpsDtos.GuideUpdateParam;
import com.chocomall.common.exception.Asserts;
import com.chocomall.model.entity.GuideItem;
import com.chocomall.model.entity.GuideSource;
import com.chocomall.model.enums.GuideStatus;
import com.chocomall.model.enums.GuideType;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.repository.GuideItemRepository;
import com.chocomall.model.repository.GuideSourceRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The operating playbook from the shop guide (production controls, 3D-printing workflow, sales and launch actions,
 * shop formats and the five-step roadmap) as a trackable checklist.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GuideService {

    private final GuideItemRepository guides;
    private final GuideSourceRepository sources;

    @Transactional(readOnly = true)
    public List<GuideItem> list(GuideType type, GuideStatus status, Priority priority, String phaseFilter) {
        String phase = phaseFilter == null || phaseFilter.isBlank() ? null : phaseFilter;
        return guides.findByGuideTypeOrderBySortAscCodeAsc(type).stream()
                .filter(g -> status == null || g.getStatus() == status)
                .filter(g -> priority == null || g.getPriority() == priority)
                .filter(g -> phase == null || phase.equalsIgnoreCase(g.getPhase()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GuideItem get(Long id) {
        return Asserts.found(guides.findById(id), "Guide item");
    }

    public GuideItem update(Long id, GuideUpdateParam p) {
        GuideItem g = get(id);
        g.setStatus(p.status());
        if (p.implementedBy() != null) {
            g.setImplementedBy(p.implementedBy());
        }
        return g;
    }

    /** Completion per guide type: how much of the playbook has been put into practice. */
    @Transactional(readOnly = true)
    public Map<String, Object> progress() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (GuideType type : GuideType.values()) {
            long total = guides.countByGuideType(type);
            long done = guides.countByGuideTypeAndStatus(type, GuideStatus.DONE);
            long inProgress = guides.countByGuideTypeAndStatus(type, GuideStatus.IN_PROGRESS);
            long na = guides.countByGuideTypeAndStatus(type, GuideStatus.NOT_APPLICABLE);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("total", total);
            row.put("done", done);
            row.put("inProgress", inProgress);
            row.put("notApplicable", na);
            long relevant = total - na;
            row.put("percentDone", relevant == 0 ? 0 : Math.round(done * 1000.0 / relevant) / 10.0);
            out.put(type.name(), row);
        }
        return out;
    }

    /** Sales and launch actions grouped by when they apply (Launch, Expand, Test). */
    @Transactional(readOnly = true)
    public Map<String, List<GuideItem>> launchChecklist() {
        Map<String, List<GuideItem>> grouped = new LinkedHashMap<>();
        for (String phase : List.of("Launch", "Expand", "Test")) {
            grouped.put(phase, list(GuideType.SALES_LAUNCH, null, null, phase));
        }
        return grouped;
    }

    @Transactional(readOnly = true)
    public List<GuideSource> sources() {
        return sources.findAll();
    }
}
