package uk.gov.hmcts.reform.fpl.controllers;

import com.google.common.collect.ImmutableList;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.OrderType;
import uk.gov.hmcts.reform.fpl.events.CaseDataChanged;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Api
@RestController
@RequestMapping("/callback/orders-needed")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrdersNeededAboutToSubmitCallbackController {

    private final ApplicationEventPublisher applicationEventPublisher;

    @PostMapping("/about-to-submit")
    @SuppressWarnings("unchecked")
    public AboutToStartOrSubmitCallbackResponse handleAboutToStartEvent(
        @RequestBody CallbackRequest callbackrequest) {
        final String showEpoFieldId = "EPO_REASONING_SHOW";
        CaseDetails caseDetails = callbackrequest.getCaseDetails();
        Map<String, Object> data = caseDetails.getData();

        Optional<List<String>> orderType = Optional.ofNullable((Map<String, Object>) data.get("orders"))
            .map(orders -> (List<String>) orders.get("orderType"));

        if (orderType.isPresent()) {
            orderType.ifPresent(orderTypes -> {
                if (orderTypes.contains(OrderType.EMERGENCY_PROTECTION_ORDER.name())) {
                    data.put(showEpoFieldId, ImmutableList.of("SHOW_FIELD"));

                } else if (data.containsKey(showEpoFieldId)) {
                    data.remove("groundsForEPO");
                    data.remove(showEpoFieldId);
                }
            });

        } else {
            data.remove("groundsForEPO");
            data.remove(showEpoFieldId);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    @PostMapping("/submitted")
    public void handleAboutToSubmit(@RequestBody CallbackRequest callbackrequest) {
        applicationEventPublisher.publishEvent(new CaseDataChanged(callbackrequest));
    }
}
