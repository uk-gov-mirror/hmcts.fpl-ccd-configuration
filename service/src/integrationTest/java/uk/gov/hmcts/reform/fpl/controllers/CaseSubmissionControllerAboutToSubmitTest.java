package uk.gov.hmcts.reform.fpl.controllers;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Orders;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.RespondentSolicitor;
import uk.gov.hmcts.reform.fpl.model.RespondentSolicitorOrganisation;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.Telephone;
import uk.gov.hmcts.reform.fpl.service.FeatureToggleService;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;
import uk.gov.hmcts.reform.fpl.service.casesubmission.CaseSubmissionService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.of;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.fpl.Constants.LOCAL_AUTHORITY_1_CODE;
import static uk.gov.hmcts.reform.fpl.Constants.LOCAL_AUTHORITY_1_NAME;
import static uk.gov.hmcts.reform.fpl.enums.OrderType.CARE_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.SolicitorRole.SOLICITORA;
import static uk.gov.hmcts.reform.fpl.enums.SolicitorRole.SOLICITORB;
import static uk.gov.hmcts.reform.fpl.enums.YesNo.YES;
import static uk.gov.hmcts.reform.fpl.utils.DocumentManagementStoreLoader.document;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.DOCUMENT_CONTENT;

@WebMvcTest(CaseSubmissionController.class)
@OverrideAutoConfiguration(enabled = true)
class CaseSubmissionControllerAboutToSubmitTest extends AbstractCallbackTest {

    @MockBean
    private UploadDocumentService uploadDocumentService;

    @MockBean
    private FeatureToggleService featureToggleService;

    @SpyBean
    private CaseSubmissionService caseSubmissionService;

    private final Document document = document();

    CaseSubmissionControllerAboutToSubmitTest() {
        super("case-submission");
    }

    @BeforeEach
    void mocking() {
        givenCurrentUserWithName("Emma Taylor");

        doReturn(document).when(caseSubmissionService).generateSubmittedFormPDF(any(), eq(false));

        given(uploadDocumentService.uploadPDF(DOCUMENT_CONTENT, "2313.pdf"))
            .willReturn(document);
        given(featureToggleService.isRestrictedFromCaseSubmission("FPLA"))
            .willReturn(true);
    }

    @Test
    void shouldReturnUnsuccessfulResponseWithNoData() {
        postAboutToSubmitEvent(new byte[]{}, SC_BAD_REQUEST);
    }

    @Test
    void shouldReturnUnsuccessfulResponseWithMalformedData() {
        postAboutToSubmitEvent("malformed json".getBytes(), SC_BAD_REQUEST);
    }

    @Test
    void shouldSetCtscPropertyToYesWhenCtscLaunchDarklyVariableIsEnabled() {
        given(featureToggleService.isCtscEnabled(anyString())).willReturn(false);

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent("fixtures/case.json");

        assertThat(callbackResponse.getData())
            .containsEntry("caseLocalAuthority", LOCAL_AUTHORITY_1_CODE)
            .containsEntry("sendToCtsc", "No")
            .containsEntry("submittedForm", ImmutableMap.<String, String>builder()
                .put("document_url", document.links.self.href)
                .put("document_binary_url", document.links.binary.href)
                .put("document_filename", document.originalDocumentName)
                .build());
    }

    @Test
    void shouldSetCtscPropertyToNoWhenCtscLaunchDarklyVariableIsDisabled() {
        given(featureToggleService.isCtscEnabled(anyString())).willReturn(true);

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent("fixtures/case.json");

        assertThat(callbackResponse.getData()).containsEntry("sendToCtsc", "Yes");
        verify(featureToggleService).isCtscEnabled(LOCAL_AUTHORITY_1_NAME);
    }

    @Test
    void shouldRetainPaymentInformationInCase() {
        given(featureToggleService.isCtscEnabled(anyString())).willReturn(true);

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(CaseDetails.builder()
            .id(2313L)
            .data(Map.of(
                "dateSubmitted", dateNow(),
                "orders", Orders.builder().orderType(List.of(CARE_ORDER)).build(),
                "caseLocalAuthority", LOCAL_AUTHORITY_1_CODE,
                "amountToPay", "233300",
                "displayAmountToPay", "Yes"
            ))
            .build());

        assertThat(callbackResponse.getData())
            .containsEntry("amountToPay", "233300")
            .containsEntry("displayAmountToPay", YES.getValue());
    }

    @Nested
    class LocalAuthorityValidation {

        final String localAuthority = LOCAL_AUTHORITY_1_CODE;
        final CaseDetails caseDetails = CaseDetails.builder()
            .data(of("caseLocalAuthority", localAuthority))
            .build();

        @Test
        void shouldReturnErrorWhenCaseSubmissionIsBlockedForLocalAuthority() {
            given(featureToggleService.isRestrictedFromCaseSubmission(localAuthority)).willReturn(true);

            AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);

            assertThat(callbackResponse.getData()).containsEntry("caseLocalAuthority", localAuthority);
            assertThat(callbackResponse.getErrors()).contains("You cannot submit this application online yet."
                + " Ask your FPL administrator for your local authority’s enrolment date");
        }

        @Test
        void shouldReturnNoErrorsWhenCaseSubmissionIsAllowedForLocalAuthority() {
            given(featureToggleService.isRestrictedFromCaseSubmission(localAuthority)).willReturn(false);

            AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);

