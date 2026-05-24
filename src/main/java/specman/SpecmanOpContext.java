package specman;

import specman.editarea.EditContainer;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

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
  void newStepPostInit(AbstractSchrittView newStep);
  javax.swing.JPanel getHauptSequenzContainer();
  java.awt.Image createDiagramImage(int width, int height);
  EditContainer getIntro();
  EditContainer getOutro();
  SchrittSequenzView getHauptSequenz();

}
