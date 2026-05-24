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
  default void scrollBy(int delta) { context().scrollBy(delta); }
  default java.io.File getDiagrammDatei() { return context().getDiagrammDatei(); }
  default javax.swing.JScrollPane getScrollPane() { return context().getScrollPane(); }
  default SpaltenResizer getBreitenAnpasser() { return context().getBreitenAnpasser(); }
  default void setDiagrammDatei(java.io.File file) { context().setDiagrammDatei(file); }
  default void addRecentFile(java.io.File file) { context().addRecentFile(file); }
  default specman.model.v001.PDFExportOptionsModel_V001 getPdfExportOptions() { return context().getPdfExportOptions(); }
  default void clearFocusHistory() { context().clearFocusHistory(); }
  default void resetPdfExportChooser() { context().resetPdfExportChooser(); }
  default void setChangeModeEnabled(boolean enabled) { context().setChangeModeEnabled(enabled); }
  default void setZoomFaktor(int prozent) { context().setZoomFaktor(prozent); }
  default void zoomFaktorAnzeigeAktualisieren(int prozent) { context().zoomFaktorAnzeigeAktualisieren(prozent); }
  default void setDiagrammbreite(int breite) { context().setDiagrammbreite(breite); }
  default void setPdfExportOptions(specman.model.v001.PDFExportOptionsModel_V001 options) { context().setPdfExportOptions(options); }
  default void setHauptSequenz(specman.view.SchrittSequenzView seq) { context().setHauptSequenz(seq); }
  default void hauptSequenzInitialisieren() { context().hauptSequenzInitialisieren(); }
  default void setDiagrammName(String name) { context().setDiagrammName(name); }
  default ScrollPause pauseScrolling() { return context().pauseScrolling(); }
  default void discardAllUndoEdits() { context().discardAllUndoEdits(); }
  default String getDiagrammName() { return context().getDiagrammName(); }
  default int getDiagrammbreite() { return context().getDiagrammbreite(); }

  // EditorI methods used by ops
  default UndoRecording composeUndo() { return context().composeUndo(); }
  default void diagrammAktualisieren(EditArea editArea) { context().diagrammAktualisieren(editArea); }
  default TextEditArea getLastFocusedTextArea() { return context().getLastFocusedTextArea(); }
  default AbstractSchrittView findeSchritt(TextEditArea area) { return context().findeSchritt(area); }
  default void addEdit(javax.swing.undo.UndoableEdit edit) { context().addEdit(edit); }
  default void deleteStepADBL(AbstractSchrittView step, InteractiveStepFragment fragment) { context().deleteStepADBL(step, fragment); }
  default void dropWelcomeMessage() { context().dropWelcomeMessage(); }
  default void fehler(String text) { context().fehler(text); }
  default void displayException(Exception e) { context().displayException(e); }
  default java.awt.Container getArbeitsbereich() { return context().getArbeitsbereich(); }
  default void newStepPostInit(AbstractSchrittView newStep) { context().newStepPostInit(newStep); }
  default javax.swing.JPanel getHauptSequenzContainer() { return context().getHauptSequenzContainer(); }
  default java.awt.Image createDiagramImage(int width, int height) { return context().createDiagramImage(width, height); }
  default void resyncStepnumberStyleADBL() { context().resyncStepnumberStyleADBL(); }
  default boolean aenderungenVerfolgen() { return context().aenderungenVerfolgen(); }
  default int showConfirmDialog(String message, String title, int optionType) { return context().showConfirmDialog(message, title, optionType); }
  default int getZoomFactor() { return context().getZoomFactor(); }
}
