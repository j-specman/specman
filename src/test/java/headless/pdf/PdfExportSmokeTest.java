package headless.pdf;

import headless.HeadlessExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDF export smoke tests: The export must run through without exceptions and produce a file
 * of reasonable size. Whether the rendering looks right is not checked here.
 * The PDFs are kept in {@link #PDF_DIRECTORY} for a manual look. They are numbered by
 * {@link #nextPdfFile()}, so each test method gets its own file.
 */
@ExtendWith(HeadlessExtension.class)
class PdfExportSmokeTest {

  private static final Path PDF_DIRECTORY = Path.of("target", "pdf-tests");
  private static final File REFERENCE_MODEL = new File("specman-reference-v1.4-model-v2.nsd");
  private static final long MIN_REFERENCE_PDF_BYTES = 130_000;

  private static int pdfCounter = 0;

  @BeforeAll
  static void deleteOldPdfs() throws IOException {
    Files.createDirectories(PDF_DIRECTORY);
    try (DirectoryStream<Path> oldPdfs = Files.newDirectoryStream(PDF_DIRECTORY, "test*.pdf")) {
      for (Path oldPdf : oldPdfs) {
        Files.delete(oldPdf);
      }
    }
  }

  private static File nextPdfFile() {
    pdfCounter++;
    return PDF_DIRECTORY.resolve("test" + pdfCounter + ".pdf").toAbsolutePath().toFile();
  }

  @Test
  void referenceModelExport(@TempDir Path modelCopyDirectory) throws IOException {
    File pdf = nextPdfFile();
    specman.loadDiagramCopy(REFERENCE_MODEL, modelCopyDirectory);

    specman.exportPdf(pdf, "A4", true, false);

    assertEquals(0, specman.shownMessages().size(), "unexpected messages: " + specman.shownMessages());
    assertTrue(pdf.length() >= MIN_REFERENCE_PDF_BYTES,
      pdf.getName() + " has only " + pdf.length() + " bytes");
  }

}
