package specman;

import specman.editarea.EditArea;
import specman.editarea.EditContainer;
import specman.editarea.InteractiveStepFragment;
import specman.editarea.TextEditArea;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;

import javax.swing.JEditorPane;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.List;

/** This interface represents the current actogramm editor and is supposed to
 * substitute the older direct access of the {@link Specman} class. This may
 * later e.g. allow to run multiple editors windows within a single editor
 * application. */
public interface EditorI extends FocusListener {
	void schrittFuerNachinitialisierungRegistrieren(AbstractSchrittView schritt);
	void vertikalLinieSetzen(int x, SpaltenResizer spaltenResizer);
	void diagrammLaden(File diagramFile);
	int getZoomFactor();
	void instrumentWysEditor(JEditorPane ed, String initialText, Integer orientation);
	void diagrammAktualisieren(EditArea editArea);
	InteractiveStepFragment getLastFocusedTextArea();
	void setLastFocusedTextArea(TextEditArea area);
	void addEdit(UndoableEdit edit);
	UndoRecording pauseUndo();
	UndoRecording composeUndo();
  void showError(EditException ex);
	AbstractSchrittView findStepByStepID(String stepnumberLinkID);
	boolean isKeyPressed(int keyCode);
	void setCursor(Cursor cursorToUse);
  boolean aenderungenVerfolgen();
	AbstractSchrittView findeSchritt(TextEditArea textEditArea);
	EditContainer getIntro();
	double scale(double length);
	void addImageViaFileChooser();
	List<AbstractSchrittView> listAllSteps();
	void addTable(int numColumns, int numRows);
	void toggleListItem(boolean ordered);
  ScrollPause pauseScrolling();
  List<JTextComponent> queryAllTextComponents(JTextComponent tc);
  AbstractSchrittView findStep(InteractiveStepFragment fragment);
  void scrollBackwardInEditHistory();
  void scrollForwardInEditHistory();
  void appendToEditHistory(EditContainer editContainer);
  void resyncStepnumberStyleUDBL();
  int showConfirmDialog(String message, String title, int optionType);
  void deleteStepUDBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment);
  void moveBranchSequenceLeftUDBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment);
  void moveBranchSequenceRightUDBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment);
  boolean autocompleteOn();
}
