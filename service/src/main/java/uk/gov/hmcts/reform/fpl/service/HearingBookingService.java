package uk.gov.hmcts.reform.fpl.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fpl.exceptions.NoHearingBookingException;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.Judge;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;
import uk.gov.hmcts.reform.fpl.model.order.selector.Selector;
import uk.gov.hmcts.reform.fpl.service.time.Time;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.DATE;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateTimeBaseUsingFormat;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.unwrapElements;
import static uk.gov.hmcts.reform.fpl.utils.JudgeAndLegalAdvisorHelper.getSelectedJudge;
import static uk.gov.hmcts.reform.fpl.utils.JudgeAndLegalAdvisorHelper.prepareJudgeFields;
import static uk.gov.hmcts.reform.fpl.utils.JudgeAndLegalAdvisorHelper.removeAllocatedJudgeProperties;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class HearingBookingService {
    public static final String HEARING_DETAILS_KEY = "hearingDetails";

    private final Time time;

    public List<Element<HearingBooking>> expandHearingBookingCollection(CaseData caseData) {
        return ofNullable(caseData.getHearingDetails())
            .orElse(newArrayList(element(HearingBooking.builder().build())));
    }

    public List<Element<HearingBooking>> getPastHearings(List<Element<HearingBooking>> hearingDetails) {
        return hearingDetails.stream().filter(this::isPastHearing).collect(toList());
    }

    public HearingBooking getMostUrgentHearingBooking(List<Element<HearingBooking>> hearingDetails) {
        return unwrapElements(hearingDetails).stream()
            .filter(hearing -> hearing.getStartDate().isAfter(time.now()))
            .min(comparing(HearingBooking::getStartDate))
            .orElseThrow(NoHearingBookingException::new);
    }

    public Optional<HearingBooking> getFirstHearing(List<Element<HearingBooking>> hearingDetails) {
        return unwrapElements(hearingDetails).stream()
            .min(comparing(HearingBooking::getStartDate));
    }

    public HearingBooking getHearingBookingByUUID(List<Element<HearingBooking>> hearingDetails, UUID elementId) {
        return hearingDetails.stream()
            .filter(hearingBookingElement -> hearingBookingElement.getId().equals(elementId))
            .map(Element::getValue)
            .findFirst()
            .orElse(null);
    }

    /**
     * Combines two lists of hearings into one, ordered by start date.
     * Implemented due to work around with hearing start date validation.
     *
     * @param newHearings the first list of hearing bookings to combine.
     * @param oldHearings the second list of hearing bookings to combine.
     * @return an ordered list of hearing bookings.
     */
    public List<Element<HearingBooking>> combineHearingDetails(List<Element<HearingBooking>> newHearings,
                                                               List<Element<HearingBooking>> oldHearings) {
        List<Element<HearingBooking>> combinedHearingDetails = newArrayList();
        combinedHearingDetails.addAll(newHearings);

        oldHearings.forEach(hearing -> {
            UUID id = hearing.getId();
            if (combinedHearingDetails.stream().noneMatch(oldHearing -> oldHearing.getId().equals(id))) {
                combinedHearingDetails.add(hearing);
            }
        });

        combinedHearingDetails.sort(comparing(element -> element.getValue().getStartDate()));

        return combinedHearingDetails;
    }

    public List<Element<HearingBooking>> setHearingJudge(List<Element<HearingBooking>> hearingBookings,
                                                         Judge allocatedJudge) {
        return hearingBookings.stream()
            .map(element -> {
                HearingBooking hearingBooking = element.getValue();

                JudgeAndLegalAdvisor selectedJudge =
                    getSelectedJudge(hearingBooking.getJudgeAndLegalAdvisor(), allocatedJudge);

                removeAllocatedJudgeProperties(selectedJudge);
                hearingBooking.setJudgeAndLegalAdvisor(selectedJudge);

                return buildHearingBookingElement(element.getId(), hearingBooking);
            }).collect(toList());
    }

    public List<Element<HearingBooking>> resetHearingJudge(List<Element<HearingBooking>> hearingBookings,
                                                           Judge allocatedJudge) {
        return hearingBookings.stream()
            .map(element -> {
                HearingBooking hearingBooking = element.getValue();
                JudgeAndLegalAdvisor judgeAndLegalAdvisor = hearingBooking.getJudgeAndLegalAdvisor();

                if (judgeAndLegalAdvisor != null) {
                    judgeAndLegalAdvisor = prepareJudgeFields(judgeAndLegalAdvisor, allocatedJudge);
                    hearingBooking.setJudgeAndLegalAdvisor(judgeAndLegalAdvisor);
                    return buildHearingBookingElement(element.getId(), hearingBooking);
                }

                return element;
            }).collect(toList());
    }

    public List<Element<HearingBooking>> getNewHearings(List<Element<HearingBooking>> newHearings, List<Element<HearingBooking>> oldHearings) {
        List<UUID> oldHearingIDs = oldHearings.stream()
            .map(Element::getId)
            .collect(Collectors.toList());

        return newHearings.stream()
            .filter(newHearing -> !oldHearingIDs.contains(newHearing.getId()))
            .collect(Collectors.toList());
    }

    public String getHearingNoticeLabel(List<Element<HearingBooking>> newHearings, List<Element<HearingBooking>> oldHearings) {
        return range(oldHearings.size(), newHearings.size())
            .mapToObj(index -> format("Hearing %d: %s hearing %s",
                index + 1,
                newHearings.get(index).getValue().getType().getLabel(),
                formatLocalDateTimeBaseUsingFormat(newHearings.get(index).getValue().getStartDate(), DATE)))
            .collect(joining("\n"));

    }

    public List<HearingBooking> getSelectedHearings(CaseData caseData) {
        Selector hearingSelector = caseData.getNewHearingSelector();
        List<Element<HearingBooking>> hearings = caseData.getHearingDetails();
        if (hearingSelector == null) {
            return List.of();
        } else {
            return hearingSelector.getSelected().stream()
                .map(hearings::get)
                .map(Element::getValue)
                .collect(toList());
        }
    }


    private boolean isPastHearing(Element<HearingBooking> element) {
        return ofNullable(element.getValue())
            .map(HearingBooking::getStartDate)
            .filter(hearingDate -> hearingDate.isBefore(time.now()))
            .isPresent();
    }

    private Element<HearingBooking> buildHearingBookingElement(UUID id, HearingBooking hearingBooking) {
        return Element.<HearingBooking>builder()
            .id(id)
            .value(hearingBooking)
            .build();
    }
}
