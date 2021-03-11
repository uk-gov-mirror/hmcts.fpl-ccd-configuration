package uk.gov.hmcts.reform.fpl.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import uk.gov.hmcts.reform.fpl.model.PBAPayment;

@Data
@Builder(toBuilder = true)
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdditionalApplicationsBundle {
    private final String uploadedDateTime;
    private final String author;
    private C2DocumentBundle c2Document;
    private OtherApplicationsBundle otherApplications;
    private PBAPayment pbaPayment;
}
