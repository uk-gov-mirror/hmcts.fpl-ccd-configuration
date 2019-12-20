package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.CaseManagementOrder;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.OrderAction;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.service.CMODocmosisTemplateDataGenerationService;
import uk.gov.hmcts.reform.fpl.service.CaseManagementOrderService;
import uk.gov.hmcts.reform.fpl.service.DocmosisDocumentGeneratorService;
import uk.gov.hmcts.reform.fpl.service.DraftCMOService;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;
import uk.gov.hmcts.reform.fpl.service.ccd.CoreCaseDataService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.fpl.enums.ActionType.SEND_TO_ALL_PARTIES;
import static uk.gov.hmcts.reform.fpl.enums.CaseManagementOrderErrorMessages.HEARING_NOT_COMPLETED;
import static uk.gov.hmcts.reform.fpl.enums.CaseManagementOrderKeys.CASE_MANAGEMENT_ORDER_JUDICIARY;
import static uk.gov.hmcts.reform.fpl.enums.CaseManagementOrderKeys.NEXT_HEARING_DATE_LIST;
import static uk.gov.hmcts.reform.fpl.enums.CaseManagementOrderKeys.ORDER_ACTION;
import static uk.gov.hmcts.reform.fpl.enums.Event.ACTION_CASE_MANAGEMENT_ORDER;
import static uk.gov.hmcts.reform.fpl.model.common.DocumentReference.buildFromDocument;

@Api
@RestController
@RequestMapping("/callback/action-cmo")
public class ActionCaseManagementOrderController {
    private final DraftCMOService draftCMOService;
    private final CaseManagementOrderService caseManagementOrderService;
    private final DocmosisDocumentGeneratorService docmosisDocumentGeneratorService;
    private final UploadDocumentService uploadDocumentService;
    private final ObjectMapper mapper;
    private final CMODocmosisTemplateDataGenerationService templateDataGenerationService;
    private final CoreCaseDataService coreCaseDataService;

    public ActionCaseManagementOrderController(DraftCMOService draftCMOService,
                                               CaseManagementOrderService caseManagementOrderService,
                                               DocmosisDocumentGeneratorService docmosisDocumentGeneratorService,
                                               UploadDocumentService uploadDocumentService,
                                               ObjectMapper mapper,
                                               CMODocmosisTemplateDataGenerationService templateDataGenerationService,
                                               CoreCaseDataService coreCaseDataService) {
        this.draftCMOService = draftCMOService;
        this.caseManagementOrderService = caseManagementOrderService;
        this.docmosisDocumentGeneratorService = docmosisDocumentGeneratorService;
        this.uploadDocumentService = uploadDocumentService;
        this.mapper = mapper;
        this.templateDataGenerationService = templateDataGenerationService;
        this.coreCaseDataService = coreCaseDataService;
    }

    @PostMapping("/about-to-start")
    public AboutToStartOrSubmitCallbackResponse handleAboutToStart(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        caseDetails.getData().putAll(
            caseManagementOrderService.extractMapFieldsFromCaseManagementOrder(caseData.getCaseManagementOrder()));

        draftCMOService.prepareCustomDirections(caseDetails, caseData.getCaseManagementOrder());

        caseDetails.getData().put(NEXT_HEARING_DATE_LIST.getKey(), getHearingDynamicList(caseData.getHearingDetails()));

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

    @PostMapping("/mid-event")
    public AboutToStartOrSubmitCallbackResponse handleMidEvent(
        @RequestHeader(value = "authorization") String authorization,
        @RequestHeader(value = "user-id") String userId,
        @RequestBody CallbackRequest callbackRequest) throws IOException {

        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        Document document = getDocument(authorization, userId, caseData, true);

        caseDetails.getData()
            .put(ORDER_ACTION.getKey(), OrderAction.builder().document(buildFromDocument(document)).build());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

    //TODO: refactor. far too much logic in this controller now
    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(
        @RequestHeader(value = "authorization") String authorization,
        @RequestHeader(value = "user-id") String userId,
        @RequestBody CallbackRequest callbackRequest) throws IOException {

        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        if (sendToAllPartiesBeforeHearingDate(caseData)) {
            return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(ImmutableList.of(HEARING_NOT_COMPLETED.getValue()))
                .build();
        }

        CaseManagementOrder order = caseData.getCaseManagementOrder();

        order = draftCMOService.prepareCMO(caseData, order).toBuilder()
            .id(order.getId())
            .hearingDate(order.getHearingDate())
            .build();

        OrderAction orderAction = caseManagementOrderService.removeDocumentFromOrderAction(caseData.getOrderAction());

        order = caseManagementOrderService.addAction(order, orderAction);

        if (!order.isDraft()) {
            order = caseManagementOrderService.addNextHearingToCMO(caseData.getNextHearingDateList(), order);
        }

        Document document = getDocument(authorization, userId, caseData, order.isDraft());

        order = caseManagementOrderService.addDocument(order, document);

        caseDetails.getData().put(CASE_MANAGEMENT_ORDER_JUDICIARY.getKey(), order);

        caseDetails.getData().put("cmoEventId", ACTION_CASE_MANAGEMENT_ORDER.getId());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

    @PostMapping("/submitted")
    public void handleSubmitted(@RequestBody CallbackRequest callbackRequest) {
        coreCaseDataService.triggerEvent(
            callbackRequest.getCaseDetails().getJurisdiction(),
            callbackRequest.getCaseDetails().getCaseTypeId(),
            callbackRequest.getCaseDetails().getId(),
            "internal-change:CMO_PROGRESSION"
        );
    }

    private boolean sendToAllPartiesBeforeHearingDate(CaseData caseData) {
        return caseData.getOrderAction().getType() == SEND_TO_ALL_PARTIES
            && caseManagementOrderService.isHearingDateInFuture(caseData);
    }

    private Document getDocument(String auth, String userId, CaseData data, boolean draft) throws IOException {
        Map<String, Object> cmoDocumentTemplateData = templateDataGenerationService.getTemplateData(data, draft);

        DocmosisDocument document = docmosisDocumentGeneratorService.generateDocmosisDocument(
            cmoDocumentTemplateData, DocmosisTemplates.CMO);

        String documentTitle = (draft ? "draft-" + document.getDocumentTitle() : document.getDocumentTitle());

        return uploadDocumentService.uploadPDF(userId, auth, document.getBytes(), documentTitle);
    }

    private DynamicList getHearingDynamicList(List<Element<HearingBooking>> hearingBookings) {
        return draftCMOService.getHearingDateDynamicList(hearingBookings, null);
    }
}
