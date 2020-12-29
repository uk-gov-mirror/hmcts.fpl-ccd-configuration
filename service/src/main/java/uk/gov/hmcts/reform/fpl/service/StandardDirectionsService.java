package uk.gov.hmcts.reform.fpl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.DirectionAssignee;
import uk.gov.hmcts.reform.fpl.enums.DirectionType;
import uk.gov.hmcts.reform.fpl.enums.YesNo;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.CustomDirection;
import uk.gov.hmcts.reform.fpl.model.Direction;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.NewDirection;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.configuration.DirectionConfiguration;
import uk.gov.hmcts.reform.fpl.model.configuration.Display;
import uk.gov.hmcts.reform.fpl.service.calendar.CalendarService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.ALL_PARTIES;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.CAFCASS;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.COURT;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.LOCAL_AUTHORITY;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.OTHERS;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.PARENTS_AND_RESPONDENTS;
import static uk.gov.hmcts.reform.fpl.enums.YesNo.YES;
import static uk.gov.hmcts.reform.fpl.model.Directions.getAssigneeToDirectionMapping;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StandardDirectionsService {
    private final CalendarService calendarService;
    private final OrdersLookupService ordersLookupService;
    private final CommonDirectionService commonDirectionService;
    private final ObjectMapper objectMapper;

    public List<Element<Direction>> getDirections(HearingBooking hearingBooking) {
        LocalDateTime hearingStartDate = ofNullable(hearingBooking).map(HearingBooking::getStartDate).orElse(null);

        return ordersLookupService.getStandardDirectionOrder().getDirections()
            .stream()
            .map(configuration -> constructDirectionForCCD(hearingStartDate, configuration))
            .collect(toList());
    }

    public boolean hasEmptyDates(CaseData caseData) {
        return Stream.of(caseData.getAllParties(),
            caseData.getLocalAuthorityDirections(),
            caseData.getRespondentDirections(),
            caseData.getCafcassDirections(),
            caseData.getOtherPartiesDirections(),
            caseData.getCourtDirections())
            .flatMap(Collection::stream)
            .map(Element::getValue)
            .map(Direction::getDateToBeCompletedBy)
            .anyMatch(Objects::isNull);
    }

    public Map<String, List<Element<Direction>>> populateStandardDirections(CaseData caseData) {
        return getAssigneeToDirectionMapping(getDirections(caseData.getFirstHearing().orElse(null)))
            .entrySet().stream().collect(toMap(pair -> pair.getKey().getValue(), Map.Entry::getValue));
    }


    public Map<DirectionAssignee, List<DirectionType>> getSelectedDirections(CaseDetails caseDetails) {

        return Map.of(
            ALL_PARTIES, "sdoDirectionsAll",
            LOCAL_AUTHORITY, "sdoDirectionsLocalAuthority",
            PARENTS_AND_RESPONDENTS, "sdoDirectionsRespondents",
            CAFCASS, "sdoDirectionsCafcass",
            OTHERS, "sdoDirectionsOthers",
            COURT, "sdoDirectionsCourt"
        ).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> directions(caseDetails, e.getValue())));
    }


    public Map<String, Object> getRequestedDirections(CaseData caseData, CaseDetails caseDetails) {

        Map<String, Object> data = new HashMap<>();

        Map<DirectionAssignee, List<DirectionType>> selectedDirections = getSelectedDirections(caseDetails);

        selectedDirections.get(ALL_PARTIES)
            .forEach(type -> data.put("sdoDirection-" + type, find(type, caseData.getAllParties())));

        selectedDirections.get(LOCAL_AUTHORITY)
            .forEach(type -> data.put("sdoDirection-" + type, find(type, caseData.getLocalAuthorityDirections())));

        selectedDirections.get(PARENTS_AND_RESPONDENTS)
            .forEach(type -> data.put("sdoDirection-" + type, find(type, caseData.getRespondentDirections())));

        selectedDirections.get(CAFCASS).stream()
            .forEach(type -> data.put("sdoDirection-" + type, find(type, caseData.getCafcassDirections())));

        selectedDirections.get(OTHERS)
            .forEach(type -> data.put("sdoDirection-" + type, find(type, caseData.getOtherPartiesDirections())));

        selectedDirections.get(COURT)
            .forEach(type -> data.put("sdoDirection-" + type, find(type, caseData.getCourtDirections())));


        List<Element<Direction>> customDirections = new ArrayList<>();

        customDirections.addAll(ObjectUtils.defaultIfNull(caseData.getAllPartiesCustom(), new ArrayList<>()));
        customDirections.addAll(ObjectUtils.defaultIfNull(caseData.getLocalAuthorityDirectionsCustom(), new ArrayList<>()));
        customDirections.addAll(ObjectUtils.defaultIfNull(caseData.getRespondentDirectionsCustom(), new ArrayList<>()));
        customDirections.addAll(ObjectUtils.defaultIfNull(caseData.getCafcassDirectionsCustom(), new ArrayList<>()));
        customDirections.addAll(ObjectUtils.defaultIfNull(caseData.getOtherPartiesDirectionsCustom(), new ArrayList<>()));
        customDirections.addAll(ObjectUtils.defaultIfNull(caseData.getCourtDirectionsCustom(), new ArrayList<>()));

        List<Element<CustomDirection>> allCustomDirections = customDirections.stream()
            .map(customDirection -> element(
                customDirection.getId(),
                CustomDirection.builder()
                    .title(customDirection.getValue().getDirectionType())
                    .description(customDirection.getValue().getDirectionText())
                    .assignee(customDirection.getValue().getAssignee())
                    .dateToBeCompletedBy(customDirection.getValue().getDateToBeCompletedBy())
                    .build()))
            .collect(Collectors.toList());

        data.put("sdoDirection-Custom", allCustomDirections);
        data.remove("allPartiesCustom");
        data.remove("localAuthorityDirectionsCustom");
        data.remove("respondentDirectionsCustom");
        data.remove("cafcassDirectionsCustom");
        data.remove("otherPartiesDirectionsCustom");
        data.remove("courtDirectionsCustom");

        return data;

    }

    public void addOrUpdateDirections(CaseData caseData, CaseDetails caseDetails) {

        List<Element<Direction>> allDirections = commonDirectionService.combineAllDirections(caseData);
        allDirections.forEach(direction -> direction.getValue().setDirectionNeeded(YesNo.NO.getValue()));

        Stream.of(DirectionType.values()).forEach(type -> {
            NewDirection newDirection = objectMapper.convertValue(caseDetails.getData().get("sdoDirection-" + type), NewDirection.class);
            DirectionConfiguration directionDef = ordersLookupService.getStandardDirectionOrder().getDirections().stream()
                .filter(def -> def.getId().equals(type))
                .findFirst()
                .orElse(null);

            if (newDirection != null) {
                Direction existingDirection = allDirections.stream()
                    .filter(direction -> direction.getValue().getDirectionType().equals(directionDef.getTitle()))
                    .map(Element::getValue)
                    .findFirst()
                    .orElse(null);

                existingDirection.setDirectionNeeded(YES.getValue());
                existingDirection.setDateToBeCompletedBy(newDirection.getDateToBeCompletedBy());
                existingDirection.setDirectionText(newDirection.getDescription());
            }
        });


        List<Element<CustomDirection>> customDirections = objectMapper.convertValue(
            caseDetails.getData().get("sdoDirection-Custom"),
            new TypeReference<>() {
            });


        if (ObjectUtils.isNotEmpty(caseData.getAllPartiesCustom())) {
            caseData.getAllPartiesCustom().clear();
        }
        if (ObjectUtils.isNotEmpty(caseData.getLocalAuthorityDirectionsCustom())) {
            caseData.getLocalAuthorityDirectionsCustom().clear();
        }
        if (ObjectUtils.isNotEmpty(caseData.getRespondentDirectionsCustom())) {
            caseData.getRespondentDirectionsCustom().clear();
        }
        if (ObjectUtils.isNotEmpty(caseData.getCafcassDirectionsCustom())) {
            caseData.getCafcassDirectionsCustom().clear();
        }
        if (ObjectUtils.isNotEmpty(caseData.getOtherPartiesDirectionsCustom())) {
            caseData.getOtherPartiesDirectionsCustom().clear();
        }
        if (ObjectUtils.isNotEmpty(caseData.getCourtDirectionsCustom())) {
            caseData.getCourtDirectionsCustom().clear();
        }

        customDirections.forEach(customDirection -> {

            List<Element<Direction>> directions = new ArrayList<>();
            if (customDirection.getValue().getAssignee().equals(ALL_PARTIES)) {
                directions = ObjectUtils.defaultIfNull(caseData.getAllPartiesCustom(), new ArrayList<>());
                caseData.setAllPartiesCustom(directions);
            }
            if (customDirection.getValue().getAssignee().equals(LOCAL_AUTHORITY)) {
                directions = ObjectUtils.defaultIfNull(caseData.getLocalAuthorityDirectionsCustom(), new ArrayList<>());
                caseData.setLocalAuthorityDirectionsCustom(directions);
            }
            if (customDirection.getValue().getAssignee().equals(PARENTS_AND_RESPONDENTS)) {
                directions = ObjectUtils.defaultIfNull(caseData.getRespondentDirectionsCustom(), new ArrayList<>());
                caseData.setRespondentDirectionsCustom(directions);
            }
            if (customDirection.getValue().getAssignee().equals(CAFCASS)) {
                directions = ObjectUtils.defaultIfNull(caseData.getCafcassDirectionsCustom(), new ArrayList<>());
                caseData.setCafcassDirectionsCustom(directions);
            }
            if (customDirection.getValue().getAssignee().equals(OTHERS)) {
                directions = ObjectUtils.defaultIfNull(caseData.getOtherPartiesDirectionsCustom(), new ArrayList<>());
                caseData.setOtherPartiesDirectionsCustom(directions);
            }
            if (customDirection.getValue().getAssignee().equals(COURT)) {
                directions = ObjectUtils.defaultIfNull(caseData.getCourtDirectionsCustom(), new ArrayList<>());
                caseData.setCafcassDirectionsCustom(directions);
            }


            directions.add(element(customDirection.getId(), Direction.builder()
                .directionText(customDirection.getValue().getDescription())
                .directionType(customDirection.getValue().getTitle())
                .dateToBeCompletedBy(customDirection.getValue().getDateToBeCompletedBy())
                .assignee(customDirection.getValue().getAssignee())
                .build()));


        });

    }


    public NewDirection find(DirectionType type, List<Element<Direction>> directions) {
        DirectionConfiguration conf = ordersLookupService.getStandardDirectionOrder().getDirections().stream()
            .filter(def -> def.getId().equals(type))
            .findFirst()
            .orElse(null);

        List<Element<Direction>> dirs = ObjectUtils.defaultIfNull(directions, new ArrayList<>());
        return dirs.stream().filter(d -> d.getValue().getDirectionType().equals(conf.getTitle()))
            .map(Element::getValue)
            .map(dir -> NewDirection.builder()
                .type(type)
                .description(dir.getDirectionText())
                .dateToBeCompletedBy(dir.getDateToBeCompletedBy())
                .readOnly(YesNo.from(conf.getDisplay().isShowDateOnly()).getValue())
                .build())
            .findFirst()
            .orElse(null);
    }

    public List<Element<Direction>> filter(List<Element<Direction>> directions, List<DirectionType> directionTypes) {

        Set<String> titles = ordersLookupService.getStandardDirectionOrder().getDirections().stream()
            .filter(def -> directionTypes.contains(def.getId()))
            .map(def -> def.getTitle())
            .collect(Collectors.toSet());

        List<Element<Direction>> dirs = ObjectUtils.defaultIfNull(directions, new ArrayList<>());

        return dirs.stream()
            .filter(d -> titles.contains(d.getValue().getDirectionType()))
            .collect(Collectors.toList());

    }

