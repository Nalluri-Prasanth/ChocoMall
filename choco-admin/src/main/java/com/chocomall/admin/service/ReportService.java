package com.chocomall.admin.service;

import com.chocomall.common.exception.Asserts;
import com.chocomall.model.entity.Product;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.PersonalizationStatus;
import com.chocomall.model.enums.ReturnStatus;
import com.chocomall.model.enums.SessionStatus;
import com.chocomall.model.repository.ExperienceSessionRepository;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.PersonalizationRequestRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ReturnApplyRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sales and economics reports. The contribution report implements the guide's rule of tracking sales, waste and
 * cost by product ("do not judge by revenue alone") with ingredient, packaging, labour and selling fees deducted.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final Set<PersonalizationStatus> OPEN_PERSONALIZATION = EnumSet.of(
            PersonalizationStatus.SUBMITTED, PersonalizationStatus.QUOTED, PersonalizationStatus.PROOF_SENT,
            PersonalizationStatus.PROOF_APPROVED, PersonalizationStatus.DEPOSIT_PAID, PersonalizationStatus.IN_PRODUCTION,
            PersonalizationStatus.READY);

    private final ShopOrderRepository orders;
    private final OrderItemRepository items;
    private final ProductRepository products;
    private final ReturnApplyRepository returns;
    private final PersonalizationRequestRepository personalization;
    private final ExperienceSessionRepository sessions;

    public Map<String, Object> dashboard() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = startOfToday.plusDays(1);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("revenueToday", orders.revenueBetween(startOfToday, tomorrow));
        out.put("ordersToday", orders.paidCountBetween(startOfToday, tomorrow));
        out.put("revenueLast7Days", orders.revenueBetween(startOfToday.minusDays(6), tomorrow));
        out.put("revenueLast30Days", orders.revenueBetween(startOfToday.minusDays(29), tomorrow));
        Map<String, Long> queue = new LinkedHashMap<>();
        for (OrderStatus s : List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, OrderStatus.SHIPPED)) {
            queue.put(s.name(), orders.countByStatus(s));
        }
        out.put("orderQueue", queue);
        out.put("lowStockProducts", products.findLowStock().size());
        out.put("pendingReturns", returns.search(ReturnStatus.PENDING, null, null, PageRequest.of(0, 1)).getTotalElements());
        out.put("openPersonalizationRequests", personalization.findAll().stream()
                .filter(r -> OPEN_PERSONALIZATION.contains(r.getStatus())).count());
        out.put("upcomingSessions", sessions.findByStatusAndStartsAtAfterOrderByStartsAt(SessionStatus.OPEN, LocalDateTime.now()).size());
        return out;
    }

    public List<Map<String, Object>> dailySales(LocalDate from, LocalDate to) {
        check(from, to);
        Map<LocalDate, Object[]> byDay = new LinkedHashMap<>();
        for (Object[] row : orders.dailySales(from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            byDay.put(toLocalDate(row[0]), row);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Object[] row = byDay.get(d);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d);
            m.put("orders", row == null ? 0L : ((Number) row[1]).longValue());
            m.put("revenue", row == null ? BigDecimal.ZERO : (BigDecimal) row[2]);
            out.add(m);
        }
        return out;
    }

    public List<Map<String, Object>> topProducts(LocalDate from, LocalDate to, int limit) {
        check(from, to);
        return items.productSales(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream().limit(Math.max(1, limit)).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId", r[0]);
            m.put("name", r[1]);
            m.put("units", ((Number) r[2]).longValue());
            m.put("revenue", r[3]);
            return m;
        }).toList();
    }

    /** Revenue minus ingredient, packaging, labour and selling fees, per product, best first. */
    public List<Map<String, Object>> contribution(LocalDate from, LocalDate to) {
        check(from, to);
        List<Object[]> sales = items.productSales(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        Map<Long, Product> byId = products.findByIdIn(sales.stream().map(r -> (Long) r[0]).toList())
                .stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : sales) {
            Product p = byId.get((Long) r[0]);
            long units = ((Number) r[2]).longValue();
            BigDecimal revenue = (BigDecimal) r[3];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId", r[0]);
            m.put("name", r[1]);
            m.put("units", units);
            m.put("revenue", revenue);
            if (p == null) {
                m.put("contribution", null);
                m.put("note", "Product no longer exists");
                out.add(m);
                continue;
            }
            BigDecimal unitCost = zero(p.getIngredientCost()).add(zero(p.getPackagingCost())).add(zero(p.getLaborCost()));
            BigDecimal costs = unitCost.multiply(BigDecimal.valueOf(units));
            BigDecimal fees = revenue.multiply(p.getSellingFeeRate()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal contribution = revenue.subtract(costs).subtract(fees);
            m.put("costs", costs);
            m.put("sellingFees", fees);
            m.put("contribution", contribution);
            m.put("marginPercent", revenue.signum() == 0 ? BigDecimal.ZERO
                    : contribution.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP));
            m.put("costsComplete", p.getIngredientCost() != null && p.getPackagingCost() != null && p.getLaborCost() != null);
            out.add(m);
        }
        out.sort(Comparator.comparing((Map<String, Object> m) -> m.get("contribution") == null ? BigDecimal.valueOf(-1_000_000) : (BigDecimal) m.get("contribution")).reversed());
        return out;
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static void check(LocalDate from, LocalDate to) {
        Asserts.isTrue(from != null && to != null && !to.isBefore(from), "Choose a valid date range");
        Asserts.isTrue(from.plusDays(366).isAfter(to), "The range cannot exceed one year");
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate d) {
            return d;
        }
        if (value instanceof Date d) {
            return d.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
