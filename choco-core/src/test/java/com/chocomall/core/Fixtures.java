package com.chocomall.core;

import com.chocomall.core.stock.StockService;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductCategory;
import com.chocomall.model.entity.ProductSku;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.FulfillmentType;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.repository.MemberLevelRepository;
import com.chocomall.model.repository.MemberRepository;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ProductSkuRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Test data builders that bypass the web layer. Every call creates uniquely named rows. */
@Component
@RequiredArgsConstructor
public class Fixtures {

    private final MemberRepository members;
    private final MemberLevelRepository levels;
    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final ProductCategoryRepository categories;
    private final ShopOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final StockService stock;
    private final TransactionTemplate tx;

    public Member member() {
        Member m = new Member();
        m.setUsername("u" + UUID.randomUUID().toString().substring(0, 12));
        m.setPassword("{noop}unused");
        levels.findFirstByDefaultStatusTrue().ifPresent(l -> m.setLevelId(l.getId()));
        return members.save(m);
    }

    public Product product(int stockUnits, String price) {
        return product(stockUnits, price, ProductKind.MADE_IN_SHOP);
    }

    public Product product(int stockUnits, String price, ProductKind kind) {
        ProductCategory c = categories.findByNameAndParentId("Test category", 0L).orElseGet(() -> {
            ProductCategory fresh = new ProductCategory();
            fresh.setName("Test category");
            fresh.setParentId(0L);
            return categories.save(fresh);
        });
        Product p = new Product();
        p.setCategoryId(c.getId());
        p.setProductSn("T" + UUID.randomUUID().toString().substring(0, 12));
        p.setName("Test product " + p.getProductSn());
        p.setPrice(new BigDecimal(price));
        p.setStock(stockUnits);
        p.setLowStock(2);
        p.setKind(kind);
        p.setLaunchTier(Priority.CORE);
        p.setPublished(true);
        p.setAllergensReviewed(true);
        Product saved = products.save(p);
        ProductSku sku = new ProductSku();
        sku.setProductId(saved.getId());
        sku.setSkuCode(saved.getProductSn() + "-1");
        sku.setLabel("Single");
        sku.setUnits(1);
        sku.setPrice(saved.getPrice());
        skus.save(sku);
        return saved;
    }

    public ProductSku skuOf(Product p) {
        return skus.findByProductIdOrderBySortAscIdAsc(p.getId()).get(0);
    }

    /** An unpaid pickup order for {@code quantity} units, with stock locked exactly as checkout would do. */
    public ShopOrder placeOrder(Member member, Product product, int quantity, String payAmount) {
        return tx.execute(status -> {
            ShopOrder o = new ShopOrder();
            o.setMemberId(member.getId());
            o.setMemberUsername(member.getUsername());
            o.setOrderSn("ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
            BigDecimal pay = new BigDecimal(payAmount);
            o.setTotalAmount(pay);
            o.setPayAmount(pay);
            o.setFulfillmentType(FulfillmentType.PICKUP);
            ShopOrder saved = orders.saveAndFlush(o);
            OrderItem item = new OrderItem();
            item.setOrderId(saved.getId());
            item.setOrderSn(saved.getOrderSn());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductSn(product.getProductSn());
            item.setUnitsPerSku(1);
            item.setProductPrice(product.getPrice());
            item.setProductQuantity(quantity);
            item.setRealAmount(pay);
            orderItems.save(item);
            stock.lock(product, quantity, saved.getOrderSn());
            return saved;
        });
    }

    public Product fresh(Product p) {
        return products.findById(p.getId()).orElseThrow();
    }
}
