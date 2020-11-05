package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList2;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicListElement2;
import uk.gov.hmcts.reform.fpl.service.ConfidentialDetailsService;
import uk.gov.hmcts.reform.fpl.utils.CaseDetailsMap;
import uk.gov.hmcts.reform.fpl.utils.ElementUtils;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.fpl.enums.ConfidentialPartyType.RESPONDENT;

@Api
@RestController
@RequestMapping("/callback/manage-respondents")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ManageRespondentController extends CallbackController {
    private final ConfidentialDetailsService confidentialDetailsService;
    private final ObjectMapper mapper;

    @PostMapping("/about-to-start")
    public AboutToStartOrSubmitCallbackResponse handleAboutToStart(@RequestBody CallbackRequest callbackrequest) {
        CaseDetails caseDetails = callbackrequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);


        caseDetails.getData().put("missingRespondents", getMissingRespondents(caseData,null));
        caseDetails.getData().put("existingRespondents", getExistingRespondents(caseData, null));

        return respond(caseDetails);
    }

    @PostMapping("/mid-event")
    public AboutToStartOrSubmitCallbackResponse handleMidEvent(@RequestBody CallbackRequest callbackrequest) {
        CaseDetails caseDetails = callbackrequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);


        Respondent current = null;

        String respondentType;

        if ("NEW_RESPONDENT".equals(caseData.getRespondentOption())) {
            respondentType = getDynamicListSelectedValue(caseData.getMissingRespondents(), mapper);
        } else {
            respondentType = getDynamicListSelectedValue(caseData.getExistingRespondents(), mapper);
        }


        if("MOTHER".equals(respondentType)){
            current = caseData.getMother();
        }

        if("FATHER".equals(respondentType)){
            current = caseData.getFather();
        }

        if("CHILD1_FATHER".equals(respondentType)){
            current = caseData.getChild1Father();
        }

        if("CHILD2_FATHER".equals(respondentType)){
            current = caseData.getChild2Father();
        }

        if("CHILD3_FATHER".equals(respondentType)){
            current = caseData.getChild3Father();
        }

        if("MATERNAL_GRANDFATHER".equals(respondentType)){
            current = caseData.getMaternalGrandfather();
        }

        if("MATERNAL_GRANDMOTHER".equals(respondentType)){
            current = caseData.getMaternalGrandmother();
        }

        if("FRATERNAL_GRANDFATHER".equals(respondentType)){
            current = caseData.getFraternalGrandfather();
        }

        if("FRATERNAL_GRANDMOTHER".equals(respondentType)){
            current = caseData.getFraternalGrandmother();
        }

        if ("DELETE_RESPONDENT".equals(caseData.getRespondentOption())) {
            current = null;
        }

        caseDetails.getData().put("currentRespondent", current);

        caseDetails.getData().put("missingRespondents", getMissingRespondents(caseData, respondentType));
        caseDetails.getData().put("existingRespondents", getExistingRespondents(caseData, respondentType));

        return respond(caseDetails);
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);
        CaseDetailsMap caseDetailsMap = CaseDetailsMap.caseDetailsMap(caseDetails);

        String respondentType;

        if ("NEW_RESPONDENT".equals(caseData.getRespondentOption())) {
            respondentType = getDynamicListSelectedValue(caseData.getMissingRespondents(), mapper);
        } else {
            respondentType = getDynamicListSelectedValue(caseData.getExistingRespondents(), mapper);
        }

        Respondent res = "DELETE_RESPONDENT".equals(caseData.getRespondentOption()) ? null : caseData.getCurrentRespondent();



        if("MOTHER".equals(respondentType)){
            caseDetailsMap.putIfNotEmpty("mother", res);
        }

        if("FATHER".equals(respondentType)){
            caseDetailsMap.putIfNotEmpty("father", res);
        }

        if("CHILD1_FATHER".equals(respondentType)){
            caseDetailsMap.putIfNotEmpty("child1Father", res);
        }

        if("CHILD2_FATHER".equals(respondentType)){
            caseDetailsMap.putIfNotEmpty("child2Father", res);
        }

        if("CHILD3_FATHER".equals(respondentType)){
            caseDetailsMap.putIfNotEmpty("child3Father", res);
        }

        if("MATERNAL_GRANDFATHER".equals(respondentType)){
            caseDetailsMap.putIfNotEmpty("maternalGrandfather", res);
        }

        if("MATERNAL_GRANDMOTHER".equals(respondentType)){
            caseDetailsMap.putIfNotEmpty("maternalGrandmother", res);
        }

        if("FRATERNAL_GRANDFATHER".equals(respondentType)){
            caseDetailsMap.putIfNotEmpty("fraternalGrandfather", res);
        }

        if("FRATERNAL_GRANDMOTHER".equals(respondentType)){
            caseDetailsMap.putIfNotEmpty("fraternalGrandmother", res);
        }

        return respond(caseDetailsMap);
    }


    private DynamicList2 getMissingRespondents(CaseData caseData, String selected) {

        List<DynamicListElement2> items = new ArrayList<>();

        if (ObjectUtils.isEmpty(caseData.getMother())) {
            items.add(DynamicListElement2.builder()
                .code("MOTHER")
                .label("Mother")
                .build());
        }

        if (ObjectUtils.isEmpty(caseData.getFather())) {
            items.add(DynamicListElement2.builder()
                .code("FATHER")
                .label("Father to all children")
                .build());
        }

        if (ObjectUtils.isEmpty(caseData.getChild1Father())) {
            items.add(DynamicListElement2.builder()
                .code("CHILD1_FATHER")
                .label("Child 1 father")
                .build());
        }

        if (ObjectUtils.isEmpty(caseData.getChild2Father())) {
            items.add(DynamicListElement2.builder()
                .code("CHILD2_FATHER")
                .label("Child 2 father")
                .build());
        }

        if (ObjectUtils.isEmpty(caseData.getChild3Father())) {
            items.add(DynamicListElement2.builder()
                .code("CHILD3_FATHER")
                .label("Child 3 father")
                .build());
        }

        if (ObjectUtils.isEmpty(caseData.getMaternalGrandfather())) {
            items.add(DynamicListElement2.builder()
                .code("MATERNAL_GRANDFATHER")
                .label("Maternal grandfather")
                .build());
        }

        if (ObjectUtils.isEmpty(caseData.getMaternalGrandmother())) {
            items.add(DynamicListElement2.builder()
                .code("MATERNAL_GRANDMOTHER")
                .label("Maternal grandmother")
                .build());
        }

        if (ObjectUtils.isEmpty(caseData.getFraternalGrandfather())) {
            items.add(DynamicListElement2.builder()
                .code("FRATERNAL_GRANDFATHER")
                .label("Fraternal grandfather")
                .build());
        }

        if (ObjectUtils.isEmpty(caseData.getFraternalGrandmother())) {
            items.add(DynamicListElement2.builder()
                .code("FRATERNAL_GRANDMOTHER")
                .label("Fraternal grandmother")
                .build());
        }

        DynamicListElement2 selectedItem;

        if(ObjectUtils.isEmpty(selected)){
            selectedItem = DynamicListElement2.EMPTY;
        }else{
            selectedItem = DynamicListElement2.builder()
                .code(selected)
                .build();
        }

        return DynamicList2.builder()
            .listItems(items)
            .value(selectedItem)
            .build();
    }


    private DynamicList2 getExistingRespondents(CaseData caseData, String selected) {

        List<DynamicListElement2> items = new ArrayList<>();

        if (ObjectUtils.isNotEmpty(caseData.getMother())) {
            items.add(DynamicListElement2.builder()
                .code("MOTHER")
                .label("Mother - " + getName(caseData.getMother()))
                .build());
        }

        if (ObjectUtils.isNotEmpty(caseData.getFather())) {
            items.add(DynamicListElement2.builder()
                .code("FATHER")
                .label("Father - " + getName(caseData.getFather()))
                .build());
        }

        if (ObjectUtils.isNotEmpty(caseData.getChild1Father())) {
            items.add(DynamicListElement2.builder()
                .code("CHILD1_FATHER")
                .label("Child 1 father - " + getName(caseData.getChild1Father()))
                .build());
        }

        if (ObjectUtils.isNotEmpty(caseData.getChild2Father())) {
            items.add(DynamicListElement2.builder()
                .code("CHILD2_FATHER")
                .label("Child 2 father - " + getName(caseData.getChild2Father()))
                .build());
        }

        if (ObjectUtils.isNotEmpty(caseData.getChild3Father())) {
            items.add(DynamicListElement2.builder()
                .code("CHILD3_FATHER")
                .label("Child 3 father - " + getName(caseData.getChild3Father()))
                .build());
        }

        if (ObjectUtils.isNotEmpty(caseData.getMaternalGrandfather())) {
            items.add(DynamicListElement2.builder()
                .code("MATERNAL_GRANDFATHER")
                .label("Maternal grandfather - " + getName(caseData.getMaternalGrandfather()))
                .build());
        }

        if (ObjectUtils.isNotEmpty(caseData.getMaternalGrandmother())) {
            items.add(DynamicListElement2.builder()
                .code("MATERNAL_GRANDMOTHER")
                .label("Maternal grandmother - " + getName(caseData.getMaternalGrandmother()))
                .build());
        }

        if (ObjectUtils.isNotEmpty(caseData.getFraternalGrandfather())) {
            items.add(DynamicListElement2.builder()
                .code("FRATERNAL_GRANDFATHER")
                .label("Fraternal grandfather - " + getName(caseData.getFraternalGrandfather()))
                .build());
        }

        if (ObjectUtils.isNotEmpty(caseData.getFraternalGrandmother())) {
            items.add(DynamicListElement2.builder()
                .code("FRATERNAL_GRANDMOTHER")
                .label("Fraternal grandmother - " + caseData.getFraternalGrandmother())
                .build());
        }

        DynamicListElement2 selectedItem = DynamicListElement2.EMPTY;

        if(ObjectUtils.isEmpty(selected)){
            selectedItem = DynamicListElement2.EMPTY;
        }else{
            selectedItem = DynamicListElement2.builder()
                .code(selected)
                .build();
        }


        return DynamicList2.builder()
            .listItems(items)
            .value(selectedItem)
            .build();
    }

    private String getName(Respondent respondent) {
        return respondent.getParty().getFirstName() + " " + respondent.getParty().getLastName();
    }


    public String getDynamicListSelectedValue(Object dynamicList, ObjectMapper mapper) {
        if (dynamicList instanceof String) {
            return (String) dynamicList;
        }

        return mapper.convertValue(dynamicList, DynamicList2.class).getValueCode();
    }
}
