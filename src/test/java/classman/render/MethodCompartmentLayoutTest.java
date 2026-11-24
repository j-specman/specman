package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.util.List;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The method compartment of a class box, below the attribute compartment. */
class MethodCompartmentLayoutTest {
  private static final String YELLOW = "#ffff00";
  private static final String BLUE = "#0000ff";

  private final ClassDiagramLayouter layouter = new ClassDiagramLayouter(new FixedTextMeasurer(),
    changeSet -> "yellow".equals(changeSet) ? YELLOW : BLUE);

  private Scene layout(String elements) throws ClassDiagramParseException {
    return layouter.layout(new ClassDiagramParser_V001().parse("classdiagram {\n" + elements + "\n}"));
  }

  private static long linesOf(Scene scene, TextKind kind) {
    return scene.textAreas.stream().filter(text -> text.kind == kind).count();
  }

  private static double heightOf(Scene scene) {
    return all(scene, SceneRectangle.class).get(0).height;
  }

  /** The methods stand in a compartment of their own, below the attributes and divided from them by a line. */
  @Test
  void compartmentBelowAttributes() throws ClassDiagramParseException {
    Scene scene = layout("class A {\n  -x : int\n  +run(a: int) : void\n}");

    assertEquals(List.of("A", "-x : int", "+run(a: int) : void"), texts(scene));
    assertEquals(2, all(scene, SceneLine.class).size());
    assertEquals(1, linesOf(scene, TextKind.ATTRIBUTE_LINE));
    assertEquals(1, linesOf(scene, TextKind.METHOD_LINE));
  }

  /** A class which has no methods and doesn't say showmethods has no method compartment, which keeps the box as it was
   * before methods came; with a method, or showmethods, it has one. */
  @Test
  void automaticCompartment() throws ClassDiagramParseException {
    Scene without = layout("class A {\n  -x\n}");
    Scene withMethod = layout("class A {\n  -x\n  run()\n}");
    Scene forced = layout("class A showmethods {\n  -x\n}");

    assertEquals(0, linesOf(without, TextKind.METHOD_LINE));
    assertEquals(1, linesOf(withMethod, TextKind.METHOD_LINE));
    assertEquals(heightOf(without) + 2 * DiagramStyle.PADDING_Y + 20, heightOf(forced), 0.001);
  }

  /** A shown compartment without methods has one blank line for the first method, like the attributes have. */
  @Test
  void blankLineWithoutMethods() throws ClassDiagramParseException {
    Scene scene = layout("class A showmethods {\n  -x\n}");

    List<SceneTextArea> methodLines = scene.textAreas.stream().filter(text -> text.kind == TextKind.METHOD_LINE).toList();
    assertEquals(1, methodLines.size());
    assertEquals(null, methodLines.get(0).member);
  }

  /** hidemethods leaves the attributes where they are, and the box is as high as without any method compartment. */
  @Test
  void hiddenCompartment() throws ClassDiagramParseException {
    Scene shown = layout("class A {\n  -x\n  run()\n}");
    Scene hidden = layout("class A hidemethods {\n  -x\n  run()\n}");

    assertEquals(List.of("A", "-x"), texts(hidden));
    assertEquals(0, linesOf(hidden, TextKind.METHOD_LINE));
    assertEquals(heightOf(shown) - 2 * DiagramStyle.PADDING_Y - 20, heightOf(hidden), 0.001);
  }

  /** An interface has no attributes: its box has the methods compartment only. */
  @Test
  void interfaceHasMethodsOnly() throws ClassDiagramParseException {
    Scene scene = layout("interface Runnable {\n  +run()\n}");

    assertEquals(List.of("<<interface>>", "Runnable", "+run()"), texts(scene));
    assertEquals(1, all(scene, SceneLine.class).size());
    assertEquals(0, linesOf(scene, TextKind.ATTRIBUTE_LINE));
    assertEquals(1, linesOf(scene, TextKind.METHOD_LINE));
  }

  /** A method which is hidden on its own is stood for by the line ". . ." under the methods which are shown. */
  @Test
  void hiddenMethodReplacedByEllipsis() throws ClassDiagramParseException {
    Scene scene = layout("class A {\n  -x\n  run() (hidden)\n  stop()\n}");

    assertEquals(List.of("A", "-x", "stop()", ". . ."), texts(scene));
  }

  /** The marks of a change set on a method can't be hidden by the compartment. */
  @Test
  void markedMethodsStayShown() throws ClassDiagramParseException {
    Scene scene = layout("class A hidemethods {\n  -x\n  run() change(added, yellow)\n}");

    assertEquals(List.of("A", "-x", "run()"), texts(scene));
  }

  /** An added method gets a colored row, a changed parameter is marked on its own. */
  @Test
  void marksOfMethods() throws ClassDiagramParseException {
    Scene added = layout("class A {\n  +run() change(added, yellow)\n}");
    Scene changed = layout("class A {\n  +run(a: int was(yellow, \"a: long\"))\n}");

    assertTrue(all(added, SceneRectangle.class).stream().anyMatch(rectangle -> YELLOW.equals(rectangle.fill)));
    SceneText line = SceneTestHelper.textNamed(changed, "+run(a: int)");
    assertTrue(all(changed, SceneRectangle.class).stream()
      .anyMatch(rectangle -> YELLOW.equals(rectangle.fill) && rectangle.x > line.left()
        && rectangle.x + rectangle.width < line.left() + line.width));
  }

  /** A note can be attached to a method, the box around its text is where the dashed line ends. */
  @Test
  void noteAttachedToMethod() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) {\n  +run() (0a0a0a0a)\n}\nnote (bbbbbbbb) at(200, 0) \"n\"\n"
      + "anchor bbbbbbbb -- 0a0a0a0a");

    assertEquals(1, scene.attachLines.size());
    assertTrue(scene.attachTargets.find("0a0a0a0a").box().height > 0);
  }

  /** What is attached to a method of a hidden compartment is not drawn: a note which hangs on nothing else, and its line. */
  @Test
  void noteOnHiddenMethodIsHidden() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) hidemethods {\n  +run() (0a0a0a0a)\n}\nnote (bbbbbbbb) at(200, 0) \"n\"\n"
      + "anchor bbbbbbbb -- 0a0a0a0a");

    assertEquals(1, scene.elementAreas.size());
    assertTrue(scene.attachLines.isEmpty());
  }
}
