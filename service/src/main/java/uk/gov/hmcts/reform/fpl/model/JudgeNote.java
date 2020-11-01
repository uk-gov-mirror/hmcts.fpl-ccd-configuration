package uk.gov.hmcts.reform.fpl.model;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDate;

@Data
@Builder
@Setter
public class JudgeNote {
    private final String createdBy;
    private final LocalDate date;
    private final String note;
    private final String judgeEmailForReferral;
    private final String judgeResponse;
    private final String responseBy;

    public String toLabel(int counter) {
        return "Note " + counter + " for " + judgeEmailForReferral;
    }
}
