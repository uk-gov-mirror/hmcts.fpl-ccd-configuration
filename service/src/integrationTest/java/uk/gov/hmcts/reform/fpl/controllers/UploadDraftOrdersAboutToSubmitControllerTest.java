package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.fpl.controllers.orders.UploadDraftOrdersController;
import uk.gov.hmcts.reform.fpl.enums.CMOStatus;
import uk.gov.hmcts.reform.fpl.enums.CMOType;
import uk.gov.hmcts.reform.fpl.enums.HearingOrderType;
import uk.gov.hmcts.reform.fpl.enums.YesNo;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.HearingFurtherEvidenceBundle;
import uk.gov.hmcts.reform.fpl.model.SupportingEvidenceBundle;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.event.UploadDraftOrdersData;
import uk.gov.hmcts.reform.fpl.model.order.HearingOrder;
import uk.gov.hmcts.reform.fpl.utils.TestDataHelper;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.fpl.enums.CMOStatus.DRAFT;
import static uk.gov.hmcts.reform.fpl.enums.CMOStatus.SEND_TO_JUDGE;
import static uk.gov.hmcts.reform.fpl.enums.HearingOrderKind.CMO;
import static uk.gov.hmcts.reform.fpl.enums.HearingOrderType.AGREED_CMO;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.JudgeAndLegalAdvisorHelper.formatJudgeTitleAndName;

@ActiveProfiles("integration-test")
@WebMvcTest(UploadDraftOrdersController.class)
@OverrideAutoConfiguration(enabled = true)
class UploadDraftOrdersAboutToSubmitControllerTest extends AbstractUploadDraftOrdersControllerTest {

    @MockBean
    private IdamClient idamClient;

    private static final DocumentReference DOCUMENT_REFERENCE = TestDataHelper.testDocumentReference();

    @Test
    void shouldAddCMOToListWithDraftStatusAndNotMigrateDocs() {
        List<Element<SupportingEvidenceBundle>> bundles = List.of(element(
            SupportingEvidenceBundle.builder()
                .name("case summary")
                .build()
        ));

        List<Element<HearingBooking>> hearings = hearingsOnDateAndDayAfter(now().plusDays(3));

        UploadDraftOrdersData eventData = UploadDraftOrdersData.builder()
            .hearingOrderDraftKind(List.of(CMO))
            .futureHearingsForCMO(dynamicList(hearings))
            .uploadedCaseManagementOrder(DOCUMENT_REFERENCE)
            .cmoUploadType(CMOType.DRAFT)
            .cmoSupportingDocs(bundles)
            .build();

        CaseData caseData = CaseData.builder()
            .uploadDraftOrdersEventData(eventData)
            .hearingDetails(hearings)
            .build();

        CaseData responseData = extractCaseData(postAboutToSubmitEvent(caseData));

        HearingOrder cmo = orderWithDocs(hearings.get(0).getValue(), HearingOrderType.DRAFT_CMO, DRAFT, bundles);

        List<Element<HearingOrder>> unsealedCMOs = responseData.getDraftUploadedCMOs();

        assertThat(unsealedCMOs)
            .hasSize(1)
            .first()
            .extracting(Element::getValue)
            .isEqualTo(cmo);

        hearings.get(0).getValue().setCaseManagementOrderId(unsealedCMOs.get(0).getId());

        assertThat(responseData.getHearingDetails()).isEqualTo(hearings);

        assertThat(responseData.getHearingFurtherEvidenceDocuments()).isEmpty();
    }

