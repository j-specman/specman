package specman.ops;

import specman.ChangeSet;
import specman.EditorI;
import specman.ScrollPause;
import specman.SpaltenResizer;
import specman.Specman;
import specman.editarea.EditContainer;
import specman.model.v001.PDFExportOptionsModel_V001;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import java.awt.Container;
import java.awt.Image;
import java.io.File;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Interface for operations extracted from the {@link Specman} monolith.
 * Extends {@link EditorI} so that all editor operations are available to ops
 * without duplication. Additional methods cover internal state access needed
 * only by ops, not by view components.
 */
public interface SpecmanOpContext extends EditorI {

  void diagrammLaden();
  void diagrammSpeichern(boolean dateiauswahlErzwingen);
  void exportAsPDF();
  void exportAsGraphviz();
  void exit();
  void zusammenklappenFuerReview();
  void showMessage(String text);
  void dropWelcomeMessage();
  void fehler(String text);
  void displayException(Exception e);
  Container getArbeitsbereich();
  void scrollBy(int delta);
  File getDiagrammDatei();
  JScrollPane getScrollPane();
  SpaltenResizer getBreitenAnpasser();
  void setDiagrammDatei(File file);
  void addRecentFile(File file);
  PDFExportOptionsModel_V001 getPdfExportOptions();
  void clearFocusHistory();
  void resetPdfExportChooser();
  void setChangeModeEnabled(boolean enabled);
  void setZoomFaktor(int prozent);
  void zoomFaktorAnzeigeAktualisieren(int prozent);
  void setDiagrammbreite(int breite);
  void setPdfExportOptions(PDFExportOptionsModel_V001 options);
  void setHauptSequenz(SchrittSequenzView seq);
  void hauptSequenzInitialisieren();
  void setDiagrammName(String name);
  ScrollPause pauseScrolling();
  void discardAllUndoEdits();
  void updateChangeSet(ChangeSet changeSet);
  String getDiagrammName();
  int getDiagrammbreite();
  void newStepPostInit(AbstractSchrittView newStep);
  JPanel getHauptSequenzContainer();
  Image createDiagramImage(int width, int height);
  EditContainer getIntro();
  EditContainer getOutro();
  SchrittSequenzView getHauptSequenz();

}
