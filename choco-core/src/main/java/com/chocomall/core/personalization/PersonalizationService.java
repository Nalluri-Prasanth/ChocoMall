package com.chocomall.core.personalization;

import com.chocomall.common.exception.ApiException;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.SnGenerator;
import com.chocomall.core.payment.PaymentService;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.PersonalizationProof;
import com.chocomall.model.entity.PersonalizationRequest;
import com.chocomall.model.enums.PayMethod;
import com.chocomall.model.enums.PaymentStatus;
import com.chocomall.model.enums.PersonalizationKind;
import com.chocomall.model.enums.PersonalizationStatus;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.PersonalizationProofRepository;
import com.chocomall.model.repository.PersonalizationRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Paid personalisation (names, monograms, plaques, message strips...), following the 3D-printing guide:
 * <ol>
 *   <li>the customer submits the text and the date they need it;</li>
 *   <li>staff quote a price and a <em>tested</em> lead time (D21, D23);</li>
 *   <li>staff send an artwork proof; every version is kept, and the customer approves a specific version (D14);</li>
 *   <li>only after approval can the deposit be paid, so nothing is charged against unagreed artwork (D23);</li>
 *   <li>production, ready and collected.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PersonalizationService {

    private static final Set<PersonalizationStatus> OPEN_FOR_CUSTOMER_CANCEL = EnumSet.of(
            PersonalizationStatus.SUBMITTED, PersonalizationStatus.QUOTED, PersonalizationStatus.PROOF_SENT,
            PersonalizationStatus.PROOF_APPROVED);

    private final PersonalizationRequestRepository requests;
    private final PersonalizationProofRepository proofs;
    private final PaymentRepository payments;
    private final PaymentService paymentService;

    public PersonalizationRequest submit(Long memberId, PersonalizationKind kind, String text, int quantity,
                                         String occasionNote, LocalDate neededBy) {
        Asserts.isTrue(text != null && !text.isBlank(), "Enter the text to personalise");
        int maxLength = kind == PersonalizationKind.MESSAGE_STRIP ? 60 : 40;
        Asserts.isTrue(text.length() <= maxLength,
                "Keep the text within " + maxLength + " characters; thin or long designs break easily");
        Asserts.isTrue(quantity >= 1 && quantity <= 500, "Quantity must be between 1 and 500");
        Asserts.isTrue(neededBy == null || neededBy.isAfter(LocalDate.now()), "Choose a date in the future");
        PersonalizationRequest r = new PersonalizationRequest();
        r.setRequestSn(SnGenerator.next("PER"));
        r.setMemberId(memberId);
        r.setKind(kind);
        r.setTextContent(text.trim());
        r.setQuantity(quantity);
        r.setOccasionNote(occasionNote);
        r.setRequestedDate(neededBy);
        return requests.save(r);
    }

    /** Staff: price and lead time. A logo needs a note recording that permission to use the mark was checked. */
    public PersonalizationRequest quote(Long id, BigDecimal quote, BigDecimal deposit, int leadTimeDays, String adminNote) {
        PersonalizationRequest r = load(id);
        Asserts.state(r.getStatus() == PersonalizationStatus.SUBMITTED || r.getStatus() == PersonalizationStatus.QUOTED,
                "A request that is " + r.getStatus() + " can no longer be quoted");
        Asserts.isTrue(quote != null && quote.signum() > 0, "The quote must be positive");
        Asserts.isTrue(deposit != null && deposit.signum() >= 0 && deposit.compareTo(quote) <= 0,
                "The deposit must be between zero and the quote");
        Asserts.isTrue(leadTimeDays >= 1, "Enter the tested lead time in days");
        if (r.getKind() == PersonalizationKind.LOGO_PLAQUE) {
            Asserts.isTrue(adminNote != null && !adminNote.isBlank(),
                    "Record how permission to use the logo or protected mark was confirmed");
        }
        r.setQuoteAmount(quote);
        r.setDepositAmount(deposit);
        r.setLeadTimeDays(leadTimeDays);
        r.setAdminNote(adminNote);
        r.setStatus(PersonalizationStatus.QUOTED);
        return r;
    }

    /** Staff: send the next artwork proof. Earlier versions stay on record. */
    public PersonalizationProof sendProof(Long id, String artworkNote, String operator) {
        PersonalizationRequest r = load(id);
        Asserts.state(r.getStatus() == PersonalizationStatus.QUOTED || r.getStatus() == PersonalizationStatus.PROOF_SENT,
                "Quote the request before sending a proof");
        Asserts.isTrue(artworkNote != null && !artworkNote.isBlank(), "Describe the proof (text, size, layout)");
        int version = r.getProofVersion() + 1;
        PersonalizationProof p = new PersonalizationProof();
        p.setRequestId(id);
        p.setVersion(version);
        p.setArtworkNote(artworkNote);
        p.setCreatedBy(operator);
        r.setProofVersion(version);
        r.setStatus(PersonalizationStatus.PROOF_SENT);
        return proofs.save(p);
    }

    /** Customer: approve exactly the version they were shown. */
    public PersonalizationRequest approveProof(Long memberId, Long id, int version) {
        PersonalizationRequest r = owned(memberId, id);
        Asserts.state(r.getStatus() == PersonalizationStatus.PROOF_SENT, "There is no proof waiting for approval");
        Asserts.state(version == r.getProofVersion(), "A newer proof (version " + r.getProofVersion() + ") was sent. Review it first.");
        PersonalizationProof p = Asserts.found(proofs.findByRequestIdAndVersion(id, version), "Proof");
        p.setApprovedByMember(true);
        p.setApprovedTime(LocalDateTime.now());
        r.setStatus(PersonalizationStatus.PROOF_APPROVED);
        return r;
    }

    /** Customer: ask for changes; staff answer with a new proof version. */
    public PersonalizationRequest requestChanges(Long memberId, Long id, String note) {
        PersonalizationRequest r = owned(memberId, id);
        Asserts.state(r.getStatus() == PersonalizationStatus.PROOF_SENT, "There is no proof to comment on");
        Asserts.isTrue(note != null && !note.isBlank(), "Describe the changes you need");
        String prefix = r.getAdminNote() == null ? "" : r.getAdminNote() + "\n";
        r.setAdminNote(prefix + "Customer change request on v" + r.getProofVersion() + ": " + note.trim());
        return r;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public PersonalizationRequest payDeposit(Long memberId, Long id, String token) {
        PersonalizationRequest r = owned(memberId, id);
        if (r.getStatus() == PersonalizationStatus.DEPOSIT_PAID) {
            return r;
        }
        Asserts.state(r.getStatus() == PersonalizationStatus.PROOF_APPROVED, "Approve the artwork proof before paying the deposit");
        BigDecimal amount = r.getDepositAmount();
        if (amount.signum() > 0) {
            Payment draft = paymentService.draft(amount, PayMethod.CARD);
            draft.setPersonalizationId(id);
            paymentService.charge(draft, token);
        }
        r.setStatus(PersonalizationStatus.DEPOSIT_PAID);
        return r;
    }

    public PersonalizationRequest startProduction(Long id) {
        return move(id, PersonalizationStatus.DEPOSIT_PAID, PersonalizationStatus.IN_PRODUCTION, "Only a request with a paid deposit can start production");
    }

    public PersonalizationRequest markReady(Long id) {
        return move(id, PersonalizationStatus.IN_PRODUCTION, PersonalizationStatus.READY, "Only a request in production can be marked ready");
    }

    public PersonalizationRequest complete(Long id) {
        return move(id, PersonalizationStatus.READY, PersonalizationStatus.COMPLETED, "Only a ready request can be completed");
    }

    public PersonalizationRequest reject(Long id, String reason) {
        PersonalizationRequest r = load(id);
        Asserts.state(r.getStatus() == PersonalizationStatus.SUBMITTED || r.getStatus() == PersonalizationStatus.QUOTED,
                "Only an unquoted or quoted request can be rejected");
        r.setStatus(PersonalizationStatus.REJECTED);
        r.setAdminNote((r.getAdminNote() == null ? "" : r.getAdminNote() + "\n") + "Rejected: " + reason);
        return r;
    }

    public PersonalizationRequest cancelByMember(Long memberId, Long id) {
        PersonalizationRequest r = owned(memberId, id);
        Asserts.state(OPEN_FOR_CUSTOMER_CANCEL.contains(r.getStatus()),
                "Once the deposit is paid, ask the shop to cancel (materials may already be prepared)");
        r.setStatus(PersonalizationStatus.CANCELLED);
        return r;
    }

    /** Staff cancel at any stage before completion; a paid deposit is marked refunded. */
    public PersonalizationRequest cancelByStaff(Long id, String reason) {
        PersonalizationRequest r = load(id);
        Asserts.state(r.getStatus() != PersonalizationStatus.COMPLETED && r.getStatus() != PersonalizationStatus.CANCELLED
                && r.getStatus() != PersonalizationStatus.REJECTED, "This request is already closed");
        for (Payment p : payments.findByPersonalizationId(id)) {
            if (p.getStatus() == PaymentStatus.SUCCEEDED) {
                p.setStatus(PaymentStatus.REFUNDED);
            }
        }
        r.setStatus(PersonalizationStatus.CANCELLED);
        r.setAdminNote((r.getAdminNote() == null ? "" : r.getAdminNote() + "\n") + "Cancelled: " + reason);
        return r;
    }

    @Transactional(readOnly = true)
    public PersonalizationRequest get(Long id) {
        return load(id);
    }

    @Transactional(readOnly = true)
    public PersonalizationRequest getOwned(Long memberId, Long id) {
        return owned(memberId, id);
    }

    @Transactional(readOnly = true)
    public List<PersonalizationProof> proofs(Long id) {
        return proofs.findByRequestIdOrderByVersionAsc(id);
    }

    @Transactional(readOnly = true)
    public List<PersonalizationRequest> ofMember(Long memberId) {
        return requests.findByMemberIdOrderByIdDesc(memberId);
    }

    /** The date the shop can promise, based on the quoted lead time. */
    public static LocalDate readyBy(PersonalizationRequest r) {
        return r.getLeadTimeDays() == null ? null : LocalDate.now().plusDays(r.getLeadTimeDays());
    }

    private PersonalizationRequest move(Long id, PersonalizationStatus from, PersonalizationStatus to, String message) {
        PersonalizationRequest r = load(id);
        Asserts.state(r.getStatus() == from, message);
        r.setStatus(to);
        return r;
    }

    private PersonalizationRequest load(Long id) {
        return Asserts.found(requests.findById(id), "Personalisation request");
    }

    private PersonalizationRequest owned(Long memberId, Long id) {
        return Asserts.found(requests.findByIdAndMemberId(id, memberId), "Personalisation request");
    }
}
