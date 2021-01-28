package uk.gov.hmcts.reform.fpl.controllers;

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
import uk.gov.hmcts.reform.fpl.model.CaseNote;

import java.util.List;

import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;

@Api
@RestController
@RequestMapping("/callback/test-event")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TestController extends CallbackController {

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(
        @RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();

        caseDetails.getData().remove("standardDirectionOrder");
        //caseDetails.getData().put("testCollection", wrapElements(CaseNote.builder().note("Not good!").build()));

        return respond(caseDetails);
    }
}
