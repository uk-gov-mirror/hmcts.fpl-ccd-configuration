package uk.gov.hmcts.reform.fpl.handlers;

import com.google.common.collect.ImmutableList;
import uk.gov.hmcts.reform.fpl.model.Representative;
import uk.gov.hmcts.reform.fpl.model.notify.allocatedjudge.AllocatedJudgeTemplate;

import java.util.List;

import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.DIGITAL_SERVICE;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.EMAIL;

public class NotificationEventHandlerTestData {
    public static final String LOCAL_AUTHORITY_CODE = "example";
    public static final String LOCAL_AUTHORITY_NAME = "Example Local Authority";
    public static final String COURT_EMAIL_ADDRESS = "admin@family-court.com";
    public static final String COURT_NAME = "Family Court";
    public static final String AUTH_TOKEN = "Bearer token";
    public static final String CAFCASS_EMAIL_ADDRESS = "FamilyPublicLaw+cafcass@gmail.com";
    public static final String CAFCASS_NAME = "cafcass";
    public static final String GATEKEEPER_EMAIL_ADDRESS = "FamilyPublicLaw+gatekeeper@gmail.com";
    public static final String LOCAL_AUTHORITY_EMAIL_ADDRESS = "FamilyPublicLaw+sa@gmail.com";
    public static final String ALLOCATED_JUDGE_EMAIL_ADDRESS = "judge@gmail.com";
    public static final String COURT_CODE = "11";
    public static final String CTSC_INBOX = "Ctsc+test@gmail.com";
    public static final String PARTY_ADDED_TO_CASE_BY_EMAIL_ADDRESS = "barney@rubble.com";
    public static final String PARTY_ADDED_TO_CASE_THROUGH_DIGITAL_SERVICE_EMAIL = "fred@flinstone.com";
    public static final byte[] DOCUMENT_CONTENTS = {1, 2, 3, 4, 5};

    private NotificationEventHandlerTestData() {
    }

    public static AllocatedJudgeTemplate getExpectedAllocatedJudgeNotificationParameters() {
        return AllocatedJudgeTemplate.builder()
            .judgeTitle("Her Honour Judge")
            .judgeName("Moley")
            .caseName("test")
            .caseUrl("http://fake-url/cases/case-details/12345")
            .familyManCaseNumber("12345L")
            .build();
    }

    public static List<Representative> expectedRepresentatives() {
        return ImmutableList.of(Representative.builder()
            .email("abc@example.com")
            .fullName("Jon Snow")
            .servingPreferences(DIGITAL_SERVICE)
            .build());
    }

    public static List<Representative> getExpectedEmailRepresentativesForAddingPartiesToCase() {
        return ImmutableList.of(
            Representative.builder()
                .email("barney@rubble.com")
                .fullName("Barney Rubble")
                .servingPreferences(EMAIL)
                .build());
    }

    public static List<Representative> getExpectedDigitalRepresentativesForAddingPartiesToCase() {
        return ImmutableList.of(
            Representative.builder()
                .email("fred@flinstone.com")
                .fullName("Fred Flinstone")
                .servingPreferences(DIGITAL_SERVICE)
                .build());
    }
}
