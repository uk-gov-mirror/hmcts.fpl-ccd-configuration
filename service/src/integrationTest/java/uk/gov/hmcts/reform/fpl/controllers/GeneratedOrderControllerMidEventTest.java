package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates;
import uk.gov.hmcts.reform.fpl.enums.GeneratedOrderSubtype;
import uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType;
import uk.gov.hmcts.reform.fpl.enums.YesNo;
import uk.gov.hmcts.reform.fpl.model.Address;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.ChildParty;
import uk.gov.hmcts.reform.fpl.model.OrderTypeAndDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisData;
import uk.gov.hmcts.reform.fpl.model.emergencyprotectionorder.EPOChildren;
import uk.gov.hmcts.reform.fpl.model.emergencyprotectionorder.EPOPhrase;
import uk.gov.hmcts.reform.fpl.model.order.generated.FurtherDirections;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.model.order.generated.InterimEndDate;
import uk.gov.hmcts.reform.fpl.model.order.selector.ChildSelector;
import uk.gov.hmcts.reform.fpl.service.FeatureToggleService;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;
import uk.gov.hmcts.reform.fpl.service.docmosis.DocmosisDocumentGeneratorService;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.fpl.controllers.CloseCaseControllerAboutToStartTest.EXPECTED_LABEL_TEXT;
import static uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates.EPO;
import static uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates.ORDER;
import static uk.gov.hmcts.reform.fpl.enums.EPOType.REMOVE_TO_ACCOMMODATION;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderSubtype.FINAL;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderSubtype.INTERIM;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.BLANK_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.CARE_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.EMERGENCY_PROTECTION_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.SUPERVISION_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.ccd.fixedlists.InterimEndDateType.END_OF_PROCEEDINGS;
import static uk.gov.hmcts.reform.fpl.utils.DocumentManagementStoreLoader.document;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testChild;

@ActiveProfiles("integration-test")
@WebMvcTest(GeneratedOrderController.class)
@OverrideAutoConfiguration(enabled = true)
public class GeneratedOrderControllerMidEventTest extends AbstractControllerTest {

    private static final String FAMILY_MAN_CASE_NUMBER = "SACCCCCCCC5676576567";
    private static final String LOCAL_AUTHORITY_CODE = "example";
    private final byte[] pdf = {1, 2, 3, 4, 5};
    private Document document;

    @MockBean
    private DocmosisDocumentGeneratorService docmosisDocumentGeneratorService;

    @MockBean
    private FeatureToggleService toggleService;

    @MockBean
    private UploadDocumentService uploadDocumentService;

    GeneratedOrderControllerMidEventTest() {
        super("create-order");
    }

    @BeforeEach
    void setUp() {
        document = document();
        DocmosisDocument docmosisDocument = new DocmosisDocument("order.pdf", pdf);

        given(docmosisDocumentGeneratorService.generateDocmosisDocument(any(DocmosisData.class),
            any())).willReturn(docmosisDocument);
        given(uploadDocumentService.uploadPDF(any(), any())).willReturn(document);
    }

    private List<Element<Child>> createChildren(String... firstNames) {
        Child[] children = new Child[firstNames.length];
        for (int i = 0; i < firstNames.length; i++) {
            children[i] = Child.builder()
                .party(ChildParty.builder()
                    .firstName(firstNames[i])
                    .build())
                .build();
        }
        return wrapElements(children);
    }

    private Child createChild(String name, boolean finalOrderIssued) {
        Child child = Child.builder()
            .party(ChildParty.builder()
                .firstName(name)
                .build())
            .build();
        if (finalOrderIssued) {
            child.setFinalOrderIssued(YesNo.YES.getValue());
            child.setFinalOrderIssuedType(CARE_ORDER.getLabel());
        }
        return child;
    }

    private CaseDetails buildCaseDetails(List<Element<Child>> children) {
        return buildCaseDetails(null, children);
    }

