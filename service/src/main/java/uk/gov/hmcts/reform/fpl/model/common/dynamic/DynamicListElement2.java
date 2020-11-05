package uk.gov.hmcts.reform.fpl.model.common.dynamic;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

/**
 * An element of the {@link DynamicList}.
 *
 * <p>There are two properties which map to the relevant items of an option html tag.
 */
@Data
@Jacksonized
@Builder
public class DynamicListElement2 {
    public static final DynamicListElement2 EMPTY = DynamicListElement2.builder().build();

    /**
     * Property that maps to the value attribute of the option tag.
     */
    private final String code;

    /**
     * Property that maps to the label attribute of the option tag.
     */
    private final String label;
}
