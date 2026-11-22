package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.RelationType_V001;
import classman.model.v001.TaggedValueModel_V001;
import classman.model.v001.change.ElementTextChanger_V001;
import classman.model.v001.change.ElementDeleter_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import classman.render.Scene;
import classman.render.DiagramStyle;
import classman.render.SceneAnchorLine;
import classman.render.SceneConnectionLine;
import classman.render.SceneRelationLine;
import classman.render.SceneElementArea;
import classman.render.SceneTextArea;
import classman.render.TextKind;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The operations of the diagram editor on the diagram, without any window. Each operation writes the new source
 * and reports it to the host. The host keeps the history: undo and redo are forwarded to it, but only for the sources
 * which this editing reported itself, so they never reach back to what happened before the editor was opened. */
public class DiagramEditing {
  private static final double DEFAULT_LINE_TOLERANCE = 4;
  private final EditorHost host;
  private final ClassDiagramSerializer_V001 serializer = new ClassDiagramSerializer_V001();
  private final ElementDeleter_V001 deleter = new ElementDeleter_V001();
  private final ElementTextChanger_V001 textChanger = new ElementTextChanger_V001();
  private final RouteEditing routeEditing = new RouteEditing();
  private final ElementIds ids = new ElementIds(new SecureRandom());
  private ClassDiagramModel_V001 model;
  private AttributeEditing attributeEditing;
  private TaggedValueEditing taggedValueEditing;
  private PendingStereotype pendingStereotype;
  private AbstractBoxModel_V001 pendingBox;
  private String source;
  private Scene scene;
  private AbstractDiagramElementModel_V001 selected;
  private Drag drag;
  private ConnectionDrawing drawing;
  private Tool armedTool = Tool.NONE;
  private int undoableSteps;
  private int redoableSteps;

  /** @throws ClassDiagramParseException if the source has a syntax error
   * @throws IllegalArgumentException if the diagram refers to an element which it doesn't have */
  public DiagramEditing(String source, EditorHost host) throws ClassDiagramParseException {
    this.host = host;
    load(source);
  }

  private void load(String newSource) throws ClassDiagramParseException {
    model = new ClassDiagramParser_V001().parse(newSource);
    attributeEditing = new AttributeEditing(model);
    taggedValueEditing = new TaggedValueEditing();
    pendingStereotype = new PendingStereotype();
    pendingBox = null;
    scene = host.renderer().layout(model);
    source = newSource;
    selected = null;
    drag = null;
    drawing = null;
    armedTool = Tool.NONE;
  }

  public String source() { return source; }

  /** @return the picture of the diagram as it is now, with the line of the selected relation or anchor drawn thicker,
   * and the line of the relation which a connection which is drawn would end at */
  public Scene scene() {
    Scene shown = scene;
    SceneConnectionLine selectedLine = selectedLine();
    if (selectedLine != null) {
      shown = shown.withStrokeWidth(selectedLine, DiagramStyle.SELECTED_LINE_WIDTH);
    }
    SceneConnectionLine targetLine = drawing != null ? drawing.targetLine() : null;
    if (targetLine != null && targetLine != selectedLine) {
      shown = shown.withStrokeWidth(targetLine, DiagramStyle.SELECTED_LINE_WIDTH);
    }
    return shown;
  }

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

  /** Selects the box at the point of the diagram, or else the relation whose line is at most the tolerance away, or
   * nothing if there is none. A box wins over a line, so that a click near the border of a box selects the box. The
   * handles and the placeholders of a selected relation are above everything: a point on one of them keeps the
   * relation selected. The dashed line of a note, its anchor, is selected like the line of a relation. A point at the left or right side of a class, where its width is changed, selects the class,
   * also if the point is a little outside of it. */
  public void selectAt(Point2D point, double tolerance) {
    if (handleIndexAt(point, tolerance) >= 0 || selectedPlaceholderAt(point) != null) {
      return;
    }
    ResizeTarget edge = resizeTargetAt(point, tolerance);
    if (edge != null) {
      selected = edge.box;
      return;
    }
    SceneElementArea hit = scene.elementAreaAt(point);
    if (hit != null) {
      selected = hit.element;
      return;
    }
    SceneConnectionLine line = scene.connectionLineAt(point, tolerance);
    if (line instanceof SceneAnchorLine anchorLine) {
      selected = new AnchorSelection(anchorLine.note);
    }
    else {
      selected = line != null ? ((SceneRelationLine) line).relation : null;
    }
  }