    private CaseDetails buildCaseDetails(String choice, List<Element<Child>> children) {
        return buildCaseDetails(choice, children, CARE_ORDER, FINAL);
    }

    private CaseDetails buildCaseDetails(String choice, GeneratedOrderType type, GeneratedOrderSubtype subType) {
        return buildCaseDetails(choice, createChildren("Wallace", "Gromit"), type, subType);
    }

    private CaseDetails buildCaseDetails(String choice,
                                         List<Element<Child>> children,
                                         GeneratedOrderType type,
                                         GeneratedOrderSubtype subType) {
        CaseData caseData = CaseData.builder()
            .children1(children)
            .orderAppliesToAllChildren(choice)
            .orderTypeAndDocument(OrderTypeAndDocument.builder().type(type).subtype(subType).build())
            .build();

        return CaseDetails.builder()
            .data(mapper.convertValue(caseData, new TypeReference<>() {}))
            .build();
    }

    @Nested
    class PopulateChildSelectorMidEvent {
        @Test
        void shouldPopulateChildSelectorAndLabelWithBasicInfoWhenNoIsSelectedAndOrderIsNotFinal() {
            AboutToStartOrSubmitCallbackResponse callbackResponse = postMidEvent(
                buildCaseDetails("No", SUPERVISION_ORDER, INTERIM), "populate-selector");

            CaseData caseData = mapper.convertValue(callbackResponse.getData(), CaseData.class);

            assertThat(callbackResponse.getData().get("children_label"))
                .isEqualTo("Child 1: Wallace\nChild 2: Gromit\n");

            assertThat(caseData.getChildSelector()).isEqualTo(getExpectedChildSelector());
        }

        @Test
        void shouldPopulateChildSelectorAndLabelWhenNoIsSelectedAndOrderIsFinalAndNoOneHasFinalOrderIssued() {
            AboutToStartOrSubmitCallbackResponse response = postMidEvent(
                buildCaseDetails("No", CARE_ORDER, FINAL), "populate-selector");

            CaseData caseData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(response.getData().get("children_label"))
                .isEqualTo("Child 1: Wallace\nChild 2: Gromit\n");

            assertThat(caseData.getChildSelector()).isEqualTo(getExpectedChildSelector());
        }

        @Test
        void shouldNotPopulateChildSelectorAndLabelWhenYesIsSelected() {
            AboutToStartOrSubmitCallbackResponse callbackResponse = postMidEvent(
                buildCaseDetails("Yes", CARE_ORDER, FINAL), "populate-selector");

            assertThat(callbackResponse.getData()).extracting("children_label", "childSelector").containsOnlyNulls();
        }

        @Test
        void shouldPopulateChildSelectorAndLabelWhenNoIsSelectedAndFinalOrderIssuedOnChildren() {

            List<Element<Child>> children = wrapElements(createChild("Fred", false),
                createChild("Jane", true),
                createChild("Paul", true),
                createChild("Bill", false));

            AboutToStartOrSubmitCallbackResponse callbackResponse = postMidEvent(
                buildCaseDetails("No", children), "populate-selector");

            CaseData caseData = mapper.convertValue(callbackResponse.getData(), CaseData.class);

            assertThat(callbackResponse.getData().get("children_label"))
                .isEqualTo("Child 1: Fred\nChild 2: Jane - Care order issued\nChild 3: Paul - Care order issued\n"
                    + "Child 4: Bill\n");

            ChildSelector expected = ChildSelector.builder()
                .childCount("1234")
                .build();
            assertThat(caseData.getChildSelector()).isEqualTo(expected);
        }

        private ChildSelector getExpectedChildSelector() {
            return ChildSelector.builder()
                .childCount("12")
                .build();
        }
    }