            assertThat(callbackResponse.getData()).containsEntry("caseLocalAuthority", localAuthority);
            assertThat(callbackResponse.getErrors()).isEmpty();
        }
    }

    @Test
    void shouldMapRespondentsToRespondentSolicitorOrganisationsWhenHasRSOCaseAccessIsToggledOn() {
        UUID respondentElementOneId = UUID.randomUUID();
        UUID respondentElementTwoId = UUID.randomUUID();

        RespondentParty respondentParty = buildRespondentParty();

        Organisation solicitorOrganisation = Organisation.builder()
            .organisationName("Summers Inc")
            .organisationID("12345")
            .build();

        RespondentSolicitor respondentSolicitor = RespondentSolicitor.builder()
            .firstName("Ben")
            .lastName("Summers")
            .email("bensummers@gmail.com")
            .organisation(solicitorOrganisation)
            .build();

        Respondent respondentOne = Respondent.builder()
            .party(respondentParty)
            .legalRepresentation("Yes")
            .solicitor(respondentSolicitor)
            .build();

        Respondent respondentTwo = Respondent.builder()
            .party(respondentParty)
            .legalRepresentation("No")
            .build();

        List<Element<Respondent>> respondents = List.of(
            element(respondentElementOneId, respondentOne),
            element(respondentElementTwoId, respondentTwo));

        Map<String, Object> data = new HashMap<>();
        data.put("caseLocalAuthority", LOCAL_AUTHORITY_1_CODE);
        data.put("respondents1", respondents);

        given(featureToggleService.hasRSOCaseAccess()).willReturn(true);

        CaseDetails caseDetails = CaseDetails.builder().data(data).build();
        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);
        CaseData updatedCaseData = mapper.convertValue(callbackResponse.getData(), CaseData.class);

        RespondentSolicitorOrganisation expectedRespondentOne = RespondentSolicitorOrganisation.builder()
            .respondentId(respondentElementOneId)
            .party(respondentParty)
            .solicitor(respondentSolicitor)
            .organisationPolicy(OrganisationPolicy.builder()
                .organisation(solicitorOrganisation)
                .orgPolicyCaseAssignedRole(SOLICITORA.getCaseRoleLabel())
                .build())
            .build();

        RespondentSolicitorOrganisation expectedRespondentTwo = RespondentSolicitorOrganisation.builder()
            .respondentId(respondentElementTwoId)
            .party(respondentParty)
            .organisationPolicy(OrganisationPolicy.builder()
                .orgPolicyCaseAssignedRole(SOLICITORB.getCaseRoleLabel())
                .build())
            .build();

        assertThat(updatedCaseData.getRespondents1()).isEqualTo(respondents);
        assertThat(updatedCaseData.getRespondent1()).isEqualTo(expectedRespondentOne);
        assertThat(updatedCaseData.getRespondent2()).isEqualTo(expectedRespondentTwo);
        assertThat(updatedCaseData.getRespondent3()).isNull();
    }

    @Test
    void shouldNotMapRespondentsToRespondentSolicitorOrganisationsWhenHasRSOCaseAccessIsToggledOff() {
        UUID respondentElementOneId = UUID.randomUUID();
        UUID respondentElementTwoId = UUID.randomUUID();

        RespondentParty respondentParty = buildRespondentParty();

        Organisation solicitorOrganisation = Organisation.builder()
            .organisationName("Summers Inc")
            .organisationID("12345")
            .build();

        RespondentSolicitor respondentSolicitor = RespondentSolicitor.builder()
            .firstName("Ben")
            .lastName("Summers")
            .email("bensummers@gmail.com")
            .organisation(solicitorOrganisation)
            .build();

        Respondent respondentOne = Respondent.builder()
            .party(respondentParty)
            .legalRepresentation("Yes")
            .solicitor(respondentSolicitor)
            .build();

        Respondent respondentTwo = Respondent.builder()
            .party(respondentParty)
            .legalRepresentation("No")
            .build();

        List<Element<Respondent>> respondents = List.of(
            element(respondentElementOneId, respondentOne),
            element(respondentElementTwoId, respondentTwo));

        Map<String, Object> data = new HashMap<>();
        data.put("caseLocalAuthority", LOCAL_AUTHORITY_1_CODE);
        data.put("respondents1", respondents);

        given(featureToggleService.hasRSOCaseAccess()).willReturn(false);

        CaseDetails caseDetails = CaseDetails.builder().data(data).build();
        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);
        CaseData updatedCaseData = mapper.convertValue(callbackResponse.getData(), CaseData.class);

        assertThat(updatedCaseData.getRespondents1()).isEqualTo(respondents);
        assertThat(updatedCaseData.getRespondent1()).isNull();
        assertThat(updatedCaseData.getRespondent2()).isNull();
        assertThat(updatedCaseData.getRespondent3()).isNull();
    }

    private RespondentParty buildRespondentParty() {
        return RespondentParty.builder()
            .firstName("Joe")
            .lastName("Bloggs")
            .relationshipToChild("Father")
            .dateOfBirth(LocalDate.now())
            .telephoneNumber(Telephone.builder()
                .contactDirection("By telephone")
                .telephoneNumber("02838882333")
                .telephoneUsageType("Personal home number")
                .build())
            .gender("Male")
            .placeOfBirth("Newry")
            .build();
    }
}
