package classman.editor;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.DiagramStyle;
import classman.render.Scene;
import classman.render.SceneRelationLine;
import classman.render.ScenePolyline;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationSelectionTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String RELATED = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  association (33333333) 11111111 -> 22222222\n}";
  private static final double TOLERANCE = 4;

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final DiagramEditing editing;

  RelationSelectionTest() throws ClassDiagramParseException {
    editing = new DiagramEditing(RELATED, host);
  }

  /** A press on the line of a relation selects it, and the line is drawn thicker. */
  @Test
  void selectedLineThicker() {
    ScenePolyline line = lineOf(editing);
    assertEquals(DiagramStyle.STROKE_WIDTH, line.strokeWidth);

    editing.selectAt(onLine(line, 0), TOLERANCE);

    assertTrue(editing.hasSelection());
    assertNull(editing.selectedArea());
    assertEquals(DiagramStyle.SELECTED_LINE_WIDTH, lineOf(editing).strokeWidth);
  }

  /** The line and the marks at its ends are drawn in their places of the drawing order, the marks above the line. */
  @Test
  void thickLineKeepsItsPlaceInTheDrawingOrder() {
    Scene before = editing.scene();
    SceneRelationLine relationBefore = before.relationLines.get(0);
    int lineBefore = before.elements.indexOf(relationBefore.line);
    int arrowBefore = before.elements.indexOf(relationBefore.decorations.get(0));

    editing.selectAt(onLine(lineOf(editing), 0), TOLERANCE);

    Scene after = editing.scene();
    SceneRelationLine relationAfter = after.relationLines.get(0);
    assertEquals(lineBefore, after.elements.indexOf(relationAfter.line));
    assertEquals(arrowBefore, after.elements.indexOf(relationAfter.decorations.get(0)));
    assertTrue(lineBefore < arrowBefore);
  }

  /** The arrowhead is as thick as the line. */
  @Test
  void selectedArrowheadThicker() {
    assertEquals(1, editing.scene().relationLines.get(0).decorations.size());

    editing.selectAt(onLine(lineOf(editing), 0), TOLERANCE);

    ScenePolyline arrow = (ScenePolyline) editing.scene().relationLines.get(0).decorations.get(0);
    assertEquals(DiagramStyle.SELECTED_LINE_WIDTH, arrow.strokeWidth);
  }

  /** The line is hit within the tolerance, not farther away. */
  @Test
  void hitWithinTolerance() {
    ScenePolyline line = lineOf(editing);
    Point2D onTheLine = onLine(line, 0);

    editing.selectAt(new Point2D.Double(onTheLine.getX(), onTheLine.getY() + 3), TOLERANCE);
    assertTrue(editing.hasSelection());

    editing.selectAt(new Point2D.Double(onTheLine.getX(), onTheLine.getY() + 10), TOLERANCE);
    assertFalse(editing.hasSelection());
  }

  /** Next to the border of a box, the box is selected and not the line which starts there. */
  @Test
  void boxWinsOverLine() {
    ScenePolyline line = lineOf(editing);

    editing.selectAt(new Point2D.Double(line.xs[0] - 1, line.ys[0]), TOLERANCE);

    assertTrue(editing.selectedArea().contains(line.xs[0] - 1, line.ys[0]));
    assertEquals(DiagramStyle.STROKE_WIDTH, lineOf(editing).strokeWidth);
  }

  /** A press on empty canvas takes the selection of a relation away. */
  @Test
  void pressElsewhereDeselects() {
    editing.selectAt(onLine(lineOf(editing), 0), TOLERANCE);

    editing.selectAt(new Point2D.Double(150, 300), TOLERANCE);

    assertFalse(editing.hasSelection());
    assertEquals(DiagramStyle.STROKE_WIDTH, lineOf(editing).strokeWidth);
  }

  /** A selected relation is deleted by the Delete key, its classes stay. */
  @Test
  void deletingSelectedRelation() throws ClassDiagramParseException {
    editing.selectAt(onLine(lineOf(editing), 0), TOLERANCE);

    editing.deleteSelected();

    assertEquals(2, new ClassDiagramParser_V001().parse(editing.source()).classes().size());
    assertTrue(new ClassDiagramParser_V001().parse(editing.source()).relations().isEmpty());
  }

  /** The ends of a selected relation are marked. */
  @Test
  void handlesAtTheEnds() {
    assertTrue(editing.selectedHandles().isEmpty());
    ScenePolyline line = lineOf(editing);

    editing.selectAt(onLine(line, 0), TOLERANCE);

    assertEquals(List.of(new Point2D.Double(line.xs[0], line.ys[0]), new Point2D.Double(line.xs[1], line.ys[1])),
      editing.selectedHandles());
  }

  /** Every waypoint of a selected relation is marked as well as the ends. */
  @Test
  void handlesAtWaypoints() throws ClassDiagramParseException {
    DiagramEditing routed = new DiagramEditing("classdiagram {\n  class A (11111111) at(10, 20)\n"
      + "  class B (22222222) at(300, 20)\n  association (33333333) 11111111 -- 22222222\n"
      + "  route 11111111.bottom@10 (20, 150) (200, 150) 22222222.bottom@10\n}", host);
    ScenePolyline line = lineOf(routed);

    routed.selectAt(onLine(line, 1), TOLERANCE);

    assertEquals(4, routed.selectedHandles().size());
    assertTrue(routed.selectedHandles().contains(new Point2D.Double(20, 150)));
    assertTrue(routed.selectedHandles().contains(new Point2D.Double(200, 150)));
  }

  /** A selected box has no handles. */
  @Test
  void noHandlesForBox() {
    ScenePolyline line = lineOf(editing);

    editing.selectAt(new Point2D.Double(line.xs[0] - 1, line.ys[0]), TOLERANCE);

    assertTrue(editing.selectedHandles().isEmpty());
  }

  private static ScenePolyline lineOf(DiagramEditing editing) {
    return editing.scene().relationLines.get(0).line;
  }

  private static Point2D onLine(ScenePolyline line, int part) {
    return new Point2D.Double((line.xs[part] + line.xs[part + 1]) / 2, (line.ys[part] + line.ys[part + 1]) / 2);
  }
}