//    public Map<String, Object> flags(Map<DirectionAssignee, List<DirectionType>> requested){
//        return Map.of(
//            ALL_PARTIES, "sdoDirectionsAllNeeded",
//            LOCAL_AUTHORITY, "sdoDirectionsLocalAuthorityNeeded",
//            PARENTS_AND_RESPONDENTS, "sdoDirectionsRespondentsNeeded",
//            CAFCASS, "sdoDirectionsCafcassNeeded",
//            OTHERS, "sdoDirectionsOthers",
//            COURT, "sdoDirectionsCourt"
//        )
//    }

    @SuppressWarnings("unchecked")
    private List<DirectionType> directions(CaseDetails caseDetails, String filed) {
        return ((List<String>) caseDetails.getData().get(filed)).stream()
            .map(t -> EnumUtils.getEnum(DirectionType.class, t))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Element<Direction> constructDirectionForCCD(LocalDateTime hearingDate, DirectionConfiguration direction) {
        LocalDateTime dateToBeCompletedBy = ofNullable(hearingDate)
            .map(date -> getCompleteByDate(date, direction.getDisplay()))
            .orElse(null);

        return element(Direction.builder()
            .directionType(direction.getTitle())
            .directionText(direction.getText())
            .assignee(direction.getAssignee())
            .directionNeeded(YES.getValue())
            .directionRemovable(booleanToYesOrNo(direction.getDisplay().isDirectionRemovable()))
            .readOnly(booleanToYesOrNo(direction.getDisplay().isShowDateOnly()))
            .dateToBeCompletedBy(dateToBeCompletedBy)
            .build());
    }

    private LocalDateTime getCompleteByDate(LocalDateTime startDate, Display display) {
        return ofNullable(display.getDelta())
            .map(delta -> addDelta(startDate, parseInt(delta)))
            .map(date -> getLocalDateTime(display.getTime(), date))
            .orElse(null);
    }

    private LocalDateTime getLocalDateTime(String time, LocalDate date) {
        return ofNullable(time).map(item -> LocalDateTime.of(date, LocalTime.parse(item))).orElse(date.atStartOfDay());
    }

    private LocalDate addDelta(LocalDateTime date, int delta) {
        if (delta == 0) {
            return date.toLocalDate();
        }
        return calendarService.getWorkingDayFrom(date.toLocalDate(), delta);
    }

    private String booleanToYesOrNo(boolean value) {
        return value ? "Yes" : "No";
    }
}
