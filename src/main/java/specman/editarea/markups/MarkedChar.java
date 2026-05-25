package specman.editarea.markups;

import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedElement;
import specman.editarea.document.WrappedPosition;

public class MarkedChar {
  public final char c;
  public final TextMarkup markup;

  public MarkedChar(WrappedDocument doc, WrappedPosition p) {
    c = doc.getChar(p);
    WrappedElement element = doc.getCharacterElement(p);
    markup = TextMarkup.fromBackground(element);
  }

  public MarkedChar(char c, TextMarkup markup) {
    this.c = c;
    this.markup = markup;
  }

}
