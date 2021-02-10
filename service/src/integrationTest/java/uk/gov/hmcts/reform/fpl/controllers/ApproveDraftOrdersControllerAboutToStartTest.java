package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.fpl.controllers.orders.ApproveDraftOrdersController;
import uk.gov.hmcts.reform.fpl.enums.HearingOrderType;
import uk.gov.hmcts.reform.fpl.enums.HearingType;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.ReviewDecision;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicListElement;
import uk.gov.hmcts.reform.fpl.model.event.ReviewDraftOrdersData;
import uk.gov.hmcts.reform.fpl.model.order.HearingOrder;
import uk.gov.hmcts.reform.fpl.model.order.HearingOrdersBundle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.enums.CMOReviewOutcome.SEND_TO_ALL_PARTIES;
import static uk.gov.hmcts.reform.fpl.enums.CMOStatus.DRAFT;
import static uk.gov.hmcts.reform.fpl.enums.CMOStatus.SEND_TO_JUDGE;
import static uk.gov.hmcts.reform.fpl.enums.HearingOrderType.AGREED_CMO;
import static uk.gov.hmcts.reform.fpl.enums.HearingOrderType.C21;
import static uk.gov.hmcts.reform.fpl.enums.HearingOrderType.DRAFT_CMO;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocumentReference;

@ActiveProfiles("integration-test")
@WebMvcTest(ApproveDraftOrdersController.class)
@OverrideAutoConfiguration(enabled = true)
class ApproveDraftOrdersControllerAboutToStartTest extends AbstractControllerTest {

    private final HearingBooking hearing1 = buildHearing(LocalDateTime.now().plusDays(2));
    private final HearingBooking hearing2 = buildHearing(LocalDateTime.now().plusDays(5));

    private final Element<HearingOrder> agreedCMO = element(buildDraftOrder(hearing1.toLabel(), AGREED_CMO));
    private final Element<HearingOrder> draftCMO = element(buildDraftOrder(hearing1.toLabel(), DRAFT_CMO));
    private final Element<HearingOrder> draftOrder1 = element(buildDraftOrder(hearing1.toLabel(), C21));
    private final Element<HearingOrder> draftOrder2 = element(buildDraftOrder(hearing2.toLabel(), C21));

    public static final UUID HEARING_ORDERS_BUNDLE_1 = UUID.randomUUID();
    public static final UUID HEARING_ORDERS_BUNDLE_2 = UUID.randomUUID();

    ApproveDraftOrdersControllerAboutToStartTest() {
        super("approve-draft-orders");
    }

    @Test
    void shouldReturnCorrectDataWhenMultipleHearingDraftOrdersBundlesExist() {
        List<Element<HearingOrdersBundle>> hearingOrdersBundles = List.of(
            buildHearingDraftOrdersBundles(
                HEARING_ORDERS_BUNDLE_1, hearing1.toLabel(), newArrayList(agreedCMO, draftOrder1)),
            buildHearingDraftOrdersBundles(
                HEARING_ORDERS_BUNDLE_2, hearing2.toLabel(), newArrayList(draftCMO, draftOrder2)));

        CaseData caseData = CaseData.builder()
            .draftUploadedCMOs(newArrayList(agreedCMO, draftCMO))
            .hearingOrdersBundlesDrafts(hearingOrdersBundles)
            .hearingDetails(List.of(element(hearing1), element(hearing2)))
            .build();

        AboutToStartOrSubmitCallbackResponse response = postAboutToStartEvent(caseData);

        DynamicList bundlesList = DynamicList.builder()
            .value(DynamicListElement.EMPTY)
            .listItems(hearingOrdersBundles.stream().map(bundle -> DynamicListElement.builder()
                .code(bundle.getId())
                .label(bundle.getValue().getHearingName())
                .build())
                .collect(Collectors.toList()))
            .build();

        CaseData responseData = extractCaseData(response);

        assertThat(responseData.getNumDraftCMOs()).isEqualTo("MULTI");
        assertThat(responseData.getCmoToReviewList()).isEqualTo(
            mapper.convertValue(bundlesList, new TypeReference<Map<String, Object>>() {
            }));
    }

