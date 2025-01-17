package uk.gov.hmcts.reform.fpl.service.orders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.enums.OrderStatus;
import uk.gov.hmcts.reform.fpl.enums.docmosis.RenderFormat;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.event.ManageOrdersEventData;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;
import uk.gov.hmcts.reform.fpl.service.orders.generator.OrderDocumentGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocument;

class OrderCreationServiceTest {

    private static final Order ORDER = mock(Order.class);
    private static final RenderFormat FORMAT = mock(RenderFormat.class);
    private static final byte[] BYTES = {1, 2, 3, 4, 5};
    private static final DocmosisDocument DOCMOSIS_DOCUMENT = new DocmosisDocument("", BYTES);
    private static final Document UPLOADED_DOCUMENT = testDocument();
    private static final DocumentReference DOCUMENT = DocumentReference.buildFromDocument(UPLOADED_DOCUMENT);
    private static final String FILE_NAME = "mock_order.mock_format";
    private static final String DRAFT_FILE_NAME = "Preview order.pdf";
    private static final String MEDIA_TYPE = "mock/media_type";

    private final UploadDocumentService uploadService = mock(UploadDocumentService.class);
    private final OrderDocumentGenerator documentGenerator = mock(OrderDocumentGenerator.class);

    private final OrderCreationService underTest = new OrderCreationService(documentGenerator, uploadService);

    @Test
    void createDraftOrderDocument() {
        CaseData caseData = CaseData.builder()
            .manageOrdersEventData(ManageOrdersEventData.builder()
                .manageOrdersType(ORDER)
                .build())
            .build();

        when(documentGenerator.generate(ORDER, caseData, OrderStatus.DRAFT, FORMAT)).thenReturn(DOCMOSIS_DOCUMENT);
        when(FORMAT.getMediaType()).thenReturn(MEDIA_TYPE);
        when(uploadService.uploadDocument(BYTES, DRAFT_FILE_NAME, MEDIA_TYPE)).thenReturn(UPLOADED_DOCUMENT);

        assertThat(underTest.createOrderDocument(caseData, OrderStatus.DRAFT, FORMAT)).isEqualTo(DOCUMENT);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
    void createNonDraftOrderDocument(OrderStatus status) {
        CaseData caseData = CaseData.builder()
            .manageOrdersEventData(ManageOrdersEventData.builder()
                .manageOrdersType(ORDER)
                .build())
            .build();

        when(documentGenerator.generate(ORDER, caseData, status, FORMAT)).thenReturn(DOCMOSIS_DOCUMENT);
        when(ORDER.fileName(FORMAT)).thenReturn(FILE_NAME);
        when(FORMAT.getMediaType()).thenReturn(MEDIA_TYPE);
        when(uploadService.uploadDocument(BYTES, FILE_NAME, MEDIA_TYPE)).thenReturn(UPLOADED_DOCUMENT);

        assertThat(underTest.createOrderDocument(caseData, status, FORMAT)).isEqualTo(DOCUMENT);
    }
}
