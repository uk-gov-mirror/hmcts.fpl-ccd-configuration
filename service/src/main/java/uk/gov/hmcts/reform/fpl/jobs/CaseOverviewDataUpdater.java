package uk.gov.hmcts.reform.fpl.jobs;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.State;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.service.CaseConverter;
import uk.gov.hmcts.reform.fpl.service.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.fpl.service.search.SearchService;
import uk.gov.hmcts.reform.fpl.utils.OverviewDataHelper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.of;
import static uk.gov.hmcts.reform.fpl.CaseDefinitionConstants.CASE_TYPE;
import static uk.gov.hmcts.reform.fpl.CaseDefinitionConstants.JURISDICTION;

@Slf4j
public class CaseOverviewDataUpdater implements Job {

    private static final String HEARING_DATE_PROPERTY = "data.hearingDetails.value.startDate";

    @Autowired
    private SearchService searchService;

    @Autowired
    private CaseConverter caseConverter;

    @Autowired
    private CoreCaseDataService coreCaseDataService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        final String jobName = context.getJobDetail().getKey().getName();
        log.info("Job {} started", jobName);

        List<CaseDetails> cases = searchCasesWithHearingTime();

        if (cases.isEmpty()) {
            log.info("Job '{}' did not find any cases", jobName);
        } else {
            log.info("Job '{}' updating next hearing date for case references: {}",
                jobName,
                cases.stream().map(details -> details.getId().toString()).collect(Collectors.joining(","))
            );

            // TODO: case states which allows update
            List<String> updatedCaseReferences = cases.stream()
                .filter(caseDetails -> State.SUBMITTED.getValue().equals(caseDetails.getState()))
                .map(caseDetails -> {
                    log.info("Updating case {} with status {}", caseDetails.getId(), caseDetails.getState());
                    CaseData caseData = caseConverter.convert(caseDetails);
                    coreCaseDataService.triggerEvent(
                        JURISDICTION,
                        CASE_TYPE,
                        caseDetails.getId(),
                        "internal-update-overview-data",
                        OverviewDataHelper.updateOverviewHearingsData(caseData)
                    );
                    return caseData.getId().toString();
                }).collect(Collectors.toList());

            log.info("Job '{}' Updated overview hearing dates for cases {}",
                jobName,
                String.join(",", updatedCaseReferences)
            );
        }

        log.info("Job '{}' finished", jobName);
    }

    private List<CaseDetails> searchCasesWithHearingTime() {
        LocalDateTime now = ZonedDateTime.now(ZoneId.of("Europe/London")).toLocalDateTime();
        final Map<String, Object> dateTimeRange = of(
            "gte", now,
            "lt", now.plusMinutes(15)
        );

        String hearingDateTimeRangeQuery = new JSONObject(
            of("query", of("range", of(HEARING_DATE_PROPERTY, dateTimeRange)))
        ).toString();

        return coreCaseDataService.searchCases(CASE_TYPE, hearingDateTimeRangeQuery);
    }

}
