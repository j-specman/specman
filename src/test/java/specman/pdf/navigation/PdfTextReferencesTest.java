package specman.pdf.navigation;

import org.junit.jupiter.api.Test;
import specman.ChangeSet;
import specman.editarea.document.WrappedDocument;

import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextReferencesTest {
  @Test
  void readsOnlyMarkedNumbersIncludingChangeMarkedDeletedReferences() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      WrappedDocument document = html("<div>Normal 1; <span style='background-color:#dcdcdc'>2.3</span>; "
        + "<strike><span style='background-color:" + ChangeSet.DEFAULT.stepnumberLinkHtmlColor()
        + "'>4</span></strike>; <span style='background-color:#dcdcdc'>5?</span></div>");
      String before = text(document);
      var references = PdfTextReferences.read(document);
      assertEquals(List.of("2.3", "4", "5?"), references.stream().map(PdfTextReferences.Reference::text).toList());
      for (var reference : references) {
        assertEquals(reference.text(), before.substring(reference.start(), reference.end()));
      }
      assertEquals(before, text(document));
      assertEquals(references, PdfTextReferences.read(document));
    });
  }

  @Test
  void fontFragmentsOfOneMarkedNumberCannotResolveAsDifferentSteps() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      WrappedDocument document = html("<div>Siehe <span style='background-color:#dcdcdc'>"
        + "<b>12</b><i>.3</i></span> und <span style='background-color:#dcdcdc'>7</span>.</div>");
      var references = PdfTextReferences.read(document);
      assertEquals(List.of("12.3", "7"), references.stream().map(PdfTextReferences.Reference::text).toList());
      assertEquals("12.3", text(document).substring(references.getFirst().start(), references.getFirst().end()));
    });
  }

  @Test
  void emptyAndUnmarkedTextHaveNoReferences() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      assertTrue(PdfTextReferences.read(html("<div></div>")).isEmpty());
      assertTrue(PdfTextReferences.read(html("<div>1, 2.3, 99 und 1?</div>")).isEmpty());
    });
  }

  private WrappedDocument html(String content) {
    try {
      HTMLEditorKit kit = new HTMLEditorKit();
      StyledDocument document = (StyledDocument) kit.createDefaultDocument();
      kit.read(new StringReader("<html><body>" + content + "</body></html>"), document, 0);
      return new WrappedDocument(document);
    }
    catch (Exception e) { throw new AssertionError(e); }
  }

  private String text(WrappedDocument document) {
    return document.getText(document.start(), document.getCore().getLength() - document.getVisibleTextStart());
  }
}
