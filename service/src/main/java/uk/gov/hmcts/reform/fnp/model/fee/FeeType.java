package uk.gov.hmcts.reform.fnp.model.fee;

import com.google.common.collect.ImmutableList;
import uk.gov.hmcts.reform.fpl.enums.C2ApplicationType;
import uk.gov.hmcts.reform.fpl.enums.C2OrdersRequested;
import uk.gov.hmcts.reform.fpl.enums.OrderType;
import uk.gov.hmcts.reform.fpl.enums.OtherApplicationType;
import uk.gov.hmcts.reform.fpl.enums.ParentalResponsibilityType;
import uk.gov.hmcts.reform.fpl.enums.SecureAccommodationType;
import uk.gov.hmcts.reform.fpl.enums.Supplements;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static uk.gov.hmcts.reform.fpl.enums.C2ApplicationType.WITH_NOTICE;
import static uk.gov.hmcts.reform.fpl.enums.C2OrdersRequested.CHANGE_SURNAME_OR_REMOVE_JURISDICTION;
import static uk.gov.hmcts.reform.fpl.enums.C2OrdersRequested.TERMINATION_OF_APPOINTMENT_OF_GUARDIAN;
import static uk.gov.hmcts.reform.fpl.enums.OtherApplicationType.C100_CHILD_ARRANGEMENTS;
import static uk.gov.hmcts.reform.fpl.enums.OtherApplicationType.C12_WARRANT_TO_ASSIST_PERSON;
import static uk.gov.hmcts.reform.fpl.enums.OtherApplicationType.C17A_EXTENSION_OF_ESO;
import static uk.gov.hmcts.reform.fpl.enums.OtherApplicationType.C17_EDUCATION_SUPERVISION_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.OtherApplicationType.C19_WARRANT_TO_ASSISTANCE;
import static uk.gov.hmcts.reform.fpl.enums.OtherApplicationType.C1_APPOINTMENT_OF_A_GUARDIAN;
import static uk.gov.hmcts.reform.fpl.enums.OtherApplicationType.C1_CHANGE_SURNAME_OR_REMOVE_FROM_JURISDICTION;
import static uk.gov.hmcts.reform.fpl.enums.OtherApplicationType.C1_TERMINATION_OF_APPOINTMENT_OF_A_GUARDIAN;
import static uk.gov.hmcts.reform.fpl.enums.OtherApplicationType.C3_SEARCH_TAKE_CHARGE_AND_DELIVERY_OF_A_CHILD;

public enum FeeType {
    // Names should match OrderType enum names exactly
    C2_WITHOUT_NOTICE,
    C2_WITH_NOTICE,
    CARE_ORDER,
    EDUCATION_SUPERVISION_ORDER,
    EMERGENCY_PROTECTION_ORDER,
    INTERIM_CARE_ORDER,
    INTERIM_SUPERVISION_ORDER,
    OTHER,
    PLACEMENT,
    SUPERVISION_ORDER,
    ESO_EXTENSION,
    CHILD_ARRANGEMENTS,
    WARRANT_OF_ASSISTANCE,
    RECOVERY_ORDER,
    WARRANT_TO_ASSIST_PERSON,
    CHILD_ASSESSMENT,
    CONTACT_WITH_CHILD_IN_CARE,
    CHANGE_SURNAME,
    SECURE_ACCOMMODATION_ENGLAND,
    SPECIAL_GUARDIANSHIP,
    APPOINTMENT_OF_GUARDIAN,
    PARENTAL_RESPONSIBILITY_FATHER,
    PARENTAL_RESPONSIBILITY_FEMALE_PARENT,
    SECURE_ACCOMMODATION_WALES;

    private static final Map<OrderType, FeeType> orderToFeeMap;

    static {
        orderToFeeMap = Map.of(
            OrderType.CARE_ORDER, CARE_ORDER,
            OrderType.EDUCATION_SUPERVISION_ORDER, EDUCATION_SUPERVISION_ORDER,
            OrderType.EMERGENCY_PROTECTION_ORDER, EMERGENCY_PROTECTION_ORDER,
            OrderType.INTERIM_CARE_ORDER, INTERIM_CARE_ORDER,
            OrderType.INTERIM_SUPERVISION_ORDER, INTERIM_SUPERVISION_ORDER,
            OrderType.SUPERVISION_ORDER, SUPERVISION_ORDER,
            OrderType.OTHER, OTHER
        );
    }

    private static final Map<Supplements, FeeType> supplementToFeeMap;

