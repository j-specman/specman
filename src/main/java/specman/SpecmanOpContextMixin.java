package specman;

import specman.editarea.EditArea;
import specman.undo.manager.UndoRecording;
import specman.editarea.EditContainer;
import specman.editarea.InteractiveStepFragment;
import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

public interface SpecmanOpContextMixin {
  SpecmanOpContext context();

  // SpecmanOpContext
  default void exportAsPDF() { context().exportAsPDF(); }
  default void zusammenklappenFuerReview() { context().zusammenklappenFuerReview(); }
  default void showMessage(String text) { context().showMessage(text); }
  default EditContainer getIntro() { return context().getIntro(); }
  default EditContainer getOutro() { return context().getOutro(); }
  default SchrittSequenzView getHauptSequenz() { return context().getHauptSequenz(); }

  // EditorI methods used by ops
  default void showError(EditException ex) { context().showError(ex); }
  default UndoRecording composeUndo() { return context().composeUndo(); }
  default void diagrammAktualisieren(EditArea editArea) { context().diagrammAktualisieren(editArea); }
  default InteractiveStepFragment getLastFocusedTextArea() { return context().getLastFocusedTextArea(); }
  default AbstractSchrittView findeSchritt(TextEditArea area) { return context().findeSchritt(area); }
  default void addEdit(javax.swing.undo.UndoableEdit edit) { context().addEdit(edit); }
  default void deleteStepADBL(AbstractSchrittView step, InteractiveStepFragment fragment) { context().deleteStepADBL(step, fragment); }
  default void dropWelcomeMessage() { context().dropWelcomeMessage(); }
  default void fehler(String text) { context().fehler(text); }
  default void newStepPostInit(AbstractSchrittView newStep) { context().newStepPostInit(newStep); }
  default javax.swing.JPanel getHauptSequenzContainer() { return context().getHauptSequenzContainer(); }
  default java.awt.Image createDiagramImage(int width, int height) { return context().createDiagramImage(width, height); }
  default void resyncStepnumberStyleADBL() { context().resyncStepnumberStyleADBL(); }
  default int getZoomFactor() { return context().getZoomFactor(); }
}
