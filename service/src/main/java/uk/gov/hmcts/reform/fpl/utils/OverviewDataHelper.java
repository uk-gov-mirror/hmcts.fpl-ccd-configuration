package uk.gov.hmcts.reform.fpl.utils;

import uk.gov.hmcts.reform.fpl.exceptions.NoHearingBookingException;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.HearingPreferences;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.EMERGENCY_PROTECTION_ORDER;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.parseLocalDateTimeFromStringUsingFormat;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.unwrapElements;

public class OverviewDataHelper {

    private OverviewDataHelper() {
        // no-op
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> updateOverviewHearingsData(CaseData caseData) {
        Map<String, Object> overviewData = new HashMap<>();
        overviewData.put("overviewCaseState", caseData.getState().getValue());

        updateNextHearingDetails(caseData, overviewData);
        updatePreviousHearingDetails(caseData.getHearingDetails(), overviewData);

        /*if (caseData.getCancelledHearingDetails() != null) {
            updateAdjournedHearingDetails(caseData.getCancelledHearingDetails(), overviewData);
        }*/

        updateHearingPreferences(caseData, overviewData);

        return overviewData;
    }

    public static void updateHearingPreferences(
        CaseData caseData,
        Map<String, Object> overviewData
    ) {
        HearingPreferences hearingPreferences = caseData.getHearingPreferences();
        overviewData.put("showHearingNeeds", hearingPreferences != null ? "Yes" : "No");
        if (hearingPreferences != null) {
            overviewData.put("overviewInterpreterDetails", hearingPreferences.getInterpreterDetails());
            overviewData.put("overviewDisabilityAssistanceDetails", hearingPreferences.getDisabilityAssistanceDetails());
            overviewData.put("overviewExtraSecurityDetails", hearingPreferences.getExtraSecurityMeasuresDetails());
            overviewData.put("overviewWelshDetails", hearingPreferences.getWelshDetails());
            overviewData.put("overviewIntermediaryDetails", hearingPreferences.getInterpreterDetails());
            overviewData.put("overviewHearingAdditionalDetails", hearingPreferences.getSomethingElseDetails());
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> updateOverviewOrdersData(CaseData caseData) {
        Map<String, Object> overviewData = new HashMap<>();
        overviewData.put("overviewCaseState", overviewData.getOrDefault("state", caseData.getState().getLabel()));

        List<Element<GeneratedOrder>> orders = caseData.getOrderCollection();

        List<String> requestedOrders = unwrapElements(orders).stream().map(GeneratedOrder::getType).distinct().collect(toList());

        overviewData.put("overviewRequestedOrders", String.join(",", requestedOrders));

        // TODO: urgent case condition
        overviewData.put("urgentCase", requestedOrders.contains(EMERGENCY_PROTECTION_ORDER.getLabel()) ? "Yes" : "No");

        Optional<GeneratedOrder> lastOrder = unwrapElements(orders)
            .stream()
            .max(comparing(order -> parseLocalDateTimeFromStringUsingFormat(order.getDate(), DateFormatterHelper.TIME_DATE)));

        lastOrder.ifPresent(order -> {
            overviewData.put("overviewLastOrderType", order.asLabel());
            overviewData.put("overviewLastOrderDate", order.getDate());
            overviewData.put("overviewLastOrderDetails", order.getDetails());
            if (order.getFurtherDirections() != null) {
                overviewData.put("overviewFurtherDirectionsNeeded", order.getFurtherDirections().getDirectionsNeeded());
                overviewData.put("overviewFurtherDirections", order.getFurtherDirections().getDirections());
            }
        });
        return overviewData;
    }

    private static void updateAdjournedHearingDetails(
        List<Element<HearingBooking>> cancelledHearings,
        Map<String, Object> overviewData
    ) {
        HearingBooking adjournedHearing = unwrapElements(cancelledHearings)
            .stream()
            .max(comparing(HearingBooking::getStartDate))
            .orElseThrow(NoSuchFieldError::new);

        if (adjournedHearing != null) {
            overviewData.put("adjournedHearingType", adjournedHearing.getType().getLabel());
            overviewData.put("adjournedHearingDate", adjournedHearing.getStartDate());
            overviewData.put("adjournedHearingReason", adjournedHearing.getCancellationReason());
        }
    }

    private static void updatePreviousHearingDetails(
        List<Element<HearingBooking>> hearings,
        Map<String, Object> overviewData
    ) {
        unwrapElements(hearings)
            .stream()
            .filter(hearingBooking -> !hearingBooking.startsAfterToday())
            .max(comparing(HearingBooking::getStartDate))
            .ifPresent(hearing -> {
                overviewData.put("previousHearingDate", hearing.getStartDate());
                overviewData.put("previousHearingType", hearing.getType().getLabel());
            });
    }

    private static void updateNextHearingDetails(
        CaseData caseData,
        Map<String, Object> overviewData
    ) {
        HearingBooking hearingBooking = unwrapElements(caseData.getHearingDetails())
            .stream()
            .filter(HearingBooking::startsAfterToday)
            .min(comparing(HearingBooking::getStartDate))
            .orElseThrow(NoHearingBookingException::new);

        overviewData.put("nextHearingDate", hearingBooking.getStartDate());

        if (hearingBooking.getJudgeAndLegalAdvisor() != null) {

            overviewData.put("overviewAllocatedJudgeLabel",
                String.join(
                    " ", hearingBooking.getJudgeAndLegalAdvisor().getJudgeOrMagistrateTitle(), hearingBooking.getJudgeAndLegalAdvisor().getJudgeName()
                ));
        }
    }
}
