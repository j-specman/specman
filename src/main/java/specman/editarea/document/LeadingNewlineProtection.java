package specman.editarea.document;

import javax.swing.JEditorPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;
import java.util.function.Consumer;

import static specman.editarea.document.WrappedDocument.VISIBLE_TEXT_START;

/**
 * Swing's HTMLEditorKit lets every document it builds start with an invisible newline (the empty first
 * paragraph). {@link WrappedDocument} relies on that newline always being there, so this protection
 * keeps it in place: Nothing can be removed from or replaced at offset 0, nothing can be inserted before
 * it, and the caret stays behind it. An empty text consists of exactly that newline, i.e. it has length 1.
 */
public class LeadingNewlineProtection {

  /** The HTML of an empty text: a document with just the leading newline, which is a length of 1. */
  public static final String EMPTY_HTML = "<body></body>";

  private final JEditorPane editor;
  private final AbstractDocument document;
  private final DocumentFilter documentFilter;
  private final NavigationFilter keepCaretBehindNavigationFilter;

  private LeadingNewlineProtection(JEditorPane editor) {
    this.editor = editor;
    this.document = (AbstractDocument) editor.getDocument();
    this.documentFilter = new ProtectingDocumentFilter();
    this.keepCaretBehindNavigationFilter = new KeepCaretBehindNavigationFilter();
  }

  /** Protects the editor's current document, which must already have its content. Setting another document removes the protection. */
  public static LeadingNewlineProtection installOn(JEditorPane editor) {
    LeadingNewlineProtection protection = new LeadingNewlineProtection(editor);
    protection.document.setDocumentFilter(protection.documentFilter);
    editor.setNavigationFilter(protection.keepCaretBehindNavigationFilter);
    protection.moveCaretBehindLeadingNewline();
    return protection;
  }

  /**
   * To be called by the editor's setText. JEditorPane#setText removes the complete content and reads the new content
   * at offset 0. HTMLDocument can't do that on a document which isn't empty at offset 0, so the protection is
   * suspended meanwhile. An empty text is read as an empty body, which brings a document with just the leading newline.
   */
  public void setText(String text, Consumer<String> setTextOfEditor) {
    document.setDocumentFilter(null);
    try {
      setTextOfEditor.accept(text == null || text.isEmpty() ? EMPTY_HTML : text);
    }
    finally {
      document.setDocumentFilter(documentFilter);
    }
    moveCaretBehindLeadingNewline();
  }

  private void moveCaretBehindLeadingNewline() {
    if (hasLeadingNewline() && editor.getCaretPosition() < VISIBLE_TEXT_START) {
      editor.setCaretPosition(VISIBLE_TEXT_START);
    }
  }

  private boolean hasLeadingNewline() {
    try {
      return document.getLength() > 0 && document.getText(0, 1).equals("\n");
    }
    catch (BadLocationException blx) {
      throw new WrappedBadLocationException(blx);
    }
  }

  private class ProtectingDocumentFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException {
      super.insertString(fb, protectedOffset(offset), text, attrs);
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
      int protectedOffset = protectedOffset(offset);
      int protectedLength = length - (protectedOffset - offset);
      if (protectedLength > 0) {
        super.remove(fb, protectedOffset, protectedLength);
      }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
      int protectedOffset = protectedOffset(offset);
      super.replace(fb, protectedOffset, Math.max(length - (protectedOffset - offset), 0), text, attrs);
    }

    private int protectedOffset(int offset) {
      return offset < VISIBLE_TEXT_START && hasLeadingNewline() ? VISIBLE_TEXT_START : offset;
    }
  }

  private class KeepCaretBehindNavigationFilter extends NavigationFilter {
    @Override
    public void setDot(FilterBypass fb, int dot, Position.Bias bias) {
      fb.setDot(behindLeadingNewline(dot), bias);
    }

    @Override
    public void moveDot(FilterBypass fb, int dot, Position.Bias bias) {
      fb.moveDot(behindLeadingNewline(dot), bias);
    }

    private int behindLeadingNewline(int dot) {
      return dot < VISIBLE_TEXT_START && hasLeadingNewline() ? VISIBLE_TEXT_START : dot;
    }
  }
}
