package uk.gov.hmcts.reform.fpl.model.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.APPROVAL_DATE;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.APPROVER;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.FURTHER_DIRECTIONS;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.REVIEW_DRAFT_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.WHICH_CHILDREN;

@Getter
@RequiredArgsConstructor
public enum Order {
    C32_CARE_ORDER(
        List.of(APPROVER, APPROVAL_DATE, WHICH_CHILDREN, FURTHER_DIRECTIONS, REVIEW_DRAFT_ORDER),
        "Care order", "Section 31 Children Act 1989"
    );

    private final List<OrderQuestionBlock> questions;
    private final String title;
    private final String childrenAct;

    public Optional<OrderSection> nextSection(OrderSection currentSection) {
        Set<OrderSection> sectionsForOrder = this.getQuestions()
            .stream()
            .map(OrderQuestionBlock::getSection)
            .collect(Collectors.toSet());

        for (int i = 0; i < OrderSection.values().length - 1; i++) {
            if (currentSection.equals(OrderSection.values()[i])) { // current section found
                for (int j = i + 1; j < OrderSection.values().length; j++) { // assume sections in order
                    if (sectionsForOrder.contains(OrderSection.values()[j])) { // question sections contain section
                        return Optional.of(OrderSection.values()[j]);
                    }
                }
            }
        }

        return Optional.empty();
    }
}
