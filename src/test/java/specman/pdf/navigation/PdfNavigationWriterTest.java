package specman.pdf.navigation;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static specman.pdf.navigation.PdfNavigationData.*;
import static specman.pdf.navigation.PdfNavigationPlan.TargetId;

class PdfNavigationWriterTest {
  @TempDir
  Path tempDir;

  private final TargetId first = TargetId.step("first");
  private final TargetId second = TargetId.catchHeading("second");
  private final PdfNavigationWriter writer = new PdfNavigationWriter();

  @Test
  void samePageSelfAndRepeatedLinksUseBorderlessInternalGoToActions() throws Exception {
    PdfNavigationData data = new PdfNavigationData(Map.of(first, new Destination(1, 40, 750)),
      List.of(area(1, 700, first), area(1, 650, first), area(1, 750, first)));
    try (PdfDocument pdf = read(writer.addNavigation(pdf(1), data))) {
      assertEquals(3, pdf.getPage(1).getAnnotations().size());
      for (var annotation : pdf.getPage(1).getAnnotations()) {
        assertDestination(pdf, annotation.getPdfObject(), 1, 40, 750);
        PdfArray border = annotation.getPdfObject().getAsArray(PdfName.Border);
        assertEquals(3, border.size());
        for (int i = 0; i < 3; i++) assertEquals(0, border.getAsNumber(i).intValue());
      }
      PdfArray rect = pdf.getPage(1).getAnnotations().getFirst().getRectangle();
      assertEquals(40, rect.getAsNumber(0).floatValue(), 0.01F);
      assertEquals(700, rect.getAsNumber(1).floatValue(), 0.01F);
      assertEquals(160, rect.getAsNumber(2).floatValue(), 0.01F);
      assertEquals(720, rect.getAsNumber(3).floatValue(), 0.01F);
      assertNull(pdf.getCatalog().getPdfObject().get(PdfName.OpenAction));
      assertNull(pdf.getCatalog().getPdfObject().get(PdfName.AA));
    }
  }

  @Test
  void forwardAndBackwardLinksReferToTheFinalDocumentsActualPages() throws Exception {
    PdfNavigationData data = new PdfNavigationData(
      Map.of(first, new Destination(1, 50, 760), second, new Destination(2, 60, 740)),
      List.of(area(1, 700, second), area(2, 700, first)));
    try (PdfDocument pdf = read(writer.addNavigation(pdf(2), data))) {
      assertDestination(pdf, pdf.getPage(1).getAnnotations().getFirst().getPdfObject(), 2, 60, 740);
      assertDestination(pdf, pdf.getPage(2).getAnnotations().getFirst().getPdfObject(), 1, 50, 760);
    }
  }

  @Test
  void unresolvedTargetsAreSkippedWithoutAffectingValidLinks() throws Exception {
    PdfNavigationData data = new PdfNavigationData(Map.of(first, new Destination(1, 50, 760)),
      List.of(area(1, 700, second), area(1, 650, first)));
    try (PdfDocument pdf = read(writer.addNavigation(pdf(1), data))) {
      assertEquals(1, pdf.getPage(1).getAnnotations().size());
      assertDestination(pdf, pdf.getPage(1).getAnnotations().getFirst().getPdfObject(), 1, 50, 760);
    }
  }

  @Test
  void emptyAndEntirelyUnresolvedNavigationReturnUnchangedIndependentBytes() throws Exception {
    byte[] original = pdf(1);
    for (PdfNavigationData data : List.of(PdfNavigationData.empty(),
      new PdfNavigationData(Map.of(), List.of(area(1, 700, first))),
      new PdfNavigationData(Map.of(first, new Destination(1, 50, 760)), List.of()))) {
      byte[] result = writer.addNavigation(original, data);
      assertArrayEquals(original, result);
      assertNotSame(original, result);
    }
  }

  @Test
  void invalidPdfsFailEvenWhenThereAreNoLinks() {
    assertThrows(Exception.class, () -> writer.addNavigation(new byte[] {1, 2, 3}, PdfNavigationData.empty()));
    assertThrows(Exception.class, () -> writer.addNavigation(new byte[0],
      new PdfNavigationData(Map.of(first, new Destination(1, 50, 760)), List.of(area(1, 700, first)))));
  }

