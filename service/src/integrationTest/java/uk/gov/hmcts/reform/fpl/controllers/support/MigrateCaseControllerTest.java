package uk.gov.hmcts.reform.fpl.controllers.support;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.controllers.AbstractCallbackTest;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.common.C2DocumentBundle;
import uk.gov.hmcts.reform.fpl.model.common.Element;

import java.util.List;
import java.util.UUID;

import static com.launchdarkly.shaded.com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;

@WebMvcTest(MigrateCaseController.class)
@OverrideAutoConfiguration(enabled = true)
class MigrateCaseControllerTest extends AbstractCallbackTest {

    MigrateCaseControllerTest() {
        super("migrate-case");
    }

    @Nested
    class Fpla2774 {
        String familyManNumber = "NE21C50007";
        String migrationId = "FPLA-2774";
        UUID hearingIdOne = UUID.randomUUID();
        UUID hearingIdTwo = UUID.randomUUID();
        HearingBooking hearingBooking = HearingBooking.builder().build();

        @Test
        void shouldRemoveSecondHearing() {
            Element<HearingBooking> hearingOne = element(hearingIdOne, hearingBooking);
            Element<HearingBooking> hearingTwo = element(hearingIdTwo, hearingBooking);

            List<Element<HearingBooking>> hearingBookings = newArrayList(hearingOne, hearingTwo);
            CaseDetails caseDetails = caseDetails(migrationId, familyManNumber, hearingBookings);
            CaseData extractedCaseData = extractCaseData(postAboutToSubmitEvent(caseDetails));

            assertThat(extractedCaseData.getHearingDetails()).isEqualTo(List.of(hearingOne));
        }

        @Test
        void shouldNotChangeCaseIfNotExpectedMigrationId() {
            String incorrectMigrationId = "FPLA-1111";

            Element<HearingBooking> hearingOne = element(hearingIdOne, hearingBooking);
            Element<HearingBooking> hearingTwo = element(hearingIdTwo, hearingBooking);

            List<Element<HearingBooking>> hearingBookings = newArrayList(hearingOne, hearingTwo);
            CaseDetails caseDetails = caseDetails(incorrectMigrationId, familyManNumber, hearingBookings);
            CaseData extractedCaseData = extractCaseData(postAboutToSubmitEvent(caseDetails));

            assertThat(extractedCaseData.getHearingDetails()).isEqualTo(hearingBookings);
        }

        @Test
        void shouldNotChangeCaseIfNotExpectedFamilyManCaseNumber() {
            String invalidFamilyManNumber = "PO20C50031";

            Element<HearingBooking> hearingOne = element(hearingIdOne, hearingBooking);
            Element<HearingBooking> hearingTwo = element(hearingIdTwo, hearingBooking);

            List<Element<HearingBooking>> hearingBookings = newArrayList(hearingOne, hearingTwo);
            CaseDetails caseDetails = caseDetails(migrationId, invalidFamilyManNumber, hearingBookings);
            CaseData extractedCaseData = extractCaseData(postAboutToSubmitEvent(caseDetails));

            assertThat(extractedCaseData.getHearingDetails()).isEqualTo(hearingBookings);
        }

        @Test
        void shouldThrowAnExceptionIfCaseContainsFewerHearingsThanExpected() {
            Element<HearingBooking> hearingOne = element(hearingIdOne, hearingBooking);
            List<Element<HearingBooking>> hearingBookings = newArrayList(hearingOne);

            CaseDetails caseDetails = caseDetails(
                migrationId, familyManNumber, hearingBookings);

            assertThatThrownBy(() -> postAboutToSubmitEvent(caseDetails))
                .getRootCause()
                .hasMessage("Expected 2 hearings in the case but found 1");
        }

        @Test
        void shouldThrowAnExceptionIfCaseDoesNotContainHearings() {
            CaseDetails caseDetails = caseDetails(
                migrationId, familyManNumber, null);

            assertThatThrownBy(() -> postAboutToSubmitEvent(caseDetails))
                .getRootCause()
                .hasMessage("No hearings in the case");
        }

