package headless.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises navigation through the upstream export operation, including its headless hooks. */
@ExtendWith(HeadlessExtension.class)
class PdfNavigationExportTest {
  @Test
  void referenceModelExportContainsInternalLinks(@TempDir Path directory) throws Exception {
    specman.loadDiagramCopy(new File("specman-reference-v1.4-model-v2.nsd"), directory);
    File output = directory.resolve("navigation.pdf").toFile();
    specman.exportPdf(output, "A4", true, true);

    assertTrue(specman.shownMessages().isEmpty(), "unexpected messages: " + specman.shownMessages());
    int links = 0;
    try (PdfDocument pdf = new PdfDocument(new com.itextpdf.kernel.pdf.PdfReader(output.getAbsolutePath()))) {
      for (int pageNo = 1; pageNo <= pdf.getNumberOfPages(); pageNo++) {
        for (var annotation : pdf.getPage(pageNo).getAnnotations()) {
          if (!PdfName.Link.equals(annotation.getPdfObject().getAsName(PdfName.Subtype))) continue;
          var action = annotation.getPdfObject().getAsDictionary(PdfName.A);
          assertEquals(PdfName.GoTo, action.getAsName(PdfName.S));
          assertNotNull(action.getAsArray(PdfName.D));
          links++;
        }
      }
    }
    assertTrue(links > 0, "The normal export operation must include PDF navigation");
  }
}
