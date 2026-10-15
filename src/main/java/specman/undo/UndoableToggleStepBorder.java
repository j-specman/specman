package specman.undo;

import specman.EditorI;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static specman.i18n.Messages.i18n;

public class UndoableToggleStepBorder extends AbstractUndoableInteraction {
  private final EditorI editor;
  private final AbstractSchrittView schritt;
  private final SchrittSequenzView sequenz;

  public UndoableToggleStepBorder(EditorI editor, AbstractSchrittView schritt, SchrittSequenzView sequenz) {
    this.editor = editor;
    this.schritt = schritt;
    this.sequenz = sequenz;
  }

  @Override
  public void undoEdit() throws CannotUndoException {
    sequenz.toggleBorderType(schritt);
    editor.diagrammAktualisieren(schritt.getFirstEditArea());
  }

  @Override
  public void redoEdit() throws CannotRedoException {
    sequenz.toggleBorderType(schritt);
    editor.diagrammAktualisieren(schritt.getFirstEditArea());
  }

  @Override
  public String getPresentationName() {
    return i18n("undo.step.border.toggled");
  }

  @Override
  public String getUndoPresentationName() {
    return i18n("undo.step.border.toggled.undo");
  }

  @Override
  public String getRedoPresentationName() {
    return i18n("undo.step.border.toggled.redo");
  }
}
