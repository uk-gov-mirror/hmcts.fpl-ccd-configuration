package uk.gov.hmcts.reform.fpl.pdfToDocxConversion.poi;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContentFormat {
    @Builder.Default
    private String color = "000000";
    @Builder.Default
    private boolean isBold = false;
    @Builder.Default
    private String fontFamily = "Courier";
    @Builder.Default
    private int fontSize = 18;
}
