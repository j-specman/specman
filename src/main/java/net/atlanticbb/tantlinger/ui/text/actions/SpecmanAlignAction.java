package net.atlanticbb.tantlinger.ui.text.actions;

import net.atlanticbb.tantlinger.ui.text.HTMLUtils;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The alignment action of the Shef library with the following fixes for Specman:
 * <ul>
 *   <li>Text which is not wrapped in an element yet (all new text) can be aligned. The library ignores paragraphs
 *   which are direct children of the body, so such text is wrapped in an aligned div first.</li>
 *   <li>The selected state follows the paragraph at the caret, and a paragraph without alignment counts as left
 *   aligned. The buttons of the four actions are therefore never selected both at once, and none of them is without
 *   selection.</li>
 *   <li>The change marks survive the edit, and the edit is one undo step.</li>
 * </ul>
 */
public class SpecmanAlignAction extends HTMLAlignAction {

  private final int alignment;
  private final Runnable afterAlign;

  /** @param afterAlign is called after the text has been aligned, so that the state of all alignment
   *                    actions can be refreshed from the document */
  public SpecmanAlignAction(int alignment, Runnable afterAlign) throws IllegalArgumentException {
    super(alignment);
    this.alignment = alignment;
    this.afterAlign = afterAlign;
  }

  @Override
  protected void wysiwygEditPerformed(ActionEvent event, JEditorPane editor) {
    MarkupPreservingEdit.run(editor, () -> {
      wrapBodyLevelParagraphs(editor);
      super.wysiwygEditPerformed(event, editor);
    });
    afterAlign.run();
  }

  @Override
  protected void updateWysiwygContextState(JEditorPane editor) {
    setSelected(alignmentAtCaret(editor).equals(ALIGNMENTS[alignment]));
  }

  private static String alignmentAtCaret(JEditorPane editor) {
    HTMLDocument document = (HTMLDocument) editor.getDocument();
    Element paragraph = document.getParagraphElement(editor.getCaretPosition());
    Element aligned = HTMLUtils.isImplied(paragraph) ? paragraph.getParentElement() : paragraph;
    Object attribute = aligned.getAttributes().getAttribute(HTML.Attribute.ALIGN);
    return attribute != null ? attribute.toString() : ALIGNMENTS[LEFT];
  }

  /** The library skips paragraphs directly in the body, because it can't replace the body. Wrapping them in a
   * div makes them what it can handle. Selection and caret stay where they were, because only a div is added.
   * Left aligned is the default of text without a div, so there is nothing to wrap for it. */
  private void wrapBodyLevelParagraphs(JEditorPane editor) {
    if (isDefaultAlignment()) {
      return;
    }
    HTMLDocument document = (HTMLDocument) editor.getDocument();
    int selectionStart = editor.getSelectionStart();
    int selectionEnd = editor.getSelectionEnd();
    Element body = bodyOf(document, selectionStart);
    if (body == null) {
      return;
    }
    List<Element> toWrap = bodyLevelParagraphsInRange(body, selectionStart, selectionEnd);
    if (toWrap.isEmpty()) {
      return;
    }
    try {
      document.setInnerHTML(body, htmlOfBody(body, toWrap));
    }
    catch (BadLocationException | IOException e) {
      throw new IllegalStateException("Text can't be aligned", e);
    }
    editor.select(selectionStart, selectionEnd);
  }

  private boolean isDefaultAlignment() {
    return alignment == LEFT;
  }

  private static Element bodyOf(HTMLDocument document, int offset) {
    Element element = document.getParagraphElement(offset);
    while (element != null && !HTML.Tag.BODY.toString().equals(element.getName())) {
      element = element.getParentElement();
    }
    return element;
  }

  private static List<Element> bodyLevelParagraphsInRange(Element body, int start, int end) {
    List<Element> paragraphs = new ArrayList<>();
    for (int i = 0; i < body.getElementCount(); i++) {
      Element child = body.getElement(i);
      boolean inRange = child.getStartOffset() <= Math.max(start, end) && child.getEndOffset() > start;
      if (inRange && HTMLUtils.isImplied(child)) {
        paragraphs.add(child);
      }
    }
    return paragraphs;
  }

  private String htmlOfBody(Element body, List<Element> toWrap) {
    StringBuilder html = new StringBuilder();
    for (int i = 0; i < body.getElementCount(); i++) {
      Element child = body.getElement(i);
      if (toWrap.contains(child)) {
        html.append("<div align=\"").append(ALIGNMENTS[alignment]).append("\">")
          .append(HTMLUtils.getElementHTML(child, false)).append("</div>");
      }
      else {
        html.append(HTMLUtils.getElementHTML(child, true));
      }
    }
    return html.toString();
  }
}
