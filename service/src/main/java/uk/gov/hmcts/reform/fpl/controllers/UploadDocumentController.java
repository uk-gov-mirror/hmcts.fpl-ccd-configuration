package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.service.DocumentsValidatorService;
import uk.gov.hmcts.reform.fpl.service.docmosis.DocumentConversionService;

@Api
@RestController
@RequestMapping("/callback/upload-document")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UploadDocumentController {
    private final ObjectMapper objectMapper;
    private final DocumentsValidatorService documentsValidatorService;
    private final DocumentConversionService documentsConversionService;


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
    public AboutToStartOrSubmitCallbackResponse submit(@RequestBody CallbackRequest callbackrequest) {
        CaseDetails caseDetails = callbackrequest.getCaseDetails();
        CaseData caseData = objectMapper.convertValue(caseDetails.getData(), CaseData.class);

        caseDetails.getData().put("documentY", documentsConversionService.convertDocument(caseData.getDocumentX()));
        caseDetails.getData().remove("documentX");
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(callbackrequest.getCaseDetails().getData())
                .build();
    }
}
