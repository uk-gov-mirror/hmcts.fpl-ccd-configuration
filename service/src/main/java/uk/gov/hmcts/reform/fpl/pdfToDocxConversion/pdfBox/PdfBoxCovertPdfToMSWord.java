package uk.gov.hmcts.reform.fpl.pdfToDocxConversion.pdfBox;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import uk.gov.hmcts.reform.fpl.pdfToDocxConversion.poi.ContentFormat;
import uk.gov.hmcts.reform.fpl.pdfToDocxConversion.poi.PoiUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfBoxCovertPdfToMSWord {

    public static void main(String[] args) {
        ClassLoader classLoader = PdfBoxCovertPdfToMSWord.class.getClassLoader();

        File file = new File(classLoader.getResource("CMO-sealed-doc.pdf").getFile());
        try (PDDocument pdfDoc = PDDocument.load(file)) {
            XWPFDocument wordDocument = new XWPFDocument();

            PDPage page = pdfDoc.getPage(0);
            PDResources pdResources = page.getResources();
            readText(wordDocument);
            /*
            //Tried reading each content stream
            Iterator<PDStream> contentStreams = page.getContentStreams();
            for (Iterator<PDStream> it = contentStreams; it.hasNext(); ) {

                PDStream stream = it.next();
                String text = new String(stream.createInputStream().readAllBytes());
                PoiUtils.addParagraph(wordDocument, 2, text, ContentFormat.builder().build());

            }*/

            // Images
            int i = 0;
            for (COSName csName : pdResources.getXObjectNames()) {
                System.out.println("Resource NAME:::::" + csName);
                PDXObject pdxObject = pdResources.getXObject(csName);
                if (pdxObject instanceof PDImageXObject) {
                    PDStream pdStream = pdxObject.getStream();
                    PDImageXObject pdImage = new PDImageXObject(pdStream, pdResources);
                    i++;
                    // pdImage storage location and pdImage name
                    File imgFile = new File(file.getParent() + "/img" + i + ".png");

                    ImageIO.write(pdImage.getImage(), "png", imgFile);
                    PoiUtils.addPdfBoxImage(wordDocument, imgFile, pdImage);
                }
            }
            FileOutputStream out = new FileOutputStream("write-to-docx-with-pdfbox-and-poi.docx");
            wordDocument.write(out);
            out.close();
            wordDocument.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void readText(XWPFDocument wordDocument) {
        ClassLoader classLoader = PdfBoxCovertPdfToMSWord.class.getClassLoader();

        File sourcePdf = new File(classLoader.getResource("CMO-sealed-doc.pdf").getFile());

        //String data = FileUtils.readFileToString(sourcePdf, "UTF-8");
        try (PDDocument document = PDDocument.load(sourcePdf)) {
            Class<? extends PDDocument> aClass = document.getClass();

            if (!document.isEncrypted()) {
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);

                PDFTextStripper tStripper = new PDFTextStripper();

                String pdfFileInText = tStripper.getText(document);
                System.out.println("Text:" + pdfFileInText);
                PoiUtils.addParagraph(wordDocument, 1, pdfFileInText, ContentFormat.builder().build());

            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
