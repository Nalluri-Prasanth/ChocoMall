package com.chocomall.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.chocomall.model.entity.Product;
import com.chocomall.model.enums.BrandStockingStatus;
import com.chocomall.model.enums.GuideType;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.repository.AdminUserRepository;
import com.chocomall.model.repository.BrandRepository;
import com.chocomall.model.repository.ChocolateTypeRepository;
import com.chocomall.model.repository.FlavorRepository;
import com.chocomall.model.repository.FoodPairingRepository;
import com.chocomall.model.repository.GuideItemRepository;
import com.chocomall.model.repository.GuideSourceRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ProductSkuRepository;
import com.chocomall.model.repository.RoleRepository;
import com.chocomall.model.repository.ServingFormatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifies the Flyway schema matches the entities (ddl-auto=validate) and that the guide data is fully loaded. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:modeltest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "chocomall.seed.reference-data=true",
        "chocomall.seed.demo-shop=true",
        "chocomall.seed.admin-password=test-admin-pass",
        "chocomall.seed.demo-password=test-demo-pass"})
class SeedDataTest {

    @Autowired ChocolateTypeRepository types;
    @Autowired FlavorRepository flavors;
    @Autowired BrandRepository brands;
    @Autowired FoodPairingRepository pairings;
    @Autowired ServingFormatRepository servings;
    @Autowired GuideItemRepository guides;
    @Autowired GuideSourceRepository sources;
    @Autowired ProductRepository products;
    @Autowired ProductSkuRepository skus;
    @Autowired RoleRepository roles;
    @Autowired AdminUserRepository admins;

    @Test
    void everyCatalogOfTheGuideIsLoaded() {
        assertThat(types.count()).isEqualTo(28);
        assertThat(flavors.count()).isEqualTo(50);
        assertThat(brands.count()).isEqualTo(199);
        assertThat(pairings.count()).isEqualTo(80);
        assertThat(servings.count()).isEqualTo(45);
        assertThat(products.count()).isEqualTo(60);
        assertThat(guides.countByGuideType(GuideType.PRODUCTION)).isEqualTo(36);
        assertThat(guides.countByGuideType(GuideType.PRINTING)).isEqualTo(26);
        assertThat(guides.countByGuideType(GuideType.SALES_LAUNCH)).isEqualTo(30);
        assertThat(guides.countByGuideType(GuideType.SHOP_FORMAT)).isEqualTo(8);
        assertThat(guides.countByGuideType(GuideType.ROADMAP)).isEqualTo(5);
        assertThat(sources.count()).isEqualTo(8);
    }

    @Test
    void shortlistedBrandsStartInSampling() {
        long sampling = brands.findAll().stream()
                .filter(b -> b.getStockingStatus() == BrandStockingStatus.SAMPLING).count();
        assertThat(sampling).isEqualTo(13);
    }

    @Test
    void launchTierDrivesVisibilityAndPersonalisedItemsAreNotShelfStock() {
        Product pretzels = products.findByProductSn("P25").orElseThrow();
        assertThat(pretzels.getLaunchTier()).isEqualTo(Priority.CORE);
        assertThat(pretzels.isPublished()).isTrue();

        Product gelato = products.findByProductSn("P43").orElseThrow();
        assertThat(gelato.getLaunchTier()).isEqualTo(Priority.SPECIALIST);
        assertThat(gelato.isPublished()).isFalse();
        assertThat(gelato.isRequiresColdChain()).isTrue();

        Product plaque = products.findByProductSn("P54").orElseThrow();
        assertThat(plaque.getKind()).isEqualTo(ProductKind.PERSONALIZED);
        assertThat(plaque.getStock()).isZero();

        Product buildABox = products.findByProductSn("P47").orElseThrow();
        assertThat(buildABox.isBuildABox()).isTrue();
        assertThat(skus.findByProductIdOrderBySortAscIdAsc(buildABox.getId()))
                .extracting("units").containsExactly(6, 12, 24);
    }

    @Test
    void productsAreLinkedToFlavorsAndAllergens() {
        Product saltedCaramel = products.findByProductSn("P02").orElseThrow();
        assertThat(saltedCaramel.getFlavorIds()).isNotEmpty();
        Product peanutButter = products.findByProductSn("P06").orElseThrow();
        assertThat(peanutButter.getAllergenIds()).isNotEmpty();
    }

    @Test
    void rbacBaselineAndBootstrapAccountsExist() {
        assertThat(roles.findByName("SUPER_ADMIN")).isPresent();
        assertThat(roles.findByName("SUPER_ADMIN").orElseThrow().getResourceIds()).hasSizeGreaterThan(20);
        assertThat(admins.findByUsername("admin")).isPresent();
        assertThat(admins.findByUsername("catalog")).isPresent();
    }
}
