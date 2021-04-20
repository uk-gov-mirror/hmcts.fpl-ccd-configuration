package uk.gov.hmcts.reform.fpl.service.orders.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.EPO_ORDER_DETAILS;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class EPOEndDateValidator implements QuestionBlockOrderValidator {

    private static final String INVALID_TIME_MESSAGE = "Enter a valid time";
    private static final String FUTURE_DATE_MESSAGE = "Enter an end date in the future";
    private static final String END_DATE_RANGE_MESSAGE = "Emergency protection orders cannot last longer than 8 days";

    private static final LocalTime MIDNIGHT = LocalTime.of(0, 0, 0);

    private static final Duration EPO_END_DATE_RANGE = Duration.of(8, ChronoUnit.DAYS);

    @Override
    public OrderQuestionBlock accept() {
        return EPO_ORDER_DETAILS;
    }

    @Override
    public List<String> validate(CaseData caseData) {
        final LocalDateTime epoApprovalTime = caseData.getManageOrdersEventData().getManageOrdersApprovalDateTime();
        final LocalDateTime epoEndTime = caseData.getManageOrdersEventData().getManageOrdersEndDateTime();

        List<String> errors = new ArrayList<>();

        if (epoEndTime == null) {
            return List.of();
        }

        if (epoEndTime.isBefore(LocalDateTime.now())) {
            errors.add(FUTURE_DATE_MESSAGE);
        }

        if (epoEndTime.toLocalTime().equals(MIDNIGHT)) {
            errors.add(INVALID_TIME_MESSAGE);
        }

        if (epoEndTime.isAfter(epoApprovalTime.plus(EPO_END_DATE_RANGE))) {
            return List.of(END_DATE_RANGE_MESSAGE);
        }

        return errors;
    }
}
