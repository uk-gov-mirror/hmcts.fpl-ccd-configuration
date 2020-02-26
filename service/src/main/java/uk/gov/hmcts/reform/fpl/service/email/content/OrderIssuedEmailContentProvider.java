package uk.gov.hmcts.reform.fpl.service.email.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.config.HmctsCourtLookupConfiguration;
import uk.gov.hmcts.reform.fpl.enums.IssuedOrderType;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.service.DateFormatterService;
import uk.gov.hmcts.reform.fpl.service.HearingBookingService;

import java.util.Map;

import static uk.gov.hmcts.reform.fpl.enums.IssuedOrderType.NOTICE_OF_PLACEMENT_ORDER;
import static uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper.buildSubjectLine;
import static uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper.buildSubjectLineWithHearingBookingDateSuffix;
import static uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper.formatCaseUrl;
import static uk.gov.hmcts.reform.fpl.utils.PeopleInCaseHelper.getFirstRespondentLastName;

@Slf4j
@Service
public class OrderIssuedEmailContentProvider extends AbstractEmailContentProvider {

    private final HmctsCourtLookupConfiguration hmctsCourtLookupConfiguration;
    private final ObjectMapper objectMapper;

    public OrderIssuedEmailContentProvider(@Value("${ccd.ui.base.url}") String uiBaseUrl,
                                           ObjectMapper objectMapper,
                                           HearingBookingService hearingBookingService,
                                           HmctsCourtLookupConfiguration hmctsCourtLookupConfiguration,
                                           DateFormatterService dateFormatterService) {
        super(uiBaseUrl, dateFormatterService, hearingBookingService);
        this.objectMapper = objectMapper;
        this.hmctsCourtLookupConfiguration = hmctsCourtLookupConfiguration;
    }

    public Map<String, Object> buildOrderNotificationParametersForHmctsAdmin(final CaseDetails caseDetails,
                                                                             final String localAuthorityCode,
                                                                             final IssuedOrderType issuedOrderType) {
        CaseData caseData = objectMapper.convertValue(caseDetails.getData(), CaseData.class);

        return ImmutableMap.<String, Object>builder()
            .put("callout", (issuedOrderType != NOTICE_OF_PLACEMENT_ORDER)
                ? "^" + buildSubjectLineWithHearingBookingDateSuffix(buildSubjectLine(caseData),
                caseData.getHearingDetails()) : "")
            .put("courtName", hmctsCourtLookupConfiguration.getCourt(localAuthorityCode).getName())
            .put("caseUrl", formatCaseUrl(uiBaseUrl, caseDetails.getId()))
            .put("respondentLastName", getFirstRespondentLastName(caseData.getRespondents1()))
            .build();
    }
}