  /** Selects the box at the point of the diagram, or else the relation whose line is {@value #DEFAULT_LINE_TOLERANCE}
   * units away at most, or nothing if there is none. */
  public void selectAt(Point2D point) {
    selectAt(point, DEFAULT_LINE_TOLERANCE);
  }

  /** @return the index in {@link #selectedHandles()} of the handle which is at most the tolerance away from the
   * point, the closest one if there are several, -1 if there is none */
  private int handleIndexAt(Point2D point, double tolerance) {
    List<Point2D> handles = selectedHandles();
    int closest = -1;
    double closestDistance = tolerance;
    for (int i = 0; i < handles.size(); i++) {
      double distance = handles.get(i).distance(point);
      if (distance <= closestDistance) {
        closest = i;
        closestDistance = distance;
      }
    }
    return closest;
  }

  /** Deletes the waypoint of the selected relation or anchor at the point, if there is one: the handles at the ends of a
   * relation are no waypoints and a relation without a route has none. Without its last waypoint a relation has no
   * route any more. Changing a route is never marked as a change, so this is the same with and without change
   * tracking. One operation.
   * @return true if a waypoint was deleted */
  public boolean deleteWaypointAt(Point2D point, double tolerance) {
    int handle = handleIndexAt(point, tolerance);
    if (selected instanceof AnchorSelection anchor) {
      boolean isAnchorWaypoint = handle > 0 && handle <= anchor.note.attachWaypoints.size();
      if (isAnchorWaypoint) {
        routeEditing.deleteWaypoint(anchor.note, handle - 1);
        publish();
      }
      return isAnchorWaypoint;
    }
    boolean isWaypoint = handle > 0 && handle < selectedHandles().size() - 1;
    if (!isWaypoint || !(selected instanceof AbstractRelationModel_V001 relation) || relation.route == null) {
      return false;
    }
    routeEditing.deleteWaypoint(relation, handle - 1);
    publish();
    return true;
  }

  /** Puts a new waypoint on the line of the selected relation or anchor, at the point of the line which is closest to the given
   * point, if that is at most the tolerance away. Nothing happens on a handle, on a box, and on a text of the relation,
   * where a press means something else. A relation without a route gets one which runs along its line as it is now.
   * Not marked as a change, one operation.
   * @return true if a waypoint was added */
  public boolean insertWaypointAt(Point2D point, double tolerance) {
    SceneConnectionLine line = selectedLine();
    if (line == null || line.distanceTo(point) > tolerance || handleIndexAt(point, tolerance) >= 0
        || scene.elementAreaAt(point) != null || scene.textAreaAt(point) != null
        || selectedPlaceholderAt(point) != null) {
      return false;
    }
    int segment = line.nearestSegment(point);
    Point2D onLine = line.nearestPointOnSegment(segment, point);
    if (selected instanceof AnchorSelection anchor) {
      routeEditing.insertWaypoint(anchor.note, segment, onLine);
    }
    else {
      AbstractRelationModel_V001 relation = ((SceneRelationLine) line).relation;
      routeEditing.insertWaypoint(relation, line.points(), segment, onLine, classArea(relation.fromId),
        classArea(relation.toId));
    }
    publish();
    return true;
  }

  private Rectangle2D classArea(String classId) {
    for (ClassModel_V001 box : model.classes()) {
      if (classId.equals(box.id)) {
        return scene.elementAreaOf(box).area;
      }
    }
    throw new IllegalStateException("The diagram has no class with the ID " + classId);
  }

  public void deselect() { selected = null; }

  public boolean hasSelection() { return selected != null; }

  /** @return the selected box if it is a class of the diagram (not an association class), null if something else or
   * nothing is selected */
  public ClassModel_V001 selectedClass() {
    return selected instanceof ClassModel_V001 box && model.classes().contains(box) ? box : null;
  }

  /** @return the selected box if it is a note, null if something else or nothing is selected */
  public NoteModel_V001 selectedNote() {
    return selected instanceof NoteModel_V001 note ? note : null;
  }

