package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.DocumentType;
import uk.gov.hmcts.reform.fpl.model.ApplicationDocument;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.common.C2DocumentBundle;
import uk.gov.hmcts.reform.fpl.model.common.Document;
import uk.gov.hmcts.reform.fpl.model.common.DocumentSocialWorkOther;
import uk.gov.hmcts.reform.fpl.model.common.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;

@Api
@RestController
@RequestMapping("/callback/migrate-case")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MigrateCaseController {

    private final ObjectMapper mapper;

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        Map<String, Object> data = caseDetails.getData();
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        //Document socialWorkAssessmentDocument = caseData.getSocialWorkAssessmentDocument();
        //Document otherSocialWorkDocument = caseData.getOtherSocialWorkDocuments();
        if (1606492119106271L == caseDetails.getId()) {
            processCaseDataAndExtractOldDocuments(caseData);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    private void processCaseDataAndExtractOldDocuments(CaseData caseData) {
        // Extract All Old documents
        Document checklistDocument = caseData.getChecklistDocument();
        Document thresholdDocument = caseData.getThresholdDocument();
        Document socialWorkStatementDocument = caseData.getSocialWorkStatementDocument();
        Document socialWorkChronologyDocument = caseData.getSocialWorkChronologyDocument();
        Document socialWorkCarePlanDocument = caseData.getSocialWorkCarePlanDocument();
        Document socialWorkEvidenceTemplateDocument = caseData.getSocialWorkEvidenceTemplateDocument();
        List<Element<DocumentSocialWorkOther>> otherSocialWorkDocuments =   caseData.getOtherSocialWorkDocuments();


        if (checklistDocument != null) {
            caseData.setDocuments(convertOldDocumentsToNewApplicationDocuments(checklistDocument,
                                                                                DocumentType.CHECKLIST_DOCUMENT));

        }
        if (thresholdDocument != null) {
            caseData.setDocuments(convertOldDocumentsToNewApplicationDocuments(thresholdDocument,
                                                                                DocumentType.THRESHOLD));
        }
        if (socialWorkStatementDocument != null) {
            caseData.setDocuments(convertOldDocumentsToNewApplicationDocuments(socialWorkStatementDocument,
                                                                                DocumentType.SOCIAL_WORK_STATEMENT));
        }
        if (socialWorkChronologyDocument != null) {
            caseData.setDocuments(convertOldDocumentsToNewApplicationDocuments(socialWorkChronologyDocument,
                                                                                DocumentType.SOCIAL_WORK_CHRONOLOGY));
        }
        if (socialWorkCarePlanDocument != null) {
            caseData.setDocuments(convertOldDocumentsToNewApplicationDocuments(socialWorkCarePlanDocument,
                                                                                DocumentType.CARE_PLAN));
        }
        if (socialWorkEvidenceTemplateDocument != null) {
            caseData.setDocuments(convertOldDocumentsToNewApplicationDocuments(socialWorkEvidenceTemplateDocument,
                                                                                DocumentType.SWET));
        }



    }

    private List<Element<ApplicationDocument>> convertOldDocumentsToNewApplicationDocuments(Document document,
                                                                             DocumentType documentType) {
        List<Element<ApplicationDocument>> caseDataDocuments = new ArrayList<>();
        List<ApplicationDocument> applicationDocuments = new ArrayList<>();

        ApplicationDocument applicationDocument = new ApplicationDocument(
            document.getTypeOfDocument(),
            documentType,
        document.getDateTimeUploaded(),
            document.getUploadedBy(),
        document.getTypeOfDocument().getFilename(),
        "includedInSWET");
        applicationDocuments.add(applicationDocument);
        caseDataDocuments.add(element(applicationDocument));
        return caseDataDocuments;
    }
}