        private CaseDetails caseDetails(String migrationId,
                                        String familyManCaseNumber,
                                        List<Element<HearingBooking>> hearingBookings) {
            CaseDetails caseDetails = asCaseDetails(CaseData.builder()
                .familyManCaseNumber(familyManCaseNumber)
                .hearingDetails(hearingBookings)
                .build());

            caseDetails.getData().put("migrationId", migrationId);
            return caseDetails;
        }
    }

    @Nested
    class Fpla2905 {
        String familyManNumber = "CF20C50047";
        String migrationId = "FPLA-2905";

        @Test
        void shouldRemoveSecondC2DocumentBundle() {
            UUID elementIdOne = UUID.randomUUID();
            UUID elementIdTwo = UUID.randomUUID();

            Element<C2DocumentBundle> c2DocumentBundleElementOne = element(elementIdOne,
                C2DocumentBundle.builder().build());

            Element<C2DocumentBundle> c2DocumentBundleElementTwo = element(elementIdTwo,
                C2DocumentBundle.builder().build());

            CaseDetails caseData = caseDetails(migrationId, familyManNumber, List.of(c2DocumentBundleElementOne,
                c2DocumentBundleElementTwo));

            CaseData updatedCaseData = extractCaseData(postAboutToSubmitEvent(caseData));

            assertThat(updatedCaseData.getC2DocumentBundle()).containsExactly(c2DocumentBundleElementOne);
        }

        @Test
        void shouldNotChangeCaseIfNotExpectedMigrationId() {
            String incorrectMigrationId = "FPLA-1111";

            UUID elementIdOne = UUID.randomUUID();
            UUID elementIdTwo = UUID.randomUUID();

            Element<C2DocumentBundle> c2DocumentBundleElementOne = element(elementIdOne,
                C2DocumentBundle.builder().build());

            Element<C2DocumentBundle> c2DocumentBundleElementTwo = element(elementIdTwo,
                C2DocumentBundle.builder().build());

            CaseDetails caseData = caseDetails(incorrectMigrationId, familyManNumber,
                List.of(c2DocumentBundleElementOne, c2DocumentBundleElementTwo));

            List<Element<C2DocumentBundle>> expectedBundle = List.of(c2DocumentBundleElementOne,
                c2DocumentBundleElementTwo);

            CaseData updatedCaseData = extractCaseData(postAboutToSubmitEvent(caseData));

            assertThat(updatedCaseData.getC2DocumentBundle()).isEqualTo(expectedBundle);
        }

        @Test
        void shouldThrowExceptionWhenUnexpectedFamilyManNumber() {
            CaseDetails caseData = caseDetails(migrationId, "test", emptyList());

            assertThatThrownBy(() -> postAboutToSubmitEvent(caseData))
                .getRootCause()
                .hasMessage("Unexpected FMN test");
        }

        @Test
        void shouldThrowExceptionWhenC2documentBundleIsMissing() {
            CaseDetails caseData = caseDetails(migrationId, familyManNumber, null);

            assertThatThrownBy(() -> postAboutToSubmitEvent(caseData))
                .getRootCause()
                .hasMessage("No C2 document bundles in the case");
        }

        private CaseDetails caseDetails(String migrationId,
                                        String familyManCaseNumber,
                                        List<Element<C2DocumentBundle>> bundles) {
            CaseDetails caseDetails = asCaseDetails(CaseData.builder()
                .familyManCaseNumber(familyManCaseNumber)
                .c2DocumentBundle(bundles)
                .build());

            caseDetails.getData().put("migrationId", migrationId);
            return caseDetails;
        }
    }

    @Nested
    class Fpla2872 {
        String familyManNumber = "NE20C50023";
        String migrationId = "FPLA-2872";
        UUID elementIdOne = UUID.randomUUID();
        UUID elementIdTwo = UUID.randomUUID();
        UUID elementIdThree = UUID.randomUUID();
        UUID elementIdFour = UUID.randomUUID();
        UUID elementIdFive = UUID.randomUUID();
        UUID elementIdSix = UUID.randomUUID();

