package com.chocomall.model.seed;

import com.chocomall.model.entity.AdminUser;
import com.chocomall.model.entity.Allergen;
import com.chocomall.model.entity.Brand;
import com.chocomall.model.entity.Bundle;
import com.chocomall.model.entity.BundleItem;
import com.chocomall.model.entity.ChocolateType;
import com.chocomall.model.entity.Coupon;
import com.chocomall.model.entity.ExperienceSession;
import com.chocomall.model.entity.Flavor;
import com.chocomall.model.entity.HomeSlot;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.MemberAddress;
import com.chocomall.model.entity.MemberLevel;
import com.chocomall.model.entity.PreorderWindow;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductCategory;
import com.chocomall.model.entity.ProductSku;
import com.chocomall.model.entity.Role;
import com.chocomall.model.enums.BrandStockingStatus;
import com.chocomall.model.enums.CouponType;
import com.chocomall.model.enums.HomeSlotType;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.repository.AdminUserRepository;
import com.chocomall.model.repository.AllergenRepository;
import com.chocomall.model.repository.BrandRepository;
import com.chocomall.model.repository.BundleItemRepository;
import com.chocomall.model.repository.BundleRepository;
import com.chocomall.model.repository.ChocolateTypeRepository;
import com.chocomall.model.repository.CouponRepository;
import com.chocomall.model.repository.ExperienceSessionRepository;
import com.chocomall.model.repository.FlavorRepository;
import com.chocomall.model.repository.HomeSlotRepository;
import com.chocomall.model.repository.MemberAddressRepository;
import com.chocomall.model.repository.MemberLevelRepository;
import com.chocomall.model.repository.MemberRepository;
import com.chocomall.model.repository.PreorderWindowRepository;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ProductSkuRepository;
import com.chocomall.model.repository.RoleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds a browsable demo shop from the "Products to Sell" sheet of the guide.
 *
 * <p>What comes from the guide: product names, categories, selling formats, sales purposes and launch tiers
 * (Core / Expand / Test / Specialist). What this class adds so the shop can actually trade: demo prices and
 * costs, starting stock, SKU sizes, and keyword-based flavor / chocolate-type / allergen suggestions.
 * All prices, costs and stock levels are placeholders to be replaced with the owner's real figures, and the
 * allergen links must be verified against the real recipes before any real sale.
 *
 * <p>Launch discipline from the guide is reflected in the default visibility: Core and Expand items are on the
 * shelf, Test items are only live when they are sold by request (personalisation, experiences), and Specialist
 * items stay in draft until the shop has proven demand.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class DemoShopSeeder implements ApplicationRunner {

    /** guide category -> top-level storefront group */
    private static final Map<String, String> GROUP_OF = new LinkedHashMap<>();
    /** guide category -> display name of the storefront sub-category */
    private static final Map<String, String> DISPLAY = new LinkedHashMap<>();
    /** demo unit price per guide category */
    private static final Map<String, String> BASE_PRICE = new LinkedHashMap<>();

    static {
        String[][] rows = {
                // category, group, display name, demo unit price
                {"Bonbon", "Chocolates", "Bonbons", "2.20"}, {"Truffle", "Chocolates", "Truffles", "1.60"},
                {"Bar", "Chocolates", "Bars", "5.50"}, {"Filled bar", "Chocolates", "Filled bars", "6.50"},
                {"Bark", "Chocolates", "Barks", "7.50"}, {"Cup", "Chocolates", "Cups", "3.00"},
                {"Cluster", "Chocolates", "Clusters", "6.00"},
                {"Dipped snack", "Snacks", "Dipped snacks", "5.50"}, {"Fresh fruit", "Snacks", "Fresh fruit", "14.00"},
                {"Coated nuts", "Snacks", "Coated nuts", "6.50"}, {"Coated fruit", "Snacks", "Coated fruit", "5.50"},
                {"Drink", "Cafe", "Drinks", "4.50"}, {"Bakery", "Cafe", "Bakery", "3.50"},
                {"Dessert", "Cafe", "Desserts", "5.50"}, {"Frozen", "Cafe", "Frozen", "5.00"},
                {"Spread", "Pantry", "Spreads and sauces", "9.00"},
                {"Gift", "Gifts and seasonal", "Gifts", "28.00"}, {"Seasonal", "Gifts and seasonal", "Seasonal", "22.00"},
                {"Retail", "Gifts and seasonal", "Imported shelf", "6.90"},
                {"Personalized", "Personalised and experiences", "Personalised", "12.00"},
                {"Experience", "Personalised and experiences", "Experiences", "35.00"},
        };
        for (String[] r : rows) {
            GROUP_OF.put(r[0], r[1]);
            DISPLAY.put(r[0], r[2]);
            BASE_PRICE.put(r[0], r[3]);
        }
    }

    /** Specific demo prices that differ from the category default. */
    private static final Map<String, String> PRICE_OVERRIDE = Map.of(
            "P47", "2.20", "P48", "32.00", "P49", "48.00", "P50", "34.00",
            "P51", "18.00", "P52", "32.00", "P53", "45.00");

    private static final List<String> NUT_WORDS = List.of("almond", "hazelnut", "pistachio", "pecan", "walnut",
            "macadamia", "praline", "kataifi");
    private static final List<String> MILK_WORDS = List.of("milk", "white", "ganache", "caramel", "mousse", "gelato",
            "shake", "hot chocolate", "iced chocolate", "mocha", "sundae", "croissant", "cookie", "tart", "cups");
    private static final List<String> WHEAT_WORDS = List.of("pretzel", "brownie", "cookie", "croissant", "shortbread",
            "tart", "sundae");
    private static final List<String> EGG_WORDS = List.of("brownie", "cookie", "mousse", "tart", "croissant", "sundae");
    private static final List<String> SESAME_WORDS = List.of("sesame", "tahini", "halva");

    private final SeedProperties props;
    private final ProductCategoryRepository categories;
    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final FlavorRepository flavors;
    private final ChocolateTypeRepository types;
    private final AllergenRepository allergens;
    private final PreorderWindowRepository windows;
    private final ExperienceSessionRepository sessions;
    private final CouponRepository coupons;
    private final BundleRepository bundles;
    private final BundleItemRepository bundleItems;
    private final HomeSlotRepository slots;
    private final BrandRepository brands;
    private final MemberLevelRepository levels;
    private final MemberRepository members;
    private final MemberAddressRepository addresses;
    private final AdminUserRepository admins;
    private final RoleRepository roles;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.demoShop() || products.count() > 0) {
            return;
        }
        Map<String, Long> categoryIds = seedCategories();
        List<Product> saved = seedProducts(categoryIds);
        seedPreorderAndExperiences(saved);
        seedPromotions(saved, categoryIds);
        seedAccounts();
        log.info("Demo shop ready: {} products, {} SKUs", saved.size(), skus.count());
    }

    // ------------------------------------------------------------------ categories

    private Map<String, Long> seedCategories() {
        Map<String, Long> groupIds = new LinkedHashMap<>();
        int sort = 100;
        for (String group : new LinkedHashSet<>(GROUP_OF.values())) {
            ProductCategory c = category(group, 0L, 0, sort--);
            groupIds.put(group, categories.save(c).getId());
        }
        Map<String, Long> ids = new LinkedHashMap<>();
        sort = 100;
        for (Map.Entry<String, String> e : DISPLAY.entrySet()) {
            ProductCategory c = category(e.getValue(), groupIds.get(GROUP_OF.get(e.getKey())), 1, sort--);
            ids.put(e.getKey(), categories.save(c).getId());
        }
        return ids;
    }

    private ProductCategory category(String name, Long parent, int level, int sort) {
        ProductCategory c = new ProductCategory();
        c.setName(name);
        c.setParentId(parent);
        c.setLevel(level);
        c.setSort(sort);
        c.setKeywords(name.toLowerCase(Locale.ROOT));
        return c;
    }

    // ------------------------------------------------------------------ products

    private List<Product> seedProducts(Map<String, Long> categoryIds) {
        List<Flavor> allFlavors = flavors.findAll();
        Map<String, ChocolateType> typeByCode = new LinkedHashMap<>();
        types.findAll().forEach(t -> typeByCode.put(t.getCode(), t));
        Map<String, Allergen> allergenByCode = new LinkedHashMap<>();
        allergens.findAll().forEach(a -> allergenByCode.put(a.getCode(), a));

        List<Product> result = new ArrayList<>();
        int sort = 1000;
        for (Map<String, String> row : SeedCsv.read("products.csv")) {
            String code = row.get("code");
            String category = row.get("category");
            String name = row.get("name");
            String lower = name.toLowerCase(Locale.ROOT);
            String format = row.get("selling_format");
            String purpose = row.get("sales_purpose");
            Priority tier = ReferenceDataSeeder.priority(row.get("priority"));

            ProductKind kind = kindOf(category, code);
            Product p = new Product();
            p.setProductSn(code);
            p.setName(name);
            p.setCategoryId(categoryIds.get(category));
            p.setKind(kind);
            p.setLaunchTier(tier);
            p.setSellingFormat(format);
            p.setSalesPurpose(purpose);
            p.setSubTitle(purpose);
            p.setDescription(format + ". " + purpose + ".");
            p.setKeywords((name + " " + DISPLAY.get(category)).toLowerCase(Locale.ROOT));
            p.setUnit(unitOf(category));
            p.setSort(sort--);
            p.setBuildABox(lower.startsWith("build-a-box"));
            p.setPreorderOnly(kind != ProductKind.PERSONALIZED && kind != ProductKind.EXPERIENCE
                    && (format.toLowerCase(Locale.ROOT).contains("preorder") || purpose.toLowerCase(Locale.ROOT).contains("preorder")));
            p.setRequiresColdChain("Frozen".equals(category) || "Fresh fruit".equals(category)
                    || lower.contains("mousse") || lower.contains("tart") || lower.contains("sundae"));

            BigDecimal price = new BigDecimal(PRICE_OVERRIDE.getOrDefault(code, BASE_PRICE.get(category)));
            p.setPrice(price);
            applyCosts(p, price, kind);

            boolean tracked = tracksStock(p);
            p.setStock(tracked ? initialStock(category, tier) : 0);
            p.setLowStock(tracked ? 20 : 0);

            boolean pilotSoldByRequest = tier == Priority.TEST
                    && (kind == ProductKind.PERSONALIZED || kind == ProductKind.EXPERIENCE);
            boolean live = tier == Priority.CORE || tier == Priority.EXPAND || pilotSoldByRequest;
            p.setPublished(live);
            p.setRecommendStatus(tier == Priority.CORE);
            p.setNewStatus(tier == Priority.EXPAND && live);
            p.setAllergensReviewed(live);

            chocolateType(lower, typeByCode).ifPresent(t -> p.setChocolateTypeId(t.getId()));
            matchFlavors(lower, allFlavors).forEach(f -> p.getFlavorIds().add(f.getId()));
            matchAllergens(lower, category, allergenByCode).forEach(a -> p.getAllergenIds().add(a.getId()));

            Product saved = products.save(p);
            seedSkus(saved, category);
            result.add(saved);
        }
        // keep category product counts in sync
        categoryIds.values().forEach(id -> {
            ProductCategory c = categories.findById(id).orElseThrow();
            c.setProductCount((int) products.countByCategoryId(id));
        });
        return result;
    }

    static ProductKind kindOf(String category, String code) {
        return switch (category) {
            case "Drink" -> ProductKind.MADE_TO_ORDER;
            case "Bakery", "Dessert", "Frozen", "Fresh fruit" -> ProductKind.MADE_FRESH;
            case "Experience" -> ProductKind.EXPERIENCE;
            case "Personalized" -> ProductKind.PERSONALIZED;
            case "Retail" -> ProductKind.SEALED_RETAIL;
            default -> "P48".equals(code) ? ProductKind.SEALED_RETAIL : ProductKind.MADE_IN_SHOP;
        };
    }

    private static String unitOf(String category) {
        return switch (category) {
            case "Bonbon", "Truffle" -> "piece";
            case "Drink" -> "cup";
            default -> "item";
        };
    }

    /** Made-to-order drinks, experiences, personalised items and preorder lines do not draw down shelf stock. */
    private static boolean tracksStock(Product p) {
        return !p.isPreorderOnly() && (p.getKind() == ProductKind.MADE_IN_SHOP
                || p.getKind() == ProductKind.MADE_FRESH || p.getKind() == ProductKind.SEALED_RETAIL)
                && !p.isBuildABox();
    }

    private static int initialStock(String category, Priority tier) {
        if ("Bonbon".equals(category) || "Truffle".equals(category)) {
            return 300;
        }
        return tier == Priority.CORE ? 150 : 60;
    }

    /** Placeholder cost model following the guide's "contribution pricing" idea (ingredients, pack, labour, fees). */
    private static void applyCosts(Product p, BigDecimal price, ProductKind kind) {
        if (kind == ProductKind.EXPERIENCE || kind == ProductKind.PERSONALIZED) {
            return;
        }
        boolean resale = kind == ProductKind.SEALED_RETAIL;
        p.setIngredientCost(price.multiply(new BigDecimal(resale ? "0.62" : "0.24")).setScale(2, RoundingMode.HALF_UP));
        p.setPackagingCost(price.multiply(new BigDecimal(resale ? "0.00" : "0.06")).setScale(2, RoundingMode.HALF_UP));
        p.setLaborCost(price.multiply(new BigDecimal(resale ? "0.02" : "0.20")).setScale(2, RoundingMode.HALF_UP));
        p.setSellingFeeRate(new BigDecimal("0.0150"));
    }

    private void seedSkus(Product p, String category) {
        String sn = p.getProductSn();
        if ("Bonbon".equals(category) && !p.isBuildABox()) {
            sku(p, sn + "-1", "Single piece", 1, "1.00", 0);
            sku(p, sn + "-6", "Box of 6", 6, "0.92", 1);
            sku(p, sn + "-12", "Box of 12", 12, "0.88", 2);
        } else if ("Truffle".equals(category)) {
            sku(p, sn + "-6", "Bag of 6", 6, "0.95", 0);
            sku(p, sn + "-12", "Box of 12", 12, "0.90", 1);
        } else if (p.isBuildABox()) {
            sku(p, sn + "-6", "Box of 6", 6, "0.92", 0);
            sku(p, sn + "-12", "Box of 12", 12, "0.88", 1);
            sku(p, sn + "-24", "Box of 24", 24, "0.85", 2);
        } else {
            sku(p, sn + "-STD", "Standard", 1, "1.00", 0);
        }
    }

    private void sku(Product p, String code, String label, int units, String factor, int sort) {
        ProductSku s = new ProductSku();
        s.setProductId(p.getId());
        s.setSkuCode(code);
        s.setLabel(label);
        s.setUnits(units);
        s.setPrice(p.getPrice().multiply(BigDecimal.valueOf(units)).multiply(new BigDecimal(factor))
                .setScale(2, RoundingMode.HALF_UP));
        s.setSort(sort);
        skus.save(s);
    }

    // ------------------------------------------------------------------ keyword heuristics

    private static Optional<ChocolateType> chocolateType(String name, Map<String, ChocolateType> byCode) {
        String code = null;
        if (name.contains("single-origin")) {
            code = "T14";
        } else if (name.contains("ruby")) {
            code = "T06";
        } else if (name.contains("blond")) {
            code = "T07";
        } else if (name.contains("white")) {
            code = "T05";
        } else if (name.contains("milk")) {
            code = "T01";
        } else if (name.contains("dark") || name.contains("espresso")) {
            code = "T02";
        }
        return Optional.ofNullable(code == null ? null : byCode.get(code));
    }

    /** A flavor matches when its full name, or its first significant word, appears in the product name. */
    static List<Flavor> matchFlavors(String productName, List<Flavor> all) {
        String stemmedProduct = stem(productName);
        List<Flavor> hits = new ArrayList<>();
        for (Flavor f : all) {
            String flavor = stem(f.getName().toLowerCase(Locale.ROOT));
            String first = flavor.split(" ")[0];
            boolean full = stemmedProduct.contains(flavor);
            boolean firstWord = first.length() >= 4 && wordIn(first, stemmedProduct)
                    && !List.of("sweet", "salted").contains(first);
            if (full || firstWord) {
                hits.add(f);
            }
        }
        return hits;
    }

    private static boolean wordIn(String word, String text) {
        for (String w : text.split("[^a-z]+")) {
            if (w.equals(word)) {
                return true;
            }
        }
        return false;
    }

    private static String stem(String text) {
        StringBuilder sb = new StringBuilder();
        for (String w : text.split(" ")) {
            if (w.endsWith("ies") && w.length() > 4) {
                w = w.substring(0, w.length() - 3) + "y";
            } else if (w.endsWith("s") && w.length() > 4 && !w.endsWith("ss")) {
                w = w.substring(0, w.length() - 1);
            }
            sb.append(w).append(' ');
        }
        return sb.toString().trim();
    }

    private static List<Allergen> matchAllergens(String name, String category, Map<String, Allergen> byCode) {
        Set<String> codes = new LinkedHashSet<>();
        boolean chocolateProduct = !List.of("Retail", "Personalized", "Experience", "Drink").contains(category) || name.contains("chocolate");
        if (containsAny(name, MILK_WORDS) || ("Bonbon".equals(category) && chocolateProduct && !name.contains("dark"))) {
            codes.add("MILK");
        }
        if (containsAny(name, NUT_WORDS)) {
            codes.add("TREE_NUTS");
        }
        if (name.contains("peanut")) {
            codes.add("PEANUT");
        }
        if (containsAny(name, WHEAT_WORDS)) {
            codes.add("WHEAT");
        }
        if (containsAny(name, EGG_WORDS)) {
            codes.add("EGG");
        }
        if (containsAny(name, SESAME_WORDS)) {
            codes.add("SESAME");
        }
        List<Allergen> out = new ArrayList<>();
        codes.forEach(c -> out.add(byCode.get(c)));
        return out;
    }

    private static boolean containsAny(String text, List<String> words) {
        return words.stream().anyMatch(text::contains);
    }

    // ------------------------------------------------------------------ preorders, sessions, promotions

    private void seedPreorderAndExperiences(List<Product> saved) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        windows.save(window("Holiday gifts and Advent preorders", "Advent", now.plusDays(30), now.plusDays(35).toLocalDate(),
                now.plusDays(60).toLocalDate(), 400));
        windows.save(window("Valentine gift boxes", "Valentine's Day", now.plusDays(120), now.plusDays(125).toLocalDate(),
                now.plusDays(135).toLocalDate(), 250));

        Map<String, Integer> capacity = Map.of("P51", 8, "P52", 10, "P53", 12);
        Map<String, String> price = Map.of("P51", "18.00", "P52", "32.00", "P53", "45.00");
        for (Product p : saved) {
            if (!capacity.containsKey(p.getProductSn())) {
                continue;
            }
            for (int week : new int[] {1, 2}) {
                ExperienceSession s = new ExperienceSession();
                s.setProductId(p.getId());
                s.setStartsAt(now.plusDays(7L * week + 3).withHour(18).withMinute(0).withSecond(0));
                s.setDurationMinutes(90);
                s.setCapacity(capacity.get(p.getProductSn()));
                s.setPrice(new BigDecimal(price.get(p.getProductSn())));
                s.setLocation("Main boutique");
                sessions.save(s);
            }
        }
    }

    private PreorderWindow window(String name, String occasion, LocalDateTime cutoff, LocalDate from, LocalDate to, int capacity) {
        PreorderWindow w = new PreorderWindow();
        w.setName(name);
        w.setOccasion(occasion);
        w.setOrderCutoff(cutoff);
        w.setFulfilFrom(from);
        w.setFulfilTo(to);
        w.setCapacityUnits(capacity);
        w.setNote("Capacity is capped to what the kitchen can produce; adjust it after a capacity test.");
        return w;
    }

    private void seedPromotions(List<Product> saved, Map<String, Long> categoryIds) {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Coupon welcome = coupon("WELCOME5", "Welcome: 5 off your first order over 25", CouponType.ALL, "5.00", "25.00", now);
        coupons.save(welcome);
        Coupon bonbons = coupon("BONBON3", "3 off bonbon boxes (orders over 20)", CouponType.CATEGORY, "3.00", "20.00", now);
        bonbons.getCategoryIds().add(categoryIds.get("Bonbon"));
        coupons.save(bonbons);

        Optional<Product> drink = saved.stream().filter(p -> "P33".equals(p.getProductSn())).findFirst();
        Optional<Product> treat = saved.stream().filter(p -> "P37".equals(p.getProductSn())).findFirst();
        if (drink.isPresent() && treat.isPresent()) {
            Bundle b = new Bundle();
            b.setName("Drink + treat");
            b.setDescription("House hot chocolate with a house brownie");
            BigDecimal original = drink.get().getPrice().add(treat.get().getPrice());
            b.setOriginalPrice(original);
            b.setPrice(original.subtract(BigDecimal.ONE));
            bundles.save(b);
            for (Product p : List.of(drink.get(), treat.get())) {
                BundleItem i = new BundleItem();
                i.setBundleId(b.getId());
                i.setProductId(p.getId());
                skus.findByProductIdOrderBySortAscIdAsc(p.getId()).stream().findFirst().ifPresent(s -> i.setSkuId(s.getId()));
                bundleItems.save(i);
            }
        }

        int sort = 100;
        for (String title : List.of("Gifts made to order", "Taste the three bases")) {
            HomeSlot banner = new HomeSlot();
            banner.setSlotType(HomeSlotType.BANNER);
            banner.setName(title);
            banner.setUrl("/subject");
            banner.setSort(sort--);
            slots.save(banner);
        }
        saved.stream().filter(p -> p.isPublished() && p.isRecommendStatus()).limit(8).forEach(p -> slots(HomeSlotType.RECOMMEND_PRODUCT, p));
        saved.stream().filter(p -> p.isPublished() && p.isNewStatus()).limit(6).forEach(p -> slots(HomeSlotType.NEW_PRODUCT, p));
        for (Brand b : brands.findByStockingStatusAndShowStatusTrue(BrandStockingStatus.SAMPLING)) {
            HomeSlot s = new HomeSlot();
            s.setSlotType(HomeSlotType.BRAND);
            s.setRefId(b.getId());
            s.setName(b.getName());
            s.setSort(sort--);
            slots.save(s);
        }
    }

    private void slots(HomeSlotType type, Product p) {
        HomeSlot s = new HomeSlot();
        s.setSlotType(type);
        s.setRefId(p.getId());
        s.setName(p.getName());
        s.setSort(p.getSort());
        slots.save(s);
    }

    private Coupon coupon(String code, String name, CouponType type, String amount, String minSpend, LocalDateTime now) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setName(name);
        c.setCouponType(type);
        c.setAmount(new BigDecimal(amount));
        c.setMinSpend(new BigDecimal(minSpend));
        c.setTotalCount(500);
        c.setPerLimit(1);
        c.setStartTime(now.minusDays(1));
        c.setEndTime(now.plusYears(1));
        return c;
    }

    // ------------------------------------------------------------------ demo accounts

    private void seedAccounts() {
        String password = props.demoPassword();
        if (password == null || password.isBlank()) {
            return;
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        demoAdmin("catalog", "Catalog manager", "CATALOG_MANAGER", encoder.encode(password));
        demoAdmin("orders", "Order manager", "ORDER_MANAGER", encoder.encode(password));

        MemberLevel level = levels.findFirstByDefaultStatusTrue().orElse(null);
        Member m = new Member();
        m.setUsername("demo");
        m.setPassword(encoder.encode(password));
        m.setNickname("Demo customer");
        m.setPhone("+10000000001");
        m.setEmail("demo@chocomall.local");
        m.setLevelId(level == null ? null : level.getId());
        m.setIntegration(500);
        m.setHistoryIntegration(500);
        m = members.save(m);

        MemberAddress a = new MemberAddress();
        a.setMemberId(m.getId());
        a.setName("Demo customer");
        a.setPhone("+10000000001");
        a.setProvince("Demo state");
        a.setCity("Demo city");
        a.setRegion("Central");
        a.setPostCode("00000");
        a.setDetailAddress("1 Cocoa Street");
        a.setDefaultStatus(true);
        addresses.save(a);
    }

    private void demoAdmin(String username, String nick, String roleName, String hash) {
        if (admins.existsByUsername(username)) {
            return;
        }
        Role role = roles.findByName(roleName).orElseThrow();
        AdminUser u = new AdminUser();
        u.setUsername(username);
        u.setNickName(nick);
        u.setPassword(hash);
        u.getRoleIds().add(role.getId());
        admins.save(u);
        role.setAdminCount(role.getAdminCount() + 1);
    }
}
