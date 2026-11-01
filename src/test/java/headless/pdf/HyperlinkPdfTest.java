package headless.pdf;

import com.itextpdf.kernel.colors.ColorConstants;
import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hyperlinks in the PDF look like in the editor: blue and underlined. The characters of the link text are "zxq", those
 * of the text behind it "abc", so each one can be found in the PDF by itself. */
@ExtendWith(HeadlessExtension.class)
class HyperlinkPdfTest {

  private static final String LINK_CHARACTERS = "zxq";
  private static final String PLAIN_CHARACTERS = "abc";

  @TempDir
  Path directory;

  private File exportStepWithText(String html) {
    specman.clickNewSimpleStep().getFirstEditArea().asTextArea().setText(html);
    File pdf = directory.resolve("hyperlink.pdf").toFile();
    specman.exportPdf(pdf, "A4", true, false);
    assertEquals(List.of(), specman.shownMessages());
    return pdf;
  }

  private static String hyperlinkAndPlainText() {
    return "<a href=\"https://specman.org\">" + LINK_CHARACTERS + "</a> " + PLAIN_CHARACTERS;
  }

  /** Scenario: The text of a step contains a hyperlink. The link text is drawn in blue, the text next to it stays black. */
  @Test
  void testLinkTextIsBlue() throws IOException {
    File pdf = exportStepWithText(hyperlinkAndPlainText());

    for (char c : LINK_CHARACTERS.toCharArray()) {
      assertEquals(List.of(ColorConstants.BLUE), PdfReader.fillColorsOfText(pdf, String.valueOf(c)), "link character " + c);
    }
    for (char c : PLAIN_CHARACTERS.toCharArray()) {
      assertEquals(List.of(ColorConstants.BLACK), PdfReader.fillColorsOfText(pdf, String.valueOf(c)), "plain character " + c);
    }
  }

  /** Scenario: The link text is underlined in the color of the link, while the text next to it isn't underlined at all. */
  @Test
  void testLinkTextIsUnderlined() throws IOException {
    File pdf = exportStepWithText(hyperlinkAndPlainText());

    assertTrue(PdfReader.strokeColorsOfPaths(pdf).contains(ColorConstants.BLUE));
  }

  /** Without a hyperlink, nothing is blue and nothing underlined. */
  @Test
  void testTextWithoutHyperlinkIsNeitherBlueNorUnderlined() throws IOException {
    File pdf = exportStepWithText(LINK_CHARACTERS + " " + PLAIN_CHARACTERS);

    assertEquals(List.of(ColorConstants.BLACK), PdfReader.fillColorsOfText(pdf, "z"));
    assertFalse(PdfReader.strokeColorsOfPaths(pdf).contains(ColorConstants.BLUE));
  }
}
