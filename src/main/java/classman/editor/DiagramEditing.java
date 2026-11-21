package classman.editor;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import classman.render.Scene;
import classman.render.SceneElementArea;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

/** The operations of the diagram editor on the diagram, without any window. Each operation writes the new source
 * and reports it to the host. The host keeps the history: undo and redo are forwarded to it, but only for the sources
 * which this editing reported itself, so they never reach back to what happened before the editor was opened. */
public class DiagramEditing {
  private final EditorHost host;
  private final EditorTexts texts;
  private final ClassDiagramSerializer_V001 serializer = new ClassDiagramSerializer_V001();
  private final ElementIds ids = new ElementIds(new SecureRandom());
  private ClassDiagramModel_V001 model;
  private String source;
  private Scene scene;
  private AbstractDiagramElementModel_V001 selected;
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
