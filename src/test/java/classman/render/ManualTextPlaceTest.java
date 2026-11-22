package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static classman.render.SceneTestHelper.layout;
import static classman.render.SceneTestHelper.textNamed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Texts which the user has moved stand relative to the line. A at (0, 0) and B at (300, 0): the line runs from (40, 28)
 * to (300, 28), 260 long. A text line is 20 high with its baseline at 15.
 */
class ManualTextPlaceTest {
  private static final String SIDE_BY_SIDE = "class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(300, 0)\n";

  private static Point2D middleOf(Scene scene, String text) {
    SceneText line = textNamed(scene, text);
    return new Point2D.Double(line.x, line.y - 15 + 10);
  }

  /** The middle of a label which is moved stands at the percent of the line, left of the line is above it. */
  @Test
  void labelAbovePercent() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association aaaaaaaa -- bbbbbbbb \"owns\" at(25%, 20)");
    assertEquals(new Point2D.Double(105, 8), middleOf(scene, "owns"));
  }

  /** A negative distance puts the label right of the line, below a line which runs to the right. */
  @Test
  void labelBelowLine() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association aaaaaaaa -- bbbbbbbb \"owns\" at(50%, -20)");
    assertEquals(new Point2D.Double(170, 48), middleOf(scene, "owns"));
  }

  /** A moved label of several lines stands around its place. */
  @Test
  void multiLineLabelAroundPlace() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association aaaaaaaa -- bbbbbbbb \"first\nsecond\" at(50%, 40)");
    double first = textNamed(scene, "first").y;
    double second = textNamed(scene, "second").y;
    assertEquals(20, second - first, 1e-9);
    assertEquals(-12, (first - 15 + second - 15 + 20) / 2, 1e-9);
  }

  /** A multiplicity is placed along the line from the end where it belongs. */
  @Test
  void multiplicitiesFromTheirEnds() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association aaaaaaaa 0..1 at(14, -8) -> 1..* at(14, 8) bbbbbbbb");
    assertEquals(new Point2D.Double(54, 36), middleOf(scene, "0..1"));
    assertEquals(new Point2D.Double(286, 20), middleOf(scene, "1..*"));
  }

  /** The text follows the line when a class is moved: same percent, same distance. */
  @Test
  void followsRoute() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(300, 200)\n"
        + "association aaaaaaaa -- bbbbbbbb \"owns\" at(50%, 20)");
    PathSpot spot = PathGeometry.spotNear(scene.relationLines.get(0).points(), middleOf(scene, "owns"));
    double length = PathGeometry.length(scene.relationLines.get(0).points());
    assertEquals(length / 2, spot.along, 1e-6);
    assertEquals(20, spot.across, 1e-6);
  }

  /** A text with a place stays there although a box lies at the place: the user has chosen it. */
  @Test
  void notAvoidingBoxes() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "class D (dddddddd) at(150, -20)\n"
        + "association aaaaaaaa -- bbbbbbbb \"owns\" at(50%, 20)");
    Rectangle2D box = scene.elementAreas.get(2).area;
    assertTrue(box.contains(middleOf(scene, "owns")));
  }

  /** The place belongs to the text: the area where the user double clicks is the text there. */
  @Test
  void textAreaAtPlace() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association aaaaaaaa -- bbbbbbbb \"owns\" at(50%, 20)");
    List<SceneTextArea> labels = scene.textAreas.stream().filter(area -> area.kind == TextKind.RELATION_LABEL).toList();
    assertEquals(1, labels.size());
    assertEquals(170, labels.get(0).area.getCenterX(), 1e-9);
    assertEquals(8, labels.get(0).area.getCenterY(), 1e-9);
  }

  /** A text which is moved far from the line gets a leader line to the line, one near the line doesn't. The leader
   * is no part of the picture. */
  @Test
  void leaderOnlyForFarText() throws ClassDiagramParseException {
    Scene near = layout(SIDE_BY_SIDE + "association aaaaaaaa -- bbbbbbbb \"owns\" at(50%, 20)");
    Scene far = layout(SIDE_BY_SIDE + "association aaaaaaaa -- bbbbbbbb \"owns\" at(50%, 60)");

    assertTrue(leadersOf(near).isEmpty());
    List<Line2D.Double> leaders = leadersOf(far);
    assertEquals(1, leaders.size());
    assertEquals(170, leaders.get(0).x1, 1e-9);
    assertEquals(28, leaders.get(0).y1, 1e-9);
    assertEquals(170, leaders.get(0).x2, 1e-9);
    assertTrue(SceneTestHelper.all(far, SceneLine.class).stream().noneMatch(line -> line.dashed));
  }

  private static List<Line2D.Double> leadersOf(Scene scene) {
    SceneRelationLine line = scene.relationLines.get(0);
    Map<TextKind, Rectangle2D> areas = new EnumMap<>(TextKind.class);
    scene.textAreas.stream().filter(text -> text.element == line.relation)
      .forEach(text -> areas.put(text.kind, text.area));
    return TextLeaders.of(line.relation, line.points(), areas);
  }
}
