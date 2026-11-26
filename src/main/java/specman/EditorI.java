package specman;

import specman.draganddrop.DragMouseAdapter;
import specman.editarea.EditArea;
import specman.editarea.EditContainer;
import specman.editarea.InteractiveStepFragment;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.editarea.image.plantuml.PlantUmlImageEditArea;
import specman.metatag.generic.GenericMetaTag;
import specman.metatag.generic.config.MetaTagConfig;
import specman.metatag.generic.config.MetaTagConfigListener;
import specman.metatag.generic.config.MetaTagConfigsEditor;
import specman.undo.manager.SpecmanUndoManager;
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
import java.util.function.Predicate;

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
	void addPlantUmlImage();
	/** Asks the user for a file with the source of a new Classman diagram, or to start with an empty diagram. */
	void addClassmanImage();
	/** Asks the user for a file and replaces the source of the diagram by its content. */
	void importPlantUmlSource(PlantUmlImageEditArea diagram);
	/** Asks the user for a file and replaces the source of the diagram by its content. */
	void importClassmanSource(ClassmanImageEditArea diagram);
	void undoLastEditAndForget();
	SpecmanUndoManager getUndoManager();
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
  EditContainer getIntro();
  EditContainer getOutro();
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
  void pasteClassmanInto(TextEditArea initiatingTextArea);
  void moveBranchSequenceLeftADBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment);
  void moveBranchSequenceRightADBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment);
  DragMouseAdapter createDragMouseAdapter();
  ChangeSet changeset();
  void addMetaTagConfigListener(MetaTagConfigListener listener);
  void removeMetaTagConfigListener(MetaTagConfigListener listener);
  List<MetaTagConfig> metaTagConfigs();
  MetaTagConfig findMetaTagConfig(String name);
  Set<String> queryMetaTagConfigsInUse();
  /** Opens the editor for the meta tag configurations, in a dialog of its own.
   * @return the editor, which the dialog shows */
  MetaTagConfigsEditor openMetaTagConfigs();
  /** Asks the user for a meta tag configuration, in a dialog of its own: a new one or an edited one.
   * @param existing the configuration to edit, null to create a new one
   * @param nameEditable whether the name may be changed - not if tags at steps or diagrams refer to it
   * @param isNameTaken tells whether a name is already used by another configuration
   * @return the configuration which the user confirmed, null if the user cancelled */
  MetaTagConfig editMetaTagConfig(MetaTagConfig existing, boolean nameEditable, Predicate<String> isNameTaken);
  void showToast(String message);
  /** Follows the target of a hyperlink in a text, e.g. a Specman link, a web address or a mail address. */
  void followHyperlink(String target);
}
