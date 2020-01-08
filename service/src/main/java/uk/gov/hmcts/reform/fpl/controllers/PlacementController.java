package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.Placement;
import uk.gov.hmcts.reform.fpl.model.common.Document;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicListElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Api
@RestController
@RequestMapping("/callback/placement")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PlacementController {

    private final ObjectMapper mapper;

    @PostMapping("/about-to-start")
    public AboutToStartOrSubmitCallbackResponse handleAboutToStart(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        caseDetails.getData().put("childrenList", getChildrenDynamicList(caseData, null));

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

    @PostMapping("/mid-event")
    public AboutToStartOrSubmitCallbackResponse handleMidEvent(
        @RequestHeader(value = "authorization") String authorisation,
        @RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();

        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        //If ccd bug still exists
        if(caseDetails.getData().get("childrenList") instanceof String){
            UUID childId = UUID.fromString((String) caseDetails.getData().get("childrenList"));

            Element<Child> child = caseData.getChildren1().stream()
                .filter(e -> e.getId().equals(childId))
                .findFirst()
                .orElse(null);

            caseDetails.getData().put("childrenList", getChildrenDynamicList(caseData, child));

            Placement placement= caseData.getPlacement(childId).orElse(Placement.builder().childId(childId).childName(child.getValue().getParty().getFullName()).build());

            caseDetails.getData().put("placement", placement);
        }
        //if ccd bug fixed
        else {
            UUID childId = UUID.fromString((String)((Map)((Map)callbackRequest.getCaseDetailsBefore().getData().get("childrenList")).get("value")).get("code"));

            Element<Child> child = caseData.getChildren1().stream()
                .filter(e -> e.getId().equals(childId))
                .findFirst()
                .orElse(null);

            caseDetails.getData().put("placement", String.format("%s (%s)", child.getValue().getParty().getFullName(), childId));
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSave(
        @RequestHeader(value = "authorization") String authorisation,
        @RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        Placement placement = mapper.convertValue(caseDetails.getData().get("placement"), Placement.class);

        caseData.addOrUpdatePlacement(placement);

        caseDetails.getData().put("placements", caseData.getPlacements());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

    private DynamicList getChildrenDynamicList(CaseData caseData, Element<Child> selectedChild) {
        DynamicListElement child = Optional.ofNullable(selectedChild)
            .map(this::asDynamicListElement)
            .orElse(DynamicListElement.EMPTY);

        List<DynamicListElement> children = caseData.getChildren1().stream()
            .map(this::asDynamicListElement)
            .collect(Collectors.toList());

        return DynamicList.builder().listItems(children).value(child).build();
    }

    private DynamicListElement asDynamicListElement(Element<Child> child) {
        return DynamicListElement.builder()
            .code(child.getId())
            .label(child.getValue().getParty().getFullName())
            .build();
    }


}