    @Test
    void shouldReturnAgreedCMOWhenOneHearingBundleExistsWithADraftCMOsReadyForApproval() {
        Element<HearingOrdersBundle> hearingOrdersBundle =
            buildHearingDraftOrdersBundles(HEARING_ORDERS_BUNDLE_1, hearing1.toLabel(),
                newArrayList(agreedCMO, draftCMO));

        CaseData caseData = CaseData.builder()
            .draftUploadedCMOs(List.of(agreedCMO, draftCMO))
            .hearingOrdersBundlesDrafts(singletonList(hearingOrdersBundle))
            .reviewCMODecision(ReviewDecision.builder().decision(SEND_TO_ALL_PARTIES).build())
            .hearingDetails(List.of(element(buildHearing(hearing1.getStartDate(), agreedCMO.getId()))))
            .reviewDraftOrdersData(ReviewDraftOrdersData.builder()
                .reviewDecision1(ReviewDecision.builder().decision(SEND_TO_ALL_PARTIES).build())
                .build())
            .build();

        ReviewDraftOrdersData expectedReviewDraftOrdersData = ReviewDraftOrdersData.builder()
            .cmoDraftOrderTitle(agreedCMO.getValue().getTitle())
            .cmoDraftOrderDocument(agreedCMO.getValue().getOrder())
            .draftCMOExists("Y")
            .build();

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToStartEvent(caseData);
        CaseData responseData = extractCaseData(callbackResponse);

        assertThat(callbackResponse.getData()).doesNotContainKeys(ReviewDraftOrdersData.reviewDecisionFields());
        assertThat(responseData.getNumDraftCMOs()).isEqualTo("SINGLE");
        assertThat(responseData.getReviewDraftOrdersData()).isEqualTo(expectedReviewDraftOrdersData);
    }

    @Test
    void shouldReturnCMOsFromUploadDraftCMOsAndHearingBundlesWhenUploadDraftCMOsContainTheAgreedCMO() {
        UUID agreedCMOId2 = UUID.randomUUID();

        Element<HearingBooking> hearing = element(UUID.randomUUID(),
            HearingBooking.builder().type(HearingType.CASE_MANAGEMENT)
                .startDate(LocalDateTime.now().plusDays(1))
                .caseManagementOrderId(agreedCMOId2).build());

        Element<HearingOrder> agreedCMO2 = element(agreedCMOId2,
            buildDraftOrder(hearing1.toLabel(), AGREED_CMO));

        Element<HearingOrdersBundle> hearingBundle1 = buildHearingDraftOrdersBundles(
            HEARING_ORDERS_BUNDLE_1, hearing1.toLabel(), newArrayList(agreedCMO, draftOrder1));
        Element<HearingOrdersBundle> hearingBundle2 = buildHearingDraftOrdersBundles(
            HEARING_ORDERS_BUNDLE_2, hearing.getValue().toLabel(), newArrayList(agreedCMO2));

        List<Element<HearingOrdersBundle>> hearingOrdersBundles = List.of(hearingBundle1);

        CaseData caseData = CaseData.builder()
            .draftUploadedCMOs(newArrayList(agreedCMO2))
            .hearingOrdersBundlesDrafts(hearingOrdersBundles)
            .hearingDetails(List.of(element(hearing1), hearing))
            .build();

        AboutToStartOrSubmitCallbackResponse response = postAboutToStartEvent(caseData);

        CaseData responseData = extractCaseData(response);

        DynamicList actualDynamicList = mapper.convertValue(responseData.getCmoToReviewList(), DynamicList.class);

        assertThat(responseData.getNumDraftCMOs()).isEqualTo("MULTI");
        assertThat(actualDynamicList.getValue()).isEqualTo(DynamicListElement.EMPTY);
        assertThat(actualDynamicList.getListItems()).extracting("label")
            .containsExactlyInAnyOrder(
                hearingBundle1.getValue().getHearingName(),
                hearingBundle2.getValue().getHearingName());
    }

    @Test
    void shouldNotReturnOrdersFromUploadDraftCMOsWhenUploadDraftCMOsContainOnlyDraftCMOs() {
        UUID agreedCMOId2 = UUID.randomUUID();

        Element<HearingBooking> hearing3 = element(buildHearing(LocalDateTime.now().plusDays(5), draftCMO.getId()));

        Element<HearingOrder> agreedCMO2 = element(agreedCMOId2, buildDraftOrder(hearing1.toLabel(), AGREED_CMO));

        Element<HearingOrdersBundle> hearingBundle1 = buildHearingDraftOrdersBundles(
            HEARING_ORDERS_BUNDLE_1, hearing1.toLabel(), newArrayList(agreedCMO, draftOrder1));
        Element<HearingOrdersBundle> hearingBundle2 = buildHearingDraftOrdersBundles(
            HEARING_ORDERS_BUNDLE_2, hearing2.toLabel(), newArrayList(agreedCMO2));

        List<Element<HearingOrdersBundle>> hearingOrdersBundles = List.of(hearingBundle1, hearingBundle2);

        CaseData caseData = CaseData.builder().draftUploadedCMOs(newArrayList(draftCMO))
            .hearingOrdersBundlesDrafts(hearingOrdersBundles)
            .hearingDetails(List.of(element(hearing1), element(hearing2), hearing3))
            .build();

        AboutToStartOrSubmitCallbackResponse response = postAboutToStartEvent(caseData);

        CaseData responseData = extractCaseData(response);

        DynamicList actualDynamicList = mapper.convertValue(responseData.getCmoToReviewList(), DynamicList.class);

        assertThat(responseData.getNumDraftCMOs()).isEqualTo("MULTI");
        assertThat(actualDynamicList.getValue()).isEqualTo(DynamicListElement.builder().build());
        assertThat(actualDynamicList.getListItems()).extracting("label")
            .containsExactlyInAnyOrder(
                hearingBundle1.getValue().getHearingName(),
                hearingBundle2.getValue().getHearingName());
    }