  /** @return where the selected box is drawn, null if nothing is selected */
  public Rectangle2D selectedArea() {
    SceneElementArea selectedArea = selected != null ? scene.elementAreaOf(selected) : null;
    return selectedArea != null ? selectedArea.area : null;
  }

  /** @return the points of the selected relation or anchor where its line starts, bends and ends, empty if none is
   * selected. An anchor which is attached to something else than a class has no handle at its end. */
  public List<Point2D> selectedHandles() {
    SceneConnectionLine selectedLine = selectedLine();
    if (selectedLine == null) {
      return List.of();
    }
    List<Point2D> points = selectedLine.points();
    if (selected instanceof AnchorSelection anchor && !isAttachedToClass(anchor.note)) {
      return points.subList(0, points.size() - 1);
    }
    return points;
  }

  /** A note which is attached to an attribute or a relation has no end of its own to move: it ends where the layout
   * puts it. */
  private boolean isAttachedToClass(NoteModel_V001 note) {
    return model.classes().stream().anyMatch(box -> note.attachedToId.equals(box.id));
  }

  /** @return where the line is drawn of the selected relation or anchor, null if none is selected */
  private SceneConnectionLine selectedLine() {
    if (selected instanceof AnchorSelection anchor) {
      return scene.anchorLineOf(anchor.note);
    }
    return selected != null ? scene.relationLineOf(selected) : null;
  }

  /** @return what the canvas shows to mark the selection: a frame around the selected box and around the class which
   * the end of a relation is going to be hung onto or a relation which is drawn ends at, the line of that relation, the
   * handles and the places for missing texts of a selected relation, which are filled with the pale color of the change set while changes are tracked */
  SelectionMarks selectionMarks() {
    List<Rectangle2D> frames = new ArrayList<>();
    if (selectedArea() != null) {
      frames.add(selectedArea());
    }
    if (connectionTargetArea() != null) {
      frames.add(connectionTargetArea());
    }
    return new SelectionMarks(frames, selectedHandles(), selectedPlaceholders(), trackedFieldColor(),
      drawing != null ? drawing.line() : null, specialSelectionColor());
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

  /** @return the text at the point if the user can change it: it belongs to the selected box or to a relation. Where
   * the selected relation has no text yet, its placeholder at the point counts: the user creates the text there. */
  public SceneTextArea editableTextAt(Point2D point) {
    SceneTextArea text = scene.textAreaAt(point);
    boolean editable = text != null && (text.element == selected || text.element instanceof AbstractRelationModel_V001);
    if (editable) {
      return text;
    }
    return text == null ? selectedPlaceholderAt(point) : null;
  }

  private SceneTextArea selectedPlaceholderAt(Point2D point) {
    SceneTextArea placeholder = scene.placeholderAt(point);
    return placeholder != null && placeholder.element == selected ? placeholder : null;
  }

  /** @return where the selected relation could get the texts which it doesn't have yet, empty if no relation is
   * selected or it has all */
  public List<Rectangle2D> selectedPlaceholders() {
    List<Rectangle2D> areas = new ArrayList<>();
    for (SceneTextArea placeholder : scene.placeholderAreas) {
      if (placeholder.element == selected) {
        areas.add(placeholder.area);
      }
    }
    return areas;
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
      case STEREOTYPE -> stereotypeOf((ClassModel_V001) text.element);
      case NOTE_TEXT -> ((NoteModel_V001) text.element).text;
      case RELATION_LABEL -> orEmpty(((AbstractRelationModel_V001) text.element).label);
      case FROM_MULTIPLICITY -> orEmpty(((AssociationRelationModel_V001) text.element).fromMultiplicity);
      case TO_MULTIPLICITY -> orEmpty(((AssociationRelationModel_V001) text.element).toMultiplicity);
      case ATTRIBUTE_LINE -> attributeEditing.textOf(text);
      case TAGGED_VALUE -> taggedValueEditing.textOf(text);
    };
  }

