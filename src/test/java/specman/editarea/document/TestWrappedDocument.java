package specman.editarea.document;

import specman.editarea.document.WrappedBadLocationException;
import specman.editarea.document.WrappedDocumentI;
import specman.editarea.document.WrappedPosition;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

public class TestWrappedDocument implements WrappedDocumentI {
  private String content;

  /** An HTML document always ends with a paragraph end, represented by a
   * line break. It is not displayed, but it is part of the document length.
   * {@link #getText(WrappedPosition, int)} with the document's length-1 as
   * offset, returns this line break. Higher offsets lead to a BadLocationException. */
  public TestWrappedDocument(String content) { this.content = content + "\n"; }

  @Override
  public int getLength() { return content.length(); }

  @Override
  public void remove(WrappedPosition offs, int len) {
    content =
      content.substring(0, offs.unwrap()) +
      content.substring(offs.unwrap() + len);
  }

  @Override
  public void insertString(WrappedPosition offset, String str, AttributeSet a) {
    content =
      content.substring(0, offset.unwrap()) +
      str +
      content.substring(offset.unwrap());
  }

  @Override
  public String getText(WrappedPosition offset, int length) {
    if (offset.toModel() >= content.length()) {
      throw new WrappedBadLocationException(new BadLocationException("Offset out of bounds", offset.toModel()));
    }
    return content.substring(offset.unwrap(), offset.unwrap() + length);
  }

  @Override
  public WrappedPosition fromModel(int position) {
    return new WrappedPosition(position, this);
  }

  @Override
  public WrappedPosition fromUI(int position) {
    return new WrappedPosition(position, this);
  }

  @Override
  public int getVisibleTextStart() {
    return 0;
  }
}
