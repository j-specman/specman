package specman.editarea.keylistener;

import specman.ChangeSet;
import specman.clipboard.SpecmanTextTransferable;
import specman.clipboard.SpecmanTableTransferable;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedElement;
import specman.editarea.document.WrappedPosition;
import specman.editarea.document.WrappedBadLocationException;
import specman.editarea.markups.TextMarkup;
import specman.model.v002.Markup_V002;
import specman.undo.manager.UndoRecording;
import specman.model.v002.TextEditAreaModel_V002;
import specman.view.AbstractSchrittView;

import static specman.Specman.editor;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;

import static specman.ChangeSet.changeset;

class PasteKeyPressedHandler extends AbstractKeyEventHandler {
  PasteKeyPressedHandler(TextEditArea textArea, KeyEvent keyEvent) {
    super(textArea, keyEvent);
  }

  void handle() {
    try (UndoRecording ur = editor().composeUndo()) {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable contents = clipboard.getContents(null);
      if (contents != null) {
        if (contents.isDataFlavorSupported(SpecmanTextTransferable.SPECMAN_TEXT_FLAVOR)) {
          event.consume();
          TextEditAreaModel_V002 model = (TextEditAreaModel_V002)
              contents.getTransferData(SpecmanTextTransferable.SPECMAN_TEXT_FLAVOR);
          pasteFormatted(model, contents, clipboard);
          return;
        }
        // Checked before the stringFlavor fallback below, which would otherwise swallow a
        // copied table as its raw serialized text (SpecmanTableTransferable also supports
        // stringFlavor, for interop with pasting into a plain text editor).
        if (contents.isDataFlavorSupported(SpecmanTableTransferable.SPECMAN_TABLE_FLAVOR)) {
          event.consume();
          editor().pasteTableInto(textArea);
          return;
        }
        // External content (Word etc.): strip to plain text to avoid messy HTML,
        // then let JEditorPane's default paste action insert it.
        if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
          String stringOnly = (String)contents.getTransferData(DataFlavor.stringFlavor);
          contents = new StringSelection(stringOnly);
          clipboard.setContents(contents, null);
        }
        if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
          BufferedImage image = (BufferedImage) contents.getTransferData(DataFlavor.imageFlavor);
          textArea.addImage(image);
        }
      }
    }
    catch(Exception x) {
      x.printStackTrace();
    }
  }

  private void pasteFormatted(TextEditAreaModel_V002 model, Transferable specmanTransferable, Clipboard clipboard) throws Exception {
    List<Markup_V002> steplinkOnlyMarkups = createSteplinkOnlyMarkups(model);
    TextEditAreaModel_V002 formattingOnly = new TextEditAreaModel_V002(
        model.text, model.plainText, steplinkOnlyMarkups, (specman.ChangeInfo) null);
    TextEditArea temp = new TextEditArea(formattingOnly, textArea.getFont());
    if (hasMultipleParagraphs(temp)) {
      pasteMultipleParagraphs(temp, steplinkOnlyMarkups, specmanTransferable, clipboard);
    }
    else {
      pasteSingleParagraph(temp, steplinkOnlyMarkups, clipboard);
    }
    AbstractSchrittView step = editor().findeSchritt(textArea);
    if (step != null) {
      step.registerAllExistingStepnumbers();
    }
  }

  private List<Markup_V002> createSteplinkOnlyMarkups(TextEditAreaModel_V002 model) {
    if (model.markups != null) {
      ChangeSet targetChangeset = isTrackingChanges() ? changeset() : null;
      return model.markups.stream()
        .filter(m -> m.type.isSteplink())
        .map(m -> m.assign(targetChangeset))
        .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  /** Pastes multi-paragraph content via the clipboard copy/paste path, which preserves
   * HTML structure and paragraph types. Formatting comes for free; in change tracking
   * mode the changeset color must be applied explicitly to the inserted range.
   * The clipboard is temporarily replaced with the temp area's content for the paste,
   * and must be restored to the original Specman transferable afterwards.
   * Background colors (Steplinks, changeset) are stripped by WysiwygHTMLEditorKit
   * during paste and must be re-applied explicitly. */
  private void pasteMultipleParagraphs(TextEditArea temp, List<Markup_V002> steplinkOnlyMarkups,
                                       Transferable specmanTransferable, Clipboard clipboard) {
    int caretUIBefore = textArea.getCaretPosition();
    int caretModelBefore = getWrappedCaretPosition().toModel();
    temp.selectAll();
    temp.copy();
    textArea.paste();
    if (isTrackingChanges()) {
      int caretAfter = textArea.getCaretPosition();
      ((StyledDocument) textArea.getDocument())
          .setCharacterAttributes(caretUIBefore, caretAfter - caretUIBefore,
              changeset().textBackground(), false);
    }
    if (!steplinkOnlyMarkups.isEmpty()) {
      int offset = caretModelBefore + 1; // +1 for leading structural \n that paste inserts
      applySteplinkColors(steplinkOnlyMarkups, offset);
    }
    clipboard.setContents(specmanTransferable, null);
  }

  /** Pastes single-paragraph content as plain text, then restores character formatting
   * (bold, italic etc.) from the source. Using {@code replaceSelection} automatically
   * picks up the StyledEditorKit input attributes, which
   * {@code aenderungsStilSetzenWennNochNichtVorhanden()} already set to the changeset
   * color in change tracking mode — so the inserted text is colored correctly for free. */
  private void pasteSingleParagraph(TextEditArea temp, List<Markup_V002> nonChangeMarkups,
                                    Clipboard clipboard) throws Exception {
    textArea.replaceSelection("");
    int insertModelPos = getWrappedCaretPosition().toModel();
    int insertStart = textArea.getCaretPosition();
    String plainText = (String) clipboard.getContents(null).getTransferData(DataFlavor.stringFlavor);
    textArea.replaceSelection(plainText);
    applyCharacterFormatting(temp, insertStart);
    applySteplinkColors(nonChangeMarkups, insertModelPos);
  }

  private void applyCharacterFormatting(TextEditArea source, int insertStart) {
    WrappedDocument sourceDoc = source.getWrappedDocument();
    StyledDocument targetDoc = (StyledDocument) textArea.getDocument();
    List<WrappedElement> leaves = new ArrayList<>();
    collectLeaves(sourceDoc.getRootElements().get(0), leaves); // skip bidi root
    int targetPos = insertStart;
    for (WrappedElement leaf : leaves) {
      int length = leaf.getEndOffset().distance(leaf.getStartOffset());
      try {
        String text = sourceDoc.getText(leaf.getStartOffset(), length);
        if ("\n".equals(text)) continue;
        SimpleAttributeSet attrsWithoutBg = new SimpleAttributeSet(leaf.getAttributes());
        attrsWithoutBg.removeAttribute(javax.swing.text.html.CSS.Attribute.BACKGROUND_COLOR);
        attrsWithoutBg.removeAttribute(javax.swing.text.StyleConstants.Background);
        targetDoc.setCharacterAttributes(targetPos, text.length(), attrsWithoutBg, false);
        targetPos += text.length();
      }
      catch (Exception ignored) {}
    }
  }

  /** Applies Steplink background colors directly to specific spans with merge mode,
   * without MarkupBackgroundStyleInitializer's standardSection reset that would
   * overwrite the changeset yellow on surrounding text. */
  private void applySteplinkColors(List<Markup_V002> markups, int insertModelPos) {
    WrappedDocument wd = getWrappedDocument();
    StyledDocument doc = (StyledDocument) textArea.getDocument();
    for (Markup_V002 m : markups) {
      AttributeSet style = TextMarkup.toBackground(m.type, m.changeset);
      int from = wd.fromModel(insertModelPos + m.from).unwrap();
      doc.setCharacterAttributes(from, m.to - m.from + 1, style, false);
    }
  }

  private void collectLeaves(WrappedElement e, List<WrappedElement> leaves) {
    if (e.getElementCount() == 0) {
      leaves.add(e);
    }
    else {
      for (int i = 0; i < e.getElementCount(); i++) collectLeaves(e.getElement(i), leaves);
    }
  }

  private boolean hasMultipleParagraphs(TextEditArea temp) {
    WrappedDocument doc = temp.getWrappedDocument();
    WrappedPosition firstContent = firstNonNewline(doc, doc.start(), doc.end());
    WrappedPosition lastContent = lastNonNewline(doc, doc.end(), doc.start());
    if (firstContent == null || lastContent == null || !lastContent.greater(firstContent)) {
      return false;
    }
    WrappedElement firstPara = doc.getParagraphElement(firstContent);
    WrappedElement lastPara = doc.getParagraphElement(lastContent);
    return !firstPara.getStartOffset().equals(lastPara.getStartOffset());
  }

  private WrappedPosition firstNonNewline(WrappedDocument doc, WrappedPosition from, WrappedPosition to) {
    for (WrappedPosition p = from; !p.greater(to); p = p.inc()) {
      if (!isNewline(doc, p)) {
        return p;
      }
    }
    return null;
  }

  private WrappedPosition lastNonNewline(WrappedDocument doc, WrappedPosition from, WrappedPosition to) {
    for (WrappedPosition p = from; !p.less(to); p = p.dec()) {
      if (!isNewline(doc, p)) {
        return p;
      }
    }
    return null;
  }

  private boolean isNewline(WrappedDocument doc, WrappedPosition p) {
    try {
      return "\n".equals(doc.getText(p, 1));
    }
    catch (WrappedBadLocationException e) {
      return true; // position at or beyond document boundary — treat as newline to stop iteration
    }
  }
}
