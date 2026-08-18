package specman.editarea.markups;

import specman.editarea.document.TestWrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.model.v002.Markup_V002;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class AbstractChangemarkRecoveryTest {
  protected void assertMarked(List<Markup_V002> marks, Integer... fromTo) {
    assertNotNull(marks);
    assertEquals(fromTo.length / 2, marks.size());
    List<Markup_V002> expected = marks(fromTo);
    assertEquals(expected, marks);
  }

  protected List<Markup_V002> split(String s1, String s2) {
    String documentContent = s1
      .replace("|", "")
      .replace(" ", "")
      .replace("_", " ");
    TestWrappedDocument doc = new TestWrappedDocument(documentContent);
    WrappedPosition cut = doc.fromModel(s1.indexOf('|') / 2);
    List<Markup_V002> marks = marksString2Marks(s2);

    return new MarkupRecovery(doc, null).recover();
  }

  private List<Markup_V002> marksString2Marks(String markstring) {
    List<Markup_V002> marks = new ArrayList<>();
    String gapsRemoved = "";
    for (int i = 0; i < markstring.length()-1; i += 2) {
      gapsRemoved += markstring.substring(i+1, i+2);
    }
    Integer markstart = null;
    int marklength = 0;
    for (int i = 0; i < gapsRemoved.length(); i++) {
      if (gapsRemoved.charAt(i) == 'x') {
        if (markstart == null) {
          markstart = i;
          marklength = 1;
        }
        else {
          marklength++;
        }
      }
      else {
        if (markstart != null) {
          marks.add(new Markup_V002(markstart, markstart + marklength - 1, MarkupType.Changed, null));
          markstart = null;
        }
      }
    }
    if (markstart != null) {
      marks.add(new Markup_V002(markstart, markstart + marklength - 1, MarkupType.Changed, null));
    }
    return marks;
  }

  protected List<Markup_V002> marks(Integer... fromTo) {
    List<Markup_V002> marks = new ArrayList<>();
    for (int i = 0; i < fromTo.length; i += 2) {
      marks.add(new Markup_V002(fromTo[i], fromTo[i+1], MarkupType.Changed, null));
    }
    return marks;
  }

}
