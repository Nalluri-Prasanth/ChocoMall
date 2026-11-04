package com.chocomall.portal.web;

import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.CommonResult;
import com.chocomall.core.payment.PaymentService;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.ReturnApply;
import com.chocomall.model.entity.ReturnReason;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.portal.dto.PortalDtos.CancelParam;
import com.chocomall.portal.dto.PortalDtos.CartAddParam;
import com.chocomall.portal.dto.PortalDtos.CartView;
import com.chocomall.portal.dto.PortalDtos.CheckoutParam;
import com.chocomall.portal.dto.PortalDtos.CheckoutPreview;
import com.chocomall.portal.dto.PortalDtos.OrderView;
import com.chocomall.portal.dto.PortalDtos.PayParam;
import com.chocomall.portal.dto.PortalDtos.ReturnParam;
import com.chocomall.portal.security.CurrentMember;
import com.chocomall.portal.service.CartService;
import com.chocomall.portal.service.CheckoutService;
import com.chocomall.portal.service.PortalOrderService;
import com.chocomall.portal.service.PortalOrderService.OrderDetailView;
import com.chocomall.portal.service.ReturnPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Basket, checkout, payment, order history and returns. */
@Tag(name = "Basket and orders")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final CartService cart;
    private final CheckoutService checkout;
    private final PortalOrderService orders;
    private final PaymentService payments;
    private final ReturnPortalService returns;

    // ---- basket
    @Operation(summary = "Add to the basket. A build-a-box line needs `box`: the pieces that fill the chosen box size.")
    @PostMapping("/cart/add")
    public CommonResult<CartView> add(@Valid @RequestBody CartAddParam p) {
        return CommonResult.success(cart.add(CurrentMember.id(), p));
    }

    @GetMapping("/cart/list")
    public CommonResult<CartView> list() {
        return CommonResult.success(cart.view(CurrentMember.id()));
    }

    @PostMapping("/cart/update/quantity")
    public CommonResult<CartView> quantity(@RequestParam Long id, @RequestParam Integer quantity) {
        return CommonResult.success(cart.updateQuantity(CurrentMember.id(), id, quantity));
    }

    @PostMapping("/cart/delete")
    public CommonResult<CartView> delete(@RequestParam Set<Long> ids) {
        return CommonResult.success(cart.remove(CurrentMember.id(), ids));
    }

    @PostMapping("/cart/clear")
    public CommonResult<Void> clear() {
        cart.clear(CurrentMember.id());
        return CommonResult.success();
    }

    // ---- checkout
    @Operation(summary = "Price the basket without ordering: bundle, coupon, points, delivery and gift fees, notices")
    @PostMapping("/order/confirm")
    public CommonResult<CheckoutPreview> confirm(@Valid @RequestBody CheckoutParam p) {
        return CommonResult.success(checkout.preview(CurrentMember.id(), p));
    }

    @Operation(summary = "Place the order. Stock is reserved and the order must be paid within the payment window.")
    @PostMapping("/order/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult<OrderView> generate(@Valid @RequestBody CheckoutParam p) {
        return CommonResult.success(checkout.generate(CurrentMember.id(), p));
    }

    @Operation(summary = "Pay for an order. Card payments are charged immediately; use cardToken tok_decline to test a failure.")
    @PostMapping("/payment/pay")
    public CommonResult<Payment> pay(@Valid @RequestBody PayParam p) {
        return CommonResult.success(payments.payOrder(p.orderId(), CurrentMember.id(), p.method(), p.cardToken()));
    }

    // ---- orders
    @GetMapping("/order/list")
    public CommonResult<CommonPage<OrderView>> orders(@RequestParam(required = false) OrderStatus status,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "5") Integer pageSize) {
        return CommonResult.success(orders.list(CurrentMember.id(), status, pageNum, pageSize));
    }

    @GetMapping("/order/detail/{id}")
    public CommonResult<OrderDetailView> detail(@PathVariable Long id) {
        return CommonResult.success(orders.detail(CurrentMember.id(), id));
    }

    @PostMapping("/order/cancel/{id}")
    public CommonResult<ShopOrder> cancel(@PathVariable Long id, @RequestBody(required = false) CancelParam p) {
        return CommonResult.success(orders.cancel(CurrentMember.id(), id, p == null ? null : p.reason()));
    }

    @Operation(summary = "Confirm that you collected or received the order")
    @PostMapping("/order/confirmReceipt/{id}")
    public CommonResult<ShopOrder> confirmReceipt(@PathVariable Long id) {
        return CommonResult.success(orders.confirmReceipt(CurrentMember.id(), id));
    }

    @PostMapping("/order/delete/{id}")
    public CommonResult<Void> deleteOrder(@PathVariable Long id) {
        orders.delete(CurrentMember.id(), id);
        return CommonResult.success();
    }

    // ---- returns
    @GetMapping("/returnApply/reasons")
    public CommonResult<List<ReturnReason>> reasons() {
        return CommonResult.success(returns.reasons());
    }

    @PostMapping("/returnApply/create")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult<ReturnApply> createReturn(@Valid @RequestBody ReturnParam p) {
        return CommonResult.success(returns.create(CurrentMember.id(), p));
    }

    @GetMapping("/returnApply/list")
    public CommonResult<List<ReturnApply>> myReturns() {
        return CommonResult.success(returns.mine(CurrentMember.id()));
    }
}
