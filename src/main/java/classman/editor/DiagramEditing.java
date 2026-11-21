package classman.editor;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.Multiplicity_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.change.ElementTextChanger_V001;
import classman.model.v001.change.ElementDeleter_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import classman.render.Scene;
import classman.render.SceneElementArea;
import classman.render.SceneTextArea;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The operations of the diagram editor on the diagram, without any window. Each operation writes the new source
 * and reports it to the host. The host keeps the history: undo and redo are forwarded to it, but only for the sources
 * which this editing reported itself, so they never reach back to what happened before the editor was opened. */
public class DiagramEditing {
  private final EditorHost host;
  private final EditorTexts texts;
  private final ClassDiagramSerializer_V001 serializer = new ClassDiagramSerializer_V001();
  private final ElementDeleter_V001 deleter = new ElementDeleter_V001();
  private final ElementTextChanger_V001 textChanger = new ElementTextChanger_V001();
  private final ElementIds ids = new ElementIds(new SecureRandom());
  private ClassDiagramModel_V001 model;
  private String source;
  private Scene scene;
  private AbstractDiagramElementModel_V001 selected;
  private BoxDrag drag;
  private int undoableSteps;
  private int redoableSteps;

  /** @throws ClassDiagramParseException if the source has a syntax error
   * @throws IllegalArgumentException if the diagram refers to an element which it doesn't have */
  public DiagramEditing(String source, EditorHost host, EditorTexts texts) throws ClassDiagramParseException {
    this.host = host;
    this.texts = texts;
    load(source);
  }

  private void load(String newSource) throws ClassDiagramParseException {
    model = new ClassDiagramParser_V001().parse(newSource);
    scene = host.renderer().layout(model);
    source = newSource;
    selected = null;
    drag = null;
  }

  public String source() { return source; }

  /** @return the picture of the diagram as it is now */
  public Scene scene() { return scene; }

  public boolean canUndo() { return undoableSteps > 0; }

  public boolean canRedo() { return redoableSteps > 0; }

  /** Takes back the last operation, if there is one of this editing. */
  public void undo() {
    if (canUndo()) {
      host.undo();
      undoableSteps--;
      redoableSteps++;
      reloadFromHost();
    }
  }

  /** Does again the operation which was taken back last, if nothing was done since. */
  public void redo() {
    if (canRedo()) {
      host.redo();
      redoableSteps--;
      undoableSteps++;
      reloadFromHost();
    }
  }

  private void reloadFromHost() {
    try {
      load(host.source());
    }
    catch (ClassDiagramParseException e) {
      throw new IllegalStateException("The host holds a source which the editor wrote and can't read", e);
    }
  }

  /** Selects the box at the point of the diagram, or nothing if there is none. */
  public void selectAt(Point2D point) {
    SceneElementArea hit = scene.elementAreaAt(point);
    selected = hit != null ? hit.element : null;
  }

  public void deselect() { selected = null; }

  public boolean hasSelection() { return selected != null; }

  /** @return where the selected box is drawn, null if nothing is selected */
  public Rectangle2D selectedArea() {
    SceneElementArea selectedArea = selected != null ? scene.elementAreaOf(selected) : null;
    return selectedArea != null ? selectedArea.area : null;
  }

  /** @return the color for the frame around the selected box if the usual one can't be told from the change set which
   * marked the box, null otherwise */
  public Color specialSelectionColor() {
    if (selected == null || selected.change == null) {
      return null;
    }
    String color = host.renderer().selectionColorOf(selected.change.changeSet);
    return color != null ? Color.decode(color) : null;
  }

  /** @return the text at the point if the user can change it: it belongs to the selected box or to a relation */
  public SceneTextArea editableTextAt(Point2D point) {
    SceneTextArea text = scene.textAreaAt(point);
    boolean editable = text != null && (text.element == selected || text.element instanceof AbstractRelationModel_V001);
    return editable ? text : null;
  }

