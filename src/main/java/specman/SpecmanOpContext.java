package specman;

import specman.editarea.EditContainer;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import java.io.File;

/**
 * Interface for operations extracted from the {@link Specman} monolith.
 * Extends {@link EditorI} so that all editor operations are available to ops
 * without duplication. Additional methods cover internal state access needed
 * only by ops, not by view components.
 */
public interface SpecmanOpContext extends EditorI {

  void exportAsPDF();
  void zusammenklappenFuerReview();
  void showMessage(String text);
  void dropWelcomeMessage();
  void fehler(String text);
  void displayException(Exception e);
  java.awt.Container getArbeitsbereich();
  void scrollBy(int delta);
  java.io.File getDiagrammDatei();
  javax.swing.JScrollPane getScrollPane();
  SpaltenResizer getBreitenAnpasser();
  void setDiagrammDatei(File file);
  void addRecentFile(File file);
  specman.model.v001.PDFExportOptionsModel_V001 getPdfExportOptions();
  void clearFocusHistory();
  void resetPdfExportChooser();
  void setChangeModeEnabled(boolean enabled);
  void setZoomFaktor(int prozent);
  void zoomFaktorAnzeigeAktualisieren(int prozent);
  void setDiagrammbreite(int breite);
  void setPdfExportOptions(specman.model.v001.PDFExportOptionsModel_V001 options);
  void setHauptSequenz(specman.view.SchrittSequenzView seq);
  void hauptSequenzInitialisieren();
  void setDiagrammName(String name);
  ScrollPause pauseScrolling();
  void discardAllUndoEdits();
  String getDiagrammName();
  int getDiagrammbreite();
  void newStepPostInit(AbstractSchrittView newStep);
  javax.swing.JPanel getHauptSequenzContainer();
  java.awt.Image createDiagramImage(int width, int height);
  EditContainer getIntro();
  EditContainer getOutro();
  SchrittSequenzView getHauptSequenz();

}
