package uk.gov.hmcts.reform.fpl.service.orders.history;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.fpl.enums.OrderStatus;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.Judge;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;
import uk.gov.hmcts.reform.fpl.model.event.ManageOrdersEventData;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.service.ChildrenService;
import uk.gov.hmcts.reform.fpl.service.IdentityService;
import uk.gov.hmcts.reform.fpl.service.orders.OrderCreationService;
import uk.gov.hmcts.reform.fpl.service.time.Time;
import uk.gov.hmcts.reform.fpl.utils.JudgeAndLegalAdvisorHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.enums.docmosis.RenderFormat.PDF;
import static uk.gov.hmcts.reform.fpl.enums.docmosis.RenderFormat.WORD;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;

class SealedOrderHistoryServiceTest {

    private static final Judge JUDGE = mock(Judge.class);
    private static final Order ORDER_TYPE = Order.C32_CARE_ORDER;
    private static final LocalDate TODAY = LocalDate.of(2012, 12, 22);
    private static final LocalDateTime NOW = TODAY.atStartOfDay();
    private static final LocalDate APPROVAL_DATE = LocalDate.of(2010, 11, 6);
    private static final String CHILD_1_FULLNAME = "child1fullname";
    private static final String CHILD_2_FULLNAME = "child1fullname";
    private static final JudgeAndLegalAdvisor JUDGE_AND_LEGAL_ADVISOR = mock(JudgeAndLegalAdvisor.class);
    private static final JudgeAndLegalAdvisor TAB_JUDGE_AND_LEGAL_ADVISOR = mock(JudgeAndLegalAdvisor.class);
    private static final UUID UUID_1 = java.util.UUID.randomUUID();
    private static final Element<GeneratedOrder> ORDER_APPROVED_IN_THE_PAST = element(UUID_1,
        GeneratedOrder.builder()
            .approvalDate(APPROVAL_DATE.minusDays(1))
            .build());
    private static final Element<GeneratedOrder> ORDER_APPROVED_IN_THE_FUTURE = element(UUID_1,
        GeneratedOrder.builder()
            .approvalDate(APPROVAL_DATE.plusDays(1))
            .build());
    private static final Element<GeneratedOrder> ORDER_APPROVED_FOR_SAME_DAY = element(UUID_1,
        GeneratedOrder.builder()
            .approvalDate(APPROVAL_DATE)
            .dateTimeIssued(NOW.minusSeconds(1))
            .build());
    private static final Element<GeneratedOrder> ORDER_APPROVED_LEGACY = element(UUID_1,
        GeneratedOrder.builder()
            .approvalDate(null)
            .build());
    private static final UUID GENERATED_ORDER_UUID = java.util.UUID.randomUUID();
    private static final DocumentReference SEALED_PDF_DOCUMENT = mock(DocumentReference.class);
    private static final DocumentReference PLAIN_WORD_DOCUMENT = mock(DocumentReference.class);
    private final Child child1 = mock(Child.class);
    private final Child child2 = mock(Child.class);

    private final ChildrenService childrenService = mock(ChildrenService.class);
    private final IdentityService identityService = mock(IdentityService.class);
    private final OrderCreationService orderCreationService = mock(OrderCreationService.class);
    private final Time time = mock(Time.class);

    private final SealedOrderHistoryService underTest = new SealedOrderHistoryService(
        identityService,
        childrenService,
        orderCreationService,
        time
    );

    @Nested
    class Generate {

        @BeforeEach
        void setUp() {
            when(child1.asLabel()).thenReturn(CHILD_1_FULLNAME);
            when(child2.asLabel()).thenReturn(CHILD_2_FULLNAME);
            when(time.now()).thenReturn(NOW);
            when(identityService.generateId()).thenReturn(GENERATED_ORDER_UUID);
        }

        @Test
        void generateWhenNoPreviousOrders() {
            try (MockedStatic<JudgeAndLegalAdvisorHelper> jalMock =
                     Mockito.mockStatic(JudgeAndLegalAdvisorHelper.class)) {
                mockHelper(jalMock);
                CaseData caseData = caseData().build();
                mockDocumentUpload(caseData);
                when(childrenService.getSelectedChildren(caseData)).thenReturn(wrapElements(child1));

                Map<String, Object> actual = underTest.generate(caseData);

                assertThat(actual).isEqualTo(Map.of(
                    "orderCollection", List.of(
                        element(GENERATED_ORDER_UUID, expectedGeneratedOrder().build())
                    )
                ));
            }
        }

