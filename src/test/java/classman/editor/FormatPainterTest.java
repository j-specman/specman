package classman.editor;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.FontFamily_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatPainterTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  private static final String STYLED = "style(fill=\"#336699\", textcolor=\"#102030\", linecolor=\"#cc0000\", "
    + "font=CourierPrime, fontsize=16)";
  private static final String DIAGRAM = "classdiagram {\n  class A (aaaaaaaa) at(10, 10) " + STYLED + "\n"
    + "  class B (bbbbbbbb) at(10, 300)\n  note (dddddddd) at(300, 10) \"n\"\n"
    + "  association (cccccccc) aaaaaaaa -> bbbbbbbb \"label\"\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
  }

  private Point2D inBox(int index) {
    Rectangle2D area = editing.scene().elementAreas.get(index).area;
    return new Point2D.Double(area.getCenterX(), area.getMinY() + 4);
  }

  private Point2D onRelationLine() {
    List<Point2D> points = editing.scene().relationLines.get(0).points();
    return new Point2D.Double(points.get(0).getX(), points.get(0).getY() + 6);
  }

  private ClassDiagramModel_V001 parsed() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source());
  }

  private void pickUpAt(Point2D source) {
    editing.selectAt(source, TOLERANCE);
    editing.toggleFormatPainter();
  }

  /** The style of a class goes to another class as a whole, in one operation which isn't marked while changes are
   * tracked, and the painter is put away. The target is selected. */
  @Test
  void classToClass() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(DIAGRAM);
    pickUpAt(inBox(0));
    assertTrue(editing.isFormatPainterArmed());

    editing.paintFormatAt(inBox(1), TOLERANCE);

    ElementStyleModel_V001 painted = parsed().classes().get(1).style;
    assertEquals(parsed().classes().get(0).style, painted);
    assertEquals(FontFamily_V001.COURIER, painted.font);
    assertEquals(1, host.reportedSources.size());
    assertFalse(editing.source().contains("change("), editing.source());
    assertFalse(editing.isFormatPainterArmed());
    assertEquals("B", editing.selectedClass().name);
  }

  /** A class gives a note all five parts, a relation only four: the fill is neither taken nor given between them. */
  @Test
  void onlyCommonPartsAreGiven() throws ClassDiagramParseException {
    open(DIAGRAM);
    pickUpAt(inBox(0));

    editing.paintFormatAt(onRelationLine(), TOLERANCE);

    ElementStyleModel_V001 relationStyle = parsed().relations().get(0).style;
    assertNull(relationStyle.fill);
    assertEquals("#cc0000", relationStyle.lineColor);
    assertEquals("#102030", relationStyle.textColor);
    assertEquals(16, relationStyle.fontSize);

    pickUpAt(inBox(0));
    editing.paintFormatAt(inBox(2), TOLERANCE);

    assertEquals("#336699", parsed().notes().get(0).style.fill, "a note has a fill like a class");

    pickUpAt(onRelationLine());
    editing.paintFormatAt(inBox(2), TOLERANCE);

    assertEquals("#336699", parsed().notes().get(0).style.fill, "a relation has no fill to give, so the note keeps its own");
  }

  /** A standard style takes a style back, like in PowerPoint. */
  @Test
  void standardStyleResetsTheTarget() throws ClassDiagramParseException {
    open(DIAGRAM);
    pickUpAt(inBox(1));

    editing.paintFormatAt(inBox(0), TOLERANCE);

    assertTrue(parsed().classes().get(0).style.isStandard());
  }

  /** Where there is nothing to paint, and on what a change set has removed, nothing happens and the painter stays
   * armed, so the user can try again; a press on the label of a relation paints the relation. */
  @Test
  void staysArmedWithoutTarget() throws ClassDiagramParseException {
    open(DIAGRAM.replace("class B (bbbbbbbb)", "class B (bbbbbbbb) change(removed, yellow)"));
    pickUpAt(inBox(0));

    editing.paintFormatAt(new Point2D.Double(2000, 2000), TOLERANCE);
    editing.paintFormatAt(inBox(1), TOLERANCE);

    assertTrue(editing.isFormatPainterArmed());
    assertTrue(host.reportedSources.isEmpty());

    Rectangle2D label = editing.scene().textAreas.stream().filter(text -> "label".equals(
      text.element instanceof classman.model.v001.AbstractRelationModel_V001 relation ? relation.label : null))
      .findFirst().orElseThrow().area;
    editing.paintFormatAt(new Point2D.Double(label.getCenterX(), label.getCenterY()), TOLERANCE);

    assertEquals("#cc0000", parsed().relations().get(0).style.lineColor);
    assertFalse(editing.isFormatPainterArmed());
  }

  /** Nothing can be picked up without a selection or from what has no style, and the button toggles the tool. */
  @Test
  void pickUpNeedsAStyledElement() throws ClassDiagramParseException {
    open(DIAGRAM);
    assertFalse(editing.canPickUpFormat());
    editing.toggleFormatPainter();
    assertFalse(editing.isFormatPainterArmed());

    editing.selectAt(inBox(0), TOLERANCE);
    assertTrue(editing.canPickUpFormat());
    editing.toggleFormatPainter();
    assertTrue(editing.isFormatPainterArmed());
    editing.toggleFormatPainter();
    assertFalse(editing.isFormatPainterArmed());
    assertNotNull(editing.selectedClass());
  }

  /** A press on the diagram paints while the painter is armed, instead of selecting or moving, and the cursor is a hand. */
  @Test
  void pressPaints() throws ClassDiagramParseException {
    open(DIAGRAM);
    PointerControl control = new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels);
    pickUpAt(inBox(0));

    assertEquals(java.awt.Cursor.HAND_CURSOR, control.cursorAt(inBox(1)).getType());
    control.pressed(inBox(1));
    control.released(inBox(1));

    assertEquals(parsed().classes().get(0).style, parsed().classes().get(1).style);
  }
}
