package uk.gov.hmcts.reform.fpl.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisOrder;
import uk.gov.hmcts.reform.fpl.service.docmosis.DocmosisDocumentGeneratorService;

import static uk.gov.hmcts.reform.fpl.enums.docmosis.RenderFormat.PDF;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocumentService {
    private final DocmosisDocumentGeneratorService documentGeneratorService;
    private final UploadDocumentService uploadDocumentService;

    public <T extends DocmosisOrder> Document getDocumentFromDocmosisOrderTemplate(T templateData,
                                                                                   DocmosisTemplates template) {
        DocmosisDocument document = documentGeneratorService.generateDocmosisDocument(templateData, template);

        String documentTitle = getDocumentTitle(templateData.getDraftbackground(), document);

        return uploadDocumentService.uploadDocument(document.getBytes(), documentTitle, PDF.getMediaType());
    }

    private String getDocumentTitle(String draftBackground, DocmosisDocument document) {
        return draftBackground == null ? document.getDocumentTitle() : document.getDraftDocumentTile();
    }
}
