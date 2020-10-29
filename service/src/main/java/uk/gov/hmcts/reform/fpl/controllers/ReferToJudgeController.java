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
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.JudgeNote;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.request.RequestData;
import uk.gov.hmcts.reform.fpl.service.CaseNoteService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.asDynamicList;

@Api
@RestController
@RequestMapping("/callback/refer-to-judge")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReferToJudgeController extends CallbackController {

    private final CaseNoteService service;
    private final RequestData requestData;

    @PostMapping("/about-to-start")
    public CallbackResponse handleAboutToStart(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        // Could filter these by ones which don't have a response yet and only show them in list
        List<Element<JudgeNote>> judgeReferralNotes = caseData.getJudgeNotes();

        AtomicInteger counter = new AtomicInteger(0);

        caseDetails.getData().put("judgeReferralNoteList",
                asDynamicList(judgeReferralNotes, judgeNote ->
                    judgeNote.toLabel(counter.incrementAndGet())));

        return respond(caseDetails);
    }

    @PostMapping("/mid-event")
    public CallbackResponse handleMidEvent(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        System.out.println("I am in the mid event");
        return respond(caseDetails);
    }


    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        JudgeNote caseNote = service.buildCaseNoteForJudge(requestData.authorisation(), caseData.getJudgeNote(), caseData.getJudgeEmailForReferral());
        List<Element<JudgeNote>> caseNotes = service.addJudgeNoteToList(caseNote, caseData.getJudgeNotes());

        caseDetails.getData().put("judgeNotes", caseNotes);
        caseDetails.getData().remove("judgeNote");
        caseDetails.getData().remove("judgeEmailForReferral");

        return respond(caseDetails);
    }

}