    @Test
    void shouldAddCMOToListWithSendToJudgeStatusAndMigrateDocs() {
        given(idamClient.getUserDetails(anyString())).willReturn(UserDetails.builder()
            .email("Test LA")
            .roles(List.of("caseworker-publiclaw-solicitor"))
            .build());

        UUID bundleId = UUID.randomUUID();
        List<Element<SupportingEvidenceBundle>> cmoBundles = List.of(element(bundleId,
            SupportingEvidenceBundle.builder()
                .name("case summary")
                .build()));

        List<Element<SupportingEvidenceBundle>> hearingDocsBundles = List.of(element(bundleId,
            SupportingEvidenceBundle.builder()
                .name("case summary")
                .uploadedBy("Test LA")
                .dateTimeUploaded(now())
                .build()));

        List<Element<HearingBooking>> hearings = hearingsOnDateAndDayAfter(now().minusDays(3));

        UploadDraftOrdersData eventData = UploadDraftOrdersData.builder()
            .hearingOrderDraftKind(List.of(CMO))
            .pastHearingsForCMO(dynamicList(hearings))
            .uploadedCaseManagementOrder(DOCUMENT_REFERENCE)
            .cmoUploadType(CMOType.AGREED)
            .cmoSupportingDocs(cmoBundles)
            .build();

        CaseData caseData = CaseData.builder()
            .uploadDraftOrdersEventData(eventData)
            .hearingDetails(hearings)
            .build();

        CaseData responseData = extractCaseData(postAboutToSubmitEvent(caseData));

        HearingOrder cmo = orderWithDocs(hearings.get(0).getValue(), AGREED_CMO, SEND_TO_JUDGE, cmoBundles);

        List<Element<HearingOrder>> unsealedCMOs = responseData.getDraftUploadedCMOs();

        assertThat(unsealedCMOs)
            .hasSize(1)
            .first()
            .extracting(Element::getValue)
            .isEqualTo(cmo);

        hearings.get(0).getValue().setCaseManagementOrderId(unsealedCMOs.get(0).getId());

        assertThat(responseData.getHearingDetails()).isEqualTo(hearings);

        List<Element<HearingFurtherEvidenceBundle>> furtherEvidenceBundle = List.of(
            element(hearings.get(0).getId(), HearingFurtherEvidenceBundle.builder()
                .hearingName(hearings.get(0).getValue().toLabel())
                .supportingEvidenceBundle(hearingDocsBundles)
                .build())
        );

        assertThat(responseData.getHearingFurtherEvidenceDocuments())
            .hasSize(1)
            .isEqualTo(furtherEvidenceBundle);
    }

    @Test
    void shouldRemoveTemporaryFields() {
        List<Element<HearingBooking>> hearings = hearingsOnDateAndDayAfter(LocalDateTime.of(2020, 3, 15, 10, 7));
        List<Element<HearingOrder>> draftCMOs = List.of();

        CaseData caseData = CaseData.builder()
            .uploadDraftOrdersEventData(UploadDraftOrdersData.builder()
                .hearingOrderDraftKind(List.of(CMO))
                .uploadedCaseManagementOrder(DOCUMENT_REFERENCE)
                .pastHearingsForCMO(dynamicList(hearings))
                .futureHearingsForCMO("DUMMY DATA")
                .cmoJudgeInfo("DUMMY DATA")
                .cmoHearingInfo("DUMMY DATA")
                .showReplacementCMO(YesNo.NO)
                .replacementCMO(DOCUMENT_REFERENCE)
                .previousCMO(DOCUMENT_REFERENCE)
                .cmoSupportingDocs(List.of())
                .cmoToSend(DOCUMENT_REFERENCE)
                .showCMOsSentToJudge(YesNo.NO)
                .cmosSentToJudge("DUMMY DATA")
                .cmoUploadType(CMOType.DRAFT)
                .build())
            .hearingDetails(hearings)
            .draftUploadedCMOs(draftCMOs)
            .build();

        AboutToStartOrSubmitCallbackResponse response = postAboutToSubmitEvent(asCaseDetails(caseData));

        Set<String> keys = mapper.convertValue(caseData, new TypeReference<Map<String, Object>>() {
        }).keySet();

        keys.removeAll(List.of(
            "showCMOsSentToJudge", "cmosSentToJudge", "cmoUploadType", "pastHearingsForCMO", "futureHearingsForCMO",
            "cmoHearingInfo", "showReplacementCMO", "previousCMO", "uploadedCaseManagementOrder", "replacementCMO",
            "cmoSupportingDocs", "cmoJudgeInfo", "cmoToSend", "hearingsForHearingOrderDrafts",
            "currentHearingOrderDrafts", "hearingOrderDraftKind"
        ));

        assertThat(response.getData().keySet()).isEqualTo(keys);
    }

    private HearingOrder orderWithDocs(HearingBooking hearing, HearingOrderType type, CMOStatus status,
                                       List<Element<SupportingEvidenceBundle>> supportingDocs) {
        return HearingOrder.builder()
            .type(type)
            .title(type == AGREED_CMO ? "Agreed CMO discussed at hearing" : "Draft CMO from advocates' meeting")
            .status(status)
            .hearing(hearing.toLabel())
            .order(DOCUMENT_REFERENCE)
            .dateSent(dateNow())
            .judgeTitleAndName(formatJudgeTitleAndName(hearing.getJudgeAndLegalAdvisor()))
            .supportingDocs(supportingDocs)
            .build();
    }

    private DynamicList dynamicList(List<Element<HearingBooking>> hearings) {
        return dynamicListWithFirstSelected(
            Pair.of(hearings.get(0).getValue().toLabel(), hearings.get(0).getId()),
            Pair.of(hearings.get(1).getValue().toLabel(), hearings.get(1).getId())
        );
    }

    private List<Element<HearingBooking>> hearingsOnDateAndDayAfter(LocalDateTime startDate) {
        return List.of(
            hearing(startDate),
            hearing(startDate.plusDays(1))
        );
    }
}
