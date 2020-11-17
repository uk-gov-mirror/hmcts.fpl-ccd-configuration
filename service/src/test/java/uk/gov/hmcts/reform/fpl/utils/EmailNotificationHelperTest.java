package uk.gov.hmcts.reform.fpl.utils;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.common.Element;

import java.time.LocalDateTime;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.utils.CaseDataGeneratorHelper.createHearingBookingsFromInitialDate;
import static uk.gov.hmcts.reform.fpl.utils.CaseDataGeneratorHelper.createRespondents;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateToString;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.unwrapElements;
import static uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper.buildCallout;
import static uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper.buildSubjectLine;
import static uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper.buildSubjectLineWithHearingBookingDateSuffix;
import static uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper.formatCaseUrl;

class EmailNotificationHelperTest {

    @Test
    void subjectLineShouldBeEmptyWhenNoRespondentOrCaseNumberEmpty() {
        CaseData data = CaseData.builder()
            .build();
        String subjectLine = buildSubjectLine(data.getFamilyManCaseNumber(), data.getRespondents1());
        assertThat(subjectLine).isEmpty();
    }

    @Test
    void subjectLineShouldMatchWhenRespondentAndCaseNumberGiven() {
        CaseData caseData = CaseData.builder()
            .familyManCaseNumber("FamilyManCaseNumber")
            .respondents1(createRespondents())
            .build();

        String expectedSubjectLine = "Jones, FamilyManCaseNumber";
        String subjectLine = buildSubjectLine(caseData.getFamilyManCaseNumber(), caseData.getRespondents1());
        assertThat(subjectLine).isEqualTo(expectedSubjectLine);
    }

    @Test
    void subjectLineShouldNotBeEmptyWhenOnlyRespondentGiven() {
        CaseData caseData = CaseData.builder()
            .respondents1(createRespondents())
            .build();

        String expectedSubjectLine = "Jones";
        String subjectLine = buildSubjectLine(caseData.getFamilyManCaseNumber(), caseData.getRespondents1());
        assertThat(subjectLine).isEqualTo(expectedSubjectLine);
    }

    @Test
    void subjectLineShouldReturnFirstRespondentElementAlwaysWhenMultipleRespondentsGiven() {
        List<Element<Respondent>> respondents = ImmutableList.of(
            Element.<Respondent>builder()
                .id(UUID.randomUUID())
                .value(Respondent.builder().party(
                    RespondentParty.builder()
                        .firstName("Timothy")
                        .lastName(null)
                        .relationshipToChild("Father")
                        .build())
                    .build())
                .build(),
            Element.<Respondent>builder()
                .id(UUID.randomUUID())
                .value(Respondent.builder().party(
                    RespondentParty.builder()
                        .firstName("Timothy")
                        .lastName("Jones")
                        .relationshipToChild("Father")
                        .build())
                    .build())
                .build(),
            Element.<Respondent>builder()
                .id(UUID.randomUUID())
                .value(Respondent.builder().party(
                    RespondentParty.builder()
                        .firstName("Sarah")
                        .lastName("Simpson")
                        .relationshipToChild("Mother")
                        .build())
                    .build())
                .build()
        );

        CaseData caseData = CaseData.builder()
            .respondents1(respondents)
            .familyManCaseNumber("FamilyManCaseNumber-With-Empty-Lastname")
            .build();

        String expectedSubjectLine = "FamilyManCaseNumber-With-Empty-Lastname";
        String subjectLine = buildSubjectLine(caseData.getFamilyManCaseNumber(), caseData.getRespondents1());
        assertThat(subjectLine).isEqualTo(expectedSubjectLine);
    }

    @Test
    void subjectLineShouldBeSuffixedWithHearingDate() {
        final LocalDateTime futureDate = LocalDateTime.of(2022, 05, 23, 0, 0, 0);
        List<Element<HearingBooking>> hearingBookingsFromInitialDate =
            createHearingBookingsFromInitialDate(futureDate);
        CaseData caseData = CaseData.builder()
            .respondents1(createRespondents())
            .hearingDetails(hearingBookingsFromInitialDate)
            .familyManCaseNumber("FamilyManCaseNumber")
            .build();

        HearingBooking hearingBooking = unwrapElements(caseData.getHearingDetails()).get(2);

        String expectedSubjectLine = "Jones, FamilyManCaseNumber, hearing 23 May 2022";
        String returnedSubjectLine = buildSubjectLineWithHearingBookingDateSuffix(caseData
                .getFamilyManCaseNumber(),
            caseData.getRespondents1(), hearingBooking);
        assertThat(returnedSubjectLine).isEqualTo(expectedSubjectLine);
    }

