package uk.gov.hmcts.reform.fpl.service.email.content;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.service.email.content.base.AbstractEmailContentProvider;

import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateToString;

@Service
public class UpcomingHearingsContentProvider extends AbstractEmailContentProvider {

    public Map<String, Object> buildParameters(final LocalDate dateOfHearing, final List<CaseDetails> casesToBeHeard) {
        final String formattedCases = casesToBeHeard.stream()
            .map(this::formatCase)
            .collect(joining(lineSeparator()));

        return Map.of(
            "hearing_date", formatLocalDateToString(dateOfHearing, FormatStyle.LONG),
            "cases", formattedCases
        );
    }

    private String formatCase(CaseDetails caseDetails) {
        Object caseNumber = caseDetails.getData().get("familyManCaseNumber");
        Object caseName = caseDetails.getData().get("caseName");
        String caseUrl = getCaseUrl(caseDetails.getId(), "OrdersTab");

        return Stream.of(caseNumber, caseName, caseUrl)
            .filter(Objects::nonNull)
            .map(Object::toString)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" "));
    }
}
