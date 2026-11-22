package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationTextPlaceholderTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n";
  private static final String BARE = CLASSES + "  association (33333333) 11111111 -- 22222222\n}";
  private static final double TOLERANCE = 4;

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final AtomicInteger textEdits = new AtomicInteger();
  private DiagramEditing editing;
  private PointerControl control;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    control = new PointerControl(editing, () -> { }, text -> textEdits.incrementAndGet(), at -> { }, pixels -> pixels);
  }

  private void selectRelation() {
    var points = editing.scene().relationLines.get(0).points();
    editing.selectAt(new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2), TOLERANCE);
  }

  private SceneTextArea placeholderOf(TextKind kind) {
    return editing.scene().placeholderAreas.stream().filter(area -> area.kind == kind).findFirst().orElseThrow();
  }

  private List<SceneTextArea> relationPlaceholders() {
    return editing.scene().placeholderAreas.stream().filter(area -> area.element instanceof AbstractRelationModel_V001)
      .toList();
  }

  private static Point2D centerOf(SceneTextArea area) {
    return new Point2D.Double(area.area.getCenterX(), area.area.getCenterY());
  }

  /** A relation which has no texts shows a place for the label and both multiplicities, when it is selected. */
  @Test
  void placesOfMissingTexts() throws ClassDiagramParseException {
    open(BARE);
    assertTrue(editing.selectedPlaceholders().isEmpty());

    selectRelation();

    assertEquals(3, editing.selectedPlaceholders().size());
  }

  /** The places don't count as texts of the diagram. */
  @Test
  void placesAreNoTexts() throws ClassDiagramParseException {
    open(BARE);

    assertTrue(editing.scene().textAreas.stream().noneMatch(area -> area.element instanceof AbstractRelationModel_V001));
    assertEquals(3, relationPlaceholders().size());
  }

  /** An inheritance has no multiplicities, only a place for the label. */
  @Test
  void inheritanceOnlyLabel() throws ClassDiagramParseException {
    open(CLASSES + "  extends 11111111 -> 22222222\n}");
    selectRelation();

    assertEquals(1, editing.selectedPlaceholders().size());
    assertEquals(TextKind.RELATION_LABEL, relationPlaceholders().get(0).kind);
  }

  /** Only the texts which are missing get a place. */
  @Test
  void placesOnlyWhereMissing() throws ClassDiagramParseException {
    open(CLASSES + "  association (33333333) 11111111 1 -> 22222222 \"owns\"\n}");
    selectRelation();

    assertEquals(1, editing.selectedPlaceholders().size());
    assertEquals(TextKind.TO_MULTIPLICITY, relationPlaceholders().get(0).kind);
  }

  /** A label which a change set has removed counts as missing. */
  @Test
  void removedLabelCountsAsMissing() throws ClassDiagramParseException {
    open(CLASSES + "  association (33333333) 11111111 1 -> 2 22222222 \"\" was(yellow, \"owns\")\n}");
    selectRelation();

    assertEquals(1, editing.selectedPlaceholders().size());
    assertEquals(TextKind.RELATION_LABEL, relationPlaceholders().get(0).kind);
  }

  /** Another relation which is not selected shows no places, and they can't be clicked. */
  @Test
  void onlyForSelectedRelation() throws ClassDiagramParseException {
    open(BARE);
    SceneTextArea label = placeholderOf(TextKind.RELATION_LABEL);

    assertNull(editing.editableTextAt(centerOf(label)));
    control.doubleClicked(centerOf(label));

    assertEquals(0, textEdits.get());
    assertFalse(editing.hasSelection());
  }

  /** A double click on a place starts the field for the text, and the relation stays selected. */
  @Test
  void doubleClickStartsField() throws ClassDiagramParseException {
    open(BARE);
    selectRelation();
    SceneTextArea label = placeholderOf(TextKind.RELATION_LABEL);

    control.pressed(centerOf(label));
    control.released(centerOf(label));
    control.doubleClicked(centerOf(label));

    assertEquals(1, textEdits.get());
    assertEquals(label, editing.editableTextAt(centerOf(label)));
    assertEquals(2, editing.selectedHandles().size());
  }

  /** A text which is typed into a place is created. */
  @Test
  void textCreated() throws ClassDiagramParseException {
    open(BARE);
    selectRelation();

    editing.setText(placeholderOf(TextKind.RELATION_LABEL), "owns");
    assertEquals("owns", relationOf(editing.source()).label);
    selectRelation();
    editing.setText(placeholderOf(TextKind.FROM_MULTIPLICITY), "1");
    selectRelation();
    editing.setText(placeholderOf(TextKind.TO_MULTIPLICITY), "0..*");

    AssociationRelationModel_V001 association = (AssociationRelationModel_V001) relationOf(editing.source());
    assertEquals("1", association.fromMultiplicity);
    assertEquals("0..*", association.toMultiplicity);
    assertTrue(editing.selectedPlaceholders().isEmpty());
  }

  /** An empty text creates nothing and is no step. */
  @Test
  void emptyTextCreatesNothing() throws ClassDiagramParseException {
    open(BARE);
    selectRelation();

    editing.setText(placeholderOf(TextKind.RELATION_LABEL), "");

    assertFalse(editing.canUndo());
  }

  /** While changes are tracked, the new text has the mark of the change set. */
  @Test
  void createdTextTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(BARE);
    selectRelation();

    editing.setText(placeholderOf(TextKind.RELATION_LABEL), "owns");

    AbstractRelationModel_V001 relation = relationOf(editing.source());
    assertNotNull(relation.labelChange);
    assertEquals("yellow", relation.labelChange.changeSet);
  }

  /** The place is a bit away from the line: a double click on the line below it adds a waypoint, not a text. */
  @Test
  void lineNextToPlaceStaysLine() throws ClassDiagramParseException {
    open(BARE);
    selectRelation();
    var points = editing.scene().relationLines.get(0).points();
    Point2D onLine = new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2);

    control.doubleClicked(onLine);

    assertEquals(0, textEdits.get());
    assertEquals(3, editing.selectedHandles().size());
  }

  /** While changes are tracked, the places are filled with the pale color of the change set, otherwise not. */
  @Test
  void placesFilledWhileTracking() throws ClassDiagramParseException {
    open(BARE);
    selectRelation();
    assertNull(editing.selectionMarks().placeholderFill);

    host.trackedChangeSet = "yellow";

    assertNotNull(editing.selectionMarks().placeholderFill);
    assertEquals(editing.trackedFieldColor(), editing.selectionMarks().placeholderFill);
    assertEquals(3, editing.selectionMarks().placeholders.size());
  }

  private static AbstractRelationModel_V001 relationOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source).relations().get(0);
  }
}
