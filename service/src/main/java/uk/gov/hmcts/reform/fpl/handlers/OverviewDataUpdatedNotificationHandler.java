package uk.gov.hmcts.reform.fpl.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.events.GeneratedOrderEvent;
import uk.gov.hmcts.reform.fpl.events.HearingDataUpdated;
import uk.gov.hmcts.reform.fpl.service.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.fpl.utils.OverviewDataHelper;

import static uk.gov.hmcts.reform.fpl.CaseDefinitionConstants.CASE_TYPE;
import static uk.gov.hmcts.reform.fpl.CaseDefinitionConstants.JURISDICTION;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OverviewDataUpdatedNotificationHandler {

    private final CoreCaseDataService coreCaseDataService;

    @EventListener
    public void updateCaseOverviewHearingsData(HearingDataUpdated event) {
        if (event.getCaseData() != null) {
            coreCaseDataService.triggerEvent(
                JURISDICTION,
                CASE_TYPE,
                event.getCaseData().getId(),
                "internal-update-overview-data",
                OverviewDataHelper.updateOverviewHearingsData(event.getCaseData())
            );
            log.info("Updating overview next hearing details for case reference: {}", event.getCaseData().getId());
        }
    }

    @EventListener
    public void updateCaseOverviewOrdersData(GeneratedOrderEvent event) {
        if (event.getCaseData() != null) {
            coreCaseDataService.triggerEvent(
                JURISDICTION,
                CASE_TYPE,
                event.getCaseData().getId(),
                "internal-update-overview-data",
                OverviewDataHelper.updateOverviewOrdersData(event.getCaseData())
            );
            log.info("Updating overview orders data for case reference: {}", event.getCaseData().getId());
        }
    }
}
