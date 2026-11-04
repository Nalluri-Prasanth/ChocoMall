package com.chocomall.portal.web;

import com.chocomall.common.api.CommonResult;
import com.chocomall.core.experience.ExperienceBookingService;
import com.chocomall.core.personalization.PersonalizationService;
import com.chocomall.model.entity.ExperienceBooking;
import com.chocomall.model.entity.PersonalizationRequest;
import com.chocomall.portal.dto.PortalDtos.ApproveParam;
import com.chocomall.portal.dto.PortalDtos.BookParam;
import com.chocomall.portal.dto.PortalDtos.ChangesParam;
import com.chocomall.portal.dto.PortalDtos.PersonalizationSubmit;
import com.chocomall.portal.dto.PortalDtos.TokenParam;
import com.chocomall.portal.security.CurrentMember;
import com.chocomall.model.repository.ExperienceBookingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Personalised names and monograms, and bookable tastings and workshops. */
@Tag(name = "Personalisation and experiences")
@RestController
@RequiredArgsConstructor
public class ExtrasController {

    private final PersonalizationService personalization;
    private final ExperienceBookingService experiences;
    private final ExperienceBookingRepository bookings;

    // ---- personalisation
    @Operation(summary = "Request a personalised piece (name plaque, monogram, message strip...). Staff reply with a quote and lead time.")
    @PostMapping("/personalization/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult<PersonalizationRequest> submit(@Valid @RequestBody PersonalizationSubmit p) {
        return CommonResult.success(personalization.submit(CurrentMember.id(), p.kind(), p.text(), p.quantity(), p.occasionNote(), p.neededBy()));
    }

    @GetMapping("/personalization/list")
    public CommonResult<List<PersonalizationRequest>> mine() {
        return CommonResult.success(personalization.ofMember(CurrentMember.id()));
    }

    @Operation(summary = "A request with its artwork proofs and the date it can be ready")
    @GetMapping("/personalization/{id}")
    public CommonResult<Map<String, Object>> detail(@PathVariable Long id) {
        PersonalizationRequest r = personalization.getOwned(CurrentMember.id(), id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("request", r);
        out.put("proofs", personalization.proofs(id));
        LocalDate readyBy = PersonalizationService.readyBy(r);
        out.put("readyByIfApprovedToday", readyBy);
        return CommonResult.success(out);
    }

    @Operation(summary = "Approve the artwork proof you were shown (version must match the latest proof)")
    @PostMapping("/personalization/{id}/approve")
    public CommonResult<PersonalizationRequest> approve(@PathVariable Long id, @Valid @RequestBody ApproveParam p) {
        return CommonResult.success(personalization.approveProof(CurrentMember.id(), id, p.version()));
    }

    @PostMapping("/personalization/{id}/changes")
    public CommonResult<PersonalizationRequest> changes(@PathVariable Long id, @Valid @RequestBody ChangesParam p) {
        return CommonResult.success(personalization.requestChanges(CurrentMember.id(), id, p.note()));
    }

    @Operation(summary = "Pay the deposit after approving the proof")
    @PostMapping("/personalization/{id}/payDeposit")
    public CommonResult<PersonalizationRequest> payDeposit(@PathVariable Long id, @RequestBody(required = false) TokenParam p) {
        return CommonResult.success(personalization.payDeposit(CurrentMember.id(), id, p == null ? null : p.cardToken()));
    }

    @PostMapping("/personalization/{id}/cancel")
    public CommonResult<PersonalizationRequest> cancel(@PathVariable Long id) {
        return CommonResult.success(personalization.cancelByMember(CurrentMember.id(), id));
    }

    // ---- experiences
    @Operation(summary = "Reserve seats; pay within the payment window to confirm")
    @PostMapping("/experience/book")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult<ExperienceBooking> book(@Valid @RequestBody BookParam p) {
        return CommonResult.success(experiences.book(CurrentMember.id(), p.sessionId(), p.seats()));
    }

    @PostMapping("/experience/pay/{bookingId}")
    public CommonResult<ExperienceBooking> pay(@PathVariable Long bookingId, @RequestBody(required = false) TokenParam p) {
        return CommonResult.success(experiences.pay(CurrentMember.id(), bookingId, p == null ? null : p.cardToken()));
    }

    @GetMapping("/experience/my")
    public CommonResult<List<ExperienceBooking>> myBookings() {
        return CommonResult.success(bookings.findByMemberIdOrderByIdDesc(CurrentMember.id()));
    }

    @PostMapping("/experience/cancel/{bookingId}")
    public CommonResult<ExperienceBooking> cancelBooking(@PathVariable Long bookingId) {
        return CommonResult.success(experiences.cancelByMember(CurrentMember.id(), bookingId));
    }
}
