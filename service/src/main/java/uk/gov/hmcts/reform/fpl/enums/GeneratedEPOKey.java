package uk.gov.hmcts.reform.fpl.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GeneratedEPOKey {
    DATE_AND_TIME_OF_ISSUE("dateAndTimeOfIssue"),
    EPO_REMOVAL_ADDRESS("epoRemovalAddress"),
    EPO_CHILDREN("epoChildren"),
    EPO_END_DATE("epoEndDate"),
    EPO_PHRASE("epoPhrase"),
    EPO_TYPE("epoType");

    private final String key;
}
