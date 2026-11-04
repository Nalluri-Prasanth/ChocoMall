package com.chocomall.admin.service;

import com.chocomall.admin.dto.SalesDtos.LevelParam;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.loyalty.LoyaltyService;
import com.chocomall.model.entity.IntegrationHistory;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.MemberLevel;
import com.chocomall.model.repository.IntegrationHistoryRepository;
import com.chocomall.model.repository.MemberLevelRepository;
import com.chocomall.model.repository.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer accounts and loyalty levels. Passwords never leave the server. */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberAdminService {

    public record MemberView(Long id, String username, String nickname, String phone, String email, Integer status,
                             LocalDateTime createTime, Long levelId, String levelName, Integer integration, Integer growth,
                             Integer historyIntegration, boolean marketingConsent, String city, LocalDate birthday) {
    }

    private final MemberRepository members;
    private final MemberLevelRepository levels;
    private final IntegrationHistoryRepository history;
    private final LoyaltyService loyalty;

    @Transactional(readOnly = true)
    public CommonPage<MemberView> list(String keyword, Integer pageNum, Integer pageSize) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return CommonPage.of(members.search(kw, PageParams.of(pageNum, pageSize, Sort.by(Sort.Order.desc("id")))), this::view);
    }

    @Transactional(readOnly = true)
    public MemberView get(Long id) {
        return view(load(id));
    }

    public void setStatus(Long id, int status) {
        load(id).setStatus(status);
    }

    public void adjustPoints(Long id, int delta, String note) {
        load(id);
        Asserts.isTrue(delta != 0, "The adjustment cannot be zero");
        loyalty.adjust(id, delta, note);
    }

    @Transactional(readOnly = true)
    public CommonPage<IntegrationHistory> pointsHistory(Long id, Integer pageNum, Integer pageSize) {
        return CommonPage.of(history.findByMemberIdOrderByIdDesc(id, PageParams.of(pageNum, pageSize)));
    }

    // ---------------------------------------------------------------------------- levels

    @Transactional(readOnly = true)
    public List<MemberLevel> levels() {
        return levels.findAllByOrderByGrowthPointAsc();
    }

    public MemberLevel createLevel(LevelParam p) {
        MemberLevel l = new MemberLevel();
        applyLevel(l, p);
        return levels.save(l);
    }

    public MemberLevel updateLevel(Long id, LevelParam p) {
        MemberLevel l = Asserts.found(levels.findById(id), "Level");
        applyLevel(l, p);
        return l;
    }

    public void deleteLevel(Long id) {
        MemberLevel l = Asserts.found(levels.findById(id), "Level");
        Asserts.state(!l.isDefaultStatus(), "The default level cannot be deleted");
        Asserts.state(members.findAll().stream().noneMatch(m -> id.equals(m.getLevelId())), "Members are still on this level");
        levels.delete(l);
    }

    private void applyLevel(MemberLevel l, LevelParam p) {
        if (Boolean.TRUE.equals(p.defaultStatus())) {
            levels.findAll().forEach(other -> other.setDefaultStatus(false));
        }
        l.setName(p.name());
        l.setGrowthPoint(p.growthPoint());
        l.setDefaultStatus(Boolean.TRUE.equals(p.defaultStatus()));
        l.setPointsMultiplier(p.pointsMultiplier());
        l.setFreeDeliveryThreshold(p.freeDeliveryThreshold());
        l.setNote(p.note());
    }

    private Member load(Long id) {
        return Asserts.found(members.findById(id), "Member");
    }

    private MemberView view(Member m) {
        String levelName = m.getLevelId() == null ? null : levels.findById(m.getLevelId()).map(MemberLevel::getName).orElse(null);
        return new MemberView(m.getId(), m.getUsername(), m.getNickname(), m.getPhone(), m.getEmail(), m.getStatus(),
                m.getCreateTime(), m.getLevelId(), levelName, m.getIntegration(), m.getGrowth(), m.getHistoryIntegration(),
                m.isMarketingConsent(), m.getCity(), m.getBirthday());
    }
}