  @Test
  void sourceAndDestinationPagesMustExist() throws Exception {
    byte[] original = pdf(1);
    assertThrows(IllegalArgumentException.class, () -> writer.addNavigation(original,
      new PdfNavigationData(Map.of(first, new Destination(2, 50, 760)), List.of(area(1, 700, first)))));
    assertThrows(IllegalArgumentException.class, () -> writer.addNavigation(original,
      new PdfNavigationData(Map.of(first, new Destination(1, 50, 760)), List.of(area(2, 700, first)))));
    assertThrows(IllegalArgumentException.class, () -> writer.addNavigation(original,
      new PdfNavigationData(Map.of(), List.of(area(2, 700, first)))));
  }

  @Test
  void positionsOutsideTheVisiblePageAreRejectedInsteadOfClippedOrSilentlyIgnored() throws Exception {
    byte[] original = pdf(1);
    assertThrows(IllegalArgumentException.class, () -> writer.addNavigation(original,
      new PdfNavigationData(Map.of(first, new Destination(1, -1, 750)), List.of(area(1, 700, first)))));
    assertThrows(IllegalArgumentException.class, () -> writer.addNavigation(original,
      new PdfNavigationData(Map.of(first, new Destination(1, 50, 900)), List.of(area(1, 700, first)))));
    assertThrows(IllegalArgumentException.class, () -> writer.addNavigation(original,
      new PdfNavigationData(Map.of(first, new Destination(1, 50, 750)),
        List.of(new LinkArea(1, 580, 700, 100, 20, first)))));
  }

