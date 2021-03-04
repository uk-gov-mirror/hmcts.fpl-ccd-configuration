package uk.gov.hmcts.reform.fpl.service.docmosis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fpl.model.Recipient;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisCoverDocument;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates.COVER_DOCS;
import static uk.gov.hmcts.reform.fpl.service.docmosis.DocmosisTemplateDataGeneration.getHmctsLogoLarge;
import static uk.gov.hmcts.reform.fpl.service.docmosis.DocmosisTemplateDataGeneration.getHmctsLogoSmall;
import static uk.gov.hmcts.reform.fpl.utils.CaseDetailsHelper.formatCCDCaseNumber;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocmosisCoverDocumentsService {
    private final DocmosisDocumentGeneratorService docmosisDocumentGeneratorService;

    public DocmosisDocument createCoverDocuments(String familyManCaseNumber,
                                                 Long caseNumber,
                                                 Recipient addressee) {
        DocmosisCoverDocument coverDocumentData = buildCoverDocumentsData(familyManCaseNumber,
            caseNumber,
            addressee);
        return docmosisDocumentGeneratorService.generateDocmosisDocument(coverDocumentData, COVER_DOCS);
    }

    public DocmosisCoverDocument buildCoverDocumentsData(String familyManCaseNumber,
                                                         Long caseNumber,
                                                         Recipient addressee) {
        return DocmosisCoverDocument.builder()
            .familyManCaseNumber(defaultIfNull(familyManCaseNumber, ""))
            .ccdCaseNumber(formatCCDCaseNumber(caseNumber))
            .representativeName(addressee.getFullName())
            .representativeAddress(addressee.getAddress().getAddressAsString("\n"))
            .hmctsLogoLarge(getHmctsLogoLarge())
            .hmctsLogoSmall(getHmctsLogoSmall())
            .build();
    }
}
