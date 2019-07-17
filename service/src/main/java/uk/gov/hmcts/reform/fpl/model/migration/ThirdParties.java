package uk.gov.hmcts.reform.fpl.model.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.fpl.model.ThirdParty;

@Data
@Builder
@AllArgsConstructor
public class ThirdParties {
    private final ThirdParty party;
}