    static {
        supplementToFeeMap = Map.of(
            Supplements.C13A_SPECIAL_GUARDIANSHIP, SPECIAL_GUARDIANSHIP,
            Supplements.C14_AUTHORITY_TO_REFUSE_CONTACT_WITH_CHILD, CONTACT_WITH_CHILD_IN_CARE,
            Supplements.C15_CONTACT_WITH_CHILD_IN_CARE, CONTACT_WITH_CHILD_IN_CARE,
            Supplements.C16_CHILD_ASSESSMENT, CHILD_ASSESSMENT,
            Supplements.C18_RECOVERY_ORDER, RECOVERY_ORDER
        );
    }

    private static final Map<OtherApplicationType, FeeType> applicationToFeeMap;

    static {
        applicationToFeeMap = Map.of(
            C1_CHANGE_SURNAME_OR_REMOVE_FROM_JURISDICTION, CHANGE_SURNAME,
            C1_APPOINTMENT_OF_A_GUARDIAN, APPOINTMENT_OF_GUARDIAN,
            C1_TERMINATION_OF_APPOINTMENT_OF_A_GUARDIAN, APPOINTMENT_OF_GUARDIAN,
            C3_SEARCH_TAKE_CHARGE_AND_DELIVERY_OF_A_CHILD, C2_WITH_NOTICE,
            C12_WARRANT_TO_ASSIST_PERSON, WARRANT_TO_ASSIST_PERSON,
            C17_EDUCATION_SUPERVISION_ORDER, EDUCATION_SUPERVISION_ORDER,
            C17A_EXTENSION_OF_ESO, ESO_EXTENSION,
            C19_WARRANT_TO_ASSISTANCE, WARRANT_OF_ASSISTANCE,
            C100_CHILD_ARRANGEMENTS, CHILD_ARRANGEMENTS);
    }

    private static final Map<C2OrdersRequested, FeeType> c2OrdersRequestedToFeesMap;

    static {
        c2OrdersRequestedToFeesMap = Map.of(
            CHANGE_SURNAME_OR_REMOVE_JURISDICTION, CHANGE_SURNAME,
            C2OrdersRequested.APPOINTMENT_OF_GUARDIAN, APPOINTMENT_OF_GUARDIAN,
            TERMINATION_OF_APPOINTMENT_OF_GUARDIAN, APPOINTMENT_OF_GUARDIAN);
    }

    public static List<FeeType> fromOrderType(List<OrderType> orderTypes) {
        if (isEmpty(orderTypes)) {
            return ImmutableList.of();
        }

        return orderTypes.stream()
            .map(orderToFeeMap::get)
            .collect(toUnmodifiableList());
    }

    public static FeeType fromC2ApplicationType(C2ApplicationType c2ApplicationType) {
        if (c2ApplicationType == WITH_NOTICE) {
            return C2_WITH_NOTICE;
        }
        return C2_WITHOUT_NOTICE;
    }

    public static List<FeeType> fromC2OrdersRequestedType(List<C2OrdersRequested> c2OrdersRequestedList) {
        if (isEmpty(c2OrdersRequestedList)) {
            return ImmutableList.of();
        }

        return c2OrdersRequestedList.stream()
            .map(c2OrdersRequestedToFeesMap::get)
            .collect(toUnmodifiableList());
    }

    public static Optional<FeeType> fromApplicationType(OtherApplicationType applicationType) {
        if (!applicationToFeeMap.containsKey(applicationType)) {
            return Optional.empty();
        }
        return Optional.of(applicationToFeeMap.get(applicationType));
    }

    public static FeeType fromParentalResponsibilityTypes(ParentalResponsibilityType parentalResponsibilityType) {
        if (ParentalResponsibilityType.PR_BY_FATHER == parentalResponsibilityType) {
            return PARENTAL_RESPONSIBILITY_FATHER;
        }
        return PARENTAL_RESPONSIBILITY_FEMALE_PARENT;
    }

    public static List<FeeType> fromSupplementTypes(List<Supplements> supplementTypes) {
        if (isEmpty(supplementTypes)) {
            return ImmutableList.of();
        }

        return supplementTypes.stream()
            .map(supplementToFeeMap::get)
            .collect(toUnmodifiableList());
    }

    public static List<FeeType> fromSecureAccommodationTypes(List<SecureAccommodationType> accommodationTypes) {
        if (isEmpty(accommodationTypes)) {
            return ImmutableList.of();
        }

        return accommodationTypes.stream()
            .map(FeeType::getSecureAccommodationFeeType)
            .collect(toUnmodifiableList());
    }

    private static FeeType getSecureAccommodationFeeType(SecureAccommodationType secureAccommodationType) {
        if (SecureAccommodationType.SECTION_25_ENGLAND == secureAccommodationType) {
            return SECURE_ACCOMMODATION_ENGLAND;
        }
        return SECURE_ACCOMMODATION_WALES;
    }
}