  /** @return the area of the box which the text belongs to, the area of the text itself if it belongs to a relation */
  public Rectangle2D boxOf(SceneTextArea text) {
    SceneElementArea box = scene.elementAreaOf(text.element);
    return box != null ? box.area : text.area;
  }

  /** @return the text which the area shows, empty if the relation doesn't have it */
  public String textOf(SceneTextArea text) {
    return switch (text.kind) {
      case CLASS_NAME -> ((ClassModel_V001) text.element).name;
      case NOTE_TEXT -> ((NoteModel_V001) text.element).text;
      case RELATION_LABEL -> orEmpty(((AbstractRelationModel_V001) text.element).label);
      case FROM_MULTIPLICITY -> orEmpty(((AssociationRelationModel_V001) text.element).fromMultiplicity);
      case TO_MULTIPLICITY -> orEmpty(((AssociationRelationModel_V001) text.element).toMultiplicity);
    };
  }

  private static String orEmpty(String text) {
    return text != null ? text : "";
  }

  /** @return the pale color of the change set which tracks changes, for the background of a text field, null if
   * changes are not tracked */
  public Color trackedFieldColor() {
    String changeSet = host.trackedChangeSet();
    return changeSet != null ? Color.decode(host.renderer().paleColorOf(changeSet)) : null;
  }

  /** @return false if the text can't have that content: a multiplicity must be a number, a range like 0..1 or 1..*,
   * or *, and while changes are tracked one which isn't new can't be removed, because the removal can't be marked. */
  public boolean accepts(SceneTextArea text, String newText) {
    return switch (text.kind) {
      case FROM_MULTIPLICITY -> acceptsMultiplicity(text, RelationEnd_V001.FROM, newText);
      case TO_MULTIPLICITY -> acceptsMultiplicity(text, RelationEnd_V001.TO, newText);
      default -> true;
    };
  }

  private boolean acceptsMultiplicity(SceneTextArea text, RelationEnd_V001 end, String newText) {
    if (!newText.isEmpty() && !Multiplicity_V001.isValid(newText)) {
      return false;
    }
    String changeSet = host.trackedChangeSet();
    return changeSet == null
      || textChanger.canSetMultiplicity((AssociationRelationModel_V001) text.element, end, newText, changeSet);
  }

  /** Gives the text a new content. While changes are tracked, the change set gets the mark for the change. The text it
   * has already is no operation. An empty text removes a label or multiplicity.
   * @throws IllegalArgumentException if {@link #accepts} says no */
  public void setText(SceneTextArea text, String newText) {
    if (textOf(text).equals(newText)) {
      return;
    }
    if (!accepts(text, newText)) {
      throw new IllegalArgumentException("The text can't have the content: " + newText);
    }
    String changeSet = host.trackedChangeSet();
    switch (text.kind) {
      case CLASS_NAME -> {
        ClassModel_V001 box = (ClassModel_V001) text.element;
        if (changeSet != null) {
          textChanger.setClassName(box, newText, changeSet);
        }
        else {
          textChanger.setClassName(box, newText);
        }
      }
      case NOTE_TEXT -> {
        NoteModel_V001 note = (NoteModel_V001) text.element;
        if (changeSet != null) {
          textChanger.setNoteText(note, newText, changeSet);
        }
        else {
          textChanger.setNoteText(note, newText);
        }
      }
      case RELATION_LABEL -> {
        AbstractRelationModel_V001 relation = (AbstractRelationModel_V001) text.element;
        if (changeSet != null) {
          textChanger.setLabel(relation, newText, changeSet);
        }
        else {
          textChanger.setLabel(relation, newText);
        }
      }
      case FROM_MULTIPLICITY -> setMultiplicity(text, RelationEnd_V001.FROM, newText, changeSet);
      case TO_MULTIPLICITY -> setMultiplicity(text, RelationEnd_V001.TO, newText, changeSet);
    }
    publish();
  }

