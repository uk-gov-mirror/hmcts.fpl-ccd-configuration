package uk.gov.hmcts.reform.fpl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.fpl.enums.DocumentType;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ApplicationDocument {
    private DocumentReference document;
    private DocumentType documentType;
    private LocalDateTime dateTimeUploaded;
    private String uploadedBy;
    private String documentName;
    private String includedInSWET;
}
