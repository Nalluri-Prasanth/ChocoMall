package com.chocomall.model.seed;

import com.chocomall.model.entity.Allergen;
import com.chocomall.model.entity.Brand;
import com.chocomall.model.entity.ChocolateType;
import com.chocomall.model.entity.Flavor;
import com.chocomall.model.entity.FoodPairing;
import com.chocomall.model.entity.GuideItem;
import com.chocomall.model.entity.GuideSource;
import com.chocomall.model.entity.ServingFormat;
import com.chocomall.model.enums.BrandStockingStatus;
import com.chocomall.model.enums.GuideStatus;
import com.chocomall.model.enums.GuideType;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.repository.AllergenRepository;
import com.chocomall.model.repository.BrandRepository;
import com.chocomall.model.repository.ChocolateTypeRepository;
import com.chocomall.model.repository.FlavorRepository;
import com.chocomall.model.repository.FoodPairingRepository;
import com.chocomall.model.repository.GuideItemRepository;
import com.chocomall.model.repository.GuideSourceRepository;
import com.chocomall.model.repository.ServingFormatRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the catalogs of the shop guide: chocolate types, flavors, allergens, the global brand directory,
 * food pairings, serving formats and the operational guides. Each table is only filled while it is empty,
 * so restarts never create duplicates.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class ReferenceDataSeeder implements ApplicationRunner {

    /** Brand names from the guide's "practical buying shortlist"; they start in the sampling stage. */
    private static final List<String> SHORTLIST = List.of(
            "Callebaut", "Cacao Barry", "Valrhona", "Guittard", "Lindt", "Ferrero Rocher", "Cadbury", "Ritter Sport",
            "Tony's Chocolonely", "Auro", "Marou", "Menakao", "Paccari");

    /** The nine major allergens listed in the guide's allergen reference. */
    private static final Map<String, String> ALLERGENS = new java.util.LinkedHashMap<>(Map.of(
            "MILK", "Milk", "EGG", "Egg", "FISH", "Fish", "CRUSTACEAN_SHELLFISH", "Crustacean shellfish",
            "TREE_NUTS", "Tree nuts", "PEANUT", "Peanut", "WHEAT", "Wheat", "SOY", "Soy", "SESAME", "Sesame"));

    private final SeedProperties props;
    private final ChocolateTypeRepository types;
    private final FlavorRepository flavors;
    private final AllergenRepository allergens;
    private final BrandRepository brands;
    private final FoodPairingRepository pairings;
    private final ServingFormatRepository servings;
    private final GuideItemRepository guideItems;
    private final GuideSourceRepository guideSources;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.referenceData()) {
            return;
        }
        seedTypes();
        seedFlavors();
        seedAllergens();
        seedBrands();
        seedPairings();
        seedServingFormats();
        seedGuides();
    }

    private void seedTypes() {
        if (types.count() > 0) {
            return;
        }
        for (Map<String, String> r : SeedCsv.read("chocolate_types.csv")) {
            ChocolateType t = new ChocolateType();
            t.setCode(r.get("code"));
            t.setName(r.get("name"));
            t.setClassification(r.get("classification"));
            t.setDistinguishing(r.get("distinguishing"));
            t.setBestShopUse(r.get("best_shop_use"));
            t.setPriority(priority(r.get("priority")));
            types.save(t);
        }
        log.info("Seeded {} chocolate types", types.count());
    }

    private void seedFlavors() {
        if (flavors.count() > 0) {
            return;
        }
        for (Map<String, String> r : SeedCsv.read("flavors.csv")) {
            Flavor f = new Flavor();
            f.setCode(r.get("code"));
            f.setFlavorGroup(r.get("flavor_group"));
            f.setName(r.get("name"));
            f.setChocolateBase(r.get("chocolate_base"));
            f.setApplication(r.get("application"));
            f.setPriority(priority(r.get("priority")));
            flavors.save(f);
        }
        log.info("Seeded {} flavors", flavors.count());
    }

    private void seedAllergens() {
        if (allergens.count() > 0) {
            return;
        }
        ALLERGENS.forEach((code, name) -> {
            Allergen a = new Allergen();
            a.setCode(code);
            a.setName(name);
            allergens.save(a);
        });
    }

    private void seedBrands() {
        if (brands.count() > 0) {
            return;
        }
        int sort = 1000;
        List<Brand> batch = new ArrayList<>();
        for (Map<String, String> r : SeedCsv.read("brands.csv")) {
            Brand b = new Brand();
            b.setCode(r.get("code"));
            b.setName(r.get("name"));
            b.setRegion(r.get("region"));
            b.setAssociatedMarket(r.get("associated_market"));
            b.setSegment(r.get("segment"));
            b.setTypicalFormats(r.get("typical_formats"));
            b.setShopRole(r.get("shop_role"));
            b.setEvidenceStatus(r.get("evidence_status"));
            b.setReferenceUrl(SeedCsv.blankToNull(r.get("reference_url")));
            b.setStockingStatus(isShortlisted(b.getName()) ? BrandStockingStatus.SAMPLING : BrandStockingStatus.CANDIDATE);
            // Only brands backed by official or government evidence appear on the storefront brand shelf.
            String evidence = b.getEvidenceStatus() == null ? "" : b.getEvidenceStatus().toLowerCase(Locale.ROOT);
            b.setShowStatus(evidence.startsWith("official") || evidence.startsWith("government"));
            b.setSort(sort--);
            batch.add(b);
        }
        brands.saveAll(batch);
        log.info("Seeded {} brands ({} on the shortlist)", batch.size(),
                batch.stream().filter(b -> b.getStockingStatus() == BrandStockingStatus.SAMPLING).count());
    }

    private boolean isShortlisted(String name) {
        String n = name.toLowerCase(Locale.ROOT).replace('’', '\'');
        return SHORTLIST.stream().map(s -> s.toLowerCase(Locale.ROOT)).anyMatch(s ->
                n.equals(s) || n.equals(s + " chocolate") || n.startsWith(s + " ("));
    }

    private void seedPairings() {
        if (pairings.count() > 0) {
            return;
        }
        for (Map<String, String> r : SeedCsv.read("food_pairings.csv")) {
            FoodPairing p = new FoodPairing();
            p.setCode(r.get("code"));
            p.setFoodGroup(r.get("food_group"));
            p.setFood(r.get("food"));
            p.setChocolateCombination(r.get("chocolate_combination"));
            p.setShopFormat(r.get("shop_format"));
            p.setPriority(priority(r.get("priority")));
            pairings.save(p);
        }
        log.info("Seeded {} food pairings", pairings.count());
    }

    private void seedServingFormats() {
        if (servings.count() > 0) {
            return;
        }
        for (Map<String, String> r : SeedCsv.read("serving_formats.csv")) {
            ServingFormat s = new ServingFormat();
            s.setCode(r.get("code"));
            s.setEatingFormat(r.get("eating_format"));
            s.setHowToServe(r.get("how_to_serve"));
            s.setSuitableProduct(r.get("suitable_product"));
            s.setSalesApplication(r.get("sales_application"));
            s.setPriority(priority(r.get("priority")));
            servings.save(s);
        }
        log.info("Seeded {} serving formats", servings.count());
    }

    private void seedGuides() {
        if (guideItems.count() == 0) {
            Map<String, Map<String, String>> overlay = new java.util.HashMap<>();
            SeedCsv.read("guide_implementation.csv").forEach(r -> overlay.put(r.get("code"), r));
            int sort = 0;
            for (Map<String, String> r : SeedCsv.read("guide_items.csv")) {
                GuideItem g = new GuideItem();
                g.setGuideType(GuideType.valueOf(r.get("guide_type")));
                g.setCode(r.get("code"));
                g.setTitle(r.get("title"));
                g.setDetail(SeedCsv.blankToNull(r.get("detail")));
                g.setPurpose(SeedCsv.blankToNull(r.get("purpose")));
                g.setAppliesTo(SeedCsv.blankToNull(r.get("applies_to")));
                g.setControlNote(SeedCsv.blankToNull(r.get("control_note")));
                g.setPhase(SeedCsv.blankToNull(r.get("phase")));
                String pr = SeedCsv.blankToNull(r.get("priority"));
                g.setPriority(pr == null ? null : priority(pr));
                g.setReferenceUrl(SeedCsv.blankToNull(r.get("reference_url")));
                g.setSort(sort++);
                Map<String, String> impl = overlay.get(g.getCode());
                if (impl != null) {
                    g.setStatus(GuideStatus.valueOf(impl.get("status")));
                    g.setImplementedBy(impl.get("implemented_by"));
                }
                guideItems.save(g);
            }
            log.info("Seeded {} guide items", guideItems.count());
        }
        if (guideSources.count() == 0) {
            for (Map<String, String> r : SeedCsv.read("guide_sources.csv")) {
                GuideSource s = new GuideSource();
                s.setSource(r.get("source"));
                s.setScope(r.get("scope"));
                String date = SeedCsv.blankToNull(r.get("checked"));
                s.setCheckedOn(date == null ? null : LocalDate.parse(date));
                s.setReferenceUrl(SeedCsv.blankToNull(r.get("reference_url")));
                guideSources.save(s);
            }
        }
    }

    static Priority priority(String value) {
        return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