    @Test
    void shouldReturnCMOWhenUploadDraftCMOsContainAgreedCMOAndHearingOrderBundlesAreEmpty() {
        Element<HearingBooking> hearingBookingElement = element(UUID.randomUUID(),
            HearingBooking.builder().type(HearingType.CASE_MANAGEMENT).startDate(LocalDateTime.now().plusDays(5))
                .caseManagementOrderId(agreedCMO.getId()).build());

        CaseData caseData = CaseData.builder()
            .draftUploadedCMOs(newArrayList(agreedCMO))
            .hearingDetails(List.of(hearingBookingElement))
            .build();

        AboutToStartOrSubmitCallbackResponse response = postAboutToStartEvent(caseData);

        CaseData responseData = extractCaseData(response);

        ReviewDraftOrdersData expectedReviewDraftOrdersData = ReviewDraftOrdersData.builder()
            .cmoDraftOrderTitle(agreedCMO.getValue().getTitle())
            .cmoDraftOrderDocument(agreedCMO.getValue().getOrder())
            .draftCMOExists("Y")
            .build();

        assertThat(responseData.getNumDraftCMOs()).isEqualTo("SINGLE");
        assertThat(responseData.getReviewDraftOrdersData()).isEqualTo(expectedReviewDraftOrdersData);
    }

    @Test
    void shouldReturnCorrectDataWhenNoDraftCMOsReadyForApproval() {
        CaseData caseData = CaseData.builder().draftUploadedCMOs(emptyList())
            .hearingOrdersBundlesDrafts(emptyList()).build();

        CaseData updatedCaseData = extractCaseData(postAboutToStartEvent(caseData));
        assertThat(updatedCaseData.getNumDraftCMOs()).isEqualTo("NONE");
    }

    @Test
    void shouldReturnCorrectDataWhenNoCMOsExistForReadyForApprovalInTheSelectedBundle() {
        Element<HearingOrdersBundle> hearingOrdersBundle =
            buildHearingDraftOrdersBundles(HEARING_ORDERS_BUNDLE_1, hearing1.toLabel(), newArrayList(draftCMO));

        CaseData caseData = CaseData.builder().draftUploadedCMOs(newArrayList(draftCMO))
            .hearingDetails(List.of(element(hearing1)))
            .hearingOrdersBundlesDrafts(singletonList(hearingOrdersBundle)).build();

        CaseData updatedCaseData = extractCaseData(postAboutToStartEvent(caseData));
        assertThat(updatedCaseData.getNumDraftCMOs()).isEqualTo("NONE");
    }

    private HearingBooking buildHearing(LocalDateTime date) {
        return buildHearing(date, null);
    }

    private HearingBooking buildHearing(LocalDateTime date, UUID cmoId) {
        return HearingBooking.builder()
            .type(HearingType.CASE_MANAGEMENT)
            .startDate(date)
            .caseManagementOrderId(cmoId)
            .build();
    }

    private Element<HearingOrdersBundle> buildHearingDraftOrdersBundles(
        UUID hearingOrdersBundleId, String hearing, List<Element<HearingOrder>> orders) {
        return element(hearingOrdersBundleId,
            HearingOrdersBundle.builder().hearingId(UUID.randomUUID())
                .orders(orders)
                .hearingName(hearing).build());
    }

    private HearingOrder buildDraftOrder(String hearing, HearingOrderType orderType) {
        return HearingOrder.builder()
            .hearing(hearing)
            .title(hearing)
            .order(testDocumentReference())
            .type(orderType)
            .status(DRAFT_CMO.equals(orderType) ? DRAFT : SEND_TO_JUDGE).build();
    }
}
