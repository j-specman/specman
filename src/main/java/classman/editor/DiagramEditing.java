package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.AssocClassLinkModel_V001;
import classman.model.v001.AttachLineModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.Compartment_V001;
import classman.model.v001.DependencyRelationModel_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.EndType_V001;
import classman.model.v001.ExtendsRelationModel_V001;
import classman.model.v001.ImplementsRelationModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.RelationType_V001;
import classman.model.v001.StereotypeReference_V001;
import classman.model.v001.TaggedValueModel_V001;
import classman.model.v001.change.ElementTextChanger_V001;
import classman.model.v001.change.RelationEndTypeChanger_V001;
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
import java.util.Collection;
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
  private final RelationEndTypeChanger_V001 endTypeChanger = new RelationEndTypeChanger_V001();
  private final RouteEditing routeEditing = new RouteEditing();
  private final ElementIds ids = new ElementIds(new SecureRandom());
  private ClassDiagramModel_V001 model;
  private final Map<Compartment_V001, MemberEditing<?, ?>> memberEditings = new EnumMap<>(Compartment_V001.class);
  private TaggedValueEditing taggedValueEditing;
  private PendingStereotype pendingStereotype;
  private AbstractBoxModel_V001 pendingBox;
  private String source;
  private Scene scene;
  private AbstractDiagramElementModel_V001 selected;
  private AbstractMemberModel_V001 selectedMember;
  private boolean isMemberSelectedByThisPress;
  private final BoxGroup group = new BoxGroup();
  private RubberBand band;
  private LineEndRepair lineEndRepair;
  private Drag drag;
  private ConnectionDrawing drawing;
  private Tool armedTool = Tool.NONE;
  private final FormatPainter formatPainter = new FormatPainter();
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
    memberEditings.put(Compartment_V001.ATTRIBUTES, new AttributeEditing(model));
    memberEditings.put(Compartment_V001.METHODS, new MethodEditing(model));
    taggedValueEditing = new TaggedValueEditing();
    pendingStereotype = new PendingStereotype();
    pendingBox = null;
    scene = host.renderer().layout(model);
    source = newSource;
    select(null);
    band = null;
    drag = null;
    drawing = null;
    armedTool = Tool.NONE;
  }

  public String source() { return source; }

  /** @return the name which the diagram has in its source, null if it has none. The editor doesn't change it. */
  public String diagramName() { return model.name; }

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

  /** @return the picture of the diagram as it is now, without any mark of the editing, for what leaves the editor */
  public Scene plainScene() { return scene; }

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
    isMemberSelectedByThisPress = false;
    if (groupMemberAt(point, tolerance) != null) {
      return;
    }
    if (handleIndexAt(point, tolerance) >= 0 || selectedPlaceholderAt(point) != null
        || movableTextAt(point) != null) {
      return;
    }
    ResizeTarget edge = resizeTargetAt(point, tolerance);
    if (edge != null) {
      select(edge.box);
      return;
    }
    if (selectMemberAt(point)) {
      return;
    }
    SceneElementArea hit = scene.elementAreaAt(point);
    if (hit != null) {
      select(hit.element);
      return;
    }
    SceneTextArea relationText = scene.textAreaAt(point);
    if (relationText != null && relationText.element instanceof AbstractRelationModel_V001 relation
        && TextPlaces.isMovable(relation, relationText.kind)) {
      select(relation);
      return;
    }
    SceneConnectionLine line = scene.connectionLineAt(point, tolerance);
    if (line instanceof SceneAttachLine attachLine) {
      select(attachLine.attachLine);
    }
    else {
      select(line != null ? ((SceneRelationLine) line).relation : null);
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
      routeEditing.insertWaypoint(relation, line.points(), segment, onLine, endArea(relation.fromId),
        endArea(relation.toId));
    }
    publish();
    return true;
  }

  /** @return where the class is, or for a dependency the attribute, which a relation starts or ends at */
  private Rectangle2D endArea(String endId) {
    return scene.attachTargets.findEnd(endId, true).box();
  }

  public void deselect() { select(null); }

  public boolean hasSelection() { return selected != null || !group.isEmpty(); }

  /** Selects one element, or nothing if it is null. That ends a selection of several boxes. */
  private void select(AbstractDiagramElementModel_V001 element) {
    selected = element;
    selectedMember = null;
    group.clear();
  }

  /** @return the box of the selected group which the point is on, or at its left or right side, null if no group is
   * selected or the point is on none of its boxes */
  private AbstractBoxModel_V001 groupMemberAt(Point2D point, double tolerance) {
    if (group.isEmpty()) {
      return null;
    }
    ResizeTarget edge = resizeTargetAt(point, tolerance);
    SceneElementArea hit = scene.elementAreaAt(point);
    AbstractBoxModel_V001 box = edge != null ? edge.box
      : hit != null && hit.element instanceof AbstractBoxModel_V001 hitBox ? hitBox : null;
    return box != null && group.contains(box) ? box : null;
  }

  /** Selects the boxes together, if there are two or more. One box is selected like any element, and no box means
   * nothing selected. */
  private void selectBoxes(List<AbstractBoxModel_V001> boxes) {
    if (boxes.size() < 2) {
      select(boxes.isEmpty() ? null : boxes.get(0));
      return;
    }
    selected = null;
    selectedMember = null;
    group.replace(boxes);
  }

  /** @return the boxes which are selected together, empty if fewer than two are */
  public List<AbstractBoxModel_V001> selectedBoxes() { return group.members(); }

  /** @return the boxes which are selected: the boxes of the group, or the one box which is selected, none if a relation
   * or a line or nothing is */
  private List<AbstractBoxModel_V001> currentBoxes() {
    if (!group.isEmpty()) {
      return group.members();
    }
    return selected instanceof AbstractBoxModel_V001 box ? List.of(box) : List.of();
  }

  /** @return the boxes of the diagram which are in the list, in the order of drawing */
  private List<AbstractBoxModel_V001> inDrawingOrder(Collection<AbstractBoxModel_V001> boxes) {
    return model.boxes().stream().filter(boxes::contains).toList();
  }

  /** A press with Ctrl: the box at the point is added to the selected boxes, or taken out if it is one of them. Where
   * there is no box, a rectangle is started which adds the boxes in it to the selected ones, unless the point is on a
   * relation or a line, which can't be selected together with boxes: nothing happens there. */
  public void pressWithCtrl(Point2D at, double tolerance) {
    AbstractBoxModel_V001 box = boxAt(at, tolerance);
    if (box != null) {
      List<AbstractBoxModel_V001> boxes = new ArrayList<>(currentBoxes());
      if (!boxes.remove(box)) {
        boxes.add(box);
      }
      selectBoxes(inDrawingOrder(boxes));
      return;
    }
    boolean onLine = handleIndexAt(at, tolerance) >= 0 || scene.connectionLineAt(at, tolerance) != null;
    if (!onLine && drag == null && drawing == null) {
      band = new RubberBand(at, currentBoxes());
    }
  }

  /** @return the box which the point is on, or at its left or right side, null if there is none */
  private AbstractBoxModel_V001 boxAt(Point2D point, double tolerance) {
    ResizeTarget edge = resizeTargetAt(point, tolerance);
    if (edge != null) {
      return edge.box;
    }
    SceneElementArea hit = scene.elementAreaAt(point);
    return hit != null && hit.element instanceof AbstractBoxModel_V001 box ? box : null;
  }

  /** Selects all boxes of the diagram, the classes, notes and association classes. */
  public void selectAllBoxes() {
    selectBoxes(model.boxes().stream().filter(box -> box != pendingBox && scene.elementAreaOf(box) != null).toList());
  }

  /** Starts to pull a rectangle open from the point, if nothing is selected. Dragging makes it grow, and the boxes
   * which lie completely inside of it are selected when the mouse is released. */
  public void startRubberBand(Point2D at) {
    if (!hasSelection() && drag == null && drawing == null) {
      band = new RubberBand(at);
    }
  }

  /** @return the selected box if it is a class of the diagram, null if something else or
   * nothing is selected */
  public ClassModel_V001 selectedClass() {
    return selected instanceof ClassModel_V001 box && model.classes().contains(box) ? box : null;
  }

  /** @return the selected box if it is a note, null if something else or nothing is selected */
  public NoteModel_V001 selectedNote() {
    return selected instanceof NoteModel_V001 note ? note : null;
  }

  /** @return the attribute, method or enum value whose row is selected within the selected class, null if none is */
  public AbstractMemberModel_V001 selectedMember() { return selectedMember; }

  /** Selects the row of a member of the selected class if the point is on it: the press which selects the class selects
   * the class only, the next one the row. A row of a class which a change set has removed can't be selected.
   * @return true if the point is on a row of the selected class */
  private boolean selectMemberAt(Point2D point) {
    SceneTextArea row = memberRowAt(point);
    if (row == null) {
      return false;
    }
    if (row.member != selectedMember) {
      selectedMember = row.member;
      isMemberSelectedByThisPress = true;
    }
    return true;
  }

  private SceneTextArea memberRowAt(Point2D point) {
    ClassModel_V001 box = selectedClass();
    SceneTextArea text = scene.textAreaAt(point);
    boolean isRow = box != null && text != null && text.element == box && text.kind.compartment() != null
      && text.member != null && MemberOrder.isShown(text.member) && !isRemoved(box.change);
    return isRow ? text : null;
  }

  /** @return the row of the member in the picture, null if the picture has none */
  private SceneTextArea rowOf(AbstractMemberModel_V001 member) {
    return scene.textAreas.stream().filter(text -> text.member == member).findFirst().orElse(null);
  }

  /** Ends the selection of the row, the class stays selected.
   * @return true if a row was selected */
  public boolean deselectMember() {
    boolean wasSelected = selectedMember != null;
    selectedMember = null;
    return wasSelected;
  }

  /** Moves the selected row by places among the shown rows of its compartment, nothing happens beyond the first or the
   * last. It is one operation, and no change which change sets mark. */
  public void moveSelectedMember(int places) {
    ClassModel_V001 box = selectedClass();
    Compartment_V001 compartment = box == null || selectedMember == null ? null : compartmentOf(box, selectedMember);
    if (compartment != null && !isRemoved(box.change)
        && MemberOrder.moveBy(box.members(compartment), selectedMember, places)) {
      publish();
    }
  }

  /** @return where the selected row is drawn, across the width of its box, null if none is selected */
  private Rectangle2D selectedMemberArea() {
    SceneTextArea row = selectedMember == null ? null : rowOf(selectedMember);
    Rectangle2D box = selectedArea();
    return row == null || box == null ? null
      : new Rectangle2D.Double(box.getX(), row.area.getY(), box.getWidth(), row.area.getHeight());
  }

  /** Deletes the member of the selected row by the rules for the empty text of the line. The class stays selected. */
  private void deleteSelectedMember() {
    SceneTextArea row = rowOf(selectedMember);
    if (row != null) {
      memberEditings.get(row.kind.compartment()).setText(row, "", host.trackedChangeSet());
      publish();
    }
    selectedMember = null;
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
    for (AbstractBoxModel_V001 member : group.members()) {
      frames.add(scene.elementAreaOf(member).area);
    }
    if (band != null) {
      for (AbstractBoxModel_V001 inside : boxesInside(band.rectangle())) {
        frames.add(scene.elementAreaOf(inside).area);
      }
    }
    if (connectionTargetArea() != null) {
      frames.add(connectionTargetArea());
    }
    return new SelectionMarks(frames, selectedHandles(), selectedPlaceholders(), selectedTextLeaders(), trackedFieldColor(),
      drawing != null ? drawing.line() : null, specialSelectionColor(), band != null ? band.rectangle() : null,
      selectedMemberArea());
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
    boolean editable = text != null && (text.element == selected || text.element instanceof AbstractRelationModel_V001)
      && !isReferenceText(text);
    if (editable) {
      return text;
    }
    return text == null ? selectedPlaceholderAt(point) : null;
  }

  /** @return true if the text is a stereotype which refers to a configuration of the host: the user chooses it from a
   * menu, but can't type it */
  private static boolean isReferenceText(SceneTextArea text) {
    return text.kind == TextKind.STEREOTYPE
      && StereotypeReference_V001.isReference(((ClassModel_V001) text.element).stereotype);
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

  /** @return true if the end handle menu of the selected relation offers the types of its ends: for an association,
   * aggregation or composition, but not if it runs to or from an enum, which doesn't know who uses it, so it is
   * only ever the target of a directed association and nothing can be chosen */
  public boolean offersEndTypes() {
    return selected instanceof AssociationRelationModel_V001 relation && !involvesEnum(relation);
  }

  /** @return what the selected relation has at its end, see {@link #offersEndTypes} */
  public EndType_V001 endTypeAt(RelationEnd_V001 end) {
    return endTypeChanger.typeAt((AssociationRelationModel_V001) selected, end);
  }

  /** @return whether the user may give the end of the selected relation the type. Not the type which it has already,
   * and not a diamond if the other end has one: a relation has one diamond at most, so the user takes it away at the
   * other end first. A removed relation can't be changed. The change is no change which a change set tracks, so it is
   * possible with and without change tracking, and marks nothing. */
  public Can canSetEndType(RelationEnd_V001 end, EndType_V001 type) {
    if (!offersEndTypes()) {
      return Can.NOT;
    }
    AssociationRelationModel_V001 relation = (AssociationRelationModel_V001) selected;
    EndType_V001 current = endTypeChanger.typeAt(relation, end);
    if (current == type) {
      return Can.NOT;
    }
    boolean isRemoved = relation.change != null && relation.change.type == ChangeType_V001.REMOVED;
    if (isRemoved) {
      return Can.not(texts.get("reason.relation.removed"));
    }
    RelationEnd_V001 otherEnd = end == RelationEnd_V001.FROM ? RelationEnd_V001.TO : RelationEnd_V001.FROM;
    if (type.isDiamond() && endTypeChanger.typeAt(relation, otherEnd).isDiamond()) {
      return Can.not(texts.get("reason.endtype.otherdiamond"));
    }
    return Can.YES;
  }

  /** Gives the end of the selected relation the type. One operation. Nothing happens if the user may not. */
  public void setEndType(RelationEnd_V001 end, EndType_V001 type) {
    if (!canSetEndType(end, type).can()) {
      return;
    }
    endTypeChanger.set(model, (AssociationRelationModel_V001) selected, end, type);
    publish();
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
      case ATTRIBUTE_LINE, METHOD_LINE -> memberEditings.get(text.kind.compartment()).textOf(text);
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
      case STEREOTYPE -> !newText.contains("\n") && !newText.contains(">")
        && StereotypeReference_V001.isFreeText(newText);
      case ATTRIBUTE_LINE, METHOD_LINE ->
        memberEditings.get(text.kind.compartment()).accepts(text, newText, host.trackedChangeSet());
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
    if (text.kind.compartment() != null) {
      if (memberEditings.get(text.kind.compartment()).setText(text, newText, changeSet)) {
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

  /** @return true if the text is the last line of its compartment, or the blank line of a class without members of
   * the kind: a new member can be typed right after it */
  public boolean isLastMemberLine(SceneTextArea text) {
    Compartment_V001 compartment = text.kind.compartment();
    return compartment != null && memberEditings.get(compartment).isLastLine(text);
  }

  /** Puts a blank line at the end of the compartment of the class, for a new attribute or method to be typed into. It
   * is part of the picture, but not of the diagram, until it is given a text with {@link #setText}.
   * @return the area of the blank line */
  public SceneTextArea newMemberAtEnd(ClassModel_V001 box, Compartment_V001 compartment) {
    AbstractMemberModel_V001 placeholder = memberEditings.get(compartment).beginAtEnd(box);
    scene = host.renderer().layout(model);
    return scene.textAreas.stream().filter(text -> text.member == placeholder).findFirst().orElseThrow();
  }

  /** @return whether the class can get parts: no change set has removed it */
  public Can canAddPartsTo(ClassModel_V001 box) {
    if (!model.classes().contains(box)) {
      return Can.NOT;
    }
    boolean isRemoved = box.change != null && box.change.type == ChangeType_V001.REMOVED;
    return Can.IF(!isRemoved, texts.get("reason.class.removed"));
  }

  /** @return whether the class can get an attribute (a value, for an enum) or a method: it can get parts, has the
   * compartment and shows it */
  public Can canAddMemberTo(ClassModel_V001 box, Compartment_V001 compartment) {
    Can parts = canAddPartsTo(box);
    if (!parts.can()) {
      return parts;
    }
    if (!compartment.existsIn(box.kind)) {
      return Can.not(texts.get("reason.interface.noattributes"));
    }
    return Can.IF(compartment.isShownIn(box), texts.get(reasonKey(compartment, "hidden")));
  }

  /** @return whether the class has the compartment, which can be hidden or shown: an interface has no attributes, no
   * change set has removed the class, and the members carry no marks of a change set, which must not be hidden */
  public Can canHideCompartmentOf(ClassModel_V001 box, Compartment_V001 compartment) {
    Can parts = canAddPartsTo(box);
    if (!parts.can()) {
      return parts;
    }
    if (!compartment.existsIn(box.kind)) {
      return Can.not(texts.get("reason.interface.noattributes"));
    }
    return Can.IF(!compartment.hasMarkedMembersIn(box), texts.get(reasonKey(compartment, "marked")));
  }

  private static String reasonKey(Compartment_V001 compartment, String reason) {
    return "reason." + (compartment == Compartment_V001.ATTRIBUTES ? "attributes" : "methods") + "." + reason;
  }

  /** @return the compartment of the class which has the member, null if the class hasn't */
  private static Compartment_V001 compartmentOf(ClassModel_V001 box, AbstractMemberModel_V001 member) {
    for (Compartment_V001 compartment : Compartment_V001.values()) {
      if (box.members(compartment).contains(member)) {
        return compartment;
      }
    }
    return null;
  }

  /** @return whether the attribute or method can be hidden: the class shows it, no change set has removed the class,
   * and it is not the line ". . ." */
  public Can canHideMember(ClassModel_V001 box, AbstractMemberModel_V001 member) {
    Compartment_V001 compartment = member == null ? null : compartmentOf(box, member);
    if (compartment == null || member.hidden || member.hiddenMarker) {
      return Can.NOT;
    }
    return canAddMemberTo(box, compartment);
  }

  /** Hides the attribute or method, which stays in the diagram. Marks of change sets on it are not lost, the line
   * ". . ." shows their color. Not marked as a change, like the width. One operation. */
  public void hideMember(ClassModel_V001 box, AbstractMemberModel_V001 member) {
    if (canHideMember(box, member).can()) {
      member.hidden = true;
      publish();
    }
  }

  /** @return whether the hidden attribute or method of the class can be shown again: no change set has removed the
   * class */
  public Can canShowMember(ClassModel_V001 box, AbstractMemberModel_V001 member) {
    if (!member.hidden || compartmentOf(box, member) == null) {
      return Can.NOT;
    }
    return canAddPartsTo(box);
  }

  /** Shows the hidden attribute or method again. Not marked as a change. One operation. */
  public void showMember(ClassModel_V001 box, AbstractMemberModel_V001 member) {
    if (canShowMember(box, member).can()) {
      member.hidden = false;
      publish();
    }
  }

  /** Hides or shows the compartment of the class. Its members stay in the diagram. Not marked as a change, like the
   * width. One operation. */
  public void setCompartmentHidden(ClassModel_V001 box, Compartment_V001 compartment, boolean hidden) {
    boolean allowed = canAddPartsTo(box).can() && (!hidden || canHideCompartmentOf(box, compartment).can());
    if (allowed && compartment.setHidden(box, hidden)) {
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

  /** @return the names of the configurations of the host which a stereotype can refer to */
  public List<String> stereotypeConfigs() {
    return host.stereotypeConfigs();
  }

  /** @return whether the class can get another stereotype, or have its stereotype changed or removed: no change set
   * has removed it */
  public Can canChangeStereotypeOf(ClassModel_V001 box) {
    return canAddPartsTo(box);
  }

  /** @return whether the class has a stereotype which can be removed: one which a change set has removed doesn't
   * count */
  public Can canRemoveStereotypeOf(ClassModel_V001 box) {
    Can parts = canAddPartsTo(box);
    if (!parts.can()) {
      return parts;
    }
    return Can.IF(!stereotypeOf(box).isEmpty(), texts.get("reason.stereotype.missing"));
  }

  /** @return the name of the configuration of the host which the stereotype of the class refers to, null if the class
   * has no stereotype or one which is a free text */
  public String stereotypeConfigOf(ClassModel_V001 box) {
    String stereotype = stereotypeOf(box);
    return StereotypeReference_V001.isReference(stereotype) ? StereotypeReference_V001.nameOf(stereotype) : null;
  }

  /** @return true if the class has a stereotype which is a free text */
  public boolean hasFreeTextStereotype(ClassModel_V001 box) {
    String stereotype = stereotypeOf(box);
    return !stereotype.isEmpty() && StereotypeReference_V001.isFreeText(stereotype);
  }

  /** Gives the class the stereotype which refers to the configuration of the host, in place of the one it has. Like
   * any other text, a stereotype which is chosen is marked while changes are tracked, with the former one as backup.
   * @throws IllegalArgumentException if the class can't get parts, or the name isn't one which a configuration can
   * have */
  public void setStereotypeConfig(ClassModel_V001 box, String configName) {
    if (!canChangeStereotypeOf(box).can() || !StereotypeReference_V001.isValidName(configName)) {
      throw new IllegalArgumentException("The class can't get the stereotype " + configName + ": " + box.name);
    }
    String reference = StereotypeReference_V001.textOf(configName);
    if (reference.equals(stereotypeOf(box))) {
      discardNewPart();
      return;
    }
    pendingStereotype.discard();
    textChanger.setStereotype(box, reference, host.trackedChangeSet());
    publish();
  }

  /** Takes the stereotype away from the class. While changes are tracked it stays, marked as removed.
   * @throws IllegalArgumentException if the class has no stereotype to remove */
  public void removeStereotype(ClassModel_V001 box) {
    if (!canRemoveStereotypeOf(box).can()) {
      throw new IllegalArgumentException("The class has no stereotype to remove: " + box.name);
    }
    pendingStereotype.discard();
    textChanger.setStereotype(box, "", host.trackedChangeSet());
    publish();
  }

  /** Gives the row to type a stereotype of free text into, for any class which can get parts. A class without a
   * stereotype gets a blank row, and so does one whose stereotype refers to a configuration, in its place; the
   * reference is back when the row is dropped or left empty. The blank row is part of the picture, but not of the
   * diagram, until it is given a text with {@link #setText}. For a free text it is the text of the stereotype, and for
   * a stereotype which a change set has removed the struck row, which is there already.
   * @return the area of the row
   * @throws IllegalArgumentException if the class can't get parts */
  public SceneTextArea stereotypeRowForFreeText(ClassModel_V001 box) {
    if (!canChangeStereotypeOf(box).can()) {
      throw new IllegalArgumentException("The class can't get a stereotype: " + box.name);
    }
    if (box.stereotype == null || StereotypeReference_V001.isReference(stereotypeOf(box))) {
      pendingStereotype.replace(box);
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
    boolean memberGone = discardPendingMembers();
    boolean stereotypeGone = pendingStereotype.discard();
    boolean taggedValueGone = taggedValueEditing.discardPending();
    boolean boxGone = discardPendingBox();
    return memberGone || stereotypeGone || taggedValueGone || boxGone;
  }

  /** Starts to drag, from the point where the mouse was pressed: a handle of the selected relation if the point is at
   * most the tolerance away from one, a side of a class if the point is at it, otherwise the selected box, if there is
   * one. A press on a box of a selected group drags the whole group. Nothing is dragged if the user only clicks: that
   * makes the box which was pressed the only selected one. */
  public void startDrag(Point2D at, double tolerance) {
    drag = newDrag(at, tolerance);
    lineEndRepair = drag != null && !drag.movedBoxes().isEmpty() ? new LineEndRepair(model, drag.movedBoxes()) : null;
  }

  private Drag newDrag(Point2D at, double tolerance) {
    AbstractBoxModel_V001 groupMember = groupMemberAt(at, tolerance);
    if (groupMember != null) {
      return new GroupDrag(group.members(), groupMember, at, scene, positionFreezer(),
        new GroupLines(model, group.members()).waypointLists());
    }
    int handle = handleIndexAt(at, tolerance);
    ResizeTarget edge = resizeTargetAt(at, tolerance);
    SceneTextArea movableText = movableTextAt(at);
    if (handle >= 0) {
      return handleDrag(handle, at);
    }
    if (movableText != null) {
      return new TextMoveDrag((AbstractRelationModel_V001) selected, movableText.kind, selectedLine().points(), at,
        movableText.area);
    }
    if (edge != null) {
      return new BoxResize(edge.box, edge.side, scene, minimumWidthOf(edge.box), positionFreezer());
    }
    SceneTextArea selectedRow = memberRowAt(at);
    if (selectedRow != null && selectedRow.member == selectedMember && !isMemberSelectedByThisPress) {
      return new MemberReorderDrag(selectedClass(), selectedRow.kind.compartment(), selectedRow, scene, at);
    }
    return selected instanceof AbstractBoxModel_V001 box && scene.elementAreaOf(box) != null
      ? new BoxDrag(box, at, scene, positionFreezer())
      : null;
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
    return new HandleDrag(routeEditing, relation, line.points(), handle, at, endArea(relation.fromId),
      endArea(relation.toId), isEnd ? classesAnEndMayHangOnto(relation, handle == 0) : Map.of());
  }

  /** The classes by ID in the order of drawing which an end of the relation may be hung onto: all of them, but while
   * changes are tracked only the class it hangs on, because hanging a relation onto another class is no change which
   * the format can mark. The user deletes the relation and adds a new one then. The end of a dependency may also be
   * hung onto the attributes which the classes show, each after its class so that it wins where it is. */
  private Map<String, Rectangle2D> classesAnEndMayHangOnto(AbstractRelationModel_V001 relation, boolean start) {
    Map<String, Rectangle2D> ends = new LinkedHashMap<>();
    String ownEndId = start ? relation.fromId : relation.toId;
    String otherEndId = start ? relation.toId : relation.fromId;
    boolean isDependency = relation.type == RelationType_V001.DEPENDENCY;
    ClassModel_V001 otherOwner = isDependency ? ownerOfEnd(otherEndId) : null;
    for (ClassModel_V001 box : model.classes()) {
      boolean isFree = host.trackedChangeSet() == null && box != otherOwner;
      if (box.id != null && (isFree || ownEndId.equals(box.id))) {
        ends.put(box.id, scene.elementAreaOf(box).area);
      }
      if (isDependency) {
        addMemberEnds(ends, box, ownEndId, isFree);
      }
    }
    return ends;
  }

  /** Adds the attributes and methods which the class shows and which an end of a dependency may hang on, the one it
   * hangs on already in any case. */
  private void addMemberEnds(Map<String, Rectangle2D> ends, ClassModel_V001 box, String ownEndId, boolean isFree) {
    for (Compartment_V001 compartment : Compartment_V001.values()) {
      if (!compartment.isShownIn(box)) {
        continue;
      }
      for (AbstractMemberModel_V001 member : box.members(compartment)) {
        boolean isShown = !member.hidden && !member.hiddenMarker && !isRemoved(member.change);
        if (member.id != null && (ownEndId.equals(member.id) || isShown && isFree)) {
          ends.put(member.id, endArea(member.id));
        }
      }
    }
  }

  /** Takes away the blank lines for new attributes and methods.
   * @return true if there was one */
  private boolean discardPendingMembers() {
    boolean gone = false;
    for (MemberEditing<?, ?> editing : memberEditings.values()) {
      gone = editing.discardPending() || gone;
    }
    return gone;
  }

  /** @return the class which is the end of a dependency, or owns the attribute or method which is, null if there is none */
  private ClassModel_V001 ownerOfEnd(String endId) {
    for (ClassModel_V001 box : model.classes()) {
      boolean isEnd = endId.equals(box.id) || box.allMembers().stream().anyMatch(member -> endId.equals(member.id));
      if (isEnd) {
        return box;
      }
    }
    return null;
  }

  private static boolean isRemoved(ChangeModel_V001 change) {
    return change != null && change.type == ChangeType_V001.REMOVED;
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
    if (band != null) {
      band.to(at);
      return;
    }
    if (drag != null) {
      drag.to(at, snapTolerance);
      if (drag.hasMoved() && !(drag instanceof MemberReorderDrag)) {
        selectedMember = null;
      }
      if (lineEndRepair != null) {
        lineEndRepair.reset();
      }
      scene = host.renderer().layout(model);
      if (lineEndRepair != null) {
        scene = lineEndRepair.apply(scene, () -> host.renderer().layout(model));
      }
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
    if (band != null) {
      finishBand();
      return;
    }
    Drag ended = drag;
    drag = null;
    lineEndRepair = null;
    if (ended instanceof GroupDrag groupDrag && !groupDrag.hasMoved()) {
      select(groupDrag.pressedBox());
      return;
    }
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
    if (band != null) {
      band = null;
      return true;
    }
    Drag cancelled = drag;
    drag = null;
    if (cancelled == null || !cancelled.hasMoved()) {
      lineEndRepair = null;
      return false;
    }
    cancelled.cancel();
    if (lineEndRepair != null) {
      lineEndRepair.reset();
      lineEndRepair = null;
    }
    scene = host.renderer().layout(model);
    return true;
  }

  /** Selects the boxes in the rectangle, together with those which were kept if it adds to the selection. A rectangle
   * which adds to the selection and hasn't been pulled open leaves the selection as it is. */
  private void finishBand() {
    RubberBand finished = band;
    band = null;
    if (finished.isAdditive() && !finished.hasMoved()) {
      return;
    }
    List<AbstractBoxModel_V001> boxes = new ArrayList<>(finished.keptBoxes());
    boxes.addAll(boxesInside(finished.rectangle()));
    selectBoxes(inDrawingOrder(boxes));
  }

  /** @return the boxes of the diagram which lie completely inside of the area, in the order of drawing */
  private List<AbstractBoxModel_V001> boxesInside(Rectangle2D area) {
    List<AbstractBoxModel_V001> inside = new ArrayList<>();
    for (AbstractBoxModel_V001 box : model.boxes()) {
      SceneElementArea boxArea = box == pendingBox ? null : scene.elementAreaOf(box);
      if (boxArea != null && area.contains(boxArea.area)) {
        inside.add(box);
      }
    }
    return inside;
  }

  public boolean isRelationToolArmed() { return armedTool == Tool.RELATION; }

  /** @return the type of the relation which the relation tool draws, the last one which it was armed with */
  public RelationType_V001 armedRelationType() { return armedRelationType; }

  public boolean isAnchorToolArmed() { return armedTool == Tool.ANCHOR; }

  public boolean isAssocClassToolArmed() { return armedTool == Tool.ASSOC_CLASS; }

  public boolean isDependencyToolArmed() { return armedTool == Tool.DEPENDENCY; }

  public boolean isFormatPainterArmed() { return armedTool == Tool.FORMAT_PAINTER; }

  /** @return true if a class, note or relation is selected, whose style the format painter can pick up */
  public boolean canPickUpFormat() {
    return selected != null && !selected.styleProperties().isEmpty();
  }

  /** Arms the format painter with the style of the selected element, or disarms it if it is armed. Nothing happens if
   * no element which has a style is selected. */
  public void toggleFormatPainter() {
    if (armedTool == Tool.FORMAT_PAINTER) {
      disarmTool();
    }
    else if (canPickUpFormat()) {
      formatPainter.pickUp(selected);
      armedTool = Tool.FORMAT_PAINTER;
      drawing = null;
    }
  }

  /** Gives the class, note or relation at the point, or the label of the relation, the style which the format painter
   * has picked up, as far as it has the same parts, and leaves the tool. The target is selected. One operation, which
   * is none if the target looks like that already. Nothing happens, and the tool stays armed so that the user can try
   * again, if there is no such element at the point or a change set has removed it. */
  public void paintFormatAt(Point2D at, double tolerance) {
    AbstractDiagramElementModel_V001 target = formatTargetAt(at, tolerance);
    if (target == null || !canChangeStyle(target).can()) {
      return;
    }
    setStyle(target, formatPainter.styleFor(target));
    select(target);
    armedTool = Tool.NONE;
  }

  private AbstractDiagramElementModel_V001 formatTargetAt(Point2D at, double tolerance) {
    SceneElementArea box = scene.elementAreaAt(at);
    if (box != null) {
      return box.element;
    }
    SceneTextArea text = scene.textAreaAt(at);
    if (text != null && text.element instanceof AbstractRelationModel_V001 relation) {
      return relation;
    }
    SceneConnectionLine line = scene.connectionLineAt(at, tolerance);
    return line instanceof SceneRelationLine relationLine ? relationLine.relation : null;
  }

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

  /** Arms the tool which draws a dependency between classes and attributes, or disarms it if it is armed. */
  public void toggleDependencyTool() {
    toggle(Tool.DEPENDENCY);
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
        drawing = new ConnectionDrawing(from, finder, at);
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
        drawing = new ConnectionDrawing(new NoteTarget(from, notes.get(from)), targets::closestForAnchor, at);
      }
    }
    else if (armedTool == Tool.ASSOC_CLASS) {
      ClassTarget from = targets.closestClass(at, tolerance, box -> AssocClassRules.canBeAssocClass(model, box));
      if (from != null) {
        TargetFinder finder = (point, distance) -> targets.closestRelation(point, distance,
          relation -> AssocClassRules.canTake(model, relation, from.box));
        drawing = new ConnectionDrawing(from, finder, at);
      }
    }
    else if (armedTool == Tool.DEPENDENCY) {
      ConnectionTarget from = targets.closestForDependency(at, tolerance, DependencyRules::canStartAt);
      if (from != null) {
        ClassModel_V001 fromBox = from instanceof MemberTarget attribute ? attribute.owner : ((ClassTarget) from).box;
        TargetFinder finder = (point, distance) -> targets.closestForDependency(point, distance,
          (box, attribute) -> DependencyRules.canEndAt(fromBox, box, attribute));
        drawing = new ConnectionDrawing(from, finder, at);
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
    else if (armedTool == Tool.DEPENDENCY) {
      createDependency(finished);
    }
    else {
      linkAssocClass(finished);
    }
  }

  /** Creates the relation of the armed type, a straight line without texts. An association is directed, the others
   * have their marks (diamond, triangle) and no arrowhead of their own. */
  private void createRelation(ConnectionDrawing finished) {
    String fromId = finished.start().ensureId(this::newId);
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
    select(relation);
    armedTool = Tool.NONE;
    publish();
  }

  /** Creates a dependency from the class or attribute where the drawing started to the one where the mouse was
   * released, a straight line without a label. Both get an ID if they have none. The dependency is selected, and marked as
   * added while changes are tracked. */
  private void createDependency(ConnectionDrawing finished) {
    String fromId = finished.start().ensureId(this::newId);
    String toId = finished.target().ensureId(this::newId);
    DependencyRelationModel_V001 dependency = new DependencyRelationModel_V001(fromId, toId);
    dependency.id = newId();
    markAsAdded(dependency);
    model.elements.add(dependency);
    select(dependency);
    armedTool = Tool.NONE;
    publish();
  }

  /** Attaches the note to the class, attribute or relation which the mouse was released on, with an anchor line. A
   * class or attribute which has no ID gets one, and so does the note. The note is selected. A note can have several
   * anchor lines, so lines it has already stay as they are. Nothing happens, and no operation is made, if it is
   * attached to that target already. The new line is marked as added while changes are tracked. */
  private void anchorNote(ConnectionDrawing finished) {
    attach((NoteModel_V001) finished.start().element(), finished.target(), AnchorLineModel_V001::new);
  }

  /** Makes the class the association class of the relation which the mouse was released on, with an association class
   * link. The class and the relation get an ID if they have none. The class is selected. The tool only starts at
   * a class which is no association class yet, see {@link AssocClassRules}. */
  private void linkAssocClass(ConnectionDrawing finished) {
    ClassModel_V001 assocClass = (ClassModel_V001) finished.start().element();
    attach(assocClass, finished.target(), AssocClassLinkModel_V001::new);
  }

  /** Attaches the box to the target with a new dashed line, unless a live line leads there already.
   * @param newLine creates a line from the ID of the box to the ID of the target */
  private void attach(AbstractBoxModel_V001 box, ConnectionTarget target,
                      BiFunction<String, String, AttachLineModel_V001> newLine) {
    String targetId = target.ensureId(this::newId);
    armedTool = Tool.NONE;
    select(box);
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

  /** Deletes the selected box, or all selected boxes, and what hangs on them, and selects nothing afterwards. It is one
   * operation. While changes are tracked, what the change set has added is removed and the rest is marked as removed,
   * so that the deletion can be rejected. */
  public void deleteSelected() {
    if (selectedMember != null) {
      deleteSelectedMember();
      return;
    }
    if (selected == null && group.isEmpty()) {
      return;
    }
    if (selected != null) {
      delete(selected);
    }
    for (AbstractBoxModel_V001 box : group.members()) {
      if (model.boxes().contains(box)) {
        delete(box);
      }
    }
    publish();
    select(null);
  }

  private void delete(AbstractDiagramElementModel_V001 element) {
    String changeSet = host.trackedChangeSet();
    if (changeSet != null) {
      deleter.delete(model, element, changeSet);
    }
    else {
      deleter.delete(model, element);
    }
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
   * Its method compartment is hidden for a class and an enum, shown for an interface.
   * @return the area of the name */
  public SceneTextArea newClass(int x, int y, ClassKind_V001 kind) {
    ClassModel_V001 box = new ClassModel_V001("", kind);
    box.methodsVisibility = kind.initialMethodsVisibility();
    return putPendingBox(box, x, y, TextKind.CLASS_NAME);
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
    select(box);
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
      select(null);
    }
    pendingBox = null;
    return true;
  }

  /** The IDs of the elements of the diagram including its attributes and methods, which notes attach to. */
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
      for (AbstractMemberModel_V001 member : box.allMembers()) {
        if (member.id != null) {
          used.add(member.id);
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
      select(null);
    }
    if (selectedMember != null && rowOf(selectedMember) == null) {
      selectedMember = null;
    }
    if (!group.isEmpty()) {
      group.retain(this::isDrawn);
      selectBoxes(group.members());
    }
    if (!newSource.equals(source)) {
      undoableSteps++;
      redoableSteps = 0;
    }
    source = newSource;
    host.sourceChanged(source);
  }

  /** Boxes, dashed lines and dependencies which hang on hidden attributes only are not in the picture, so they can't
   * stay selected. */
  private boolean isDrawn(AbstractDiagramElementModel_V001 element) {
    if (element instanceof AbstractBoxModel_V001) {
      return scene.elementAreaOf(element) != null;
    }
    if (element instanceof AttachLineModel_V001 attachLine) {
      return scene.attachLineOf(attachLine) != null;
    }
    if (element instanceof AbstractRelationModel_V001) {
      return scene.relationLineOf(element) != null;
    }
    return true;
  }
}
