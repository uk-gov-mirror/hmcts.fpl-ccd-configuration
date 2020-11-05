package uk.gov.hmcts.reform.fpl.model.common.dynamic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

/**
 * Representation of a CCD Dynamic List which is then converted to a select dropdown list.
 */
@Data
@Jacksonized
@Builder
public class DynamicList2 {

    /**
     * The selected value for the dropdown.
     */
    private DynamicListElement2 value;

    /**
     * List of options for the dropdown.
     */
    @JsonProperty("list_items")
    private List<DynamicListElement2> listItems;

    @JsonIgnore
    public String getValueLabel() {
        return value == null ? null : value.getLabel();
    }

    @JsonIgnore
    public String getValueCode() {
        return value == null ? null : value.getCode();
    }
}
