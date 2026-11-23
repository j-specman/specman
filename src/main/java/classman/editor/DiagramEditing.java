package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.AssocClassLinkModel_V001;
import classman.model.v001.AttachLineModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.ExtendsRelationModel_V001;
import classman.model.v001.ImplementsRelationModel_V001;
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
import classman.render.AttachTarget;
import classman.render.DiagramStyle;
import classman.render.SceneAttachLine;
import classman.render.SceneConnectionLine;
import classman.render.SceneRelationLine;
import classman.render.SceneElementArea;
import classman.render.SceneTextArea;
import classman.render.TextKind;
import classman.render.TextLeaders;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/** The operations of the diagram editor on the diagram, without any window. Each operation writes the new source
 * and reports it to the host. The host keeps the history: undo and redo are forwarded to it, but only for the sources
 * which this editing reported itself, so they never reach back to what happened before the editor was opened. */
public class DiagramEditing {
  private static final double DEFAULT_LINE_TOLERANCE = 4;
  private final EditorHost host;
  private final EditorTexts texts;
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
  private RelationType_V001 armedRelationType = RelationType_V001.ASSOCIATION;
  private int undoableSteps;
  private int redoableSteps;

  /** @throws ClassDiagramParseException if the source has a syntax error
   * @throws IllegalArgumentException if the diagram refers to an element which it doesn't have */
  public DiagramEditing(String source, EditorHost host) throws ClassDiagramParseException {
    this.host = host;
    this.texts = new EditorTexts(host.locale());
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
    if (handleIndexAt(point, tolerance) >= 0 || selectedPlaceholderAt(point) != null
        || movableTextAt(point) != null) {
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
    SceneTextArea relationText = scene.textAreaAt(point);
    if (relationText != null && relationText.element instanceof AbstractRelationModel_V001 relation
        && TextPlaces.isMovable(relation, relationText.kind)) {
      selected = relation;
      return;
    }
    SceneConnectionLine line = scene.connectionLineAt(point, tolerance);
    if (line instanceof SceneAttachLine attachLine) {
      selected = attachLine.attachLine;
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

  /** @return true if the point is at most the tolerance away from a handle of the selected relation or anchor */
  public boolean isOnHandle(Point2D point, double tolerance) {
    return handleIndexAt(point, tolerance) >= 0;
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
    if (selected instanceof AttachLineModel_V001 attachLine) {
      boolean isAttachLineWaypoint = handle > 0 && handle <= attachLine.waypoints.size();
      if (isAttachLineWaypoint) {
        routeEditing.deleteWaypoint(attachLine, handle - 1);
        publish();
      }
      return isAttachLineWaypoint;
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
    if (selected instanceof AttachLineModel_V001 attachLine) {
      routeEditing.insertWaypoint(attachLine, segment, onLine);
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

  /** @return the selected box if it is a class of the diagram, null if something else or
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
   * selected */
  public List<Point2D> selectedHandles() {
    SceneConnectionLine selectedLine = selectedLine();
    return selectedLine != null ? selectedLine.points() : List.of();
  }

  /** An anchor line leads to a class if the ID it refers to is the ID of a class, else to an attribute or a relation. */
  private boolean leadsToClass(AttachLineModel_V001 attachLine) {
    return model.classes().stream().anyMatch(box -> attachLine.targetId.equals(box.id));
  }

  /** @return where the line is drawn of the selected relation or anchor, null if none is selected */
  private SceneConnectionLine selectedLine() {
    if (selected instanceof AttachLineModel_V001 attachLine) {
      return scene.attachLineOf(attachLine);
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
    return new SelectionMarks(frames, selectedHandles(), selectedPlaceholders(), selectedTextLeaders(), trackedFieldColor(),
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

  /** @return the label or multiplicity of the selected relation at the point, which the user can move, null if there
   * is none */
  public SceneTextArea movableTextAt(Point2D point) {
    SceneTextArea text = scene.textAreaAt(point);
    boolean movable = text != null && text.element == selected && selected instanceof AbstractRelationModel_V001 relation
      && TextPlaces.isMovable(relation, text.kind);
    return movable ? text : null;
  }

  /** @return true if the user has put the text at a place, so that the layout doesn't place it */
  public boolean hasFixedPlace(SceneTextArea text) {
    return TextPlaces.of((AbstractRelationModel_V001) text.element, text.kind) != null;
  }

  /** Gives the text back to the layout, which places it where it would put it without the user. Not marked as a
   * change while changes are tracked. One operation. */
  public void resetPlace(SceneTextArea text) {
    if (hasFixedPlace(text)) {
      TextPlaces.set((AbstractRelationModel_V001) text.element, text.kind, null);
      publish();
    }
  }

  private SceneTextArea selectedPlaceholderAt(Point2D point) {
    SceneTextArea placeholder = scene.placeholderAt(point);
    return placeholder != null && placeholder.element == selected ? placeholder : null;
  }

  /** @return true if a relation is selected, and no other element, in particular no anchor line */
  public boolean isRelationSelected() {
    return selected instanceof AbstractRelationModel_V001;
  }

  /** @return the end of the selected relation or anchor line whose handle is at the point: the handle at the start of
   * its line is the end FROM (for an anchor line the end on the note), the one at the end of its line the end TO (the
   * end on the target). Null if the point is on no such handle or no such line is selected. */
  public RelationEnd_V001 endHandleAt(Point2D point, double tolerance) {
    if (!(selected instanceof AbstractRelationModel_V001) && !(selected instanceof AttachLineModel_V001)) {
      return null;
    }
    int handle = handleIndexAt(point, tolerance);
    if (handle < 0) {
      return null;
    }
    if (handle == 0) {
      return RelationEnd_V001.FROM;
    }
    return handle == selectedHandles().size() - 1 ? RelationEnd_V001.TO : null;
  }

  /** @return true if the selected relation has an arrowhead at the end */
  public boolean hasArrowAt(RelationEnd_V001 end) {
    return selected instanceof AssociationRelationModel_V001 association
      && (end == RelationEnd_V001.FROM ? association.fromArrow : association.toArrow);
  }

  /** @return whether the user may switch the arrowhead of the selected relation at the end. The relation must be one
   * which can have an arrowhead there (an association at both ends, an aggregation or composition at the target), and
   * must not run to or from an enum, which doesn't know who uses it and so is only ever the target of a directed
   * association. A removed relation can't be switched. The switching is no change which a change set tracks, so it is
   * possible with and without change tracking, and marks nothing. */
  public Can canSwitchArrowAt(RelationEnd_V001 end) {
    if (!(selected instanceof AssociationRelationModel_V001 relation)) {
      return Can.NOT;
    }
    if (!relation.type.canHaveArrowAt(end)) {
      return Can.not(texts.get("reason.arrow.notpossible"));
    }
    if (involvesEnum(relation)) {
      return Can.not(texts.get("reason.arrow.enum"));
    }
    boolean isRemoved = relation.change != null && relation.change.type == ChangeType_V001.REMOVED;
    return Can.IF(!isRemoved, texts.get("reason.relation.removed"));
  }

  private boolean involvesEnum(AbstractRelationModel_V001 relation) {
    for (ClassModel_V001 box : model.classes()) {
      if (box.kind == ClassKind_V001.ENUM && box.id != null
        && (box.id.equals(relation.fromId) || box.id.equals(relation.toId))) {
        return true;
      }
    }
    return false;
  }

  /** Switches the arrowhead of the selected relation at the end on if it is off and off if it is on. Not marked as a
   * change, see {@link #canSwitchArrowAt}. One operation. Nothing happens if the user may not switch it. */
  public void toggleArrowAt(RelationEnd_V001 end) {
    if (!canSwitchArrowAt(end).can()) {
      return;
    }
    AssociationRelationModel_V001 association = (AssociationRelationModel_V001) selected;
    if (end == RelationEnd_V001.FROM) {
      association.fromArrow = !association.fromArrow;
    }
    else {
      association.toArrow = !association.toArrow;
    }
    publish();
  }

  /** @return whether the end of the selected relation or anchor line is fixed at a point of the border of its box (a
   * point of the line of a relation, for the end of an anchor line on a relation), so that it can be linked to the
   * center of the box again */
  public Can canCenterEnd(RelationEnd_V001 end) {
    String centeredAlready = texts.get("reason.center.already");
    if (selected instanceof AttachLineModel_V001 attachLine) {
      return Can.IF((end == RelationEnd_V001.FROM ? attachLine.start : attachLine.end) != null, centeredAlready);
    }
    if (!(selected instanceof AbstractRelationModel_V001 relation)) {
      return Can.NOT;
    }
    if (relation.route == null) {
      return Can.not(centeredAlready);
    }
    return Can.IF(!(end == RelationEnd_V001.FROM ? relation.route.start : relation.route.end).center, centeredAlready);
  }

  /** Links the end of the selected relation or anchor line to the center of its box again: the line leaves the box
   * where a line from the center towards the next point of the line crosses its border, like a line without a route.
   * A route which then has no waypoints and no fixed end is dropped. Not marked as a change, a route never is. One
   * operation. */
  public void centerEnd(RelationEnd_V001 end) {
    if (!canCenterEnd(end).can()) {
      return;
    }
    if (selected instanceof AttachLineModel_V001 attachLine) {
      routeEditing.centerEnd(attachLine, end);
    }
    else {
      routeEditing.centerEnd((AbstractRelationModel_V001) selected, end);
    }
    publish();
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

  /** @return the lines which join the texts of the selected relation which the user has moved far from its line to the
   * line, none if no relation is selected */
  private List<Line2D> selectedTextLeaders() {
    if (!(selected instanceof AbstractRelationModel_V001 relation) || selectedLine() == null) {
      return List.of();
    }
    Map<TextKind, Rectangle2D> areas = new EnumMap<>(TextKind.class);
    for (SceneTextArea text : scene.textAreas) {
      if (text.element == relation) {
        areas.put(text.kind, text.area);
      }
    }
    return new ArrayList<Line2D>(TextLeaders.of(relation, selectedLine().points(), areas));
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

  /** @return whether the class can get parts: no change set has removed it */
  public Can canAddPartsTo(ClassModel_V001 box) {
    if (!model.classes().contains(box)) {
      return Can.NOT;
    }
    boolean isRemoved = box.change != null && box.change.type == ChangeType_V001.REMOVED;
    return Can.IF(!isRemoved, texts.get("reason.class.removed"));
  }

  /** @return whether the class can get an attribute or, for an enum, a value: it can get parts, is not an interface
   * and shows its attributes */
  public Can canAddAttributeTo(ClassModel_V001 box) {
    Can parts = canAddPartsTo(box);
    if (!parts.can()) {
      return parts;
    }
    if (!box.kind.hasAttributes()) {
      return Can.not(texts.get("reason.interface.noattributes"));
    }
    return Can.IF(box.showsAttributes(), texts.get("reason.attributes.hidden"));
  }

  /** @return whether the class has an attribute compartment which can be hidden or shown: it is no interface, no
   * change set has removed it, and its attributes carry no marks of a change set, which must not be hidden */
  public Can canHideAttributesOf(ClassModel_V001 box) {
    Can parts = canAddPartsTo(box);
    if (!parts.can()) {
      return parts;
    }
    if (!box.kind.hasAttributes()) {
      return Can.not(texts.get("reason.interface.noattributes"));
    }
    return Can.IF(!box.hasMarkedAttributes(), texts.get("reason.attributes.marked"));
  }

  /** @return whether the attribute can be hidden: the class shows it, no change set has removed the class, and it
   * is not the line ". . ." */
  public Can canHideAttribute(ClassModel_V001 box, AttributeModel_V001 attribute) {
    if (attribute == null || attribute.hidden || attribute.hiddenAttributesMarker
      || !box.attributes.contains(attribute)) {
      return Can.NOT;
    }
    return canAddAttributeTo(box);
  }

  /** Hides the attribute, which stays in the diagram. Marks of change sets on it are not lost, the line ". . ." shows
   * their color. Not marked as a change, like the width. One operation. */
  public void hideAttribute(ClassModel_V001 box, AttributeModel_V001 attribute) {
    if (canHideAttribute(box, attribute).can()) {
      attribute.hidden = true;
      publish();
    }
  }

  /** @return whether the hidden attribute of the class can be shown again: no change set has removed the class */
  public Can canShowAttribute(ClassModel_V001 box, AttributeModel_V001 attribute) {
    if (!attribute.hidden || !box.attributes.contains(attribute)) {
      return Can.NOT;
    }
    return canAddPartsTo(box);
  }

  /** Shows the hidden attribute again. Not marked as a change. One operation. */
  public void showAttribute(ClassModel_V001 box, AttributeModel_V001 attribute) {
    if (canShowAttribute(box, attribute).can()) {
      attribute.hidden = false;
      publish();
    }
  }

  /** Hides or shows the attribute compartment of the class. The attributes stay in the diagram. Not marked as a
   * change, like the width. One operation. */
  public void setAttributesHidden(ClassModel_V001 box, boolean hidden) {
    if (box.attributesHidden != hidden && canAddPartsTo(box).can() && (!hidden || canHideAttributesOf(box).can())) {
      box.attributesHidden = hidden;
      publish();
    }
  }

  /** @return whether the class, note or relation can get a style of its own: no change set has removed it */
  public Can canChangeStyle(AbstractDiagramElementModel_V001 element) {
    if (element instanceof ClassModel_V001 box) {
      return canAddPartsTo(box);
    }
    boolean isRemoved = element.change != null && element.change.type == ChangeType_V001.REMOVED;
    if (element instanceof NoteModel_V001) {
      return Can.IF(!isRemoved, texts.get("reason.note.removed"));
    }
    if (element instanceof AbstractRelationModel_V001) {
      return Can.IF(!isRemoved, texts.get("reason.relation.removed"));
    }
    return Can.NOT;
  }

  /** Gives the class, note or relation the style, a style without any part gives it the standard look. Not marked
   * as a change, like the width. One operation. The element keeps a copy, so the style can't be changed from
   * outside. */
  public void setStyle(AbstractDiagramElementModel_V001 element, ElementStyleModel_V001 style) {
    if (canChangeStyle(element).can() && !element.style.equals(style)) {
      element.style = style.copy();
      publish();
    }
  }

  /** @return the selected relation, null if something else or nothing is selected */
  public AbstractRelationModel_V001 selectedRelation() {
    return selected instanceof AbstractRelationModel_V001 relation ? relation : null;
  }

  /** Puts a blank row after the last tagged value of the class, for a new tagged value to be typed into. It is part
   * of the picture, but not of the diagram, until it is given a text with {@link #setText}.
   * @return the area of the blank row
   * @throws IllegalArgumentException if the class can't get parts */
  public SceneTextArea newTaggedValue(ClassModel_V001 box) {
    if (!canAddPartsTo(box).can()) {
      throw new IllegalArgumentException("The class can't get a tagged value: " + box.name);
    }
    TaggedValueModel_V001 pending = taggedValueEditing.beginAtEnd(box);
    scene = host.renderer().layout(model);
    return scene.textAreas.stream().filter(text -> text.taggedValue == pending).findFirst().orElseThrow();
  }

  /** @return whether the class has no stereotype, or only one which a change set has removed */
  public Can canAddStereotypeTo(ClassModel_V001 box) {
    Can parts = canAddPartsTo(box);
    if (!parts.can()) {
      return parts;
    }
    return Can.IF(stereotypeOf(box).isEmpty(), texts.get("reason.stereotype.exists"));
  }

  /** Puts a blank stereotype row into the class, for the stereotype to be typed into. It is part of the picture, but
   * not of the diagram, until it is given a text with {@link #setText}. A stereotype which a change set has removed is
   * still there as a struck row, which is the area to type into.
   * @return the area of the row
   * @throws IllegalArgumentException if the class has a stereotype */
  public SceneTextArea newStereotype(ClassModel_V001 box) {
    if (!canAddStereotypeTo(box).can()) {
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
    SceneTextArea movableText = movableTextAt(at);
    if (handle >= 0) {
      drag = handleDrag(handle, at);
    }
    else if (movableText != null) {
      drag = new TextMoveDrag((AbstractRelationModel_V001) selected, movableText.kind, selectedLine().points(), at,
        movableText.area);
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
    return new PositionFreezer(scene, model.boxes());
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
    if (selected instanceof AttachLineModel_V001 attachLine) {
      AttachTarget slideTarget = leadsToClass(attachLine) ? null : scene.attachTargets.find(attachLine.targetId);
      return new AttachLineHandleDrag(routeEditing, attachLine, scene.elementAreaOf(model.boxWithId(attachLine.startId)).area,
        handle, selectedLine().points(), at, classesAnAttachLineMayHangOnto(attachLine), slideTarget,
        scene.attachTargets.find(attachLine.targetId));
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

  /** The classes by ID in the order of drawing which the end of the anchor line may be hung onto: all of them, but
   * while changes are tracked only the class it leads to, unless a change set has added the line. */
  private Map<String, Rectangle2D> classesAnAttachLineMayHangOnto(AttachLineModel_V001 attachLine) {
    boolean mayChange = host.trackedChangeSet() == null || attachLine.change != null;
    Map<String, Rectangle2D> classes = new LinkedHashMap<>();
    for (ClassModel_V001 box : model.classes()) {
      boolean allowed = mayChange || attachLine.targetId.equals(box.id);
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

  public boolean isRelationToolArmed() { return armedTool == Tool.RELATION; }

  /** @return the type of the relation which the relation tool draws, the last one which it was armed with */
  public RelationType_V001 armedRelationType() { return armedRelationType; }

  public boolean isAnchorToolArmed() { return armedTool == Tool.ANCHOR; }

  public boolean isAssocClassToolArmed() { return armedTool == Tool.ASSOC_CLASS; }

  /** @return true if a tool of the palette is armed, which takes the presses on the diagram for itself */
  public boolean isToolArmed() { return armedTool != Tool.NONE; }

  /** Arms the tool which draws a relation of the type from one class to another. If the tool is armed for that type
   * already it is disarmed, if it is armed for another type it draws the new type from now on. */
  public void toggleRelationTool(RelationType_V001 type) {
    if (armedTool == Tool.RELATION && armedRelationType == type) {
      disarmTool();
    }
    else {
      armRelationTool(type);
    }
  }

  /** Arms the tool which draws a relation of the type from one class to another, also if it is armed for it already. */
  public void armRelationTool(RelationType_V001 type) {
    armedRelationType = type;
    armedTool = Tool.RELATION;
    drawing = null;
  }

  /** Arms the tool which draws the anchor of a note to a class, or disarms it if it is armed. */
  public void toggleAnchorTool() {
    toggle(Tool.ANCHOR);
  }

  /** Arms the tool which draws the link of an association class to its relation, or disarms it if it is armed. */
  public void toggleAssocClassTool() {
    toggle(Tool.ASSOC_CLASS);
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
   * for a relation or an association class link, a note for an anchor. Which boxes a relation may start and end at is told by {@link RelationRules}. Nothing happens if there is none. The connection is created where the
   * mouse is released on a class, see {@link #endDrag}. */
  public void startDrawing(Point2D at, double tolerance) {
    ConnectionTargets targets = new ConnectionTargets(model, scene);
    drawing = null;
    if (armedTool == Tool.RELATION) {
      ClassTarget from = targets.closestClass(at, tolerance, box -> RelationRules.canStart(armedRelationType, box));
      if (from != null) {
        TargetFinder finder = (point, distance) -> targets.closestClass(point, distance,
          box -> RelationRules.canEnd(armedRelationType, from.box, box));
        drawing = new ConnectionDrawing(from.box, from.frame(), finder, at);
      }
    }
    else if (armedTool == Tool.ANCHOR) {
      Map<NoteModel_V001, Rectangle2D> notes = new LinkedHashMap<>();
      for (NoteModel_V001 note : model.notes()) {
        SceneElementArea noteArea = scene.elementAreaOf(note);
        if (noteArea != null) {
          notes.put(note, noteArea.area);
        }
      }
      NoteModel_V001 from = ClassPicker.closest(notes, at, tolerance);
      if (from != null) {
        drawing = new ConnectionDrawing(from, notes.get(from), targets::closestForAnchor, at);
      }
    }
    else if (armedTool == Tool.ASSOC_CLASS) {
      ClassTarget from = targets.closestClass(at, tolerance, box -> AssocClassRules.canBeAssocClass(model, box));
      if (from != null) {
        TargetFinder finder = (point, distance) -> targets.closestRelation(point, distance,
          relation -> AssocClassRules.canTake(model, relation, from.box));
        drawing = new ConnectionDrawing(from.box, from.frame(), finder, at);
      }
    }
  }

  /** Creates what was drawn, if the mouse was released on a class, and leaves the tool. If it was released on none,
   * nothing is created and the tool stays armed, so the user can try again. */
  private void endDrawing() {
    ConnectionDrawing finished = drawing;
    drawing = null;
    if (finished.target() == null) {
      return;
    }
    if (armedTool == Tool.RELATION) {
      createRelation(finished);
    }
    else if (armedTool == Tool.ANCHOR) {
      anchorNote(finished);
    }
    else {
      linkAssocClass(finished);
    }
  }

  /** Creates the relation of the armed type, a straight line without texts. An association is directed, the others
   * have their marks (diamond, triangle) and no arrowhead of their own. */
  private void createRelation(ConnectionDrawing finished) {
    ClassModel_V001 from = (ClassModel_V001) finished.from();
    String fromId = new ClassTarget(from, scene.elementAreaOf(from).area).ensureId(this::newId);
    String toId = finished.target().ensureId(this::newId);
    AbstractRelationModel_V001 relation = switch (armedRelationType) {
      case EXTENDS -> new ExtendsRelationModel_V001(fromId, toId);
      case IMPLEMENTS -> new ImplementsRelationModel_V001(fromId, toId);
      default -> {
        AssociationRelationModel_V001 association = new AssociationRelationModel_V001(armedRelationType, fromId, toId);
        association.toArrow = armedRelationType == RelationType_V001.ASSOCIATION;
        yield association;
      }
    };
    relation.id = newId();
    markAsAdded(relation);
    model.elements.add(relation);
    selected = relation;
    armedTool = Tool.NONE;
    publish();
  }

  /** Attaches the note to the class, attribute or relation which the mouse was released on, with an anchor line. A
   * class or attribute which has no ID gets one, and so does the note. The note is selected. A note can have several
   * anchor lines, so lines it has already stay as they are. Nothing happens, and no operation is made, if it is
   * attached to that target already. The new line is marked as added while changes are tracked. */
  private void anchorNote(ConnectionDrawing finished) {
    attach((NoteModel_V001) finished.from(), finished.target(), AnchorLineModel_V001::new);
  }

  /** Makes the class the association class of the relation which the mouse was released on, with an association class
   * link. The class and the relation get an ID if they have none. The class is selected. The tool only starts at
   * a class which is no association class yet, see {@link AssocClassRules}. */
  private void linkAssocClass(ConnectionDrawing finished) {
    ClassModel_V001 assocClass = (ClassModel_V001) finished.from();
    attach(assocClass, finished.target(), AssocClassLinkModel_V001::new);
  }

  /** Attaches the box to the target with a new dashed line, unless a live line leads there already.
   * @param newLine creates a line from the ID of the box to the ID of the target */
  private void attach(AbstractBoxModel_V001 box, ConnectionTarget target,
                      BiFunction<String, String, AttachLineModel_V001> newLine) {
    String targetId = target.ensureId(this::newId);
    armedTool = Tool.NONE;
    selected = box;
    if (box.id == null) {
      box.id = newId();
    }
    if (isAttached(box, targetId)) {
      return;
    }
    AttachLineModel_V001 attachLine = newLine.apply(box.id, targetId);
    markAsAdded(attachLine);
    model.elements.add(attachLine);
    publish();
  }

  private boolean isAttached(AbstractBoxModel_V001 box, String targetId) {
    return model.attachLines().stream().anyMatch(line -> !line.isRemoved()
      && box.id.equals(line.startId) && targetId.equals(line.targetId));
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

  /** Puts a class without a name into the picture, its top left corner at the given point of the diagram, with a new
   * ID, selected, for the name to be typed into. It is wide enough for the field of the name while that is typed; with
   * the name it gets no width of its own, so it is as wide as its content. The class is not part of the diagram until its name is given with
   * {@link #setText}: that is one operation. It goes away again with the empty name, or if it is dropped.
   * @return the area of the name */
  public SceneTextArea newClass(int x, int y) {
    return newClass(x, y, ClassKind_V001.CLASS);
  }

  /** Puts a box of the kind (class, interface, enum) without a name into the picture, see {@link #newClass(int, int)}.
   * @return the area of the name */
  public SceneTextArea newClass(int x, int y, ClassKind_V001 kind) {
    return putPendingBox(new ClassModel_V001("", kind), x, y, TextKind.CLASS_NAME);
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
    box.position = new PositionModel_V001(Math.max(0, x), Math.max(0, y));
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

  /** The IDs of the elements of the diagram including its attributes, which notes attach to. */
  private Set<String> usedIds() {
    Set<String> used = new HashSet<>();
    for (AbstractDiagramElementModel_V001 element : model.elements) {
      addIds(used, element);
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
    if (selected != null && !isDrawn(selected)) {
      selected = null;
    }
    if (!newSource.equals(source)) {
      undoableSteps++;
      redoableSteps = 0;
    }
    source = newSource;
    host.sourceChanged(source);
  }

  /** Boxes and dashed lines which hang on hidden attributes only are not in the picture, so they can't stay selected. */
  private boolean isDrawn(AbstractDiagramElementModel_V001 element) {
    if (element instanceof AbstractBoxModel_V001) {
      return scene.elementAreaOf(element) != null;
    }
    if (element instanceof AttachLineModel_V001 attachLine) {
      return scene.attachLineOf(attachLine) != null;
    }
    return true;
  }
}
