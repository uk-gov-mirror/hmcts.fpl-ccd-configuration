package uk.gov.hmcts.reform.fpl.pdfToDocxConversion.iTextPdf;

import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfContentStreamProcessor;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import uk.gov.hmcts.reform.fpl.pdfToDocxConversion.iTextPdf.events.PdfRenderListener;
import uk.gov.hmcts.reform.fpl.pdfToDocxConversion.poi.ContentFormat;
import uk.gov.hmcts.reform.fpl.pdfToDocxConversion.poi.PoiUtils;

import java.io.FileOutputStream;
import java.io.IOException;

import static com.itextpdf.text.pdf.parser.ContentByteUtils.getContentBytesForPage;

public class ITextPdfToDocxConverter {

    private static final String SRC_PDF = "CMO-sealed-doc.pdf";

    public static void main(String[] args) throws Exception {
        writeToDocWithITextLocationStrategy();
        writeToDocWithITextRenderListener();
        writeToDocWithITextSimpleTextStrategy();
    }

    // All text content is extracted from the pdf. NO images
    private static void writeToDocWithITextLocationStrategy() throws IOException {
        String dest = "src/main/resources/itextpdf-location-strategy-to-word.docx";
        XWPFDocument wordDocument = new XWPFDocument();

        PdfReader reader = new PdfReader(SRC_PDF);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);

        //RenderListener listener = new PdfRenderListener(dest, wordDocument);
        TextExtractionStrategy strategy;
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            strategy = parser.processContent(i, new LocationTextExtractionStrategy());
            PoiUtils.addParagraph(wordDocument, i, strategy.getResultantText(), ContentFormat.builder().build());
            //strategy.notifyAll();
        }
        FileOutputStream out = new FileOutputStream(dest);
        wordDocument.write(out);
        out.flush();
        out.close();
    }

    // Text fragments are populated with no formatting applied
    // images are converted but not positioned
    private static void writeToDocWithITextRenderListener() throws IOException {
        String dest = "src/main/resources/itextpdf-listener-write-to-word.docx";
        XWPFDocument targetWordDocument = new XWPFDocument();
        PdfReader reader = new PdfReader(SRC_PDF);

        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            RenderListener listener = new PdfRenderListener(dest, targetWordDocument);
            PdfContentStreamProcessor processor = new PdfContentStreamProcessor(listener);

            PdfDictionary pageDic = reader.getPageN(i);

            PdfDictionary resourcesDic = pageDic.getAsDict(PdfName.RESOURCES);
            processor.processContent(getContentBytesForPage(reader, i), resourcesDic);

            // Rectangle pageSize = reader.getPageSize(i);
            //out.println(PdfTextExtractor.getTextFromPage(reader, i));
            //Rectangle pageSize = reader.getPageSize(0); // page size
            //strategy = parser.processContent(1, new LocationTextExtractionStrategy());
        }
        FileOutputStream out = new FileOutputStream(dest);
        targetWordDocument.write(out);
        out.flush();
        out.close();
    }

    // Same as Location text strategy
    private static void writeToDocWithITextSimpleTextStrategy() throws Exception {
        String dest = "src/main/resources/itextpdf-strategy-to-word.docx";
        XWPFDocument wordDocument = new XWPFDocument();
        PdfReader reader = new PdfReader(SRC_PDF);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        TextExtractionStrategy strategy;
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
            PoiUtils.addParagraph(wordDocument, i, strategy.getResultantText(), ContentFormat.builder().build());
        }
        FileOutputStream out = new FileOutputStream(dest);
        wordDocument.write(out);
        out.flush();
        out.close();
    }
}
