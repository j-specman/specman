package headless.image.plantuml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static headless.pdf.PdfReader.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantUmlPdfTest extends AbstractPlantUmlTest {

  /** The diagram is drawn as vector graphic, so its texts are real PDF text instead of pixels of a bitmap. */
  @Test
  void testDiagramTextsAreRealPdfText(@TempDir Path directory) throws IOException {
    insertDiagramByToolbarButton(containerOfNewStep()).commitSource(TWO_CLASSES);
    specman.layoutComponents();
    File pdf = directory.resolve("diagram.pdf").toFile();

    specman.exportPdf(pdf, "A4", true, false);

    assertEquals(List.of(), specman.shownMessages());
    String pdfText = text(pdf);
    assertTrue(pdfText.contains("Hello"), pdfText);
    assertTrue(pdfText.contains("World"), pdfText);
  }
}
