package uk.gov.hmcts.reform.fpl.service.orders;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock;
import uk.gov.hmcts.reform.fpl.model.order.selector.Selector;
import uk.gov.hmcts.reform.fpl.service.ChildrenService;

import java.util.Map;

import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.WHICH_CHILDREN;
import static uk.gov.hmcts.reform.fpl.model.order.selector.Selector.newSelector;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WhichChildrenBlockPrePopulator implements QuestionBlockOrderPrePopulator {

    private final ChildrenService childrenService;

    @Override
    public OrderQuestionBlock accept() {
        return WHICH_CHILDREN;
    }

    @Override
    public Map<String, Object> prePopulate(CaseData caseData, CaseDetails caseDetails) {
        final Selector childSelector = newSelector(caseData.getAllChildren().size());

        // Are all closable???
        //        boolean closable = caseData.getOrderTypeAndDocument().isClosable();
        boolean closable = true;

        if (closable) {
            childSelector.setHidden(childrenService.getIndexesOfChildrenWithFinalOrderIssued(caseData));
        }
        return Map.of(
            "childSelector", childSelector,
            "children_label", childrenService.getChildrenLabel(caseData.getAllChildren(), closable)
        );

    }
}
