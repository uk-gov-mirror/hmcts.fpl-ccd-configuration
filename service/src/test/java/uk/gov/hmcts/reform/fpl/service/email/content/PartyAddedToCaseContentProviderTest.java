package uk.gov.hmcts.reform.fpl.service.email.content;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.DIGITAL_SERVICE;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.EMAIL;
import static uk.gov.hmcts.reform.fpl.service.email.content.AbstractEmailContentProviderTest.BASE_URL;
import static uk.gov.hmcts.reform.fpl.utils.CoreCaseDataStoreLoader.callbackRequest;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {JacksonAutoConfiguration.class, PartyAddedToCaseContentProvider.class})
@TestPropertySource(properties = {"ccd.ui.base.url=" + BASE_URL})
class PartyAddedToCaseContentProviderTest extends AbstractEmailContentProviderTest {

    @Autowired
    private PartyAddedToCaseContentProvider partyAddedToCaseContentProvider;

    @Test
    void shouldGetPartyAddedToCaseByEmailNotificationParameters() {
        final Map<String, Object> expectedParameters = ImmutableMap.<String, Object>builder()
            .put("firstRespondentLastName", "Smith")
            .put("familyManCaseNumber", "12345L")
            .build();

        assertThat(partyAddedToCaseContentProvider.getPartyAddedToCaseNotificationParameters(
            callbackRequest().getCaseDetails(), EMAIL)).isEqualTo(expectedParameters);
    }

    @Test
    void shouldGetPartyAddedToCaseThroughDigitalServiceNotificationParameters() {
        final Map<String, Object> expectedParameters = ImmutableMap.<String, Object>builder()
            .put("firstRespondentLastName", "Smith")
            .put("familyManCaseNumber", "12345L")
            .put("caseUrl", buildCaseUrl(CASE_REFERENCE))
            .build();

        assertThat(partyAddedToCaseContentProvider.getPartyAddedToCaseNotificationParameters(
            callbackRequest().getCaseDetails(), DIGITAL_SERVICE)).isEqualTo(expectedParameters);
    }
}
