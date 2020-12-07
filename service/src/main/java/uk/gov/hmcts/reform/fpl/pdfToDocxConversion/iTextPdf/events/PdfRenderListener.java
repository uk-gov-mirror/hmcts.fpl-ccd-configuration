package uk.gov.hmcts.reform.fpl.pdfToDocxConversion.iTextPdf.events;

import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import uk.gov.hmcts.reform.fpl.pdfToDocxConversion.poi.PoiUtils;

// TODO: try com.itextpdf.text.pdf.parser.ExtRenderListener
public class PdfRenderListener implements RenderListener {
    protected String path;
    protected XWPFDocument wordDoc;

    public PdfRenderListener(String path, XWPFDocument wordDoc) {
        this.path = path;
        this.wordDoc = wordDoc;
    }

    public void beginTextBlock() {
    }

    public void endTextBlock() {
    }

    public void renderImage(ImageRenderInfo renderInfo) {
        try {
            PoiUtils.insertImageWithITextImage(wordDoc, renderInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*try {
            String filename;
            FileOutputStream os;
            PdfImageObject image = renderInfo.getImage();
            PdfName filter = (PdfName) image.get(PdfName.FILTER);
            if (PdfName.DCTDECODE.equals(filter)) {
                filename = String.format(path, renderInfo.getRef().getNumber(), "jpg");
                os = new FileOutputStream(filename);
                os.write(image.getImageAsBytes());
                os.flush();
                os.close();
            } else if (PdfName.JPXDECODE.equals(filter)) {
                filename = String.format(path, renderInfo.getRef().getNumber(), "jp2");
                os = new FileOutputStream(filename);
                os.write(image.getImageAsBytes());
                os.flush();
                os.close();
            } else {
                BufferedImage awtimage = renderInfo.getImage().getBufferedImage();
                if (awtimage != null) {
                    filename = String.format(path,
                        renderInfo.getRef().getNumber(), "png");
                    ImageIO.write(awtimage, "png",
                        new FileOutputStream(filename));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public void renderText(TextRenderInfo renderInfo) {
        PoiUtils.addTextFromITextPdf(wordDoc, renderInfo);
    }

}
