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
import uk.gov.hmcts.reform.fpl.model.CaseNote;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.request.RequestData;
import uk.gov.hmcts.reform.fpl.service.CaseNoteService;

import java.util.List;

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
//
//        Judge allocatedJudge = caseData.getAllocatedJudge();
//
//        JudgeAndLegalAdvisor migratedAllocatedJudge = JudgeAndLegalAdvisor.builder()
//                .judgeTitle(allocatedJudge.getJudgeTitle())
//                .otherTitle(allocatedJudge.getOtherTitle())
//                .judgeLastName(allocatedJudge.getJudgeLastName())
//                .judgeFullName(allocatedJudge.getJudgeFullName())
//                .judgeEmailAddress(allocatedJudge.getJudgeEmailAddress())
//                .build();
//
//        List<Element<JudgeAndLegalAdvisor>> judgeList = List.of(element(migratedAllocatedJudge));
//
//        caseDetails.getData().put("referToJudgeList",
//                asDynamicList(judgeList, judgeAndLegalAdvisor ->
//                    judgeAndLegalAdvisor.toLabel()));
//        caseDetails.getData().put("hasExistingHearings", YES.getValue());


        return respond(caseDetails);
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        CaseNote caseNote = service.buildCaseNote(requestData.authorisation(), caseData.getCaseNote());
        List<Element<CaseNote>> caseNotes = service.addNoteToList(caseNote, caseData.getCaseNotes());

        caseDetails.getData().put("judgeNotes", caseNotes);
        caseDetails.getData().remove("judgeNote");

        return respond(caseDetails);
    }

}