  private void setMultiplicity(SceneTextArea text, RelationEnd_V001 end, String newText, String changeSet) {
    AssociationRelationModel_V001 association = (AssociationRelationModel_V001) text.element;
    if (changeSet != null) {
      textChanger.setMultiplicity(association, end, newText, changeSet);
    }
    else {
      textChanger.setMultiplicity(association, end, newText);
    }
  }

  /** Starts to drag the selected box, if there is one, from the point where the mouse was pressed. */
  public void startDrag(Point2D at) {
    drag = selected != null && scene.elementAreaOf(selected) != null
      ? new BoxDrag(selected, at, scene, placedBoxes(), associationClasses())
      : null;
  }

  /** Shows the dragged box where the mouse is. Nothing is reported to the host until the drag ends. */
  public void dragTo(Point2D at) {
    if (drag != null) {
      drag.to(at);
      scene = host.renderer().layout(model);
    }
  }

  /** Leaves the box where it is. If it has moved, that is one operation. */
  public void endDrag() {
    BoxDrag ended = drag;
    drag = null;
    if (ended != null && ended.hasMoved()) {
      publish();
    }
  }

  /** Puts the box back where it was before the drag.
   * @return false if there was no drag which had moved a box */
  public boolean cancelDrag() {
    BoxDrag cancelled = drag;
    drag = null;
    if (cancelled == null || !cancelled.hasMoved()) {
      return false;
    }
    cancelled.cancel();
    scene = host.renderer().layout(model);
    return true;
  }

  private List<AbstractDiagramElementModel_V001> placedBoxes() {
    List<AbstractDiagramElementModel_V001> boxes = new ArrayList<>(model.classes());
    boxes.addAll(model.notes());
    return boxes;
  }

  private List<ClassModel_V001> associationClasses() {
    List<ClassModel_V001> classes = new ArrayList<>();
    for (AbstractRelationModel_V001 relation : model.relations()) {
      if (relation instanceof AssociationRelationModel_V001 association && association.assocClass != null) {
        classes.add(association.assocClass);
      }
    }
    return classes;
  }

  /** Deletes the selected box and what hangs on it, and selects nothing afterwards. While changes are tracked, what
   * the change set has added is removed and the rest is marked as removed, so that the deletion can be rejected. */
  public void deleteSelected() {
    if (selected == null) {
      return;
    }
    String changeSet = host.trackedChangeSet();
    if (changeSet != null) {
      deleter.delete(model, selected, changeSet);
    }
    else {
      deleter.delete(model, selected);
    }
    publish();
    selected = null;
  }

  /** Adds a class with a made-up name and ID, its top left corner at the given point of the diagram. */
  public void addClass(int x, int y) {
    ClassModel_V001 box = new ClassModel_V001(newClassName());
    box.id = ids.newIdNotIn(usedIds());
    box.position = new PositionModel_V001(x, y);
    markAsAdded(box);
    model.elements.add(box);
    selected = box;
    publish();
  }

  private String newClassName() {
    Set<String> names = new HashSet<>();
    for (ClassModel_V001 box : model.classes()) {
      names.add(box.name);
    }
    String base = texts.get("class.new.name");
    String name = base;
    for (int number = 2; names.contains(name); number++) {
      name = base + number;
    }
    return name;
  }

  private Set<String> usedIds() {
    Set<String> used = new HashSet<>();
    for (AbstractDiagramElementModel_V001 element : model.elements) {
      if (element.id != null) {
        used.add(element.id);
      }
    }
    return used;
  }

  private void markAsAdded(AbstractDiagramElementModel_V001 element) {
    String changeSet = host.trackedChangeSet();
    if (changeSet != null) {
      element.change = new ChangeModel_V001(ChangeType_V001.ADDED, changeSet);
    }
  }

  private void publish() {
    String newSource = serializer.serialize(model);
    scene = host.renderer().layout(model);
    if (!newSource.equals(source)) {
      undoableSteps++;
      redoableSteps = 0;
    }
    source = newSource;
    host.sourceChanged(source);
  }
}
