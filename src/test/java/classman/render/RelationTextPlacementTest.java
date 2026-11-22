package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static classman.render.SceneTestHelper.layout;
import static classman.render.SceneTestHelper.textNamed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Texts of relations which keep clear of the boxes, of the line of their own relation and of each other. Boxes
 * without attributes are 40 wide and 56 high, a text line is 20 high with its baseline at 15, a character is 10 wide.
 */
class RelationTextPlacementTest {

  private static Rectangle2D.Double areaOf(SceneText text) {
    return new Rectangle2D.Double(text.left(), text.y - 15, text.width, 20);
  }

  private static boolean touchesLine(Scene scene, Rectangle2D area) {
    ScenePolyline line = scene.relationLines.get(0).line;
    for (int i = 0; i < line.xs.length - 1; i++) {
      if (area.intersectsLine(line.xs[i], line.ys[i], line.xs[i + 1], line.ys[i + 1])) {
        return true;
      }
    }
    return false;
  }

  private static boolean touchesBox(Scene scene, Rectangle2D area) {
    return scene.elementAreas.stream().anyMatch(box -> box.area.intersects(area));
  }

  private static double distanceToLine(Scene scene, Rectangle2D.Double area) {
    ScenePolyline line = scene.relationLines.get(0).line;
    double closest = Double.MAX_VALUE;
    for (int i = 0; i < line.xs.length - 1; i++) {
      closest = Math.min(closest, java.awt.geom.Line2D.ptSegDist(line.xs[i], line.ys[i], line.xs[i + 1], line.ys[i + 1],
          area.getCenterX(), area.getCenterY()));
    }
    return closest;
  }

  private static void assertClear(Scene scene, SceneText text) {
    Rectangle2D.Double area = areaOf(text);
    assertFalse(touchesBox(scene, area), "box covered by " + text.text);
    assertFalse(touchesLine(scene, area), "line covered by " + text.text);
  }

  // ---- slanted lines ----

  /** A label on a line which falls to the right doesn't touch the line. */
  @Test
  void labelClearsFallingLine() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(300, 200)\n"
        + "association aaaaaaaa -- bbbbbbbb \"a long label\"");
    assertClear(scene, textNamed(scene, "a long label"));
  }

  /** A label on a line which rises to the right doesn't touch the line. */
  @Test
  void labelClearsRisingLine() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 200)\nclass B (bbbbbbbb) at(300, 0)\n"
        + "association aaaaaaaa -- bbbbbbbb \"a long label\"");
    assertClear(scene, textNamed(scene, "a long label"));
  }

  /** A label on a steep line, which runs more up and down than sideways, doesn't touch the line. */
  @Test
  void labelClearsSteepLine() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(60, 300)\n"
        + "association aaaaaaaa -- bbbbbbbb \"a long label\"");
    assertClear(scene, textNamed(scene, "a long label"));
  }

  // ---- labels which find another place ----

  /** A box right of a vertical line sends the label to the left of the line. */
  @Test
  void labelOtherSideOfLine() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(150, 250)\n"
        + "class D (dddddddd) at(30, 140)\n"
        + "association aaaaaaaa -- bbbbbbbb \"label\"\n  route aaaaaaaa.bottom@20 (20, 270) bbbbbbbb.left@20");
    SceneText label = textNamed(scene, "label");
    assertEquals(TextAnchor.END, label.anchor);
    assertEquals(14, label.x, 1e-9);
    assertClear(scene, label);
  }

  /** Boxes above and below the middle of a long line move the label along the line. */
  @Test
  void labelAlongLine() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(420, 66)\n"
        + "class D (dddddddd) at(180, 30)\nclass E (eeeeeeee) at(180, 90)\n"
        + "association aaaaaaaa -- bbbbbbbb \"label\"\n"
        + "  route aaaaaaaa.bottom@20 (20, 86) (400, 86) bbbbbbbb.left@20");
    SceneText label = textNamed(scene, "label");
    assertClear(scene, label);
    assertTrue(distanceToLine(scene, areaOf(label)) < 30, "the label is near the line");
  }

  /** A label which fits nowhere stays where it would stand without obstacles. */
  @Test
  void labelWithoutFreePlace() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(100, 0)\n"
        + "association aaaaaaaa -- bbbbbbbb \"a wide label\"");
    SceneText label = textNamed(scene, "a wide label");
    assertEquals(70, label.x, 1e-9);
    assertEquals(22, label.y, 1e-9);
  }

  // ---- multiplicities ----

  /** A box above the line sends a multiplicity below the line. */
  @Test
  void multiplicityOtherSideOfLine() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(300, 0)\n"
        + "class D (dddddddd) at(50, -40)\n"
        + "association aaaaaaaa 0..1 -> 1..* bbbbbbbb");
    SceneText multiplicity = textNamed(scene, "0..1");
    assertEquals(46, multiplicity.x, 1e-9);
    assertEquals(51, multiplicity.y, 1e-9);
    assertClear(scene, multiplicity);
  }

  /** On a short line the two multiplicities don't lie over each other. */
  @Test
  void multiplicitiesOfShortLine() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(100, 0)\n"
        + "association aaaaaaaa 0..1 -> 1..* bbbbbbbb");
    assertFalse(areaOf(textNamed(scene, "0..1")).intersects(areaOf(textNamed(scene, "1..*"))));
  }

  /** The label doesn't lie over a multiplicity. */
  @Test
  void labelKeepsClearOfMultiplicity() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(160, 0)\n"
        + "association aaaaaaaa 0..1 -> 1..* bbbbbbbb \"owns it\"");
    Rectangle2D.Double label = areaOf(textNamed(scene, "owns it"));
    assertFalse(label.intersects(areaOf(textNamed(scene, "0..1"))));
    assertFalse(label.intersects(areaOf(textNamed(scene, "1..*"))));
  }

  /** The place of a missing multiplicity is chosen like the place of a real one. */
  @Test
  void placeholderKeepsClearOfBox() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(300, 0)\n"
        + "class D (dddddddd) at(50, -40)\n"
        + "association aaaaaaaa -> 1..* bbbbbbbb");
    List<Rectangle2D.Double> places = new ArrayList<>();
    scene.placeholderAreas.forEach(place -> places.add(place.area));
    assertTrue(places.stream().noneMatch(place -> scene.elementAreas.stream()
        .anyMatch(box -> box.area.intersects(place))));
  }
}
