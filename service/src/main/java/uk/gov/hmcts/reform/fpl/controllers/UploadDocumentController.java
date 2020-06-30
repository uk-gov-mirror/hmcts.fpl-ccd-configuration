package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.SecuredDocument;
import uk.gov.hmcts.reform.fpl.service.DocumentSealingService;
import uk.gov.hmcts.reform.fpl.service.DocumentsValidatorService;

import java.util.ArrayList;
import java.util.List;

@Api
@RestController
@RequestMapping("/callback/upload-document")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UploadDocumentController {
    private final ObjectMapper objectMapper;
    private final DocumentsValidatorService documentsValidatorService;
    private final DocumentSealingService documentSealingService;


    @PostMapping("/mid-event")
    public AboutToStartOrSubmitCallbackResponse handleMidEvent(@RequestBody CallbackRequest callbackrequest) {
        CaseDetails caseDetails = callbackrequest.getCaseDetails();
        CaseData caseData = objectMapper.convertValue(caseDetails.getData(), CaseData.class);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(callbackrequest.getCaseDetails().getData())
            .errors(documentsValidatorService.validateDocuments(caseData))
            .build();
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse submit(@RequestBody CallbackRequest callbackrequest) throws Exception {
        CaseDetails caseDetails = callbackrequest.getCaseDetails();
        CaseData caseData = objectMapper.convertValue(caseDetails.getData(), CaseData.class);

        List<String> errors = new ArrayList<>();

        SecuredDocument document1 = caseData.getDocument1();
        DocumentReference document2 = caseData.getDocument2();

        if (document1.getTypeOfDocument() != null) {
            try {
                DocumentReference d1 = documentSealingService.sealDocument(document1.getTypeOfDocument(), document1.getDocumentPassword());
                callbackrequest.getCaseDetails().getData().put("document1", SecuredDocument.builder().typeOfDocument(d1).build());
            } catch (InvalidPasswordException e) {
                errors.add("Password is incorrect for document 1");
            }
        }

        if (document2 != null) {
            try {
                DocumentReference d2 = documentSealingService.sealDocument(document2);
                callbackrequest.getCaseDetails().getData().put("document2", d2);
            } catch (InvalidPasswordException e) {
                errors.add("Document 2 is encoded. Please provide not secured pdf");
            }
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(callbackrequest.getCaseDetails().getData())
            .errors(errors)
            .build();
    }
}