    @Nested
    class AddFinalOrderFlagsMidEvent {
        @Test
        void shouldPopulateLabelsWhenSingleChildHasFinalOrderRemaining() {
            List<Element<Child>> children = wrapElements(createChild("Fred", true),
                createChild("Jane", false),
                createChild("Paul", true),
                createChild("Bill", true));

            AboutToStartOrSubmitCallbackResponse response = postMidEvent(
                buildCaseDetails(children), "add-final-order-flags");

            assertThat(response.getData().get("remainingChildIndex"))
                .isEqualTo("1");

            assertThat(response.getData().get("remainingChild"))
                .isEqualTo("Jane");

            assertThat(response.getData().get("otherFinalOrderChildren"))
                .isEqualTo("Fred - Care order issued\nPaul - Care order issued\nBill - Care order issued");
        }

        @Test
        void shouldNotPopulateLabelsWhenMultipleChildrenHaveFinalOrderRemaining() {
            List<Element<Child>> children = wrapElements(createChild("Fred", false),
                createChild("Jane", false),
                createChild("Paul", true),
                createChild("Bill", true));

            AboutToStartOrSubmitCallbackResponse response = postMidEvent(
                buildCaseDetails(children), "add-final-order-flags");

            assertThat(response.getData())
                .extracting("remainingChildIndex", "remainingChild", "otherFinalOrderChildren")
                .containsOnlyNulls();
        }

        @Test
        void shouldNotPopulateLabelsWhenOrderTypeIsNotClosable() {
            List<Element<Child>> children = wrapElements(createChild("Fred", true),
                createChild("Jane", false),
                createChild("Paul", true),
                createChild("Bill", true));

            AboutToStartOrSubmitCallbackResponse response = postMidEvent(
                buildCaseDetails(null, children, SUPERVISION_ORDER, INTERIM), "add-final-order-flags");

            assertThat(response.getData())
                .extracting("remainingChildIndex", "remainingChild", "otherFinalOrderChildren")
                .containsOnlyNulls();
        }

        @Test
        void shouldReturnAnErrorWhenAllChildrenHaveAFinalOrderIssuedAndTryToIssueAnother() {
            List<Element<Child>> chuckleBrothers = wrapElements(createChild("Paul", true),
                createChild("Barry", true));

            AboutToStartOrSubmitCallbackResponse response = postMidEvent(
                buildCaseDetails(chuckleBrothers), "add-final-order-flags");

            assertThat(response.getErrors()).containsOnly("All children in the case already have final orders");
        }

        @Test
        void shouldNotReturnAnErrorWhenAllChildrenHaveAFinalOrderIssuedButIssuingANonFinalOrder() {
            List<Element<Child>> chuckleBrothers = wrapElements(createChild("Paul", true),
                createChild("Barry", true));

            AboutToStartOrSubmitCallbackResponse response = postMidEvent(
                buildCaseDetails(null, chuckleBrothers, BLANK_ORDER, null), "add-final-order-flags");

            assertThat(response.getErrors()).isNull();
        }
    }

    @TestInstance(PER_CLASS)
    @Nested
    class GenerateDocumentMidEvent {

        @Test
        void shouldAddCloseCaseLabelAndSetFlagToYesWhenCloseCasePageCanBeShown() {
            given(toggleService.isCloseCaseEnabled()).willReturn(true);

            CaseDetails caseDetails = generateFinalCareOrderWithChildren("Yes");

            AboutToStartOrSubmitCallbackResponse response = postMidEvent(caseDetails, "generate-document");

            verify(docmosisDocumentGeneratorService, never()).generateDocmosisDocument(anyMap(), any());

            assertThat(response.getData()).extracting("showCloseCaseFromOrderPage", "close_case_label")
                .containsOnly("YES", EXPECTED_LABEL_TEXT);
        }

        @Test
        void shouldNotAddCloseCaseLabelAndSetFlagToNoWhenCloseCasePageCanNotBeShown() {
            given(toggleService.isCloseCaseEnabled()).willReturn(true);

            CaseDetails caseDetails = generateFinalCareOrderWithChildren("No");

            AboutToStartOrSubmitCallbackResponse response = postMidEvent(caseDetails, "generate-document");

            verify(docmosisDocumentGeneratorService, never()).generateDocmosisDocument(anyMap(), any());

            assertThat(response.getData()).extracting("showCloseCaseFromOrderPage", "close_case_label")
                .containsOnly("NO", null);

        }

