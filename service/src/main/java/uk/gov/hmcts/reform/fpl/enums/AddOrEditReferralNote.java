package uk.gov.hmcts.reform.fpl.enums;

import lombok.Getter;

@Getter
public enum AddOrEditReferralNote {
    ADD_NOTE("Add a note"),
    RESPOND_TO_NOTE("Respond to a note");

    private final String label;

    AddOrEditReferralNote(String label) {
        this.label = label;
    }
}
