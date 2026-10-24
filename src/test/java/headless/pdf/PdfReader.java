package headless.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;

/** Reads back what a test has exported to PDF. */
public class PdfReader {

  private PdfReader() {}

  /** The text of all pages in reading order. Lines are separated by line breaks. */
  public static String text(File pdf) throws IOException {
    try (PdfDocument document = new PdfDocument(new com.itextpdf.kernel.pdf.PdfReader(pdf))) {
      StringBuilder text = new StringBuilder();
      for (int pageNo = 1; pageNo <= document.getNumberOfPages(); pageNo++) {
        text.append(PdfTextExtractor.getTextFromPage(document.getPage(pageNo))).append('\n');
      }
      return text.toString();
    }
  }

}
