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
        // Extract All types of documents
        Document checklistDocument = caseData.getChecklistDocument();
        Document thresholdDocument = caseData.getThresholdDocument();
        Document socialWorkStatementDocument = caseData.getSocialWorkStatementDocument();
        Document socialWorkChronologyDocument = caseData.getSocialWorkChronologyDocument();
        Document socialWorkCarePlanDocument = caseData.getSocialWorkCarePlanDocument();
        Document socialWorkEvidenceTemplateDocument = caseData.getSocialWorkEvidenceTemplateDocument();
        Document socialWorkAssessmentDocument = caseData.getSocialWorkAssessmentDocument();

        List<Element<ApplicationDocument>> applicationDocuments = caseData.getDocuments();
        for (Element<ApplicationDocument> applicationDocument : applicationDocuments) {
            switch(applicationDocument.getValue().getDocumentType()) {
                case CHECKLIST_DOCUMENT:
                    convertOldDocumentToApplicationDocument(applicationDocument.getValue(),checklistDocument);
                case THRESHOLD:
                    convertOldDocumentToApplicationDocument(applicationDocument.getValue(),thresholdDocument);
                case SOCIAL_WORK_STATEMENT:
                    convertOldDocumentToApplicationDocument(applicationDocument.getValue(),socialWorkStatementDocument);
                case SOCIAL_WORK_CHRONOLOGY:
                    convertOldDocumentToApplicationDocument(applicationDocument.getValue(),
                                                                            socialWorkChronologyDocument);
                case CARE_PLAN:
                    convertOldDocumentToApplicationDocument(applicationDocument.getValue(),socialWorkCarePlanDocument);
                case SWET:
                    convertOldDocumentToApplicationDocument(applicationDocument.getValue(),
                                                                            socialWorkEvidenceTemplateDocument);
            }
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    private ApplicationDocument convertOldDocumentToApplicationDocument(ApplicationDocument applicationDocument,
                                                                                    Document document) {
        applicationDocument.setDateTimeUploaded(document.getDateTimeUploaded());
        applicationDocument.setDocumentName(document.getTypeOfDocument().getFilename());
        applicationDocument.setUploadedBy(document.getUploadedBy());

        return applicationDocument;
    }
}
