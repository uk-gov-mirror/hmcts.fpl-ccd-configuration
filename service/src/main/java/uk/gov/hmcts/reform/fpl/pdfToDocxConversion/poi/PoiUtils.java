package uk.gov.hmcts.reform.fpl.pdfToDocxConversion.poi;

import com.itextpdf.text.pdf.DocumentFont;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfImageObject;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.RandomStringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TextAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class PoiUtils {

    private PoiUtils() {
        // util
    }

    public static void addTextFromITextPdf(XWPFDocument document, TextRenderInfo pdfTextRenderInfo) {
        int textPosition = 1;
        String content = pdfTextRenderInfo.getText();
        DocumentFont font = pdfTextRenderInfo.getFont();
        PdfString fontFamily = font.getFontDictionary().getAsString(PdfName.FONTFAMILY);
        PdfString fontWeight = font.getFontDictionary().getAsString(PdfName.FONTWEIGHT);

        /*
        Font - BAAAAA+Verdana
        refFont - 15 0 R
        pdfTextRenderInfo.getFillColor().getRGB() -> "-16119286"
        -f5f5f6
         */
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText(content);
        //titleRun.setColor(Integer.toString(pdfTextRenderInfo.getFillColor().getRGB(), 16)); // -f5f5f6
        titleRun.setColor("000000");
        titleRun.setFontFamily(font.getPostscriptFontName());
        //titleRun.setFontSize(Integer.getInteger(fontWeight.toString()));
        titleRun.setTextPosition(textPosition);

        /*XmlCursor cursor = title.getCTP().newCursor();
        XWPFParagraph new_title = document.insertNewParagraph(cursor);
        new_title.createRun().setText("Test text");*/
    }

    public static void addParagraph(XWPFDocument document, int textPosition, String content, ContentFormat format) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText(content);
        titleRun.setColor(format.getColor());
        titleRun.setBold(format.isBold());
        titleRun.setFontFamily(format.getFontFamily());
        titleRun.setFontSize(format.getFontSize());
        titleRun.setTextPosition(textPosition);

        /*
        //TODO: set text position
        XmlCursor cursor = title.getCTP().newCursor();
        XWPFParagraph new_title = document.insertNewParagraph(cursor);
        new_title.createRun().setText("Test text");*/
    }

    public static void insertImageWithITextImage(XWPFDocument document, ImageRenderInfo imageRenderInfo) throws Exception {
        PdfImageObject pdfImage = imageRenderInfo.getImage();
        PdfImageObject.ImageBytesType pdfImageType = pdfImage.getImageBytesType();
        int poiImageType = pdfImageType == PdfImageObject.ImageBytesType.JPG ? XWPFDocument.PICTURE_TYPE_JPEG : XWPFDocument.PICTURE_TYPE_PNG;
        String pdfImageFileType = pdfImage.getFileType();

        String imgName = String.format("%s.%s", RandomStringUtils.randomAlphanumeric(8), pdfImageFileType);
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();

        ByteArrayInputStream pictureData = new ByteArrayInputStream(pdfImage.getImageAsBytes());
        run.addPicture(pictureData, poiImageType, imgName, Units.toEMU(100), Units.toEMU(30)); //tODO set size
        pictureData.close();

        CTDrawing drawing = run.getCTR().getDrawingArray(0);
        CTGraphicalObject graphicalobject = drawing.getInlineArray(0).getGraphic();

        CTAnchor anchor = getAnchorWithGraphic(graphicalobject, imgName,
            Units.toEMU(50), Units.toEMU(50),
            Units.toEMU(100), Units.toEMU(100));

        drawing.setAnchorArray(new CTAnchor[]{anchor});
        drawing.removeInline(0);
    }

    public static void addPdfBoxImage(XWPFDocument document, File pdfImagePath, PDImageXObject pdImage) throws Exception {
        String imgName = String.format("%s.%s", RandomStringUtils.randomAlphanumeric(8), "png");
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();

        ByteArrayInputStream pictureData = new ByteArrayInputStream(FileUtils.readFileToByteArray(pdfImagePath));
        run.addPicture(pictureData, XWPFDocument.PICTURE_TYPE_PNG, imgName, Units.toEMU(100), Units.toEMU(30)); //tODO set size
        pictureData.close();

        CTDrawing drawing = run.getCTR().getDrawingArray(0);
        CTGraphicalObject graphicalobject = drawing.getInlineArray(0).getGraphic();
        CTAnchor anchor = getAnchorWithGraphic(graphicalobject, imgName,
            Units.toEMU(pdImage.getWidth()), Units.toEMU(pdImage.getHeight()),
            Units.toEMU(pdImage.getImage().getTileGridXOffset()), Units.toEMU(pdImage.getImage().getTileGridYOffset()));

        drawing.setAnchorArray(new CTAnchor[]{anchor});
        drawing.removeInline(0);
        XWPFParagraph image = document.createParagraph();
        image.setAlignment(ParagraphAlignment.RIGHT);
        image.setVerticalAlignment(TextAlignment.TOP);
    }

    private static CTAnchor getAnchorWithGraphic(CTGraphicalObject graphicalobject,
                                                 String drawingDescr, int width, int height,
                                                 int left, int top) throws Exception {
        String anchorXML =
            "<wp:anchor xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" "
                + "simplePos=\"0\" relativeHeight=\"0\" behindDoc=\"1\" locked=\"0\" layoutInCell=\"1\" allowOverlap=\"1\">"
                + "<wp:simplePos x=\"0\" y=\"0\"/>"
                + "<wp:positionH relativeFrom=\"column\"><wp:posOffset>" + left + "</wp:posOffset></wp:positionH>"
                + "<wp:positionV relativeFrom=\"paragraph\"><wp:posOffset>" + top + "</wp:posOffset></wp:positionV>"
                + "<wp:extent cx=\"" + width + "\" cy=\"" + height + "\"/>"
                + "<wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>"
                + "<wp:wrapTight wrapText=\"bothSides\">"
                + "<wp:wrapPolygon edited=\"0\">"
                + "<wp:start x=\"0\" y=\"0\"/>"
                + "<wp:lineTo x=\"0\" y=\"21600\"/>" //Square polygon 21600 x 21600 leads to wrap points in fully width x height
                + "<wp:lineTo x=\"21600\" y=\"21600\"/>"// Why? I don't know. Try & error ;-).
                + "<wp:lineTo x=\"21600\" y=\"0\"/>"
                + "<wp:lineTo x=\"0\" y=\"0\"/>"
                + "</wp:wrapPolygon>"
                + "</wp:wrapTight>"
                + "<wp:docPr id=\"1\" name=\"Drawing 0\" descr=\"" + drawingDescr + "\"/><wp:cNvGraphicFramePr/>"
                + "</wp:anchor>";

        CTDrawing drawing = CTDrawing.Factory.parse(anchorXML);
        CTAnchor anchor = drawing.getAnchorArray(0);
        anchor.setGraphic(graphicalobject);
        return anchor;
    }

    // not in use
    private static void addImage(XWPFDocument document, ImageRenderInfo imageRenderInfo) throws URISyntaxException, InvalidFormatException, IOException {
        PdfImageObject pdfImage = imageRenderInfo.getImage();
        PdfImageObject.ImageBytesType pdfImageType = pdfImage.getImageBytesType();
        int poiImageType = pdfImageType == PdfImageObject.ImageBytesType.JPG ? XWPFDocument.PICTURE_TYPE_JPEG : XWPFDocument.PICTURE_TYPE_PNG;
        String pdfImageFileType = pdfImage.getFileType();

        XWPFParagraph image = document.createParagraph();
        image.setAlignment(ParagraphAlignment.RIGHT);
        image.setVerticalAlignment(TextAlignment.TOP);

        XWPFRun imageRun = image.createRun();
        //imageRun.setTextPosition(1);

        //Path imagePath = Paths.get(ClassLoader.getSystemResource("img1.png").toURI());
        String name = String.format("%s.%s", RandomStringUtils.randomAlphanumeric(8), pdfImageFileType);
        XWPFPicture xwpfPicture = imageRun.addPicture(new ByteArrayInputStream(pdfImage.getImageAsBytes()),
            poiImageType, name,
            Units.toEMU(50), Units.toEMU(50));
    }
}