        @Test
        void shouldRemoveExpectedC2DocumentBundlesFromCase() {
            Element<C2DocumentBundle> c2DocumentBundleElementOne = element(elementIdOne,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementTwo = element(elementIdTwo,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementThree = element(elementIdThree,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementFour = element(elementIdFour,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementFive = element(elementIdFive,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementSix = element(elementIdSix,
                createC2DocumentBundle());

            List<Element<C2DocumentBundle>> bundle = List.of(c2DocumentBundleElementOne,
                c2DocumentBundleElementTwo, c2DocumentBundleElementThree, c2DocumentBundleElementFour,
                c2DocumentBundleElementFive, c2DocumentBundleElementSix);

            CaseDetails caseData = caseDetails(migrationId, familyManNumber, bundle);

            CaseData updatedCaseData = extractCaseData(postAboutToSubmitEvent(caseData));

            assertThat(updatedCaseData.getC2DocumentBundle()).containsExactly(c2DocumentBundleElementOne,
                c2DocumentBundleElementTwo, c2DocumentBundleElementSix);
        }

        @Test
        void shouldNotChangeCaseIfNotExpectedMigrationId() {
            String incorrectMigrationId = "FPLA-1111";

            Element<C2DocumentBundle> c2DocumentBundleElementOne = element(elementIdOne,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementTwo = element(elementIdTwo,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementThree = element(elementIdThree,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementFour = element(elementIdFour,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementFive = element(elementIdFive,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementSix = element(elementIdSix,
                createC2DocumentBundle());

            List<Element<C2DocumentBundle>> bundle = List.of(c2DocumentBundleElementOne,
                c2DocumentBundleElementTwo, c2DocumentBundleElementThree, c2DocumentBundleElementFour,
                c2DocumentBundleElementFour, c2DocumentBundleElementFive, c2DocumentBundleElementSix);

            CaseDetails caseData = caseDetails(incorrectMigrationId, familyManNumber, bundle);
            CaseData updatedCaseData = extractCaseData(postAboutToSubmitEvent(caseData));

            assertThat(updatedCaseData.getC2DocumentBundle()).isEqualTo(bundle);
        }

        @Test
        void shouldThrowExceptionWhenUnexpectedFamilyManNumber() {
            CaseDetails caseData = caseDetails(migrationId, "test", emptyList());

            assertThatThrownBy(() -> postAboutToSubmitEvent(caseData))
                .getRootCause()
                .hasMessage("Unexpected FMN test");
        }

        @Test
        void shouldThrowExceptionWhenC2documentBundleSizeIsSmallerThanExpected() {
            Element<C2DocumentBundle> c2DocumentBundleElementOne = element(elementIdOne,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementTwo = element(elementIdTwo,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementThree = element(elementIdThree,
                createC2DocumentBundle());

            Element<C2DocumentBundle> c2DocumentBundleElementFour = element(elementIdFour,
                createC2DocumentBundle());

            List<Element<C2DocumentBundle>> bundle = List.of(c2DocumentBundleElementOne,
                c2DocumentBundleElementTwo, c2DocumentBundleElementThree, c2DocumentBundleElementFour);

            CaseDetails caseData = caseDetails(migrationId, familyManNumber, bundle);

            assertThatThrownBy(() -> postAboutToSubmitEvent(caseData))
                .getRootCause()
                .hasMessage("Expected at least 5 C2 document bundles in the case but found 4");
        }

        @Test
        void shouldThrowExceptionWhenC2documentBundleIsMissing() {
            CaseDetails caseData = caseDetails(migrationId, familyManNumber, null);

            assertThatThrownBy(() -> postAboutToSubmitEvent(caseData))
                .getRootCause()
                .hasMessage("No C2 document bundles in the case");
        }

        private CaseDetails caseDetails(String migrationId,
                                        String familyManCaseNumber,
                                        List<Element<C2DocumentBundle>> bundles) {
            CaseDetails caseDetails = asCaseDetails(CaseData.builder()
                .familyManCaseNumber(familyManCaseNumber)
                .c2DocumentBundle(bundles)
                .build());

            caseDetails.getData().put("migrationId", migrationId);
            return caseDetails;
        }
    }

    private C2DocumentBundle createC2DocumentBundle() {
        return C2DocumentBundle.builder().build();
    }
}
