package uk.gov.hmcts.reform.fpl.controllers;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fnp.client.PaymentApi;
import uk.gov.hmcts.reform.fnp.model.payment.CreditAccountPaymentRequest;
import uk.gov.hmcts.reform.fpl.request.RequestData;
import uk.gov.hmcts.reform.fpl.service.LocalAuthorityService;
import uk.gov.hmcts.reform.fpl.service.LocalAuthorityUserService;
import uk.gov.hmcts.reform.fpl.service.payment.PaymentService;

import java.util.Map;

@Api
@RestController
@RequestMapping("/callback/case-initiation")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class CaseInitiationController {
    private final LocalAuthorityService localAuthorityNameService;
    private final LocalAuthorityUserService localAuthorityUserService;
    private final PaymentApi paymentApi;
    private final RequestData requestData;
    private final AuthTokenGenerator authTokenGenerator;
    private final PaymentService service;

    @GetMapping("/test")
    public void test() throws InterruptedException {
      log.info("testing info from local");
      log.error("testing error from local");


    service.callPaymentsApi(CreditAccountPaymentRequest.builder().build());

      Thread.sleep(1000L);
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmitEvent(
        @RequestBody CallbackRequest callbackrequest) {
        String caseLocalAuthority = localAuthorityNameService.getLocalAuthorityCode();
        CaseDetails caseDetails = callbackrequest.getCaseDetails();

        Map<String, Object> data = caseDetails.getData();
        data.put("caseLocalAuthority", caseLocalAuthority);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    @PostMapping("/submitted")
    public void handleSubmittedEvent(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        String caseId = Long.toString(caseDetails.getId());
        String caseLocalAuthority = (String) caseDetails.getData()
            .get("caseLocalAuthority");

        localAuthorityUserService.grantUserAccessWithCaseRole(caseId, caseLocalAuthority);
    }
}
