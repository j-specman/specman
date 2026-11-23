package specman.model.v002.io;

import specman.model.v002.DiagramModel_V002;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.TextEditAreaModel_V002;


/** Ensures the plain=`...` field of every TextEditArea is in sync with its HTML content. */
public class PlainTextSynchronizer_V002 {

  /** Re-renders plain text for every TextEditArea where the stored plain text
   * differs from a fresh rendering of its HTML content.
   * @return number of text areas updated */
  public static int updateAllPlainTexts(DiagramModel_V002 model) {
    int[] count = {0};
    TextAreaTraversal_V002.forEachContent(model, (content, site) -> updatePlainTextsInContent(content, count));
    return count[0];
  }

  private static void updatePlainTextsInContent(EditorContentModel_V002 content, int[] count) {
    for (int i = 0; i < content.areas.size(); i++) {
      if (content.areas.get(i) instanceof TextEditAreaModel_V002 textArea) {
        TextEditAreaModel_V002 updated = refreshPlainText(textArea);
        if (updated != textArea) {
          content.areas.set(i, updated);
          count[0]++;
        }
      }
    }
  }

  private static TextEditAreaModel_V002 refreshPlainText(TextEditAreaModel_V002 model) {
    if (model.text == null) {
      return model;
    }
    String freshPlainText = HtmlToPlainText.convert(model.text);
    if (freshPlainText.equals(model.plainText)) {
      return model;
    }
    return new TextEditAreaModel_V002(model.text, freshPlainText, model.markups, model.changeInfo);
  }
}
