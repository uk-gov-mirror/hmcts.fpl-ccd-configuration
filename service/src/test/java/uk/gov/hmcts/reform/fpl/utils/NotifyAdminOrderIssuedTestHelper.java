package uk.gov.hmcts.reform.fpl.utils;

import com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.fpl.model.Address;
import uk.gov.hmcts.reform.fpl.model.Representative;
import uk.gov.hmcts.reform.fpl.model.common.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.POST;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper.formatCaseUrl;

public class NotifyAdminOrderIssuedTestHelper {

    private NotifyAdminOrderIssuedTestHelper() {
    }

    private static final String EXAMPLE_COURT = "Family Court";
    private static final String callout = "^Jones, SACCCCCCCC5676576567, hearing " + LocalDateTime.now().plusMonths(3)
        .toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).localizedBy(Locale.UK));

    public static Map<String, Object> getExpectedPlacementParametersForAdminWhenNoRepresentativesServedByPost() {
        return ImmutableMap.<String, Object>builder()
            .put("callout", "")
            .putAll(commonParametersNoPostingNeeded())
            .build();
    }

    public static Map<String, Object> getExpectedParametersForAdminWhenNoRepresentativesServedByPost() {
        return ImmutableMap.<String, Object>builder()
            .put("callout", callout)
            .putAll(commonParametersNoPostingNeeded())
            .build();
    }

    public static List<Element<Representative>> buildRepresentativesServedByPost() {
        return wrapElements(Representative.builder()
            .email("paul@example.com")
            .fullName("Paul Blart")
            .address(Address.builder()
                .addressLine1("Street")
                .postTown("Town")
                .postcode("Postcode")
                .build())
            .servingPreferences(POST)
            .build());
    }

    private static Map<String, Object> commonParametersNoPostingNeeded() {
        return Map.of("courtName", EXAMPLE_COURT,
            "caseUrl", formatCaseUrl("http://fake-url", 12345L),
            "respondentLastName", "Jones");
    }
}