  /** A stereotype which a change set has removed still stands in the header, struck through, but counts as missing:
   * typing it again takes the removal back, and another text changes the stereotype. */
  private static String stereotypeOf(ClassModel_V001 box) {
    boolean isRemoved = box.stereotypeElementChange != null && box.stereotypeElementChange.type == ChangeType_V001.REMOVED;
    return isRemoved ? "" : orEmpty(box.stereotype);
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

  /** @return false if the text can't have that content: a multiplicity is free text, but only one line, a stereotype
   * is one line without a closing angle bracket, and an attribute line needs a name. */
  public boolean accepts(SceneTextArea text, String newText) {
    return switch (text.kind) {
      case FROM_MULTIPLICITY, TO_MULTIPLICITY -> !newText.contains("\n");
      case STEREOTYPE -> !newText.contains("\n") && !newText.contains(">");
      case ATTRIBUTE_LINE -> attributeEditing.accepts(text, newText, host.trackedChangeSet());
      case TAGGED_VALUE -> taggedValueEditing.accepts(newText);
      default -> true;
    };
  }

  /** Gives the text a new content. While changes are tracked, the change set gets the mark for the change. The text it
   * has already is no operation, and a blank line for a new attribute or stereotype which stays empty goes away. An
   * empty text removes a label, multiplicity or stereotype, or deletes an attribute.
   * @throws IllegalArgumentException if {@link #accepts} says no */
  public void setText(SceneTextArea text, String newText) {
    if (textOf(text).equals(newText)) {
      discardNewPart();
      return;
    }
    if (!accepts(text, newText)) {
      throw new IllegalArgumentException("The text can't have the content: " + newText);
    }
    String changeSet = host.trackedChangeSet();
    if (text.kind == TextKind.ATTRIBUTE_LINE) {
      if (attributeEditing.setText(text, newText, changeSet)) {
        publish();
      }
      else {
        scene = host.renderer().layout(model);
      }
      return;
    }
    if (text.kind == TextKind.TAGGED_VALUE) {
      if (taggedValueEditing.setText(text, newText, changeSet)) {
        publish();
      }
      else {
        scene = host.renderer().layout(model);
      }
      return;
    }
    switch (text.kind) {
      case CLASS_NAME -> {
        ClassModel_V001 box = (ClassModel_V001) text.element;
        confirmPendingBox(box);
        if (changeSet != null) {
          textChanger.setClassName(box, newText, changeSet);
        }
        else {
          textChanger.setClassName(box, newText);
        }
      }
      case STEREOTYPE -> {
        pendingStereotype.discard();
        ClassModel_V001 box = (ClassModel_V001) text.element;
        textChanger.setStereotype(box, newText, changeSet);
      }
      case NOTE_TEXT -> {
        NoteModel_V001 note = (NoteModel_V001) text.element;
        confirmPendingBox(note);
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

  /** @return true if the text is the last attribute line of its class, or the blank line of a class without
   * attributes: a new attribute can be typed right after it */
  public boolean isLastAttributeLine(SceneTextArea text) {
    return text.kind == TextKind.ATTRIBUTE_LINE && attributeEditing.isLastLine(text);
  }

  /** Puts a blank line at the end of the class, for a new attribute to be typed into. It is part of the picture, but
   * not of the diagram, until it is given a text with {@link #setText}.
   * @return the area of the blank line */
  public SceneTextArea newAttributeAtEnd(ClassModel_V001 box) {
    AttributeModel_V001 placeholder = attributeEditing.beginAtEnd(box);
    scene = host.renderer().layout(model);
    return scene.textAreas.stream().filter(text -> text.attribute == placeholder).findFirst().orElseThrow();
  }

  /** @return true if the class can get parts: it is no association class and no change set has removed it */
  public boolean canAddPartsTo(ClassModel_V001 box) {
    boolean isRemoved = box.change != null && box.change.type == ChangeType_V001.REMOVED;
    return !isRemoved && model.classes().contains(box);
  }

  /** Puts a blank row after the last tagged value of the class, for a new tagged value to be typed into. It is part
   * of the picture, but not of the diagram, until it is given a text with {@link #setText}.
   * @return the area of the blank row
   * @throws IllegalArgumentException if the class can't get parts */
  public SceneTextArea newTaggedValue(ClassModel_V001 box) {
    if (!canAddPartsTo(box)) {
      throw new IllegalArgumentException("The class can't get a tagged value: " + box.name);
    }
    TaggedValueModel_V001 pending = taggedValueEditing.beginAtEnd(box);
    scene = host.renderer().layout(model);
    return scene.textAreas.stream().filter(text -> text.taggedValue == pending).findFirst().orElseThrow();
  }

  /** @return true if the class has no stereotype, or only one which a change set has removed */
  public boolean canAddStereotypeTo(ClassModel_V001 box) {
    return canAddPartsTo(box) && stereotypeOf(box).isEmpty();
  }

  /** Puts a blank stereotype row into the class, for the stereotype to be typed into. It is part of the picture, but
   * not of the diagram, until it is given a text with {@link #setText}. A stereotype which a change set has removed is
   * still there as a struck row, which is the area to type into.
   * @return the area of the row
   * @throws IllegalArgumentException if the class has a stereotype */
  public SceneTextArea newStereotype(ClassModel_V001 box) {
    if (!canAddStereotypeTo(box)) {
      throw new IllegalArgumentException("The class can't get a stereotype: " + box.name);
    }
    if (box.stereotype == null) {
      pendingStereotype.begin(box);
      scene = host.renderer().layout(model);
    }
    return scene.textAreas.stream().filter(text -> text.element == box && text.kind == TextKind.STEREOTYPE)
      .findFirst().orElseThrow();
  }

  /** Takes away the blank line or row for a new attribute, stereotype or tagged value, or the class without a name,
   * if it is still there. */
  public void discardNewPart() {
    if (discardPendingParts()) {
      scene = host.renderer().layout(model);
    }
  }

  private boolean discardPendingParts() {
    boolean attributeGone = attributeEditing.discardPending();
    boolean stereotypeGone = pendingStereotype.discard();
    boolean taggedValueGone = taggedValueEditing.discardPending();
    boolean boxGone = discardPendingBox();
    return attributeGone || stereotypeGone || taggedValueGone || boxGone;
  }

  /** Starts to drag, from the point where the mouse was pressed: a handle of the selected relation if the point is at
   * most the tolerance away from one, a side of a class if the point is at it, otherwise the selected box, if there is
   * one. Nothing is dragged if the user only clicks. */
  public void startDrag(Point2D at, double tolerance) {
    int handle = handleIndexAt(at, tolerance);
    ResizeTarget edge = resizeTargetAt(at, tolerance);
    if (handle >= 0) {
      drag = handleDrag(handle, at);
    }
    else if (edge != null) {
      drag = new BoxResize(edge.box, edge.side, scene, minimumWidthOf(edge.box), positionFreezer());
    }
    else {
      drag = selected instanceof AbstractBoxModel_V001 box && scene.elementAreaOf(box) != null
        ? new BoxDrag(box, at, scene, positionFreezer())
        : null;
    }
  }

  private PositionFreezer positionFreezer() {
    return new PositionFreezer(scene, model.boxes(), associationClasses());
  }

  /** @return the width below which the box can't be made: what the layout makes of it with the least width */
  private double minimumWidthOf(AbstractBoxModel_V001 box) {
    Integer widthBefore = box.width;
    box.width = 1;
    double minimum = host.renderer().layout(model).elementAreaOf(box).area.getWidth();
    box.width = widthBefore;
    return minimum;
  }

  /** @return the side of a class or note of the diagram which the point is at, where dragging changes the width of
   * the box: at most the tolerance away from the left or right side, along its whole height. The box which is drawn on
   * top wins. Null if the point is at none. */
  ResizeTarget resizeTargetAt(Point2D point, double tolerance) {
    List<AbstractBoxModel_V001> boxes = model.boxes();
    for (int i = boxes.size() - 1; i >= 0; i--) {
      AbstractBoxModel_V001 box = boxes.get(i);
      SceneElementArea boxArea = box == pendingBox ? null : scene.elementAreaOf(box);
      if (boxArea == null || point.getY() < boxArea.area.getMinY() || point.getY() > boxArea.area.getMaxY()) {
        continue;
      }
      double toLeft = Math.abs(point.getX() - boxArea.area.getMinX());
      double toRight = Math.abs(point.getX() - boxArea.area.getMaxX());
      if (Math.min(toLeft, toRight) <= tolerance) {
        return new ResizeTarget(box, toLeft < toRight ? ResizeSide.LEFT : ResizeSide.RIGHT);
      }
    }
    return null;
  }

  /** @return true if the box has a width of its own, which the user has set, and not the one of its content */
  public boolean hasFixedWidth(AbstractBoxModel_V001 box) {
    return box.width != null;
  }

  /** Takes the width of its own away from the box, so it is as wide as its content again. One operation. */
  public void resetWidth(AbstractBoxModel_V001 box) {
    if (box.width != null) {
      box.width = null;
      publish();
    }
  }

  /** Starts to drag the selected box, if there is one, from the point where the mouse was pressed. */
  public void startDrag(Point2D at) {
    startDrag(at, DEFAULT_LINE_TOLERANCE);
  }

  private Drag handleDrag(int handle, Point2D at) {
    if (selected instanceof AnchorSelection anchor) {
      NoteModel_V001 note = anchor.note;
      if (note.id == null) {
        note.id = newId();
      }
      return new AnchorHandleDrag(routeEditing, note, scene.elementAreaOf(note).area, handle,
        selectedLine().points(), at, classesAnAnchorMayHangOnto(note));
    }
    SceneRelationLine line = scene.relationLineOf(selected);
    AbstractRelationModel_V001 relation = line.relation;
    boolean isEnd = handle == 0 || handle == line.points().size() - 1;
    return new HandleDrag(routeEditing, relation, line.points(), handle, at, classArea(relation.fromId),
      classArea(relation.toId), isEnd ? classesAnEndMayHangOnto(relation, handle == 0) : Map.of());
  }

  /** The classes by ID in the order of drawing which an end of the relation may be hung onto: all of them, but while
   * changes are tracked only the class it hangs on, because hanging a relation onto another class is no change which
   * the format can mark. The user deletes the relation and adds a new one then. */
  private Map<String, Rectangle2D> classesAnEndMayHangOnto(AbstractRelationModel_V001 relation, boolean start) {
    Map<String, Rectangle2D> classes = new LinkedHashMap<>();
    String ownClassId = start ? relation.fromId : relation.toId;
    for (ClassModel_V001 box : model.classes()) {
      boolean allowed = host.trackedChangeSet() == null || ownClassId.equals(box.id);
      if (box.id != null && allowed) {
        classes.put(box.id, scene.elementAreaOf(box).area);
      }
    }
    return classes;
  }

  /** The classes by ID in the order of drawing which the end of the anchor of the note may be hung onto: all of them,
   * but while changes are tracked only the class it hangs on, unless the note was added by a change set. */
  private Map<String, Rectangle2D> classesAnAnchorMayHangOnto(NoteModel_V001 note) {
    Map<String, Rectangle2D> classes = new LinkedHashMap<>();
    for (ClassModel_V001 box : model.classes()) {
      boolean allowed = canChangeAnchor(note) || note.attachedToId.equals(box.id);
      if (box.id != null && allowed) {
        classes.put(box.id, scene.elementAreaOf(box).area);
      }
    }
    return classes;
  }

  /** @return where the class is which the end of a relation which is being dragged is going to be hung onto, null if
   * it stays on its own class or nothing is dragged */
  public Rectangle2D connectionTargetArea() {
    if (drawing != null) {
      return drawing.targetFrame();
    }
    return drag != null ? drag.connectionTarget() : null;
  }

  /** Shows the dragged thing where the mouse is. A handle of a relation snaps to the line of its neighbors if it comes
   * at most the tolerance close, so that the line is vertical or horizontal. Nothing is reported to the host until the
   * drag ends. */
  public void dragTo(Point2D at, double snapTolerance) {
    if (drawing != null) {
      drawing.to(at, snapTolerance);
      return;
    }
    if (drag != null) {
      drag.to(at, snapTolerance);
      scene = host.renderer().layout(model);
    }
  }

  /** Shows the dragged thing where the mouse is, without snapping. */
  public void dragTo(Point2D at) {
    dragTo(at, 0);
  }

  /** Leaves the dragged thing where it is. If it has moved, that is one operation. */
  public void endDrag() {
    if (drawing != null) {
      endDrawing();
      return;
    }
    Drag ended = drag;
    drag = null;
    if (ended != null && ended.hasMoved()) {
      publish();
    }
  }

  /** Puts the dragged thing back where it was before the drag.
   * @return false if there was no drag which had moved something */
  public boolean cancelDrag() {
    if (drawing != null) {
      drawing = null;
      return true;
    }
    Drag cancelled = drag;
    drag = null;
    if (cancelled == null || !cancelled.hasMoved()) {
      return false;
    }
    cancelled.cancel();
    scene = host.renderer().layout(model);
    return true;
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

  public boolean isRelationToolArmed() { return armedTool == Tool.ASSOCIATION; }

  public boolean isAnchorToolArmed() { return armedTool == Tool.ANCHOR; }

  /** @return true if a tool of the palette is armed, which takes the presses on the diagram for itself */
  public boolean isToolArmed() { return armedTool != Tool.NONE; }

  /** Arms the tool which draws an association from one class to another, or disarms it if it is armed. */
  public void toggleRelationTool() {
    toggle(Tool.ASSOCIATION);
  }

  /** Arms the tool which draws the anchor of a note to a class, or disarms it if it is armed. */
  public void toggleAnchorTool() {
    toggle(Tool.ANCHOR);
  }

  private void toggle(Tool tool) {
    armedTool = armedTool == tool ? Tool.NONE : tool;
    drawing = null;
  }

  public void disarmTool() {
    armedTool = Tool.NONE;
    drawing = null;
  }

  /** Starts to draw with the armed tool at the box which the press is on or at most the tolerance away from: a class
   * for an association, a note for an anchor. Nothing happens if there is none. The connection is created where the
   * mouse is released on a class, see {@link #endDrag}. */
  public void startDrawing(Point2D at, double tolerance) {
    ConnectionTargets targets = new ConnectionTargets(model, scene);
    drawing = null;
    if (armedTool == Tool.ASSOCIATION) {
      ClassTarget from = targets.closestClass(at, tolerance);
      if (from != null) {
        drawing = new ConnectionDrawing(from.box, from.frame(), targets::closestClass, at);
      }
    }
    else if (armedTool == Tool.ANCHOR) {
      Map<NoteModel_V001, Rectangle2D> notes = new LinkedHashMap<>();
      for (NoteModel_V001 note : model.notes()) {
        if (canChangeAnchor(note)) {
          notes.put(note, scene.elementAreaOf(note).area);
        }
      }
      NoteModel_V001 from = ClassPicker.closest(notes, at, tolerance);
      if (from != null) {
        drawing = new ConnectionDrawing(from, notes.get(from), targets::closestForAnchor, at);
      }
    }
  }

  /** While changes are tracked, only a note which a change set has added gets a new anchor or loses its anchor: for
   * another note this changes what it is attached to, which the format has no mark for, like hanging a relation onto
   * another class. */
  private boolean canChangeAnchor(NoteModel_V001 note) {
    return host.trackedChangeSet() == null || note.change != null && note.change.type == ChangeType_V001.ADDED;
  }

  /** Creates what was drawn, if the mouse was released on a class, and leaves the tool. If it was released on none,
   * nothing is created and the tool stays armed, so the user can try again. */
  private void endDrawing() {
    ConnectionDrawing finished = drawing;
    drawing = null;
    if (finished.target() == null) {
      return;
    }
    if (armedTool == Tool.ASSOCIATION) {
      createAssociation(finished);
    }
    else {
      anchorNote(finished);
    }
  }

  private void createAssociation(ConnectionDrawing finished) {
    ClassModel_V001 from = (ClassModel_V001) finished.from();
    String fromId = new ClassTarget(from, scene.elementAreaOf(from).area).ensureId(this::newId);
    AssociationRelationModel_V001 relation = new AssociationRelationModel_V001(RelationType_V001.ASSOCIATION,
      fromId, finished.target().ensureId(this::newId));
    relation.directed = true;
    relation.id = newId();
    markAsAdded(relation);
    model.elements.add(relation);
    selected = relation;
    armedTool = Tool.NONE;
    publish();
  }

  /** Attaches the note to the class, attribute or relation which the mouse was released on, so the dashed line of the
   * note leads to it. A class or attribute which has no ID gets one. The note is selected. Nothing happens, and no
   * operation is made, if it is attached to that target already. */
  private void anchorNote(ConnectionDrawing finished) {
    NoteModel_V001 note = (NoteModel_V001) finished.from();
    String targetId = finished.target().ensureId(this::newId);
    armedTool = Tool.NONE;
    selected = note;
    if (targetId.equals(note.attachedToId)) {
      return;
    }
    note.attachedToId = targetId;
    routeEditing.clearRoute(note);
    publish();
  }

  private String newId() {
    return ids.newIdNotIn(usedIds());
  }

  /** Deletes the selected box and what hangs on it, and selects nothing afterwards. While changes are tracked, what
   * the change set has added is removed and the rest is marked as removed, so that the deletion can be rejected. */
  public void deleteSelected() {
    if (selected == null) {
      return;
    }
    if (selected instanceof AnchorSelection anchor) {
      deleteAnchor(anchor.note);
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

  /** Takes the anchor away from the note, so it hangs on nothing. While changes are tracked this is only done for a note
   * which a change set has added: taking the anchor from another note is a change which the format has no mark for.
   * Nothing is selected afterwards. */
  private void deleteAnchor(NoteModel_V001 note) {
    if (!canChangeAnchor(note)) {
      return;
    }
    note.attachedToId = null;
    routeEditing.clearRoute(note);
    selected = null;
    publish();
  }

  /** Puts a class without a name into the picture, its top left corner at the given point of the diagram, with a new
   * ID, selected, for the name to be typed into. It is wide enough for the field of the name while that is typed; with
   * the name it gets no width of its own, so it is as wide as its content. The class is not part of the diagram until its name is given with
   * {@link #setText}: that is one operation. It goes away again with the empty name, or if it is dropped.
   * @return the area of the name */
  public SceneTextArea newClass(int x, int y) {
    return putPendingBox(new ClassModel_V001(""), x, y, TextKind.CLASS_NAME);
  }

  /** Puts a note without a text into the picture, in the same way as {@link #newClass} does with a class: the text is
   * typed into it, and it is part of the diagram only when it has a text.
   * @return the area of the text */
  public SceneTextArea newNote(int x, int y) {
    return putPendingBox(new NoteModel_V001(""), x, y, TextKind.NOTE_TEXT);
  }

  private SceneTextArea putPendingBox(AbstractBoxModel_V001 box, int x, int y, TextKind textKind) {
    discardPendingParts();
    box.id = newId();
    box.position = new PositionModel_V001(x, y);
    box.width = DiagramStyle.NEW_BOX_WIDTH;
    markAsAdded(box);
    model.elements.add(box);
    selected = box;
    pendingBox = box;
    scene = host.renderer().layout(model);
    return scene.textAreas.stream().filter(text -> text.element == box && text.kind == textKind)
      .findFirst().orElseThrow();
  }

  /** Makes the box which was put into the picture a box of the diagram: it keeps no width of its own. */
  private void confirmPendingBox(AbstractBoxModel_V001 box) {
    if (box == pendingBox) {
      pendingBox = null;
      box.width = null;
    }
  }

  private boolean discardPendingBox() {
    if (pendingBox == null) {
      return false;
    }
    model.elements.remove(pendingBox);
    if (selected == pendingBox) {
      selected = null;
    }
    pendingBox = null;
    return true;
  }

  /** The IDs of the elements of the diagram including its attributes and association classes, which notes attach to. */
  private Set<String> usedIds() {
    Set<String> used = new HashSet<>();
    for (AbstractDiagramElementModel_V001 element : model.elements) {
      addIds(used, element);
      if (element instanceof AssociationRelationModel_V001 association && association.assocClass != null) {
        addIds(used, association.assocClass);
      }
    }
    return used;
  }

  private void addIds(Set<String> used, AbstractDiagramElementModel_V001 element) {
    if (element.id != null) {
      used.add(element.id);
    }
    if (element instanceof ClassModel_V001 box) {
      for (AttributeModel_V001 attribute : box.attributes) {
        if (attribute.id != null) {
          used.add(attribute.id);
        }
      }
    }
  }

  private void markAsAdded(AbstractDiagramElementModel_V001 element) {
    String changeSet = host.trackedChangeSet();
    if (changeSet != null) {
      element.change = new ChangeModel_V001(ChangeType_V001.ADDED, changeSet);
    }
  }

  private void publish() {
    discardPendingParts();
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
