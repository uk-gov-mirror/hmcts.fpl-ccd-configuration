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
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.AddOrEditReferralNote;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.JudgeNote;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.request.RequestData;
import uk.gov.hmcts.reform.fpl.service.CaseNoteService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.asDynamicList;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.findElement;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.getDynamicListValueCode;

@Api
@RestController
@RequestMapping("/callback/refer-to-judge")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReferToJudgeController extends CallbackController {

    private final CaseNoteService service;
    private final RequestData requestData;
    private final ObjectMapper mapper;
    private final IdamClient idamClient;

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

        if(caseData.getAddOrEditReferralNote() == AddOrEditReferralNote.RESPOND_TO_NOTE) {

            UUID referralNoteId = getDynamicListValueCode(caseData.getJudgeReferralNoteList(), mapper);
            AtomicInteger counter = new AtomicInteger(0);
            List<Element<JudgeNote>> judgeReferralNotes = caseData.getJudgeNotes();

            caseDetails.getData().put("judgeReferralNoteList",
                asDynamicList(judgeReferralNotes, referralNoteId, judgeNote ->
                    judgeNote.toLabel(counter.incrementAndGet())));

            Optional<Element<JudgeNote>> judgeNoteElement = findElement(referralNoteId, caseData.getJudgeNotes());

            caseDetails.getData().put("judgeNote", judgeNoteElement.get().getValue().getNote());
            caseDetails.getData().put("judgeEmailForReferral", judgeNoteElement.get().getValue().getJudgeEmailForReferral());
            caseDetails.getData().put("judgeResponse", judgeNoteElement.get().getValue().getJudgeResponse());
            caseDetails.getData().put("pageShow", "Yes");

        }
        return respond(caseDetails);
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);
        List<Element<JudgeNote>> caseNotes;
        UserInfo userDetails = idamClient.getUserInfo(requestData.authorisation());

        if(caseData.getAddOrEditReferralNote() == AddOrEditReferralNote.RESPOND_TO_NOTE) {

         caseNotes = caseData.getJudgeNotes().stream()
            .map(Element::getValue)
            .map(judgeNote -> {
                if(judgeNote.getNote().equals(caseData.getJudgeNote())) {
                    JudgeNote modifiedNote = JudgeNote.builder()
                        .createdBy(judgeNote.getCreatedBy())
                        .date(judgeNote.getDate())
                        .judgeEmailForReferral(judgeNote.getJudgeEmailForReferral())
                        .note(judgeNote.getNote())
                        .responseBy(userDetails.getName())
                        .judgeResponse(caseData.getJudgeResponse())
                        .build();
                    return element(modifiedNote);
                } else {
                   return element(judgeNote);
                }

            }).collect(Collectors.toList());
        } else {
            JudgeNote caseNote = service.buildCaseNoteForJudge(requestData.authorisation(), caseData.getJudgeNote(), caseData.getJudgeEmailForReferral(),
                caseData.getJudgeResponse());
            caseNotes = service.addJudgeNoteToList(caseNote, caseData.getJudgeNotes());
        }

        //TO DO don't create a new note if only response is being added but add to existing element of judge notes
        caseDetails.getData().put("judgeNotes", caseNotes);
        caseDetails.getData().remove("judgeNote");
        caseDetails.getData().remove("judgeEmailForReferral");
        caseDetails.getData().remove("judgeResponse");
        caseDetails.getData().put("pageShow", "No");

        return respond(caseDetails);
    }

}
