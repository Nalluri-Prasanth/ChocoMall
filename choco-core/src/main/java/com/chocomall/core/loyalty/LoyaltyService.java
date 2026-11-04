package com.chocomall.core.loyalty;

import com.chocomall.common.api.ResultCode;
import com.chocomall.common.exception.ApiException;
import com.chocomall.model.entity.IntegrationHistory;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.MemberLevel;
import com.chocomall.model.entity.OrderSetting;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.IntegrationChangeType;
import com.chocomall.model.repository.IntegrationHistoryRepository;
import com.chocomall.model.repository.MemberLevelRepository;
import com.chocomall.model.repository.MemberRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transparent loyalty programme: points are earned on paid orders, can be redeemed against later orders, and
 * every movement is written to the member's history with the order it came from.
 *
 * <p>Earning: {@code floor(payAmount * pointsPerCurrencySpent * levelMultiplier)} points and
 * {@code floor(payAmount)} growth. Growth moves members up the levels; points are spendable currency.
 */
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final MemberRepository members;
    private final MemberLevelRepository levels;
    private final IntegrationHistoryRepository history;

    /** Value, in currency units, of the given number of points. */
    public BigDecimal pointsValue(int points, OrderSetting setting) {
        return BigDecimal.valueOf(points).divide(BigDecimal.valueOf(setting.getPointsPerCurrencyUnit()), 2, RoundingMode.DOWN);
    }

    /** Largest number of points a member may spend on an order of the given payable value. */
    public int maxRedeemablePoints(Long memberId, BigDecimal payable, OrderSetting setting) {
        Integer balance = members.integrationOf(memberId);
        BigDecimal cap = payable.multiply(BigDecimal.valueOf(setting.getMaxRedeemPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
        int byCap = cap.multiply(BigDecimal.valueOf(setting.getPointsPerCurrencyUnit())).intValue();
        return Math.max(0, Math.min(balance == null ? 0 : balance, byCap));
    }

    /** Deducts points when an order is placed. Fails if the balance is too low (checked atomically). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void redeem(Long memberId, int points, String orderSn) {
        if (points <= 0) {
            return;
        }
        if (members.addPoints(memberId, -points, 0, 0) == 0) {
            throw new ApiException(ResultCode.CONFLICT, "Not enough loyalty points");
        }
        record(memberId, IntegrationChangeType.REDEMPTION, -points, "Redeemed on order " + orderSn, orderSn);
    }

    /** Returns redeemed points of a cancelled order. Idempotent per order. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void returnRedeemed(Long memberId, int points, String orderSn) {
        if (points <= 0 || history.existsByOrderSnAndChangeType(orderSn, IntegrationChangeType.REFUND_REVERSAL)) {
            return;
        }
        members.addPoints(memberId, points, 0, 0);
        record(memberId, IntegrationChangeType.REFUND_REVERSAL, points, "Points returned for cancelled order " + orderSn, orderSn);
    }

    /** Awards points and growth for a paid order (once per order) and promotes the member when a level is reached. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int earn(ShopOrder order, OrderSetting setting) {
        if (history.existsByOrderSnAndChangeType(order.getOrderSn(), IntegrationChangeType.ORDER_REWARD)) {
            return order.getIntegrationEarned();
        }
        Member member = members.findById(order.getMemberId()).orElseThrow();
        BigDecimal multiplier = levels.findById(member.getLevelId() == null ? -1L : member.getLevelId())
                .map(MemberLevel::getPointsMultiplier).orElse(BigDecimal.ONE);
        int points = order.getPayAmount()
                .multiply(BigDecimal.valueOf(setting.getPointsPerCurrencySpent()))
                .multiply(multiplier).setScale(0, RoundingMode.DOWN).intValue();
        int growth = order.getPayAmount().setScale(0, RoundingMode.DOWN).intValue();
        members.addPoints(member.getId(), points, growth, points);
        order.setIntegrationEarned(points);
        order.setGrowthEarned(growth);
        record(member.getId(), IntegrationChangeType.ORDER_REWARD, points, "Earned on order " + order.getOrderSn(), order.getOrderSn());
        promote(member.getId());
        return points;
    }

    /** Takes back what a refunded order earned. Never lets the balance go negative. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reverseEarned(ShopOrder order) {
        int points = order.getIntegrationEarned();
        if (points <= 0) {
            return;
        }
        Integer balance = members.integrationOf(order.getMemberId());
        int take = Math.min(points, balance == null ? 0 : balance);
        members.addPoints(order.getMemberId(), -take, -order.getGrowthEarned(), 0);
        record(order.getMemberId(), IntegrationChangeType.REFUND_REVERSAL, -take, "Points reversed for refunded order " + order.getOrderSn(),
                order.getOrderSn() + "-R");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void adjust(Long memberId, int delta, String note) {
        if (members.addPoints(memberId, delta, 0, Math.max(delta, 0)) == 0) {
            throw new ApiException(ResultCode.CONFLICT, "Adjustment would make the balance negative");
        }
        record(memberId, IntegrationChangeType.ADJUSTMENT, delta, note, null);
    }

    private void promote(Long memberId) {
        Integer growth = members.growthOf(memberId);
        List<MemberLevel> ordered = levels.findAllByOrderByGrowthPointAsc();
        MemberLevel best = null;
        for (MemberLevel level : ordered) {
            if (growth != null && growth >= level.getGrowthPoint()) {
                best = level;
            }
        }
        if (best != null) {
            members.setLevel(memberId, best.getId());
        }
    }

    private void record(Long memberId, IntegrationChangeType type, int change, String note, String orderSn) {
        IntegrationHistory h = new IntegrationHistory();
        h.setMemberId(memberId);
        h.setChangeType(type);
        h.setChangeCount(change);
        Integer balance = members.integrationOf(memberId);
        h.setBalanceAfter(balance == null ? 0 : balance);
        h.setNote(note);
        h.setOrderSn(orderSn);
        history.save(h);
    }
}
