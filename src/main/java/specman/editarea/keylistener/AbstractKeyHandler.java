package specman.editarea.keylistener;

import specman.Specman;
import specman.editarea.autocomplete.AutoCompletion;
import specman.editarea.TextEditArea;
import specman.editarea.TextEditAreaAccessMixin;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.undo.manager.UndoRecording;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.CSS;

import static specman.editarea.TextStyles.INDIKATOR_GELOESCHT_MARKIERT;
import static specman.editarea.TextStyles.INDIKATOR_GRAU;
import static specman.editarea.TextStyles.INDIKATOR_SCHWARZ;
import static specman.editarea.TextStyles.geaendertTextBackground;
import static specman.editarea.TextStyles.standardStil;

public class AbstractKeyHandler implements TextEditAreaAccessMixin {
  protected final TextEditArea textArea;

  protected AbstractKeyHandler(TextEditArea textArea) {
    this.textArea = textArea;
  }

  @Override
  public TextEditArea textArea() { return textArea; }

  protected boolean shouldPreventActionInsideStepnumberLink() {
    if (stepnumberLinkStyleSet(getWrappedSelectionStart()) || stepnumberLinkStyleSet(getWrappedSelectionEnd())) {
      if (isCaretInsideSelection()) {
        return true;
      }

      for (WrappedPosition i = getWrappedSelectionStart(); i.less(getWrappedSelectionEnd()); i = i.inc()) {
        if (stepnumberLinkStyleSet(i)) {
          if (getStartOffsetFromPosition(i).less(getWrappedSelectionStart()) ||
            getEndOffsetFromPosition(i).greater(getWrappedSelectionEnd())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  protected boolean isCaretInsideSelection() {
    WrappedPosition linkStyleStart = getStartOffsetFromPosition(getWrappedSelectionEnd());
    WrappedPosition linkStyleEnd = getEndOffsetFromPosition(getWrappedSelectionEnd());
    return getWrappedSelectionStart().equals(getWrappedSelectionEnd()) &&
      getWrappedSelectionEnd().less(linkStyleEnd) &&
      getWrappedSelectionStart().greater(linkStyleStart);
  }

  protected boolean skipToStepnumberLinkEnd() {
    WrappedPosition selectionEnd = getWrappedSelectionEnd();
    if (stepnumberLinkStyleSet(selectionEnd)) {
      setCaretPosition(getEndOffsetFromPosition(selectionEnd).unwrap());
      return true;
    }
    return false;
  }

  protected boolean skipToStepnumberLinkStart() {
    WrappedPosition selectionStart = getWrappedSelectionStart();
    if (!selectionStart.isZero() && stepnumberLinkStyleSet(selectionStart.dec())) {
      setCaretPosition(getStartOffsetFromPosition(selectionStart.dec()).unwrap());
      return true;
    }
    return false;
  }

  protected void markRangeAsDeleted(WrappedPosition deleteStart, int deleteLength, MutableAttributeSet deleteStyle) {
    getWrappedDocument().setCharacterAttributes(deleteStart, deleteLength, deleteStyle, false);
  }

  protected AttributeSet prepareCorrectStyleForTextInput() {
    return (isTrackingChanges())
      ? aenderungsStilSetzenWennNochNichtVorhanden()
      : standardStilSetzenWennNochNichtVorhanden();
  }

  protected AttributeSet standardStilSetzenWennNochNichtVorhanden() {
    StyledEditorKit k = getEditorKit();
    MutableAttributeSet inputAttributes = k.getInputAttributes();
    if (!ganzerSchrittGeloeschtStilGesetzt()) {
      inputAttributes.addAttributes(standardStil);
    }
    return inputAttributes;
  }

  protected AttributeSet aenderungsStilSetzenWennNochNichtVorhanden() {
    // Durch die folgende If-Abfrage verhindert man, dass die als geändert
    // markierten Buchstaben alle einzelne Elements werden.
    // Wenn an der aktuellen Position schon gelbe Hintergrundfarbe
    // eingestellt ist, dann Ändern wir den aktuellen Style gar nicht mehr.
    StyledEditorKit k = getEditorKit();
    MutableAttributeSet inputAttributes = k.getInputAttributes();
    if (!aenderungsStilGesetzt() && !stepnumberLinkNormalStyleSet(getWrappedCaretPosition())) {
      StyleConstants.setStrikeThrough(inputAttributes, false); // Falls noch Gelöscht-Stil herrschte
      inputAttributes.addAttributes(geaendertTextBackground);
    }
    return inputAttributes;
  }

  protected boolean ganzerSchrittGeloeschtStilGesetzt() {
    StyledEditorKit k = getEditorKit();
    MutableAttributeSet inputAttributes = k.getInputAttributes();
    Object currentTextDecoration = inputAttributes.getAttribute(CSS.Attribute.TEXT_DECORATION);
    Object currentFontColorValue = inputAttributes.getAttribute(CSS.Attribute.COLOR);
    if (currentTextDecoration != null && currentTextDecoration.toString().equals(INDIKATOR_GELOESCHT_MARKIERT)
      && currentFontColorValue != null && currentFontColorValue.toString().equals(INDIKATOR_GRAU)) {
      return false;
    }
    Object currentBackgroundColorValue = inputAttributes.getAttribute(CSS.Attribute.BACKGROUND_COLOR);
    return currentBackgroundColorValue != null
      && currentBackgroundColorValue.toString().equalsIgnoreCase(INDIKATOR_SCHWARZ)
      && currentTextDecoration != null
      && currentTextDecoration.toString().equalsIgnoreCase(INDIKATOR_GELOESCHT_MARKIERT)
      && currentFontColorValue != null && currentFontColorValue.toString().equalsIgnoreCase(INDIKATOR_GRAU);
  }

  protected void resetSuggestedAutoCompletion() {
    AutoCompletion autoCompletion = textArea().getAutoCompletion();
    if (autoCompletion != null) {
      WrappedPosition autoCompletionEnd = autoCompletion.stop();
      if (autoCompletionEnd != null) {
        WrappedDocument doc = getWrappedDocument();
        WrappedPosition caretPosition = getWrappedCaretPosition();
        int completionLength = autoCompletionEnd.distance(caretPosition);
        try(UndoRecording ur = Specman.instance().pauseUndo()) {
          doc.remove(caretPosition, completionLength);
        }
      }
      textArea().setAutoCompletion(null);
    }
  }
}
