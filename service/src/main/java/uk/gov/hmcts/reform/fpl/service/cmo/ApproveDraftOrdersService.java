package uk.gov.hmcts.reform.fpl.service.cmo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fpl.enums.HearingType;
import uk.gov.hmcts.reform.fpl.enums.State;
import uk.gov.hmcts.reform.fpl.exceptions.CMONotFoundException;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.ReviewDecision;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.order.HearingOrder;
import uk.gov.hmcts.reform.fpl.model.order.HearingOrdersBundle;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static uk.gov.hmcts.reform.fpl.enums.CMOReviewOutcome.JUDGE_REQUESTED_CHANGES;
import static uk.gov.hmcts.reform.fpl.enums.CMOReviewOutcome.SEND_TO_ALL_PARTIES;
import static uk.gov.hmcts.reform.fpl.enums.CMOStatus.DRAFT;
import static uk.gov.hmcts.reform.fpl.enums.CMOStatus.SEND_TO_JUDGE;
import static uk.gov.hmcts.reform.fpl.enums.HearingOrderType.AGREED_CMO;
import static uk.gov.hmcts.reform.fpl.enums.HearingOrderType.DRAFT_CMO;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.asDynamicList;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.unwrapElements;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApproveDraftOrdersService {

    private final ObjectMapper mapper;
    private final DraftOrderService draftOrderService;
    private final DraftOrdersReviewDataBuilder buildDraftOrdersReviewData;
    private final ReviewDecisionValidator reviewDecisionValidator;
    private final DraftOrdersBundleHearingSelector draftOrdersBundleHearingSelector;
    private final BlankOrderGenerator blankOrderGenerator;
    private final HearingOrderGenerator hearingOrderGenerator;

    private static final String ORDERS_TO_BE_SENT = "ordersToBeSent";
    private static final String NUM_DRAFT_CMOS = "numDraftCMOs";

    /**
     * That methods shouldn't be invoked without any cmo selected as the outcome is unexpected.
     * There is dedicated method below to support this functionality.
     */
    public DynamicList buildDynamicList(CaseData caseData) {
        List<Element<HearingOrdersBundle>> bundlesReadyForApproval = caseData.getBundlesForApproval();
        Element<HearingOrdersBundle> selectedBundle = getSelectedHearingDraftOrdersBundle(caseData);

        return asDynamicList(bundlesReadyForApproval, selectedBundle.getId(), HearingOrdersBundle::getHearingName);
    }

    public DynamicList buildUnselectedDynamicList(CaseData caseData) {
        List<Element<HearingOrdersBundle>> orderBundlesForApproval = caseData.getBundlesForApproval();
        return asDynamicList(orderBundlesForApproval, null, HearingOrdersBundle::getHearingName);
    }

    public Map<String, Object> getPageDisplayControls(CaseData caseData) {
        List<Element<HearingOrdersBundle>> draftOrdersReadyForApproval = caseData.getBundlesForApproval();
        Map<String, Object> data = new HashMap<>();

        switch (draftOrdersReadyForApproval.size()) {
            case 0:
                data.put(NUM_DRAFT_CMOS, "NONE");
                break;
            case 1:
                HearingOrdersBundle hearingOrdersBundle = draftOrdersReadyForApproval.get(0).getValue();
                data.put(NUM_DRAFT_CMOS, "SINGLE");

                data.putAll(buildDraftOrdersReviewData.buildDraftOrdersReviewData(hearingOrdersBundle));
                break;
            default:
                data.put(NUM_DRAFT_CMOS, "MULTI");
                DynamicList value = buildUnselectedDynamicList(caseData);
                data.put("cmoToReviewList", value);
                break;
        }

        return data;
    }

    public Map<String, Object> populateDraftOrdersData(CaseData caseData) {
        Element<HearingOrdersBundle> selectedHearingOrdersBundle = getSelectedHearingDraftOrdersBundle(caseData);

        return buildDraftOrdersReviewData.buildDraftOrdersReviewData(selectedHearingOrdersBundle.getValue());
    }

    @SuppressWarnings("unchecked")
    public List<String> validateDraftOrdersReviewDecision(CaseData caseData, Map<String, Object> data) {
        Element<HearingOrdersBundle> selectedOrdersBundle = getSelectedHearingDraftOrdersBundle(caseData);

        List<HearingOrder> hearingOrders = unwrapElements(selectedOrdersBundle.getValue().getOrders());
        List<String> errors = new ArrayList<>();

        boolean noReviewDecisionExists = true;
        int counter = 1;
        for (HearingOrder order : hearingOrders) {
            if (order.getType().isCmo()) {
                if (caseData.getReviewCMODecision() != null && caseData.getReviewCMODecision().getDecision() != null) {

                    noReviewDecisionExists = false;
                    errors.addAll(reviewDecisionValidator.validateReviewDecision(caseData.getReviewCMODecision(),
                        "CMO"));
                }
            } else {
                Map<String, Object> reviewDecisionMap = (Map<String, Object>) data.get("reviewDecision" + counter);
                ReviewDecision reviewDecision = mapper.convertValue(reviewDecisionMap, ReviewDecision.class);
                if (reviewDecision != null && reviewDecision.getDecision() != null) {

                    noReviewDecisionExists = false;
                    errors.addAll(reviewDecisionValidator.validateReviewDecision(reviewDecision,
                        "draft order " + counter));
                }
                counter++;
            }
        }

        if (!hearingOrders.isEmpty() && noReviewDecisionExists) {
            errors.add("Approve, amend or reject draft orders");
        }

        return errors;
    }

    public Map<String, Object> reviewCMO(CaseData caseData, Element<HearingOrdersBundle> selectedOrdersBundle) {
        Map<String, Object> data = new HashMap<>();

        Element<HearingOrder> cmo = selectedOrdersBundle.getValue().getOrders(SEND_TO_JUDGE).stream()
            .filter(order -> order.getValue().getType().isCmo())
            .findFirst().orElse(null);

        if (cmo != null) {
            ReviewDecision cmoReviewDecision = caseData.getReviewCMODecision();
            if (cmoReviewDecision != null && cmoReviewDecision.getDecision() != null) {

                Element<HearingOrder> reviewedOrder;

                if (!JUDGE_REQUESTED_CHANGES.equals(cmoReviewDecision.getDecision())) {
                    reviewedOrder = hearingOrderGenerator.buildSealedHearingOrder(cmoReviewDecision, cmo);

                    List<Element<HearingOrder>> sealedCMOs = caseData.getSealedCMOs();
                    sealedCMOs.add(reviewedOrder);
                    data.put("sealedCMOs", sealedCMOs);
                    data.put("state", getStateBasedOnNextHearing(caseData, cmoReviewDecision, reviewedOrder.getId()));
                } else {
                    reviewedOrder = hearingOrderGenerator.buildRejectedHearingOrder(
                        cmo, cmoReviewDecision.getChangesRequestedByJudge());
                }

                selectedOrdersBundle.getValue().getOrders().remove(cmo);
                caseData.getDraftUploadedCMOs().remove(cmo);
                updateHearingCMO(caseData, cmo.getId());
                updateHearingDraftOrdersBundle(caseData, selectedOrdersBundle);

                data.put("hearingDetails", caseData.getHearingDetails());
                data.put("draftUploadedCMOs", caseData.getDraftUploadedCMOs());
                data.put("hearingOrdersBundlesDrafts", caseData.getHearingOrdersBundlesDrafts());

                data.put(ORDERS_TO_BE_SENT, newArrayList(reviewedOrder));
            }
        }
        return data;
    }

    public List<Element<HearingOrder>> getCMOsReadyForApproval(CaseData caseData) {
        return caseData.getDraftUploadedCMOs().stream()
            .filter(cmo -> cmo.getValue().getStatus().equals(SEND_TO_JUDGE))
            .collect(toList());
    }

    public Element<HearingOrdersBundle> getSelectedHearingDraftOrdersBundle(CaseData caseData) {
        return draftOrdersBundleHearingSelector.getSelectedHearingDraftOrdersBundle(caseData);
    }

    public HearingOrder getLatestSealedCMO(CaseData caseData) {
        List<Element<HearingOrder>> sealedCMOs = caseData.getSealedCMOs();
        if (!sealedCMOs.isEmpty()) {
            return sealedCMOs.get(sealedCMOs.size() - 1).getValue();
        } else {
            throw new CMONotFoundException("No sealed CMOs found");
        }
    }

    @SuppressWarnings("unchecked")
    public void reviewC21Orders(CaseData caseData, Map<String, Object> data,
                                Element<HearingOrdersBundle> selectedOrdersBundle) {
        List<Element<HearingOrder>> draftOrders = selectedOrdersBundle.getValue().getOrders().stream()
            .filter(order -> !order.getValue().getType().isCmo()).collect(toList());

        List<Element<HearingOrder>> ordersToBeSent = defaultIfNull((
            List<Element<HearingOrder>>) data.get(ORDERS_TO_BE_SENT), newArrayList());

        int counter = 1;
        List<Element<GeneratedOrder>> orderCollection = caseData.getOrderCollection();

        for (Element<HearingOrder> orderElement : draftOrders) {
            Map<String, Object> reviewDecisionMap = (Map<String, Object>) data.get("reviewDecision" + counter);
            ReviewDecision reviewDecision = mapper.convertValue(reviewDecisionMap, ReviewDecision.class);

            if (reviewDecision != null && reviewDecision.getDecision() != null) {
                Element<HearingOrder> reviewedOrder;

                if (!JUDGE_REQUESTED_CHANGES.equals(reviewDecision.getDecision())) {
                    reviewedOrder = hearingOrderGenerator.buildSealedHearingOrder(reviewDecision, orderElement);
                    orderCollection.add(blankOrderGenerator.buildBlankOrder(caseData,
                        selectedOrdersBundle,
                        reviewedOrder));

                    ordersToBeSent.add(reviewedOrder);
                } else {
                    ordersToBeSent.add(hearingOrderGenerator.buildRejectedHearingOrder(
                        orderElement, reviewDecision.getChangesRequestedByJudge()));
                }
                selectedOrdersBundle.getValue().getOrders().remove(orderElement);
            }
            counter++;
        }

        if (ordersToBeSent.isEmpty()) {
            data.remove(ORDERS_TO_BE_SENT);
        } else {
            data.put(ORDERS_TO_BE_SENT, ordersToBeSent);
        }

        updateHearingDraftOrdersBundle(caseData, selectedOrdersBundle);
        data.put("orderCollection", orderCollection);
        data.put("hearingOrdersBundlesDrafts", caseData.getHearingOrdersBundlesDrafts());
    }

    private void updateHearingDraftOrdersBundle(CaseData caseData, Element<HearingOrdersBundle> selectedOrdersBundle) {
        if (selectedOrdersBundle.getValue().getOrders().isEmpty()) {
            caseData.getHearingOrdersBundlesDrafts()
                .removeIf(bundle -> bundle.getId().equals(selectedOrdersBundle.getId()));
        } else {
            caseData.getHearingOrdersBundlesDrafts().stream()
                .filter(bundle -> bundle.getId().equals(selectedOrdersBundle.getId()))
                .forEach(bundle -> bundle.getValue().setOrders(selectedOrdersBundle.getValue().getOrders()));
        }
    }

    private void updateHearingCMO(CaseData caseData, UUID cmoId) {
        defaultIfNull(caseData.getHearingDetails(), new ArrayList<Element<HearingBooking>>()).stream()
            .filter(hearing -> cmoId.equals(hearing.getValue().getCaseManagementOrderId()))
            .findFirst()
            .ifPresent(h -> h.getValue().setCaseManagementOrderId(null));
    }

    private State getStateBasedOnNextHearing(CaseData caseData, ReviewDecision reviewDecision, UUID cmoID) {
        State currentState = caseData.getState();
        Optional<HearingBooking> nextHearingBooking = caseData.getNextHearingAfterCmo(cmoID);

        if (nextHearingBooking.isPresent()
            && reviewDecision.hasReviewOutcomeOf(SEND_TO_ALL_PARTIES)
            && nextHearingBooking.get().isOfType(HearingType.FINAL)) {
            return State.FINAL_HEARING;
        }
        return currentState;
    }

    public List<Element<HearingOrdersBundle>> migrateDraftCMOsToHearingBundles(CaseData caseData) {
        List<Element<HearingOrdersBundle>> hearingOrdersBundles = defaultIfNull(
            caseData.getHearingOrdersBundlesDrafts(), new ArrayList<>());
        List<Element<HearingOrder>> cmoDrafts = caseData.getDraftUploadedCMOs();

        List<UUID> cmoDraftsIds = cmoDrafts.stream().map(Element::getId).collect(Collectors.toUnmodifiableList());
        hearingOrdersBundles.forEach(bundle -> bundle.getValue().getOrders()
            .removeIf(draft -> cmoDraftsIds.contains(draft.getId())));

        // Set id value in the hearing bundle as the element id value is reset between the callbacks
        List<Element<HearingOrdersBundle>> updatedHearingBundles = hearingOrdersBundles.stream()
            .map(bundle -> element(bundle.getId(), bundle.getValue().toBuilder().id(bundle.getId()).build()))
            .collect(Collectors.toList());

        cmoDrafts.forEach(draftElement -> {

            Optional<Element<HearingBooking>> hearingOptional = caseData.getHearingLinkedToCMO(draftElement.getId());
            hearingOptional.ifPresent(hearingElement -> {
                String title = draftElement.getValue().getType() == AGREED_CMO ? "Agreed CMO discussed at hearing"
                    : "Draft CMO from advocates' meeting";

                draftElement.getValue().toBuilder()
                    .title(defaultIfNull(draftElement.getValue().getTitle(), title))
                    .build();

                Optional<Element<HearingOrdersBundle>> bundle = updatedHearingBundles.stream()
                    .filter(b -> Objects.equals(b.getValue().getHearingId(), hearingElement.getId()))
                    .findFirst();

                if (bundle.isEmpty()) {
                    UUID bundleId = UUID.randomUUID();
                    updatedHearingBundles.add(element(bundleId, HearingOrdersBundle.builder()
                        .id(bundleId)
                        .build()
                        .updateHearing(hearingElement.getId(), hearingElement.getValue())
                        .updateOrders(List.of(draftElement),
                            draftElement.getValue().getStatus() == DRAFT ? DRAFT_CMO : AGREED_CMO)));
                } else {
                    bundle.get().getValue().getOrders().add(draftElement);
                }
            });
        });

        updatedHearingBundles.removeIf(bundle -> isEmpty(bundle.getValue().getOrders()));
        caseData.setHearingOrdersBundlesDrafts(updatedHearingBundles);
        return updatedHearingBundles;
    }

    public List<Element<HearingOrdersBundle>> buildHearingOrderBundles(CaseData caseData) {
        List<Element<HearingOrdersBundle>> updatedHearingOrderBundles = unwrapElements(caseData.getHearingOrdersBundlesDrafts())
            .stream().map(orderBundle -> element(orderBundle.getId(), orderBundle))
            .collect(toList());

        caseData.setHearingOrdersBundlesDrafts(updatedHearingOrderBundles);
        return updatedHearingOrderBundles;
    }

}