        @ParameterizedTest
        @MethodSource("generateDocumentMidEventArgumentSource")
        void shouldGenerateDocumentWithCorrectNameWhenOrderTypeIsValid(CaseDetails caseDetails,
                                                                       String fileName,
                                                                       DocmosisTemplates templateName) {
            final AboutToStartOrSubmitCallbackResponse callbackResponse = postMidEvent(
                caseDetails, "generate-document");

            verify(docmosisDocumentGeneratorService).generateDocmosisDocument(any(DocmosisData.class),
                eq(templateName));
            verify(uploadDocumentService).uploadPDF(pdf, fileName);

            final CaseData caseData = mapper.convertValue(callbackResponse.getData(), CaseData.class);

            assertThat(caseData.getOrderTypeAndDocument().getDocument()).isEqualTo(expectedDraftDocument());
        }

        @Test
        void shouldNotGenerateOrderDocumentWhenOrderTypeIsCareOrderWithNoFurtherDirections() {
            postMidEvent(generateCareOrderCaseDetailsWithoutFurtherDirections(), "generate-document");

            verify(docmosisDocumentGeneratorService, never()).generateDocmosisDocument(any(DocmosisData.class), any());
            verify(uploadDocumentService, never()).uploadPDF(any(), any());
        }

        @AfterEach
        void resetInvocations() {
            reset(docmosisDocumentGeneratorService);
            reset(uploadDocumentService);
        }

        private Stream<Arguments> generateDocumentMidEventArgumentSource() {
            return Stream.of(
                Arguments.of(generateBlankOrderCaseDetails(), "blank_order_c21.pdf", ORDER),
                Arguments.of(generateCareOrderCaseDetails(INTERIM), "interim_care_order.pdf", ORDER),
                Arguments.of(generateCareOrderCaseDetails(FINAL), "final_care_order.pdf", ORDER),
                Arguments.of(generateSupervisionOrderCaseDetails(INTERIM), "interim_supervision_order.pdf", ORDER),
                Arguments.of(generateSupervisionOrderCaseDetails(FINAL), "final_supervision_order.pdf", ORDER),
                Arguments.of(generateEmergencyProtectionOrderCaseDetails(), "emergency_protection_order.pdf", EPO)
            );
        }

        private CaseDetails generateEmergencyProtectionOrderCaseDetails() {
            final CaseData.CaseDataBuilder dataBuilder = CaseData.builder();

            dataBuilder.order(GeneratedOrder.builder().details("").build())
                .orderTypeAndDocument(OrderTypeAndDocument.builder().type(EMERGENCY_PROTECTION_ORDER).build())
                .dateOfIssue(dateNow());

            generateDefaultValues(dataBuilder);
            generateEpoValues(dataBuilder);

            dataBuilder.orderFurtherDirections(FurtherDirections.builder()
                .directionsNeeded("Yes")
                .directions("Some directions")
                .build());

            return CaseDetails.builder()
                .data(mapper.convertValue(dataBuilder.build(), new TypeReference<>() {}))
                .build();
        }

        private CaseDetails generateBlankOrderCaseDetails() {
            final CaseData.CaseDataBuilder dataBuilder = generateCommonOrderDetails(BLANK_ORDER, null)
                .order(GeneratedOrder.builder().details("").build());

            return CaseDetails.builder()
                .data(mapper.convertValue(dataBuilder.build(), new TypeReference<>() {}))
                .build();
        }

        private CaseDetails generateCareOrderCaseDetailsWithoutFurtherDirections() {
            final CaseData.CaseDataBuilder dataBuilder = generateCommonOrderDetails(CARE_ORDER, INTERIM);

            return CaseDetails.builder()
                .data(mapper.convertValue(dataBuilder.build(), new TypeReference<>() {}))
                .build();
        }

