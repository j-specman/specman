package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.util.List;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.layout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A dependency is a dashed line with an open arrowhead between classes and attributes. */
class DependencyLayoutTest {
  private static final String A_LEFT = "class A (aaaaaaaa) at(0, 0) {\n  first\n  second (0a0a0a0a)\n}\n";
  private static final String B_RIGHT = "class B (bbbbbbbb) at(300, 0) {\n  other (0b0b0b0b)\n}\n";

  private static ScenePolyline line(Scene scene) {
    return scene.relationLines.get(0).line;
  }

  /** The line is dashed and has the arrowhead at the target: a polyline with three points, not dashed. */
  @Test
  void dashedLineWithArrowhead() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa)\nclass B (bbbbbbbb)\ndependency aaaaaaaa -> bbbbbbbb");

    assertTrue(line(scene).dashed);
    List<AbstractSceneElement> decorations = scene.relationLines.get(0).decorations;
    assertEquals(1, decorations.size());
    ScenePolyline arrow = (ScenePolyline) decorations.get(0);
    assertFalse(arrow.dashed);
    assertEquals(3, arrow.xs.length);
    assertEquals(line(scene).xs[1], arrow.xs[1], 1e-9);
    assertEquals(line(scene).ys[1], arrow.ys[1], 1e-9);
  }

  /** From an attribute the line leaves the box around the attribute text at the side facing the target. */
  @Test
  void startsAtAttributeSideFacingTheTarget() throws ClassDiagramParseException {
    Scene scene = layout(A_LEFT + B_RIGHT + "dependency 0a0a0a0a -> bbbbbbbb");

    Rectangle2D.Double attribute = scene.attachTargets.find("0a0a0a0a").box();
    assertEquals(attribute.getMaxX(), line(scene).xs[0], 1e-9);
    assertEquals(attribute.getCenterY(), line(scene).ys[0], 1e-9);
    Rectangle2D.Double target = scene.attachTargets.find("bbbbbbbb").box();
    assertEquals(target.getMinX(), line(scene).xs[1], 1e-6);
  }

  /** Both ends can be attributes, and an end on the left of the other leaves at the other side of its text. */
  @Test
  void attributeToAttribute() throws ClassDiagramParseException {
    Scene scene = layout(A_LEFT + B_RIGHT + "dependency 0b0b0b0b -> 0a0a0a0a");

    Rectangle2D.Double from = scene.attachTargets.find("0b0b0b0b").box();
    Rectangle2D.Double to = scene.attachTargets.find("0a0a0a0a").box();
    assertEquals(from.getMinX(), line(scene).xs[0], 1e-9);
    assertEquals(to.getMaxX(), line(scene).xs[1], 1e-9);
  }

  /** A fixed end on an attribute is a point of the border of its text box. */
  @Test
  void routeAnchoredAtAttribute() throws ClassDiagramParseException {
    Scene scene = layout(A_LEFT + B_RIGHT
      + "dependency 0a0a0a0a -> bbbbbbbb route 0a0a0a0a.top@3 (100, 200) bbbbbbbb.bottom@10");

    Rectangle2D.Double attribute = scene.attachTargets.find("0a0a0a0a").box();
    assertEquals(attribute.getMinX() + 3, line(scene).xs[0], 1e-9);
    assertEquals(attribute.getMinY(), line(scene).ys[0], 1e-9);
    assertEquals(100, line(scene).xs[1], 1e-9);
    assertEquals(3, line(scene).xs.length);
  }

  /** A dependency to a class which isn't in the diagram can't be drawn. */
  @Test
  void unknownEndRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> layout(A_LEFT + "dependency 0a0a0a0a -> deadbeef"));
    assertTrue(e.getMessage().contains("deadbeef"), e.getMessage());
  }

  /** A dependency can't end at a relation. */
  @Test
  void relationAsEndRejected() {
    assertThrows(IllegalArgumentException.class, () -> layout(
      A_LEFT + B_RIGHT + "association (11110000) aaaaaaaa -- bbbbbbbb\ndependency 0a0a0a0a -> 11110000"));
  }

  /** Other relations still can't lead to attributes. */
  @Test
  void associationToAttributeRejected() {
    assertThrows(IllegalArgumentException.class, () -> layout(
      A_LEFT + B_RIGHT + "association aaaaaaaa -> 0b0b0b0b"));
  }

  /** The label of a dependency is placed like the label of any relation, and there are no multiplicities. */
  @Test
  void labelShown() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa)\nclass B (bbbbbbbb)\ndependency aaaaaaaa -> bbbbbbbb \"uses\"");

    assertTrue(SceneTestHelper.texts(scene).contains("uses"));
    assertEquals(1, all(scene, SceneText.class).stream().filter(text -> text.text.equals("uses")).count());
  }

  /** An attribute which is hidden on its own: the line ends at the box of the line ". . .". */
  @Test
  void hiddenAttributeLeadsToMarker() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0) {\n  first\n  second (0a0a0a0a) (hidden)\n}\n" + B_RIGHT
      + "dependency 0a0a0a0a -> bbbbbbbb");

    Rectangle2D.Double marker = scene.attachTargets.find("0a0a0a0a").box();
    assertEquals(1, scene.relationLines.size());
    assertEquals(marker.getMaxX(), line(scene).xs[0], 1e-9);
    assertEquals(marker.getCenterY(), line(scene).ys[0], 1e-9);
  }

  /** The dependency of an attribute in a hidden compartment is not drawn, whichever end it is. */
  @Test
  void hiddenCompartmentHidesDependency() throws ClassDiagramParseException {
    String hiddenA = "class A (aaaaaaaa) at(0, 0) hideattributes {\n  second (0a0a0a0a)\n}\n";
    Scene fromHidden = layout(hiddenA + B_RIGHT + "dependency 0a0a0a0a -> bbbbbbbb");
    Scene toHidden = layout(hiddenA + B_RIGHT + "dependency bbbbbbbb -> 0a0a0a0a");

    assertTrue(fromHidden.relationLines.isEmpty());
    assertTrue(toHidden.relationLines.isEmpty());
    assertTrue(all(fromHidden, ScenePolyline.class).stream().noneMatch(polyline -> polyline.dashed));
  }

  /** Attributes and relations need no ID: hiding a compartment whose attributes have none doesn't hide relations without one. */
  @Test
  void relationsWithoutIdSurviveHiddenCompartment() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0) hideattributes {\n  plain\n}\n" + B_RIGHT
      + "extends aaaaaaaa -> bbbbbbbb\ndependency aaaaaaaa -> bbbbbbbb");

    assertEquals(2, scene.relationLines.size());
  }

  /** A note which hangs only on a hidden dependency is not drawn either. */
  @Test
  void noteOnHiddenDependencyHidden() throws ClassDiagramParseException {
    String hiddenA = "class A (aaaaaaaa) at(0, 0) hideattributes {\n  second (0a0a0a0a)\n}\n";
    Scene scene = layout(hiddenA + B_RIGHT + "dependency (d0000001) 0a0a0a0a -> bbbbbbbb\n"
      + "note (c0000001) at(100, 100) \"n\"\nanchor c0000001 -- d0000001");

    assertEquals(2, scene.elementAreas.size());
    assertTrue(scene.attachLines.isEmpty());
  }

  /** A dependency to the class itself, whose attribute is hidden, is still drawn. */
  @Test
  void dependencyBetweenClassesNotHidden() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0) hideattributes {\n  second (0a0a0a0a)\n}\n" + B_RIGHT
      + "dependency aaaaaaaa -> bbbbbbbb");

    assertEquals(1, scene.relationLines.size());
  }
}
