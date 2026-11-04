package com.chocomall.admin.service;

import com.chocomall.admin.dto.SalesDtos.LocationParam;
import com.chocomall.admin.dto.SalesDtos.SettingParam;
import com.chocomall.common.exception.Asserts;
import com.chocomall.model.entity.OrderSetting;
import com.chocomall.model.entity.ShopLocation;
import com.chocomall.model.repository.OrderSettingRepository;
import com.chocomall.model.repository.ShopLocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order rules (timeouts, delivery fees, loyalty) and pickup / return locations. */
@Service
@RequiredArgsConstructor
@Transactional
public class SettingsAdminService {

    private final OrderSettingRepository settings;
    private final ShopLocationRepository locations;

    @Transactional(readOnly = true)
    public OrderSetting get() {
        return settings.findAll().stream().findFirst().orElseGet(OrderSetting::new);
    }

    public OrderSetting update(SettingParam p) {
        Asserts.isTrue(p.maxRedeemPercent() <= 100, "The redeem limit cannot exceed 100%");
        OrderSetting s = settings.findAll().stream().findFirst().orElseGet(OrderSetting::new);
        s.setNormalOrderTimeoutMinutes(p.normalOrderTimeoutMinutes());
        s.setAutoConfirmDays(p.autoConfirmDays());
        s.setCommentTimeoutDays(p.commentTimeoutDays());
        s.setDeliveryFee(p.deliveryFee());
        s.setFreeDeliveryThreshold(p.freeDeliveryThreshold());
        s.setGiftPackagingFee(p.giftPackagingFee());
        s.setPointsPerCurrencyUnit(p.pointsPerCurrencyUnit());
        s.setMaxRedeemPercent(p.maxRedeemPercent());
        s.setDeliveryEnabled(p.deliveryEnabled());
        s.setPickupLeadMinutes(p.pickupLeadMinutes());
        s.setPreorderLeadDays(p.preorderLeadDays());
        s.setPointsPerCurrencySpent(p.pointsPerCurrencySpent());
        s.setReturnWindowDays(p.returnWindowDays());
        s.setExperienceCancelHours(p.experienceCancelHours());
        return settings.save(s);
    }

    @Transactional(readOnly = true)
    public List<ShopLocation> locations() {
        return locations.findAll();
    }

    public ShopLocation createLocation(LocationParam p) {
        ShopLocation l = new ShopLocation();
        apply(l, p);
        return locations.save(l);
    }

    public ShopLocation updateLocation(Long id, LocationParam p) {
        ShopLocation l = Asserts.found(locations.findById(id), "Location");
        apply(l, p);
        return l;
    }

    public void deleteLocation(Long id) {
        Asserts.state(locations.count() > 1, "Keep at least one shop location");
        locations.deleteById(id);
    }

    private void apply(ShopLocation l, LocationParam p) {
        l.setName(p.name());
        l.setPhone(p.phone());
        l.setProvince(p.province());
        l.setCity(p.city());
        l.setRegion(p.region());
        l.setDetailAddress(p.detailAddress());
        l.setPickupEnabled(p.pickupEnabled() == null || p.pickupEnabled());
        l.setReturnEnabled(p.returnEnabled() == null || p.returnEnabled());
        l.setOpeningHours(p.openingHours());
    }
}
