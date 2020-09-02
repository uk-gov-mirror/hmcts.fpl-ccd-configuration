package uk.gov.hmcts.reform.fpl.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.BLANK_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.CARE_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.DISCHARGE_OF_CARE_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.EMERGENCY_PROTECTION_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.SUPERVISION_ORDER;

class GeneratedOrderTypeTest {

    @ParameterizedTest
    @MethodSource("typeToEnumSource")
    void shouldConvertTypeStringToOrder(String type, GeneratedOrderType expectedOrderType) {
        assertThat(GeneratedOrderType.fromType(type)).isEqualTo(expectedOrderType);
    }

    @Test
    void blankOrdersAreRemovable() {
        assertThat(BLANK_ORDER.isRemovable()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, value = GeneratedOrderType.class, names = {"BLANK_ORDER"})
    void orderTypesThatAreNotBlankOrdersAreNotRemovable(GeneratedOrderType orderType) {
        assertThat(orderType.isRemovable()).isFalse();
    }

    private static Stream<Arguments> typeToEnumSource() {
        return Stream.of(
            Arguments.of("Blank order (C21)", BLANK_ORDER),
            Arguments.of("Interim care order", CARE_ORDER),
            Arguments.of("Final care order", CARE_ORDER),
            Arguments.of("Interim supervision order", SUPERVISION_ORDER),
            Arguments.of("Final supervision order", SUPERVISION_ORDER),
            Arguments.of("Emergency protection order", EMERGENCY_PROTECTION_ORDER),
            Arguments.of("Discharge of care order", DISCHARGE_OF_CARE_ORDER)
        );
    }
}
