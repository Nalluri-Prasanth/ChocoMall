package com.chocomall.admin.web;

import com.chocomall.admin.dto.SalesDtos.IdsReason;
import com.chocomall.admin.dto.SalesDtos.LocationParam;
import com.chocomall.admin.dto.SalesDtos.ManualPaymentParam;
import com.chocomall.admin.dto.SalesDtos.NoteParam;
import com.chocomall.admin.dto.SalesDtos.OrderDetail;
import com.chocomall.admin.dto.SalesDtos.OrderQuery;
import com.chocomall.admin.dto.SalesDtos.ProofParam;
import com.chocomall.admin.dto.SalesDtos.QuoteParam;
import com.chocomall.admin.dto.SalesDtos.ReasonParam;
import com.chocomall.admin.dto.SalesDtos.ReceiverParam;
import com.chocomall.admin.dto.SalesDtos.ReturnHandleParam;
import com.chocomall.admin.dto.SalesDtos.ReturnQuery;
import com.chocomall.admin.dto.SalesDtos.SettingParam;
import com.chocomall.admin.dto.SalesDtos.ShipParam;
import com.chocomall.admin.security.CurrentAdmin;
import com.chocomall.admin.service.OrderAdminService;
import com.chocomall.admin.service.ReturnAdminService;
import com.chocomall.admin.service.SettingsAdminService;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.CommonResult;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.personalization.PersonalizationService;
import com.chocomall.model.entity.OrderSetting;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.PersonalizationProof;
import com.chocomall.model.entity.PersonalizationRequest;
import com.chocomall.model.entity.ReturnApply;
import com.chocomall.model.entity.ReturnReason;
import com.chocomall.model.entity.ShopLocation;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.FulfillmentType;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.PaymentStatus;
import com.chocomall.model.enums.PersonalizationStatus;
import com.chocomall.model.enums.ReturnStatus;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.PersonalizationRequestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Orders, returns, order settings, payments and personalisation requests. */
@Tag(name = "Orders")
@RestController
@RequiredArgsConstructor
public class SalesController {

    private final OrderAdminService orders;
    private final ReturnAdminService returns;
    private final SettingsAdminService settings;
    private final PersonalizationService personalization;
    private final PersonalizationRequestRepository personalizationRequests;
    private final PaymentRepository payments;

    // ---- orders
    @Operation(summary = "Search orders by number, customer, status, fulfilment type, preorder flag and date range")
    @GetMapping("/order/list")
    public CommonResult<CommonPage<ShopOrder>> list(@RequestParam(required = false) String orderSn,
                                                    @RequestParam(required = false) String receiver,
                                                    @RequestParam(required = false) OrderStatus status,
                                                    @RequestParam(required = false) FulfillmentType fulfillmentType,
                                                    @RequestParam(required = false) Boolean preorder,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(orders.list(new OrderQuery(orderSn, receiver, status, fulfillmentType, preorder,
                createdFrom, createdTo, pageNum, pageSize)));
    }

    @GetMapping("/order/{id}")
    public CommonResult<OrderDetail> detail(@PathVariable Long id) {
        return CommonResult.success(orders.detail(id));
    }

    @Operation(summary = "Pickup orders: mark as ready on the counter")
    @PostMapping("/order/update/ready")
    public CommonResult<Void> ready(@RequestParam Set<Long> ids) {
        orders.markReady(ids);
        return CommonResult.success();
    }

    @Operation(summary = "Delivery orders: hand over to a courier with a tracking number")
    @PostMapping("/order/update/delivery")
    public CommonResult<Void> ship(@RequestBody List<@Valid ShipParam> params) {
        orders.ship(params);
        return CommonResult.success();
    }

    @PostMapping("/order/update/complete")
    public CommonResult<Void> complete(@RequestParam Set<Long> ids, @RequestParam(required = false) String note) {
        orders.complete(ids, note);
        return CommonResult.success();
    }

    @Operation(summary = "Cancel orders; paid orders are refunded and restocked")
    @PostMapping("/order/update/close")
    public CommonResult<Void> close(@Valid @RequestBody IdsReason p) {
        orders.close(p.ids(), p.reason());
        return CommonResult.success();
    }

    @PostMapping("/order/update/invalid")
    public CommonResult<Void> invalid(@RequestParam Set<Long> ids) {
        orders.invalidate(ids);
        return CommonResult.success();
    }

    @Operation(summary = "Confirm cash at the counter or a bank transfer")
    @PostMapping("/order/update/confirmPayment")
    public CommonResult<Void> confirmPayment(@Valid @RequestBody ManualPaymentParam p) {
        orders.confirmPayment(p);
        return CommonResult.success();
    }

    @PostMapping("/order/update/receiverInfo/{id}")
    public CommonResult<ShopOrder> receiver(@PathVariable Long id, @Valid @RequestBody ReceiverParam p) {
        return CommonResult.success(orders.updateReceiver(id, p));
    }

    @PostMapping("/order/update/note/{id}")
    public CommonResult<ShopOrder> note(@PathVariable Long id, @Valid @RequestBody NoteParam p) {
        return CommonResult.success(orders.updateNote(id, p.note()));
    }

    @PostMapping("/order/delete")
    public CommonResult<Void> delete(@RequestParam Set<Long> ids) {
        orders.delete(ids);
        return CommonResult.success();
    }

