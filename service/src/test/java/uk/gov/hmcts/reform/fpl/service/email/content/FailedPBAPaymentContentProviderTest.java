package uk.gov.hmcts.reform.fpl.service.email.content;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.fpl.enums.ApplicationType;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.notify.payment.FailedPBANotificationData;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.enums.ApplicationType.C110A_APPLICATION;
import static uk.gov.hmcts.reform.fpl.enums.ApplicationType.C2_APPLICATION;

@ContextConfiguration(classes = {FailedPBAPaymentContentProvider.class})
class FailedPBAPaymentContentProviderTest extends AbstractEmailContentProviderTest {

    @Autowired
    private FailedPBAPaymentContentProvider contentProvider;

    @Test
    void shouldReturnExpectedMapWithValidCtscNotificationParameters() {
        final ApplicationType applicationType = C2_APPLICATION;
        final CaseData caseData = CaseData.builder()
            .id(RandomUtils.nextLong())
            .build();

        final FailedPBANotificationData expectedParameters = FailedPBANotificationData.builder()
            .applicationType(applicationType.getType())
            .caseUrl(caseUrl(caseData.getId().toString(), "C2Tab"))
            .build();

        final FailedPBANotificationData actualParameters = contentProvider
            .buildCtscNotificationParameters(caseData, applicationType);


        assertThat(actualParameters).isEqualTo(expectedParameters);
    }

    @Test
    void shouldReturnExpectedMapWithValidLANotificationParameters() {
        final ApplicationType applicationType = C110A_APPLICATION;
        final FailedPBANotificationData expectedParameters = FailedPBANotificationData.builder()
            .applicationType(applicationType.getType())
            .build();

        final FailedPBANotificationData actualParameters = contentProvider
            .buildLANotificationParameters(applicationType);

        assertThat(actualParameters).isEqualTo(expectedParameters);
    }
}