  @Test
  void unrotatedCoordinatesRespectCropBoxesWithNonZeroOrigins() throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (PdfDocument pdf = new PdfDocument(new PdfWriter(output))) {
      pdf.addNewPage(PageSize.A4).setCropBox(new Rectangle(50, 50, 400, 600)).setRotation(90);
    }
    byte[] original = output.toByteArray();
    PdfNavigationData data = new PdfNavigationData(Map.of(first, new Destination(1, 60, 640)),
      List.of(new LinkArea(1, 60, 600, 100, 20, first)));
    try (PdfDocument pdf = read(writer.addNavigation(original, data))) {
      assertEquals(90, pdf.getPage(1).getRotation());
      assertDestination(pdf, pdf.getPage(1).getAnnotations().getFirst().getPdfObject(), 1, 60, 640);
    }
    assertThrows(IllegalArgumentException.class, () -> writer.addNavigation(original,
      new PdfNavigationData(Map.of(first, new Destination(1, 10, 640)), data.links())));
  }

  @Test
  void existingTextGeometryAndAnnotationsArePreserved() throws Exception {
    byte[] base = pdf(2);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(base)), new PdfWriter(output))) {
      pdf.getPage(1).addAnnotation(new PdfLinkAnnotation(new Rectangle(40, 50, 100, 20))
        .setAction(PdfAction.createGoTo(PdfExplicitDestination.createFit(pdf.getPage(2)))));
    }
    byte[] original = output.toByteArray();
    PdfNavigationData data = new PdfNavigationData(Map.of(first, new Destination(2, 60, 750)),
      List.of(area(1, 700, first)));
    try (PdfDocument before = read(original); PdfDocument after = read(writer.addNavigation(original, data))) {
      assertEquals(before.getNumberOfPages(), after.getNumberOfPages());
      for (int pageNo = 1; pageNo <= 2; pageNo++) {
        assertArrayEquals(before.getPage(pageNo).getContentBytes(), after.getPage(pageNo).getContentBytes());
        Rectangle originalSize = before.getPage(pageNo).getPageSize();
        Rectangle currentSize = after.getPage(pageNo).getPageSize();
        assertEquals(originalSize.getX(), currentSize.getX(), 0.01F);
        assertEquals(originalSize.getY(), currentSize.getY(), 0.01F);
        assertEquals(originalSize.getWidth(), currentSize.getWidth(), 0.01F);
        assertEquals(originalSize.getHeight(), currentSize.getHeight(), 0.01F);
        assertEquals(PdfTextExtractor.getTextFromPage(before.getPage(pageNo)),
          PdfTextExtractor.getTextFromPage(after.getPage(pageNo)));
      }
      assertEquals(2, after.getPage(1).getAnnotations().size());
      PdfArray oldDestination = after.getPage(1).getAnnotations().getFirst().getPdfObject()
        .getAsDictionary(PdfName.A).getAsArray(PdfName.D);
      assertEquals(PdfName.Fit, oldDestination.getAsName(1));
    }
  }

  @Test
  void aWriterInstanceDoesNotRetainTargetsBetweenDocuments() throws Exception {
    byte[] original = pdf(1);
    writer.addNavigation(original, new PdfNavigationData(Map.of(first, new Destination(1, 50, 750)),
      List.of(area(1, 700, first))));
    byte[] secondExport = writer.addNavigation(original,
      new PdfNavigationData(Map.of(second, new Destination(1, 50, 750)), List.of(area(1, 700, first))));
    assertArrayEquals(original, secondExport);
  }

  @Test
  void exportedLinksRemainDocumentInternalAfterRenamingTheFile() throws Exception {
    byte[] result = writer.addNavigation(pdf(2),
      new PdfNavigationData(Map.of(first, new Destination(2, 50, 750)), List.of(area(1, 700, first))));
    Path export = tempDir.resolve("export.pdf");
    Path renamed = tempDir.resolve("renamed.pdf");
    Files.write(export, result);
    Files.move(export, renamed);
    try (PdfDocument pdf = new PdfDocument(new PdfReader(renamed.toString()))) {
      assertDestination(pdf, pdf.getPage(1).getAnnotations().getFirst().getPdfObject(), 2, 50, 750);
    }
    assertFalse(Files.exists(export));
  }

  @Test
  void invalidPageNumbersNonFiniteCoordinatesAndEmptyAreasAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> new Destination(0, 10, 20));
    assertThrows(IllegalArgumentException.class, () -> new Destination(1, Float.NaN, 20));
    assertThrows(IllegalArgumentException.class, () -> new LinkArea(1, 10, Float.POSITIVE_INFINITY, 20, 30, first));
    assertThrows(IllegalArgumentException.class, () -> new LinkArea(1, 10, 20, 0, 30, first));
    assertThrows(IllegalArgumentException.class, () -> new LinkArea(1, 10, 20, 20, -1, first));
  }

  private void assertDestination(PdfDocument pdf, PdfDictionary annotation, int page, float x, float y) {
    assertEquals(PdfName.Link, annotation.getAsName(PdfName.Subtype));
    PdfDictionary action = annotation.getAsDictionary(PdfName.A);
    assertNotNull(action);
    assertEquals(PdfName.GoTo, action.getAsName(PdfName.S));
    assertNull(action.get(PdfName.F));
    assertNull(action.get(PdfName.JS));
    assertNull(action.get(PdfName.Next));
    assertNull(annotation.get(PdfName.AA));
    PdfArray destination = action.getAsArray(PdfName.D);
    assertEquals(5, destination.size());
    assertEquals(pdf.getPage(page).getPdfObject().getIndirectReference(), destination.get(0, false));
    assertEquals(PdfName.XYZ, destination.getAsName(1));
    assertEquals(x, destination.getAsNumber(2).floatValue(), 0.01F);
    assertEquals(y, destination.getAsNumber(3).floatValue(), 0.01F);
    assertEquals(0, destination.getAsNumber(4).floatValue(), 0.01F); // Keep the reader's current zoom.
  }

  private LinkArea area(int page, float y, TargetId id) { return new LinkArea(page, 40, y, 120, 20, id); }

  private PdfDocument read(byte[] bytes) throws Exception {
    return new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)));
  }

  private byte[] pdf(int pages) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (PdfDocument pdf = new PdfDocument(new PdfWriter(output))) {
      var font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
      for (int pageNo = 1; pageNo <= pages; pageNo++) {
        new PdfCanvas(pdf.addNewPage(PageSize.A4)).beginText().setFontAndSize(font, 14)
          .moveText(40, 780).showText("Seite " + pageNo).endText();
      }
    }
    return output.toByteArray();
  }
}