    // ---- payments
    @GetMapping("/payment/list")
    public CommonResult<CommonPage<Payment>> payments(@RequestParam(defaultValue = "SUCCEEDED") PaymentStatus status,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(CommonPage.of(payments.findByStatusOrderByIdDesc(status, PageParams.of(pageNum, pageSize))));
    }

    // ---- returns
    @GetMapping("/returnApply/list")
    public CommonResult<CommonPage<ReturnApply>> returns(@RequestParam(required = false) ReturnStatus status,
                                                         @RequestParam(required = false) String orderSn,
                                                         @RequestParam(required = false) String member,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(returns.list(new ReturnQuery(status, orderSn, member, pageNum, pageSize)));
    }

    @GetMapping("/returnApply/{id}")
    public CommonResult<ReturnApply> returnApply(@PathVariable Long id) {
        return CommonResult.success(returns.get(id));
    }

    @Operation(summary = "Accept, reject or complete a return (completing writes the refund and can restock)")
    @PostMapping("/returnApply/update/status/{id}")
    public CommonResult<ReturnApply> handleReturn(@PathVariable Long id, @Valid @RequestBody ReturnHandleParam p) {
        return CommonResult.success(returns.handle(id, p));
    }

    @GetMapping("/returnReason/list")
    public CommonResult<List<ReturnReason>> reasons() {
        return CommonResult.success(returns.reasons());
    }

    @PostMapping("/returnReason/create")
    public CommonResult<ReturnReason> createReason(@Valid @RequestBody ReasonParam p) {
        return CommonResult.success(returns.createReason(p));
    }

    @PostMapping("/returnReason/update/{id}")
    public CommonResult<ReturnReason> updateReason(@PathVariable Long id, @Valid @RequestBody ReasonParam p) {
        return CommonResult.success(returns.updateReason(id, p));
    }

    @PostMapping("/returnReason/delete")
    public CommonResult<Void> deleteReasons(@RequestParam Set<Long> ids) {
        returns.deleteReasons(ids);
        return CommonResult.success();
    }

    // ---- settings & locations
    @GetMapping("/orderSetting")
    public CommonResult<OrderSetting> setting() {
        return CommonResult.success(settings.get());
    }

    @PostMapping("/orderSetting/update")
    public CommonResult<OrderSetting> updateSetting(@Valid @RequestBody SettingParam p) {
        return CommonResult.success(settings.update(p));
    }

    @GetMapping("/shopLocation/list")
    public CommonResult<List<ShopLocation>> locations() {
        return CommonResult.success(settings.locations());
    }

    @PostMapping("/shopLocation/create")
    public CommonResult<ShopLocation> createLocation(@Valid @RequestBody LocationParam p) {
        return CommonResult.success(settings.createLocation(p));
    }

    @PostMapping("/shopLocation/update/{id}")
    public CommonResult<ShopLocation> updateLocation(@PathVariable Long id, @Valid @RequestBody LocationParam p) {
        return CommonResult.success(settings.updateLocation(id, p));
    }

    @PostMapping("/shopLocation/delete/{id}")
    public CommonResult<Void> deleteLocation(@PathVariable Long id) {
        settings.deleteLocation(id);
        return CommonResult.success();
    }

    // ---- personalisation
    @GetMapping("/personalization/list")
    public CommonResult<CommonPage<PersonalizationRequest>> requests(@RequestParam(required = false) PersonalizationStatus status,
                                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(CommonPage.of(personalizationRequests.search(status,
                PageParams.of(pageNum, pageSize, Sort.by(Sort.Order.desc("id"))))));
    }

    @GetMapping("/personalization/{id}")
    public CommonResult<Map<String, Object>> request(@PathVariable Long id) {
        return CommonResult.success(Map.of("request", personalization.get(id), "proofs", personalization.proofs(id)));
    }

    @Operation(summary = "Quote the price and the tested lead time")
    @PostMapping("/personalization/{id}/quote")
    public CommonResult<PersonalizationRequest> quote(@PathVariable Long id, @Valid @RequestBody QuoteParam p) {
        return CommonResult.success(personalization.quote(id, p.quoteAmount(), p.depositAmount(), p.leadTimeDays(), p.adminNote()));
    }

    @Operation(summary = "Send the next artwork proof (versions are kept)")
    @PostMapping("/personalization/{id}/proof")
    public CommonResult<PersonalizationProof> proof(@PathVariable Long id, @Valid @RequestBody ProofParam p) {
        return CommonResult.success(personalization.sendProof(id, p.artworkNote(), CurrentAdmin.username()));
    }

    @PostMapping("/personalization/{id}/start")
    public CommonResult<PersonalizationRequest> start(@PathVariable Long id) {
        return CommonResult.success(personalization.startProduction(id));
    }

    @PostMapping("/personalization/{id}/ready")
    public CommonResult<PersonalizationRequest> ready(@PathVariable Long id) {
        return CommonResult.success(personalization.markReady(id));
    }

    @PostMapping("/personalization/{id}/complete")
    public CommonResult<PersonalizationRequest> completeRequest(@PathVariable Long id) {
        return CommonResult.success(personalization.complete(id));
    }

    @PostMapping("/personalization/{id}/reject")
    public CommonResult<PersonalizationRequest> reject(@PathVariable Long id, @RequestParam String reason) {
        return CommonResult.success(personalization.reject(id, reason));
    }

    @PostMapping("/personalization/{id}/cancel")
    public CommonResult<PersonalizationRequest> cancel(@PathVariable Long id, @RequestParam String reason) {
        return CommonResult.success(personalization.cancelByStaff(id, reason));
    }
}
