package uk.gov.hmcts.reform.fpl.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.config.CafcassLookupConfiguration;
import uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences;
import uk.gov.hmcts.reform.fpl.events.NewHearingsAddedEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.event.EventData;
import uk.gov.hmcts.reform.fpl.model.notify.hearing.NewNoticeOfHearingTemplate;
import uk.gov.hmcts.reform.fpl.service.HearingBookingService;
import uk.gov.hmcts.reform.fpl.service.InboxLookupService;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.service.email.content.NewNoticeOfHearingEmailContentProvider;
import uk.gov.hmcts.reform.fpl.service.representative.RepresentativeNotificationService;

import java.util.List;

import static uk.gov.hmcts.reform.fpl.NotifyTemplates.NOTICE_OF_NEW_HEARING;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.DIGITAL_SERVICE;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.EMAIL;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NewHearingsAddedEventHandler {

    private static final List<RepresentativeServingPreferences> SERVING_PREFERENCES = List.of(EMAIL, DIGITAL_SERVICE);

    private final NewNoticeOfHearingEmailContentProvider newHearingContent;
    private final HearingBookingService hearingBookingService;
    private final ObjectMapper mapper;
    private final NotificationService notificationService;
    private final RepresentativeNotificationService representativeNotificationService;
    private final InboxLookupService inboxLookupService;
    private final CafcassLookupConfiguration cafcassLookupConfiguration;

    @Async
    @EventListener
    public void sendEmail(final NewHearingsAddedEvent event) {
        EventData eventData = new EventData(event);

        final CaseDetails caseDetails = mapper
            .convertValue(event.getCallbackRequest().getCaseDetails(), CaseDetails.class);
        final CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        List<Element<HearingBooking>> hearings = caseData.getHearingDetails();
        hearings.removeAll(hearingBookingService.getPastHearings(caseData.getHearingDetails()));

        List<Element<HearingBooking>> selectedHearings = hearingBookingService.getSelectedHearings(
            caseData.getNewHearingSelector(), hearings);

        selectedHearings.forEach(hearing -> {
            sendNotificationToLA(eventData, caseDetails, hearing.getValue());
            sendNotificationToCafcass(eventData, caseDetails, hearing.getValue());
            sendNotificationToRepresentatives(eventData, caseDetails, hearing.getValue());
        });
    }

    private void sendNotificationToLA(EventData eventData,
                                      CaseDetails caseDetails,
                                      HearingBooking hearing) {
        String email = inboxLookupService.getNotificationRecipientEmail(caseDetails,
            eventData.getLocalAuthorityCode());

        NewNoticeOfHearingTemplate templateData = newHearingContent.buildNewNoticeOfHearingNotification(caseDetails,
            hearing, DIGITAL_SERVICE);

        notificationService.sendEmail(NOTICE_OF_NEW_HEARING, email, templateData, eventData.getReference());
    }

    private void sendNotificationToCafcass(EventData eventData, CaseDetails caseDetails, HearingBooking hearing) {
        String email = cafcassLookupConfiguration.getCafcass(eventData.getLocalAuthorityCode()).getEmail();

        NewNoticeOfHearingTemplate templateData = newHearingContent.buildNewNoticeOfHearingNotification(caseDetails,
            hearing, EMAIL);

        notificationService.sendEmail(NOTICE_OF_NEW_HEARING, email, templateData, eventData.getReference());
    }

    private void sendNotificationToRepresentatives(
        EventData eventData, CaseDetails caseDetails, HearingBooking hearingBooking) {

        SERVING_PREFERENCES.forEach(
            servingPreference -> {
                NewNoticeOfHearingTemplate templateParameters = newHearingContent.buildNewNoticeOfHearingNotification(
                    caseDetails, hearingBooking, servingPreference);

                representativeNotificationService
                    .sendToRepresentativesByServedPreference(servingPreference, NOTICE_OF_NEW_HEARING,
                        templateParameters.toMap(mapper), eventData);
            }
        );
    }
}