        @Test
        void generateWhenNoPreviousOrdersWithMultipleChildren() {
            try (MockedStatic<JudgeAndLegalAdvisorHelper> jalMock =
                     Mockito.mockStatic(JudgeAndLegalAdvisorHelper.class)) {
                mockHelper(jalMock);
                CaseData caseData = caseData().build();
                mockDocumentUpload(caseData);
                when(childrenService.getSelectedChildren(caseData)).thenReturn(wrapElements(child1, child1));

                Map<String, Object> actual = underTest.generate(caseData);

                assertThat(actual).isEqualTo(Map.of(
                    "orderCollection", List.of(
                        element(GENERATED_ORDER_UUID, expectedGeneratedOrder()
                            .children(wrapElements(child1, child1))
                            .childrenDescription(String.format("%s, %s", CHILD_1_FULLNAME, CHILD_2_FULLNAME))
                            .build())
                    )));
            }
        }

        @Test
        void generateWithPreviousOrdersWithPastApprovalDate() {
            try (MockedStatic<JudgeAndLegalAdvisorHelper> jalMock =
                     Mockito.mockStatic(JudgeAndLegalAdvisorHelper.class)) {
                mockHelper(jalMock);
                CaseData caseData = caseData()
                    .orderCollection(newArrayList(
                        ORDER_APPROVED_IN_THE_PAST
                    )).build();
                mockDocumentUpload(caseData);
                when(childrenService.getSelectedChildren(caseData)).thenReturn(wrapElements(child1));

                Map<String, Object> actual = underTest.generate(caseData);

                assertThat(actual).isEqualTo(Map.of(
                    "orderCollection", List.of(
                        ORDER_APPROVED_IN_THE_PAST,
                        element(GENERATED_ORDER_UUID, expectedGeneratedOrder().build())
                    )
                ));
            }
        }

        @Test
        void generateWithPreviousOrdersWithLaterApprovalDate() {
            try (MockedStatic<JudgeAndLegalAdvisorHelper> jalMock =
                     Mockito.mockStatic(JudgeAndLegalAdvisorHelper.class)) {
                mockHelper(jalMock);
                CaseData caseData = caseData()
                    .orderCollection(newArrayList(
                        ORDER_APPROVED_IN_THE_FUTURE
                    )).build();
                mockDocumentUpload(caseData);
                when(childrenService.getSelectedChildren(caseData)).thenReturn(wrapElements(child1));

                Map<String, Object> actual = underTest.generate(caseData);

                assertThat(actual).isEqualTo(Map.of(
                    "orderCollection", List.of(
                        element(GENERATED_ORDER_UUID, expectedGeneratedOrder().build()),
                        ORDER_APPROVED_IN_THE_FUTURE
                    )
                ));
            }
        }

        @Test
        void generateWithPreviousOrdersWithSameApprovalDate() {
            try (MockedStatic<JudgeAndLegalAdvisorHelper> jalMock =
                     Mockito.mockStatic(JudgeAndLegalAdvisorHelper.class)) {
                mockHelper(jalMock);
                CaseData caseData = caseData()
                    .orderCollection(newArrayList(
                        ORDER_APPROVED_FOR_SAME_DAY,
                        ORDER_APPROVED_IN_THE_FUTURE,
                        ORDER_APPROVED_LEGACY
                    )).build();
                mockDocumentUpload(caseData);
                when(childrenService.getSelectedChildren(caseData)).thenReturn(wrapElements(child1));

                Map<String, Object> actual = underTest.generate(caseData);

                assertThat(actual).isEqualTo(Map.of(
                    "orderCollection", List.of(
                        ORDER_APPROVED_LEGACY,
                        ORDER_APPROVED_FOR_SAME_DAY,
                        element(GENERATED_ORDER_UUID, expectedGeneratedOrder().build()),
                        ORDER_APPROVED_IN_THE_FUTURE
                    )
                ));
            }
        }

