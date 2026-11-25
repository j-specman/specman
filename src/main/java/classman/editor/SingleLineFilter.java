package classman.editor;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/** Keeps a text to one line: line breaks which are typed or pasted become blanks. */
class SingleLineFilter extends DocumentFilter {

  @Override
  public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
      throws BadLocationException {
    super.insertString(bypass, offset, withoutLineBreaks(text), attributes);
  }

  @Override
  public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
      throws BadLocationException {
    super.replace(bypass, offset, length, text == null ? null : withoutLineBreaks(text), attributes);
  }

  private static String withoutLineBreaks(String text) {
    return text == null ? null : text.replaceAll("\r\n|\r|\n", " ");
  }
}