    @Test
    void subjectLineSuffixShouldNotContainHearingDateWhenHearingBookingsNotProvided() {
        CaseData caseData = CaseData.builder()
            .respondents1(createRespondents())
            .hearingDetails(null)
            .familyManCaseNumber("FamilyManCaseNumber")
            .build();

        String expectedSubjectLine = "Jones, FamilyManCaseNumber";
        String returnedSubjectLine = buildSubjectLineWithHearingBookingDateSuffix(caseData
            .getFamilyManCaseNumber(), caseData.getRespondents1(), null);
        assertThat(returnedSubjectLine).isEqualTo(expectedSubjectLine);
    }

    @Test
    void shouldNotAddHearingDateWhenNoFutureHearings() {
        LocalDateTime pastDate = LocalDateTime.now().minusYears(10);
        List<Element<HearingBooking>> hearingBookings = createHearingBookingsFromInitialDate(pastDate);
        CaseData caseData = CaseData.builder()
            .respondents1(createRespondents())
            .hearingDetails(hearingBookings)
            .familyManCaseNumber("FamilyManCaseNumber")
            .build();

        String expected = "Jones, FamilyManCaseNumber";
        String actual = buildSubjectLineWithHearingBookingDateSuffix(caseData.getFamilyManCaseNumber(),
            caseData.getRespondents1(), null);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatUrlCorrectlyWhenBaseUrlAndCaseIdProvided() {
        String formattedUrl = formatCaseUrl("http://testurl", 123L);
        String expectedUrl = "http://testurl/cases/case-details/123";
        assertThat(formattedUrl).isEqualTo(expectedUrl);
    }

    @Test
    void shouldFormatUrlCorrectlyWhenBaseUrlCaseIdAndTabProvided() {
        String formattedUrl = formatCaseUrl("http://testurl", 123L, "tab1");
        String expectedUrl = "http://testurl/cases/case-details/123#tab1";
        assertThat(formattedUrl).isEqualTo(expectedUrl);
    }

    @Test
    void shouldFormatUrlCorrectlyWhenBaseUrlCaseIdAndTabIsEmpty() {
        String formattedUrl = formatCaseUrl("http://testurl", 123L, "");
        String expectedUrl = "http://testurl/cases/case-details/123";
        assertThat(formattedUrl).isEqualTo(expectedUrl);
    }

    @Test
    void shouldFormatCallOutWhenAllRequiredFieldsArePresent() {
        LocalDateTime hearingDate = LocalDateTime.now();
        HearingBooking hearingBooking = HearingBooking.builder()
            .startDate(hearingDate)
            .build();

        CaseData caseData = CaseData.builder()
            .familyManCaseNumber("12345")
            .hearingDetails(List.of(
                element(hearingBooking)))
            .respondents1(List.of(
                element(Respondent.builder()
                    .party(RespondentParty.builder()
                        .lastName("Davids")
                        .build())
                    .build())))
            .build();

        String expectedContent = String.format("^Davids, 12345,%s", buildHearingDateText(hearingBooking));

        assertThat(buildCallout(caseData)).isEqualTo(expectedContent);
    }

    @Test
    void shouldFormatCallOutWhenOnlySomeRequiredFieldsArePresent() {
        LocalDateTime hearingDate = LocalDateTime.now();
        HearingBooking hearingBooking = HearingBooking.builder()
            .startDate(hearingDate)
            .build();

        CaseData caseData = CaseData.builder()
            .familyManCaseNumber("12345")
            .hearingDetails(List.of(
                element(hearingBooking)))
            .build();

        String expectedContent = String.format("^12345,%s", buildHearingDateText(hearingBooking));

        assertThat(buildCallout(caseData)).isEqualTo(expectedContent);
    }

    private static String buildHearingDateText(HearingBooking hearingBooking) {
        return " hearing " + formatLocalDateToString(hearingBooking
            .getStartDate().toLocalDate(), FormatStyle.MEDIUM);
    }
}
