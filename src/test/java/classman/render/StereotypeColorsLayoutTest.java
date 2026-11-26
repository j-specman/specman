package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.textNamed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A class whose stereotype refers to a configuration of the host takes the colors of the configuration which the
 * configuration has. A color of the style of the class wins over the one of the configuration, which wins over the
 * default of the class. */
class StereotypeColorsLayoutTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final Map<String, StereotypeLook> LOOKS = Map.of(
    "All", new StereotypeLook("All", null).withColors("#112233", "#445566", "#778899"),
    "Alpha", new StereotypeLook("Alpha", null).withColors("#11223380", null, null),
    "Backgroundless", new StereotypeLook("Plain", null).withColors(null, "#445566", null),
    "Colorless", new StereotypeLook("Colorless", null));
  private static final ClassDiagramLayouter LAYOUTER =
    new ClassDiagramLayouter(new FixedTextMeasurer(), ChangeColors.NONE, LOOKS::get);

  private static Scene layout(String elements) throws ClassDiagramParseException {
    return LAYOUTER.layout(new ClassDiagramParser_V001().parse("classdiagram {\n" + elements + "\n}"));
  }

  private static SceneRectangle box(Scene scene) {
    return all(scene, SceneRectangle.class).get(0);
  }

  @Test
  void colorsOfConfigurationAreTheColorsOfTheClass() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) <<:All>> {\n  -x\n}");

    assertEquals("#112233", box(scene).fill);
    assertEquals("#445566", box(scene).stroke);
    assertEquals("#778899", textNamed(scene, "A").fill);
    assertEquals("#445566", all(scene, SceneLine.class).get(0).stroke);
    assertEquals("#778899", textNamed(scene, "<<All>>").fill);
    assertEquals("#778899", textNamed(scene, "-x").fill);
  }

  @Test
  void ownStyleWinsOverConfiguration() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) style(fill=\"#aaaaaa\", textcolor=\"#bbbbbb\", linecolor=\"#cccccc\") <<:All>>");

    assertEquals("#aaaaaa", box(scene).fill);
    assertEquals("#cccccc", box(scene).stroke);
    assertEquals("#bbbbbb", textNamed(scene, "A").fill);
  }

  @Test
  void ownStyleWinsPerColor() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) style(fill=\"#aaaaaa\") <<:All>>");

    assertEquals("#aaaaaa", box(scene).fill);
    assertEquals("#445566", box(scene).stroke);
    assertEquals("#778899", textNamed(scene, "A").fill);
  }

  @Test
  void colorWhichConfigurationLeavesAtDefaultIsTheDefaultOfTheClass() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) <<:Backgroundless>>");

    assertEquals(DiagramStyle.CLASS_FILL, box(scene).fill);
    assertEquals("#445566", box(scene).stroke);
    assertEquals(null, textNamed(scene, "A").fill);
  }

  @Test
  void configurationWithoutColorsChangesNothing() throws ClassDiagramParseException {
    Scene colorless = layout("class A at(0, 0) <<:Colorless>>");
    Scene plain = layout("class A at(0, 0)");

    assertEquals(box(plain).fill, box(colorless).fill);
    assertEquals(box(plain).stroke, box(colorless).stroke);
  }

  @Test
  void unknownConfigurationChangesNothing() throws ClassDiagramParseException {
    assertEquals(DiagramStyle.CLASS_FILL, box(layout("class A at(0, 0) <<:Gone>>")).fill);
  }

  @Test
  void freeTextStereotypeChangesNothing() throws ClassDiagramParseException {
    assertEquals(DiagramStyle.CLASS_FILL, box(layout("class A at(0, 0) <<All>>")).fill);
  }

  @Test
  void transparentColorOfConfigurationKeepsItsAlpha() throws ClassDiagramParseException {
    assertEquals("#11223380", box(layout("class A at(0, 0) <<:Alpha>>")).fill);
  }

  @Test
  void removedClassVeilsColorsOfConfiguration() throws ClassDiagramParseException {
    Scene scene = layout("class A change(removed, yellow) at(0, 0) <<:All>>");

    String fill = all(scene, SceneRectangle.class).stream().filter(rectangle -> rectangle.fill != null)
      .findFirst().orElseThrow().fill;
    assertTrue(!"#112233".equals(fill), "the removed class is veiled: " + fill);
  }
}
