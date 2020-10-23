package uk.gov.hmcts.reform.fpl.controllers.cmo;

import com.fasterxml.jackson.core.type.TypeReference;
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
import uk.gov.hmcts.reform.fpl.controllers.CallbackController;
import uk.gov.hmcts.reform.fpl.events.cmo.UploadCMOEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.HearingFurtherEvidenceBundle;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.event.UploadCMOEventData;
import uk.gov.hmcts.reform.fpl.model.order.CaseManagementOrder;
import uk.gov.hmcts.reform.fpl.service.FeatureToggleService;
import uk.gov.hmcts.reform.fpl.service.cmo.UploadCMOService;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static uk.gov.hmcts.reform.fpl.utils.CaseDetailsHelper.removeTemporaryFields;

@Api
@RestController
@RequestMapping("/callback/upload-cmo")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class UploadCMOController extends CallbackController {

    private final UploadCMOService service;
    private final ObjectMapper mapper;
    private final FeatureToggleService featureToggleService;

    @PostMapping("/about-to-start")
    public AboutToStartOrSubmitCallbackResponse handleAboutToStart(@RequestBody CallbackRequest request) {
        CaseDetails caseDetails = request.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        UploadCMOEventData pageData;
        if (featureToggleService.isUploadDraftCMOEnabled()) {
            pageData = service.getInitialPageData(caseData);
        } else {
            pageData = service.getInitialPageData(caseData.getPastHearings(), caseData.getDraftUploadedCMOs());
        }

        caseDetails.getData().putAll(mapper.convertValue(pageData, new TypeReference<>() {}));

        return respond(caseDetails);
    }

    @PostMapping("/populate-cmo-info/mid-event")
    public CallbackResponse handlePopulateCmoInfo(@RequestBody CallbackRequest request) {
        CaseDetails caseDetails = request.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        caseDetails.getData().putAll(mapper.convertValue(service.getCMOInfo(caseData), new TypeReference<>() {}));

        return respond(caseDetails);
    }

    @PostMapping("/review-info/mid-event")
    public CallbackResponse handleReviewCMO(@RequestBody CallbackRequest request) {
        CaseDetails caseDetails = request.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        caseDetails.getData().putAll(mapper.convertValue(service.getReviewData(caseData), new TypeReference<>() {}));

        return respond(caseDetails);
    }

    @Deprecated // TODO: 19/10/2020 Delete when FPLA-2019 is toggled on
    @PostMapping("/mid-event")
    public AboutToStartOrSubmitCallbackResponse handleMidEvent(@RequestBody CallbackRequest request) {
        CaseDetails caseDetails = request.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);
        UploadCMOEventData eventData = caseData.getUploadCMOEventData();

        // handle document fields being set to null when clicking previous button
        DocumentReference uploadedCMO = eventData.getUploadedCaseManagementOrder();
        if (uploadedCMO != null && uploadedCMO.isEmpty()) {
            caseDetails.getData().remove("uploadedCaseManagementOrder");
        }

        // update judge and hearing labels
        UploadCMOEventData judgeAndHearingDetails = service.prepareJudgeAndHearingDetails(
            eventData.getPastHearingsForCMO(), caseData.getPastHearings(), caseData.getDraftUploadedCMOs()
        );

        caseDetails.getData().putAll(mapper.convertValue(judgeAndHearingDetails, new TypeReference<>() {}));

        return respond(caseDetails);
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest request) {
        CaseDetails caseDetails = request.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        UploadCMOEventData eventData = caseData.getUploadCMOEventData();
        List<Element<CaseManagementOrder>> unsealedCMOs = caseData.getDraftUploadedCMOs();
        List<Element<HearingBooking>> hearings = defaultIfNull(caseData.getHearingDetails(), new ArrayList<>());
        List<Element<HearingFurtherEvidenceBundle>> evidenceDocuments = caseData.getHearingFurtherEvidenceDocuments();

        // previously update if not null, now update unless not enabled and not null
        // TODO: 19/10/2020 condition can be removed when FPLA-2019 toggled on
        if (featureToggleService.isUploadDraftCMOEnabled() || eventData.getUploadedCaseManagementOrder() != null) {
            service.updateHearingsAndOrders(eventData, hearings, unsealedCMOs, evidenceDocuments);
        }

        // update case data
        caseDetails.getData().put("draftUploadedCMOs", unsealedCMOs);
        caseDetails.getData().put("hearingDetails", hearings);
        caseDetails.getData().put("hearingFurtherEvidenceDocuments", evidenceDocuments);

        // remove transient fields
        removeTemporaryFields(caseDetails, UploadCMOEventData.transientFields());

        return respond(caseDetails);
    }

    @PostMapping("/submitted")
    public void handleSubmitted(@RequestBody CallbackRequest request) {
        CaseData caseDataBefore = getCaseDataBefore(request);
        CaseData caseData = getCaseData(request);

        UploadCMOEvent event = service.buildEventToPublish(caseData, caseDataBefore);

        if (event != null) {
            publishEvent(event);
        }
    }
}
