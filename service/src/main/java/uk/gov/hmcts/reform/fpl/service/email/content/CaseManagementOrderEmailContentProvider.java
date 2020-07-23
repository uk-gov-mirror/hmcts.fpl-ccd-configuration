package uk.gov.hmcts.reform.fpl.service.email.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.notify.allocatedjudge.AllocatedJudgeTemplateForCMO;
import uk.gov.hmcts.reform.fpl.model.notify.cmo.IssuedCMOTemplate;
import uk.gov.hmcts.reform.fpl.model.notify.cmo.RejectedCMOTemplate;
import uk.gov.hmcts.reform.fpl.model.order.CaseManagementOrder;
import uk.gov.hmcts.reform.fpl.service.email.content.base.AbstractEmailContentProvider;
import uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper;

import java.util.Map;

import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.DIGITAL_SERVICE;
import static uk.gov.hmcts.reform.fpl.utils.PeopleInCaseHelper.getFirstRespondentLastName;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CaseManagementOrderEmailContentProvider extends AbstractEmailContentProvider {

    private final EmailNotificationHelper emailNotificationHelper;
    private final ObjectMapper mapper;

    private static final String CASE_URL = "caseUrl";
    private static final String SUBJECT_LINE = "subjectLineWithHearingDate";

    public Map<String, Object> buildCMOIssuedCaseLinkNotificationParameters(final CaseDetails caseDetails,
                                                                            final String recipientName) {
        return ImmutableMap.<String, Object>builder()
            .putAll(buildCommonCMONotificationParameters(caseDetails))
            .put("localAuthorityNameOrRepresentativeFullName", recipientName)
            .build();
    }

    public IssuedCMOTemplate buildCMOIssuedNotificationParameters(
        final CaseDetails caseDetails,
        CaseManagementOrder cmo,
        RepresentativeServingPreferences servingPreference) {
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        IssuedCMOTemplate template = new IssuedCMOTemplate();

        template.setRespondentLastName(getFirstRespondentLastName(caseData.getRespondents1()));
        template.setFamilyManCaseNumber(caseData.getFamilyManCaseNumber());
        template.setHearing(cmo.getHearing());
        template.setDigitalPreference(hasDigitalServingPreference(servingPreference) ? "Yes" : "No");
        template.setDocumentLink(linkToAttachedDocument(cmo.getOrder()));
        template.setCaseUrl((hasDigitalServingPreference(servingPreference) ? getCaseUrl(caseDetails.getId()) : ""));

        return template;
    }

    public RejectedCMOTemplate buildCMORejectedByJudgeNotificationParameters(final CaseDetails caseDetails,
                                                                             CaseManagementOrder cmo) {
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);
        RejectedCMOTemplate template = new RejectedCMOTemplate();

        template.setRespondentLastName(getFirstRespondentLastName(caseData.getRespondents1()));
        template.setFamilyManCaseNumber(caseData.getFamilyManCaseNumber());
        template.setHearing(cmo.getHearing());
        template.setDocumentLink(linkToAttachedDocument(cmo.getOrder()));
        template.setCaseUrl(getCaseUrl(caseDetails.getId()));

        return template;
    }

    public Map<String, Object> buildCMOPartyReviewParameters(final CaseDetails caseDetails,
                                                             byte[] documentContents,
                                                             RepresentativeServingPreferences servingPreference) {
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        return ImmutableMap.<String, Object>builder()
            .put(SUBJECT_LINE, emailNotificationHelper
                .buildSubjectLineWithHearingBookingDateSuffix(caseData,
                    caseData.getHearingDetails()))
            .put("respondentLastName", getFirstRespondentLastName(caseData.getRespondents1()))
            .put("digitalPreference", servingPreference == DIGITAL_SERVICE ? "Yes" : "No")
            .put(CASE_URL, servingPreference == DIGITAL_SERVICE ? getCaseUrl(caseDetails.getId()) : "")
            //.putAll(linkToAttachedDocument(documentContents)) - this method will be deleted with deprecated
            .build();
    }

    public AllocatedJudgeTemplateForCMO buildCMOReadyForJudgeReviewNotificationParameters(
        final CaseDetails caseDetails) {
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);
        Map<String, Object> commonCMONotificationParameters = buildCommonCMONotificationParameters(caseDetails);

        AllocatedJudgeTemplateForCMO allocatedJudgeTemplate
            = new AllocatedJudgeTemplateForCMO();
        allocatedJudgeTemplate.setSubjectLineWithHearingDate(commonCMONotificationParameters
            .get(SUBJECT_LINE)
            .toString());
        allocatedJudgeTemplate.setCaseUrl(commonCMONotificationParameters.get(CASE_URL).toString());
        allocatedJudgeTemplate.setReference(commonCMONotificationParameters.get("reference").toString());
        allocatedJudgeTemplate.setRespondentLastName(getFirstRespondentLastName(caseData.getRespondents1()));
        allocatedJudgeTemplate.setJudgeTitle(caseData.getAllocatedJudge().getJudgeOrMagistrateTitle());
        allocatedJudgeTemplate.setJudgeName(caseData.getAllocatedJudge().getJudgeName());

        return allocatedJudgeTemplate;
    }

    private Map<String, Object> buildCommonCMONotificationParameters(final CaseDetails caseDetails) {
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        return ImmutableMap.of(
            SUBJECT_LINE, emailNotificationHelper
                .buildSubjectLineWithHearingBookingDateSuffix(caseData,
                    caseData.getHearingDetails()),
            "reference", String.valueOf(caseDetails.getId()),
            CASE_URL, getCaseUrl(caseDetails.getId())
        );
    }

    private boolean hasDigitalServingPreference(RepresentativeServingPreferences servingPreference) {
        return servingPreference == DIGITAL_SERVICE;
    }
}
