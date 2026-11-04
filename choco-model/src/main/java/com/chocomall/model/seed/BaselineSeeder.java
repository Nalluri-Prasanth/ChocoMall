package com.chocomall.model.seed;

import com.chocomall.model.entity.AdminUser;
import com.chocomall.model.entity.MemberLevel;
import com.chocomall.model.entity.OrderSetting;
import com.chocomall.model.entity.ReturnReason;
import com.chocomall.model.entity.Role;
import com.chocomall.model.entity.ShopLocation;
import com.chocomall.model.repository.AdminUserRepository;
import com.chocomall.model.repository.MemberLevelRepository;
import com.chocomall.model.repository.OrderSettingRepository;
import com.chocomall.model.repository.ReturnReasonRepository;
import com.chocomall.model.repository.RoleRepository;
import com.chocomall.model.repository.ShopLocationRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operational configuration every installation needs: order rules, loyalty tiers, return reasons, the first
 * shop location and, optionally, the bootstrap super-admin. Idempotent.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class BaselineSeeder implements ApplicationRunner {

    private final SeedProperties props;
    private final OrderSettingRepository settings;
    private final MemberLevelRepository levels;
    private final ReturnReasonRepository reasons;
    private final ShopLocationRepository locations;
    private final AdminUserRepository admins;
    private final RoleRepository roles;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (settings.count() == 0) {
            settings.save(new OrderSetting());
        }
        if (levels.count() == 0) {
            levels.save(level("Bronze", 0, true, "1.00", null, "Everyone starts here"));
            levels.save(level("Silver", 500, false, "1.25", "30.00", "1.25x points and a lower free-delivery threshold"));
            levels.save(level("Gold", 2000, false, "1.50", "20.00", "1.5x points and the lowest free-delivery threshold"));
        }
        if (reasons.count() == 0) {
            List<String> names = List.of("Damaged or broken in transit", "Melted or heat-affected", "Wrong item received",
                    "Allergen or ingredient label did not match", "Quality or taste not as described", "Other");
            int sort = names.size();
            for (String n : names) {
                ReturnReason r = new ReturnReason();
                r.setName(n);
                r.setSort(sort--);
                reasons.save(r);
            }
        }
        if (locations.count() == 0) {
            ShopLocation l = new ShopLocation();
            l.setName("Main boutique");
            l.setOpeningHours("Mon-Sat 09:00-19:00");
            locations.save(l);
        }
        bootstrapAdmin();
    }

    private void bootstrapAdmin() {
        String password = props.adminPassword();
        if (password == null || password.isBlank() || admins.count() > 0) {
            return;
        }
        Role superAdmin = roles.findByName("SUPER_ADMIN").orElseThrow();
        AdminUser admin = new AdminUser();
        admin.setUsername(props.adminUsername());
        admin.setPassword(new BCryptPasswordEncoder().encode(password));
        admin.setNickName("Super admin");
        admin.getRoleIds().add(superAdmin.getId());
        admins.save(admin);
        superAdmin.setAdminCount(1);
        log.info("Bootstrap admin '{}' created", admin.getUsername());
    }

    private MemberLevel level(String name, int growth, boolean isDefault, String multiplier, String freeDelivery, String note) {
        MemberLevel l = new MemberLevel();
        l.setName(name);
        l.setGrowthPoint(growth);
        l.setDefaultStatus(isDefault);
        l.setPointsMultiplier(new BigDecimal(multiplier));
        l.setFreeDeliveryThreshold(freeDelivery == null ? null : new BigDecimal(freeDelivery));
        l.setNote(note);
        return l;
    }
}
