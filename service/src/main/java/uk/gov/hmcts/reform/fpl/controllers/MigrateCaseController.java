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
import uk.gov.hmcts.reform.fpl.model.common.C2DocumentBundle;
import uk.gov.hmcts.reform.fpl.model.common.Document;
import uk.gov.hmcts.reform.fpl.model.common.Element;

import java.util.List;
import java.util.Map;

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
        if (checklistDocument != null) {
            convertOldDocumentsToNewApplicationDocuments(checklistDocument, DocumentType.CHECKLIST_DOCUMENT);
        }
        if (thresholdDocument != null) {
            convertOldDocumentsToNewApplicationDocuments(thresholdDocument, DocumentType.THRESHOLD);
        }
        if (socialWorkStatementDocument != null) {
            convertOldDocumentsToNewApplicationDocuments(socialWorkStatementDocument, DocumentType.SOCIAL_WORK_STATEMENT);
        }
        if (socialWorkChronologyDocument != null) {
            convertOldDocumentsToNewApplicationDocuments(socialWorkChronologyDocument, DocumentType.SOCIAL_WORK_CHRONOLOGY);
        }
        if (socialWorkCarePlanDocument != null) {
            convertOldDocumentsToNewApplicationDocuments(socialWorkCarePlanDocument, DocumentType.CARE_PLAN);
        }
        if (socialWorkEvidenceTemplateDocument != null) {
            convertOldDocumentsToNewApplicationDocuments(socialWorkEvidenceTemplateDocument, DocumentType.SWET);
        }

    }

    private ApplicationDocument convertOldDocumentsToNewApplicationDocuments(Document document, DocumentType documentType) {

        ApplicationDocument applicationDocument = new ApplicationDocument(
            document.getTypeOfDocument(),
            documentType,
        document.getDateTimeUploaded(),
            document.getUploadedBy(),
        document.getTypeOfDocument().getFilename(),
        "includedInSWET");
        return applicationDocument;

    }
}
