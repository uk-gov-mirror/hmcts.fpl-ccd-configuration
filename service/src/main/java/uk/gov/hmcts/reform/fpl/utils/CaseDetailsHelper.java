package uk.gov.hmcts.reform.fpl.utils;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.State;

import static uk.gov.hmcts.reform.fpl.enums.State.GATEKEEPING;
import static uk.gov.hmcts.reform.fpl.enums.State.OPEN;
import static uk.gov.hmcts.reform.fpl.enums.State.RETURNED;

@Component
public class CaseDetailsHelper {

    public static boolean isCaseNumber(String caseNumber) {
        return StringUtils.isNumeric(caseNumber) && caseNumber.length() == 16;
    }

    public String formatCCDCaseNumber(Long caseNumber) {
        String ccdCaseNumber = String.valueOf(caseNumber);

        if (!isCaseNumber(ccdCaseNumber)) {
            throw new IllegalArgumentException("CCD Case number must be 16 digits long");
        }

        return String.join("-", Splitter.fixedLength(4).splitToList(ccdCaseNumber));
    }

    public static void removeTemporaryFields(CaseDetails caseDetails, String... fields) {
        for (String field : fields) {
            caseDetails.getData().remove(field);
        }
    }

    public static void removeTemporaryFields(CaseDetailsMap caseDetails, String... fields) {
        for (String field : fields) {
            caseDetails.remove(field);
        }
    }

    public static boolean isInOpenState(CaseDetails caseDetails) {
        return isInState(caseDetails, OPEN);
    }

    public static boolean isInReturnedState(CaseDetails caseDetails) {
        return isInState(caseDetails, RETURNED);
    }

    public static boolean isInGatekeepingState(CaseDetails caseDetails) {
        return isInState(caseDetails, GATEKEEPING);
    }

    private static boolean isInState(CaseDetails caseDetails, State state) {
        return state.getValue().equals(caseDetails.getState());
    }
}
