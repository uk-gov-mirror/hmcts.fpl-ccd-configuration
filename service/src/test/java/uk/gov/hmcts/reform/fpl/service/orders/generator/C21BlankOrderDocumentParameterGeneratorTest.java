package uk.gov.hmcts.reform.fpl.service.orders.generator;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityNameLookupConfiguration;
import uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates;
import uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.event.ManageOrdersEventData;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.service.ChildrenService;
import uk.gov.hmcts.reform.fpl.service.orders.docmosis.C21BlankOrderDocmosisParameters;
import uk.gov.hmcts.reform.fpl.service.orders.docmosis.DocmosisParameters;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;

public class C21BlankOrderDocumentParameterGeneratorTest {

    private static final String LA_CODE = "LA_CODE";
    private static final String LA_NAME = "Local Authority Name";
    private static final Child CHILD = mock(Child.class);
    private static final GeneratedOrderType TYPE = GeneratedOrderType.BLANK_ORDER;
    private static final String TITLE = "Test title";
    private static final String DIRECTIONS = "Test directions";
    private static final CaseData CASE_DATA = CaseData.builder()
        .caseLocalAuthority(LA_CODE)
        .manageOrdersEventData(ManageOrdersEventData.builder()
            .manageOrdersType(Order.C21_BLANK_ORDER)
            .manageOrdersTitle(TITLE)
            .manageOrdersDirections(DIRECTIONS)
            .build())
        .build();

    private final ChildrenService childrenService = mock(ChildrenService.class);
    private final LocalAuthorityNameLookupConfiguration laNameLookup = mock(
        LocalAuthorityNameLookupConfiguration.class
    );

    private final C21BlankOrderDocumentParameterGenerator underTest = new C21BlankOrderDocumentParameterGenerator(
        laNameLookup
    );

    @Test
    void accept() {
        assertThat(underTest.accept()).isEqualTo(Order.C21_BLANK_ORDER);
    }

    @Test
    void generate() {
        when(laNameLookup.getLocalAuthorityName(LA_CODE)).thenReturn(LA_NAME);

        List<Element<Child>> selectedChildren = wrapElements(CHILD);

        when(childrenService.getSelectedChildren(CASE_DATA)).thenReturn(selectedChildren);

        C21BlankOrderDocmosisParameters expectedParameters = C21BlankOrderDocmosisParameters.builder()
            .orderDetails(DIRECTIONS)
            .localAuthorityName(LA_NAME)
            .orderType(TYPE)
            .build();

        DocmosisParameters generatedParameters = underTest.generate(CASE_DATA);
        assertThat(generatedParameters).isEqualTo(expectedParameters);
    }

    @Test
    void template() {
        assertThat(underTest.template()).isEqualTo(DocmosisTemplates.ORDER);
    }
}
