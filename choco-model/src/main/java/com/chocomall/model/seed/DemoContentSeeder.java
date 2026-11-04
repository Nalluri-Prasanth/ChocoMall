package com.chocomall.model.seed;

import com.chocomall.model.entity.ChocolateType;
import com.chocomall.model.entity.HelpArticle;
import com.chocomall.model.entity.HelpCategory;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ServingFormat;
import com.chocomall.model.entity.Subject;
import com.chocomall.model.repository.ChocolateTypeRepository;
import com.chocomall.model.repository.HelpArticleRepository;
import com.chocomall.model.repository.HelpCategoryRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ServingFormatRepository;
import com.chocomall.model.repository.SubjectRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Editorial subjects (assembled from the guide's catalogs, so every statement traces back to the workbook)
 * and the help centre for the demo shop.
 */
@Component
@Order(30)
@RequiredArgsConstructor
public class DemoContentSeeder implements ApplicationRunner {

    private final SeedProperties props;
    private final SubjectRepository subjects;
    private final HelpCategoryRepository helpCategories;
    private final HelpArticleRepository helpArticles;
    private final ChocolateTypeRepository types;
    private final ServingFormatRepository servings;
    private final ProductRepository products;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.demoShop()) {
            return;
        }
        if (subjects.count() == 0) {
            seedSubjects();
        }
        if (helpCategories.count() == 0) {
            seedHelp();
        }
    }

    private void seedSubjects() {
        String bases = types.findAll().stream()
                .filter(t -> List.of("T01", "T02", "T05").contains(t.getCode()))
                .map((ChocolateType t) -> t.getName() + ": " + t.getDistinguishing() + ". In the shop: " + t.getBestShopUse() + ".")
                .collect(Collectors.joining("\n"));
        subject("Three chocolates, three personalities", "Basics", bases, "Start with the three bases we use across the menu.", true, 100, List.of());

        subject("How to taste chocolate", "Tasting", servingLines(List.of("E01", "E02", "E03", "E04")),
                "Four ways to taste that turn a snack into a discovery.", true, 90, List.of());

        subject("Coffee, tea and cheese pairings", "Pairings", servingLines(List.of("E19", "E20", "E21")),
                "Small guided pairings for coffee, tea and cheese lovers.", true, 80, List.of());

        List<Product> gifts = products.findAll().stream()
                .filter(p -> List.of("P47", "P48", "P49", "P50").contains(p.getProductSn())).toList();
        String giftText = gifts.stream()
                .map(p -> p.getName() + " (" + p.getSellingFormat() + "): " + p.getSalesPurpose() + ".")
                .collect(Collectors.joining("\n"));
        subject("Gifting guide", "Gifting", giftText, "Build-a-box, tasting sets, corporate assortments and seasonal collections.",
                true, 70, gifts.stream().map(Product::getId).toList());
    }

    private String servingLines(List<String> codes) {
        return servings.findAll().stream()
                .filter(s -> codes.contains(s.getCode()))
                .map((ServingFormat s) -> s.getEatingFormat() + ": " + s.getHowToServe() + " (" + s.getSuitableProduct() + ").")
                .collect(Collectors.joining("\n"));
    }

    private void subject(String title, String category, String content, String description, boolean recommend, int sort,
                         List<Long> productIds) {
        Subject s = new Subject();
        s.setTitle(title);
        s.setCategory(category);
        s.setContent(content);
        s.setDescription(description);
        s.setRecommendStatus(recommend);
        s.setSort(sort);
        s.getProductIds().addAll(productIds);
        subjects.save(s);
    }

    private void seedHelp() {
        HelpCategory ordering = helpCategory("Ordering and payment", 100);
        HelpCategory fulfilment = helpCategory("Pickup and delivery", 90);
        HelpCategory allergens = helpCategory("Allergens and dietary information", 80);
        HelpCategory gifts = helpCategory("Gifts, preorders and personalisation", 70);
        HelpCategory returns = helpCategory("Returns and refunds", 60);

        article(ordering, "How do I place an order?",
                "Add items to your basket, choose pickup or delivery, apply a coupon or loyalty points if you have them, and pay. "
                        + "Unpaid orders are cancelled automatically after the payment window (30 minutes by default) and the items return to stock.");
        article(ordering, "How do loyalty points work?",
                "You earn points on every paid order (1 point per currency unit by default, multiplied by your level). "
                        + "Points can be redeemed on later orders (100 points are worth 1 currency unit by default, up to 20% of the order). "
                        + "Every earning and redemption is listed in your points history.");
        article(fulfilment, "When can I collect my order?",
                "Choose a collection time at checkout. Ready-made items need a short preparation time (30 minutes by default); "
                        + "preorders are collected inside the fulfilment dates of the preorder window you selected.");
        article(fulfilment, "Do you deliver chilled products?",
                "Items that need refrigeration, such as fresh dipped fruit, mousse and frozen desserts, are available for pickup only. "
                        + "Everything else can be delivered; delivery is free above the free-delivery threshold shown at checkout.");
        article(allergens, "How are allergens shown?",
                "Each product lists the allergens identified from its recipe. Always read the product page and the label, and ask in the shop if you are unsure. "
                        + "Shared equipment means cross-contact is possible; dietary claims such as vegan, gluten-free or halal are only displayed when the shop has verified evidence for them.");
        article(gifts, "How do preorders and cut-off dates work?",
                "Seasonal and gift preorders belong to a preorder window with an order cut-off date and a fixed capacity. "
                        + "When the capacity is used up or the cut-off passes, the window closes so every accepted order can be produced on time.");
        article(gifts, "How do I order a name plaque or monogram?",
                "Submit a personalisation request with your text and date. We reply with a quote and lead time, then send an artwork proof for your approval. "
                        + "Production starts after you approve the proof and pay the deposit.");
        article(returns, "What if my order arrives damaged or melted?",
                "Open a return request from your order page and choose a reason. Include photos where possible. "
                        + "Our team reviews each request and refunds or replaces eligible items.");
    }

    private HelpCategory helpCategory(String name, int sort) {
        HelpCategory c = new HelpCategory();
        c.setName(name);
        c.setSort(sort);
        return helpCategories.save(c);
    }

    private void article(HelpCategory category, String title, String content) {
        HelpArticle a = new HelpArticle();
        a.setCategoryId(category.getId());
        a.setTitle(title);
        a.setContent(content);
        helpArticles.save(a);
    }
}
