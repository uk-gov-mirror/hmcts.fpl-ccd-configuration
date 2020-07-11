package uk.gov.hmcts.reform.fpl.utils;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.service.CaseDataExtractionService;
import uk.gov.hmcts.reform.fpl.service.HearingBookingService;
import uk.gov.hmcts.reform.fpl.service.HearingVenueLookUpService;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;
import uk.gov.hmcts.reform.fpl.service.config.LookupTestConfig;
import uk.gov.hmcts.reform.fpl.service.docmosis.DocmosisDocumentGeneratorService;
import uk.gov.hmcts.reform.fpl.service.docmosis.NoticeOfHearingGenerationService;
import uk.gov.hmcts.reform.fpl.service.time.Time;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.utils.CaseDataGeneratorHelper.createHearingBookingsFromInitialDate;
import static uk.gov.hmcts.reform.fpl.utils.CaseDataGeneratorHelper.createRespondents;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateTimeBaseUsingFormat;
import static uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper.formatCaseUrl;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    EmailNotificationHelper.class,
    JacksonAutoConfiguration.class,
    LookupTestConfig.class,
    HearingBookingService.class,
    CaseDataExtractionService.class,
    NoticeOfHearingGenerationService.class,
    FixedTimeConfiguration.class,
    HearingVenueLookUpService.class
})
class EmailNotificationHelperTest {

    @Autowired
    private Time time;

    @Autowired
    private EmailNotificationHelper helper;

    @MockBean
    private DocmosisDocumentGeneratorService docmosisDocumentGeneratorService;

    @MockBean
    private UploadDocumentService uploadDocumentService;

    @Test
    void subjectLineShouldBeEmptyWhenNoRespondentOrCaseNumberEmpty() {
        String subjectLine = helper.buildSubjectLine(CaseData.builder().build());
        assertThat(subjectLine).isEmpty();
    }

    @Test
    void subjectLineShouldMatchWhenRespondentAndCaseNumberGiven() {
        CaseData caseData = CaseData.builder()
            .familyManCaseNumber("FamilyManCaseNumber")
            .respondents1(createRespondents())
            .build();

        String expectedSubjectLine = "Jones, FamilyManCaseNumber";
        String subjectLine = helper.buildSubjectLine(caseData);
        assertThat(subjectLine).isEqualTo(expectedSubjectLine);
    }

    @Test
    void subjectLineShouldNotBeEmptyWhenOnlyRespondentGiven() {
        CaseData caseData = CaseData.builder()
            .respondents1(createRespondents())
            .build();

        String expectedSubjectLine = "Jones";
        String subjectLine = helper.buildSubjectLine(caseData);
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
        String subjectLine = helper.buildSubjectLine(caseData);
        assertThat(subjectLine).isEqualTo(expectedSubjectLine);
    }

    @Test
    void subjectLineShouldBeSuffixedWithHearingDate() {
        final LocalDateTime dateInTenMonths = time.now().plusMonths(10);
        List<Element<HearingBooking>> hearingBookingsFromInitialDate =
            createHearingBookingsFromInitialDate(dateInTenMonths);
        CaseData caseData = CaseData.builder()
            .respondents1(createRespondents())
            .hearingDetails(hearingBookingsFromInitialDate)
            .familyManCaseNumber("FamilyManCaseNumber")
            .build();

        String expectedSubjectLine = "Jones, FamilyManCaseNumber, hearing "
            + formatLocalDateTimeBaseUsingFormat(dateInTenMonths, "d MMM yyyy");
        String returnedSubjectLine = helper.buildSubjectLineWithHearingBookingDateSuffix(caseData,
            caseData.getHearingDetails());
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
        String returnedSubjectLine = helper.buildSubjectLineWithHearingBookingDateSuffix(caseData,
            caseData.getHearingDetails());
        assertThat(returnedSubjectLine).isEqualTo(expectedSubjectLine);
    }

    @Test
    void shouldNotAddHearingDateWhenNoFutureHearings() {
        LocalDateTime pastDate = time.now().minusYears(10);
        List<Element<HearingBooking>> hearingBookings = createHearingBookingsFromInitialDate(pastDate);
        CaseData caseData = CaseData.builder()
            .respondents1(createRespondents())
            .hearingDetails(hearingBookings)
            .familyManCaseNumber("FamilyManCaseNumber")
            .build();

        String expected = "Jones, FamilyManCaseNumber";
        String actual = helper.buildSubjectLineWithHearingBookingDateSuffix(caseData, caseData.getHearingDetails());

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
}
