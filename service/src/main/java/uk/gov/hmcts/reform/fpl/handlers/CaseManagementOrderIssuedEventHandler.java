package uk.gov.hmcts.reform.fpl.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.config.CafcassLookupConfiguration;
import uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences;
import uk.gov.hmcts.reform.fpl.events.CaseManagementOrderIssuedEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Representative;
import uk.gov.hmcts.reform.fpl.model.event.EventData;
import uk.gov.hmcts.reform.fpl.model.notify.cmo.IssuedCMOTemplate;
import uk.gov.hmcts.reform.fpl.model.order.CaseManagementOrder;
import uk.gov.hmcts.reform.fpl.service.DocumentDownloadService;
import uk.gov.hmcts.reform.fpl.service.InboxLookupService;
import uk.gov.hmcts.reform.fpl.service.RepresentativeService;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.service.email.content.CaseManagementOrderEmailContentProvider;

import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.fpl.NotifyTemplates.CMO_ORDER_ISSUED_NOTIFICATION_TEMPLATE;
import static uk.gov.hmcts.reform.fpl.enums.IssuedOrderType.CMO;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.DIGITAL_SERVICE;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.EMAIL;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CaseManagementOrderIssuedEventHandler {
    private final ObjectMapper mapper;
    private final InboxLookupService inboxLookupService;
    private final NotificationService notificationService;
    private final RepresentativeService representativeService;
    private final CaseManagementOrderEmailContentProvider caseManagementOrderEmailContentProvider;
    private final CafcassLookupConfiguration cafcassLookupConfiguration;
    private final IssuedOrderAdminNotificationHandler issuedOrderAdminNotificationHandler;
    private final DocumentDownloadService documentDownloadService;

    @EventListener
    public void sendEmailsForIssuedCaseManagementOrder(final CaseManagementOrderIssuedEvent event) {
        EventData eventData = new EventData(event);
        CaseManagementOrder issuedCmo = event.getCmo();

        //TODO Document is downloaded 5 times, this could impact performance. Investigate in FPLA-2061
        sendToLocalAuthority(eventData, issuedCmo);
        sendToCafcass(eventData, issuedCmo);
        sendToRepresentatives(eventData, issuedCmo, DIGITAL_SERVICE);
        sendToRepresentatives(eventData, issuedCmo, EMAIL);
        issuedOrderAdminNotificationHandler.sendToAdmin(eventData,
            documentDownloadService.downloadDocument(issuedCmo.getOrder().getBinaryUrl()), CMO);
    }

    private void sendToLocalAuthority(final EventData eventData, CaseManagementOrder cmo) {
        final IssuedCMOTemplate localAuthorityNotificationParameters = caseManagementOrderEmailContentProvider
            .buildCMOIssuedNotificationParameters(eventData.getCaseDetails(), cmo, DIGITAL_SERVICE);

        final String email = inboxLookupService.getNotificationRecipientEmail(eventData.getCaseDetails(),
            eventData.getLocalAuthorityCode());

        notificationService.sendEmail(CMO_ORDER_ISSUED_NOTIFICATION_TEMPLATE, email,
            localAuthorityNotificationParameters, eventData.getReference());
    }

    private void sendToCafcass(final EventData eventData, CaseManagementOrder cmo) {
        final IssuedCMOTemplate cafcassParameters =
            caseManagementOrderEmailContentProvider.buildCMOIssuedNotificationParameters(
                eventData.getCaseDetails(), cmo, EMAIL);

        final String cafcassEmail = cafcassLookupConfiguration.getCafcass(eventData.getLocalAuthorityCode()).getEmail();

        notificationService.sendEmail(CMO_ORDER_ISSUED_NOTIFICATION_TEMPLATE, cafcassEmail, cafcassParameters,
            eventData.getReference());
    }

    private void sendToRepresentatives(final EventData eventData,
                                       CaseManagementOrder cmo,
                                       RepresentativeServingPreferences servingPreference) {
        CaseData caseData = mapper.convertValue(eventData.getCaseDetails().getData(), CaseData.class);
        List<Representative> representatives = representativeService.getRepresentativesByServedPreference(
            caseData.getRepresentatives(), servingPreference);

        representatives.stream()
            .filter(representative -> isNotBlank(representative.getEmail()))
            .forEach(representative -> {
                IssuedCMOTemplate representativeNotificationParameters =
                    caseManagementOrderEmailContentProvider.buildCMOIssuedNotificationParameters(
                        eventData.getCaseDetails(), cmo, servingPreference);

                notificationService.sendEmail(CMO_ORDER_ISSUED_NOTIFICATION_TEMPLATE, representative.getEmail(),
                    representativeNotificationParameters, eventData.getReference());
            });
    }
}