        private CaseDetails generateCareOrderCaseDetails(GeneratedOrderSubtype subtype) {
            final CaseData.CaseDataBuilder dataBuilder = generateCommonOrderDetails(CARE_ORDER, subtype);

            dataBuilder.orderFurtherDirections(generateOrderFurtherDirections());

            if (subtype == INTERIM) {
                dataBuilder.interimEndDate(generateInterimEndDate());
            }

            return CaseDetails.builder()
                .data(mapper.convertValue(dataBuilder.build(), new TypeReference<>() {}))
                .build();
        }

        private CaseDetails generateSupervisionOrderCaseDetails(GeneratedOrderSubtype subtype) {
            final CaseData.CaseDataBuilder dataBuilder = generateCommonOrderDetails(SUPERVISION_ORDER, subtype);

            dataBuilder.orderFurtherDirections(generateOrderFurtherDirections())
                .orderMonths(5);

            if (subtype == INTERIM) {
                dataBuilder.interimEndDate(generateInterimEndDate());
            }

            return CaseDetails.builder()
                .data(mapper.convertValue(dataBuilder.build(), new TypeReference<>() {}))
                .build();
        }

        private CaseDetails generateFinalCareOrderWithChildren(String finalOrderIssued) {
            final CaseData.CaseDataBuilder dataBuilder = generateCommonOrderDetails(CARE_ORDER, FINAL);

            dataBuilder.children1(List.of(
                childWithFinalOrderIssued("Yes"),
                childWithFinalOrderIssued(finalOrderIssued)
            ));

            return CaseDetails.builder()
                .data(mapper.convertValue(dataBuilder.build(), new TypeReference<>() {}))
                .build();
        }

        private CaseData.CaseDataBuilder generateCommonOrderDetails(GeneratedOrderType type,
                                                                    GeneratedOrderSubtype subtype) {
            final CaseData.CaseDataBuilder builder = CaseData.builder()
                .orderTypeAndDocument(OrderTypeAndDocument.builder()
                    .type(type)
                    .subtype(subtype)
                    .build())
                .dateOfIssue(dateNow());

            generateDefaultValues(builder);

            return builder;
        }

        private InterimEndDate generateInterimEndDate() {
            return InterimEndDate.builder().type(END_OF_PROCEEDINGS).build();
        }

        private void generateEpoValues(CaseData.CaseDataBuilder builder) {
            builder
                .epoChildren(EPOChildren.builder()
                    .description("Description")
                    .descriptionNeeded("Yes")
                    .build())
                .epoPhrase(EPOPhrase.builder()
                    .includePhrase("Yes")
                    .build())
                .epoEndDate(now())
                .epoType(REMOVE_TO_ACCOMMODATION)
                .epoRemovalAddress(Address.builder()
                    .addressLine1("Unit 1")
                    .addressLine2("Petty France")
                    .postTown("Lurgan")
                    .postcode("BT66 7RR")
                    .build());
        }

        private void generateDefaultValues(CaseData.CaseDataBuilder builder) {
            builder.caseLocalAuthority(LOCAL_AUTHORITY_CODE);
            builder.familyManCaseNumber(FAMILY_MAN_CASE_NUMBER);
            builder.judgeAndLegalAdvisor(JudgeAndLegalAdvisor.builder().build());
        }

        private FurtherDirections generateOrderFurtherDirections() {
            return FurtherDirections.builder()
                .directionsNeeded("Yes")
                .directions("Some directions")
                .build();
        }

        private DocumentReference expectedDraftDocument() {
            return DocumentReference.builder()
                .binaryUrl(document.links.binary.href)
                .filename(document.originalDocumentName)
                .url(document.links.self.href)
                .build();
        }

        private Element<Child> childWithFinalOrderIssued(String finalOrderIssued) {
            Element<Child> childElement = testChild();
            childElement.getValue().setFinalOrderIssued(finalOrderIssued);
            return childElement;
        }
    }
}
