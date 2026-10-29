package specman;

import specman.draganddrop.DragMouseAdapter;
import specman.editarea.EditArea;
import specman.editarea.EditContainer;
import specman.editarea.InteractiveStepFragment;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.metatag.generic.GenericMetaTag;
import specman.metatag.generic.config.MetaTagConfig;
import specman.metatag.generic.config.MetaTagConfigListener;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;
import specman.visitor.DiagramVisitor;

import javax.swing.JEditorPane;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.List;
import java.util.Set;

/** This interface represents the current actogramm editor and is supposed to
 * substitute the older direct access of the {@link Specman} class. This may
 * later e.g. allow to run multiple editors windows within a single editor
 * application. */
public interface EditorI extends FocusListener {
	void vertikalLinieSetzen(int x, Component source);
	void diagrammLaden(File diagramFile);
	/** The file the diagram was loaded from or saved to, null as long as there is none. */
	File getDiagrammDatei();
	int getZoomFactor();
	String instanceId();
	void instrumentWysEditor(JEditorPane ed, String initialText, Integer orientation);
	void diagrammAktualisieren(EditArea editArea);
	TextEditArea getLastFocusedTextArea();
	void setLastFocusedTextArea(TextEditArea area);
	/** Gives the keyboard focus to the passed edit area. Tests override this, as there is no real focus without a displayed frame. */
	void focus(EditArea editArea);
	/** The panel all steps live in, i.e. the common coordinate system of all edit areas. */
	Container getArbeitsbereich();
	void addEdit(UndoableEdit edit);
	UndoRecording pauseUndo();
	UndoRecording composeUndo();
  void showError(EditException ex);
	AbstractSchrittView findStepByStepID(String stepnumberLinkID);
	boolean isKeyPressed(int keyCode);
	void setCursor(Cursor cursorToUse);
  boolean aenderungenVerfolgen();
	AbstractSchrittView findeSchritt(TextEditArea textEditArea);
	double scale(double length);
	void addImageViaFileChooser();
	List<AbstractSchrittView> listAllSteps();
	void addTable(int numColumns, int numRows);
	void toggleListItem(boolean ordered);
  ScrollPause pauseScrolling();
  List<JTextComponent> queryAllTextComponents(JTextComponent tc);
  /** Lets the visitor traverse the whole diagram: intro, steps and outro. Returns once the traversal is done,
   * or the visitor has stopped it by throwing a {@link specman.visitor.StopTraversalException}. */
  void traverseDiagram(DiagramVisitor visitor);
  AbstractSchrittView findStep(InteractiveStepFragment fragment);
  boolean unfoldToShow(InteractiveStepFragment fragment);
  List<GenericMetaTag> queryTagsWithFreetext();
  void scrollBackwardInEditHistory();
  void scrollForwardInEditHistory();
  void appendToEditHistory(EditContainer editContainer);
  void resyncStepnumberStyleADBL();
  int showConfirmDialog(String message, String title, int optionType);
  void deleteStepADBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment);
  void newStepPostInit(AbstractSchrittView newStep);
  /** The clipboard the step operations copy to and paste from. Tests substitute an in-memory clipboard. */
  Clipboard clipboard();
  void copyStepToClipboard(AbstractSchrittView step);
  void cutStepToClipboard(AbstractSchrittView step, InteractiveStepFragment initiatingFragment);
  void pasteStepsAfter(AbstractSchrittView referenceStep);
  void copyTableToClipboard(TableEditArea table);
  void cutTableToClipboard(TableEditArea table);
  void pasteTableInto(TextEditArea initiatingTextArea);
  void moveBranchSequenceLeftADBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment);
  void moveBranchSequenceRightADBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment);
  DragMouseAdapter createDragMouseAdapter();
  ChangeSet changeset();
  void addMetaTagConfigListener(MetaTagConfigListener listener);
  void removeMetaTagConfigListener(MetaTagConfigListener listener);
  List<MetaTagConfig> metaTagConfigs();
  MetaTagConfig findMetaTagConfig(String name);
  Set<String> queryMetaTagConfigsInUse();
  void openMetaTagConfigs();
  void showToast(String message);
}