        @Test
        void generateWithPreviousLegacyOrdersWithoutApprovalDate() {
            try (MockedStatic<JudgeAndLegalAdvisorHelper> jalMock =
                     Mockito.mockStatic(JudgeAndLegalAdvisorHelper.class)) {
                mockHelper(jalMock);
                CaseData caseData = caseData()
                    .orderCollection(newArrayList(
                        ORDER_APPROVED_LEGACY
                    )).build();
                mockDocumentUpload(caseData);
                when(childrenService.getSelectedChildren(caseData)).thenReturn(wrapElements(child1));

                Map<String, Object> actual = underTest.generate(caseData);

                assertThat(actual).isEqualTo(Map.of(
                    "orderCollection", List.of(
                        ORDER_APPROVED_LEGACY,
                        element(GENERATED_ORDER_UUID, expectedGeneratedOrder().build())
                    )
                ));
            }
        }
    }

    @Nested
    class LastGeneratedOrder {

        @Test
        void testEmptyElements() {
            assertThrows(IllegalStateException.class, () -> underTest.lastGeneratedOrder(CaseData.builder().build()));
        }

        @Test
        void testSingleElement() {
            GeneratedOrder order = GeneratedOrder.builder()
                .dateTimeIssued(NOW)
                .build();

            GeneratedOrder actual = underTest.lastGeneratedOrder(CaseData.builder()
                .orderCollection(wrapElements(order)).build());

            assertThat(actual).isEqualTo(order);
        }

        @Test
        void testElementsInThePast() {
            GeneratedOrder order = GeneratedOrder.builder()
                .dateTimeIssued(NOW)
                .build();
            GeneratedOrder anotherPastOrder = GeneratedOrder.builder()
                .dateTimeIssued(NOW.minusSeconds(1))
                .build();

            GeneratedOrder actual = underTest.lastGeneratedOrder(CaseData.builder()
                .orderCollection(wrapElements(order, anotherPastOrder)).build());

            assertThat(actual).isEqualTo(order);
        }

        @Test
        void testLegacyElementsInThePast() {
            GeneratedOrder order = GeneratedOrder.builder()
                .dateTimeIssued(NOW)
                .build();
            GeneratedOrder anotherPastOrder = GeneratedOrder.builder()
                .dateTimeIssued(null)
                .build();

            GeneratedOrder actual = underTest.lastGeneratedOrder(CaseData.builder()
                .orderCollection(wrapElements(order, anotherPastOrder)).build());

            assertThat(actual).isEqualTo(order);
        }
    }

    private void mockDocumentUpload(CaseData caseData) {
        when(orderCreationService.createOrderDocument(caseData, OrderStatus.SEALED, PDF)).thenReturn(
            SEALED_PDF_DOCUMENT);
        when(orderCreationService.createOrderDocument(caseData, OrderStatus.PLAIN, WORD)).thenReturn(
            PLAIN_WORD_DOCUMENT);
    }

    private GeneratedOrder.GeneratedOrderBuilder expectedGeneratedOrder() {
        return GeneratedOrder.builder()
            .orderType(ORDER_TYPE.name())
            .title(ORDER_TYPE.getHistoryTitle())
            .judgeAndLegalAdvisor(TAB_JUDGE_AND_LEGAL_ADVISOR)
            .children(wrapElements(child1))
            .childrenDescription(CHILD_1_FULLNAME)
            .approvalDate(APPROVAL_DATE)
            .document(SEALED_PDF_DOCUMENT)
            .unsealedDocumentCopy(PLAIN_WORD_DOCUMENT)
            .dateTimeIssued(NOW);
    }

    private void mockHelper(MockedStatic<JudgeAndLegalAdvisorHelper> jalMock) {
        jalMock.when(() -> JudgeAndLegalAdvisorHelper.getJudgeForTabView(JUDGE_AND_LEGAL_ADVISOR, JUDGE))
            .thenReturn(TAB_JUDGE_AND_LEGAL_ADVISOR);
    }

    private CaseData.CaseDataBuilder caseData() {
        return CaseData.builder()
            .allocatedJudge(JUDGE)
            .judgeAndLegalAdvisor(JUDGE_AND_LEGAL_ADVISOR)
            .manageOrdersEventData(ManageOrdersEventData.builder()
                .manageOrdersType(ORDER_TYPE)
                .manageOrdersApprovalDate(APPROVAL_DATE)
                .build());
    }
}
