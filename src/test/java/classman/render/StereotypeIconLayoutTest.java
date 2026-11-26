package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.textNamed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The icon of a configuration which a stereotype refers to stands right of the class name. */
class StereotypeIconLayoutTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\">"
    + "<rect width=\"10\" height=\"10\" fill=\"#0000ff\"/></svg>";
  private static final Map<String, StereotypeLook> LOOKS = Map.of(
    "With_Icon", new StereotypeLook("Domain", VectorImage.of(SVG)),
    "Icon_Only", new StereotypeLook("", VectorImage.of(SVG)),
    "No_Icon", new StereotypeLook("Plain", null));
  private static final FixedTextMeasurer MEASURER = new FixedTextMeasurer();
  private static final ClassDiagramLayouter LAYOUTER =
    new ClassDiagramLayouter(MEASURER, ChangeColors.NONE, LOOKS::get);

  private static Scene layout(String elements) throws ClassDiagramParseException {
    return LAYOUTER.layout(new ClassDiagramParser_V001().parse("classdiagram {\n" + elements + "\n}"));
  }

  private static double boxWidth(Scene scene) {
    return all(scene, SceneRectangle.class).get(0).width;
  }

  @Test
  void iconStandsRightOfNameInItsLine() throws ClassDiagramParseException {
    Scene scene = layout("class Customer at(0, 0) <<:With_Icon>>");
    SceneText name = textNamed(scene, "Customer");

    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);

    assertTrue(icon.x >= name.left() + name.width, "icon " + icon.x + " name ends " + (name.left() + name.width));
    assertEquals(MEASURER.lineHeight(), icon.height);
    assertEquals(icon.height, icon.width);
    assertTrue(icon.y <= name.y && name.y <= icon.y + icon.height, "icon " + icon.y + " name " + name.y);
  }

  /** The icon belongs to the whole class, so it stands right of the longest line of a name which is wrapped, and in the
   * height of the first one. */
  @Test
  void iconStandsRightOfLongestNameLine() throws ClassDiagramParseException {
    Scene scene = layout("class \"Customer Acc\" at(0, 0) width=120 <<:With_Icon>>");
    SceneText first = textNamed(scene, "Customer");
    SceneText second = textNamed(scene, "Acc");

    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);

    assertTrue(second.y > first.y);
    assertEquals(first.left() + first.width + DiagramStyle.STEREOTYPE_ICON_GAP, icon.x, 0.01);
    assertTrue(icon.y <= first.y && first.y <= icon.y + icon.height);
    assertTrue(icon.y + icon.height <= second.y - MEASURER.ascent() + 0.01);
  }

  @Test
  void iconWithoutLabelStillShowsIcon() throws ClassDiagramParseException {
    assertEquals(1, all(layout("class A at(0, 0) <<:Icon_Only>>"), SceneVectorImage.class).size());
  }

  @Test
  void boxIsWiderByIcon() throws ClassDiagramParseException {
    double with = boxWidth(layout("class Customer at(0, 0) <<:With_Icon>>"));
    double without = boxWidth(layout("class Customer at(0, 0) <<:No_Icon>>"));

    assertTrue(with > without, with + " vs " + without);
  }

  @Test
  void nameAndIconAreCenteredTogether() throws ClassDiagramParseException {
    Scene scene = layout("class Customer at(0, 0) <<:With_Icon>>");
    SceneText name = textNamed(scene, "Customer");
    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);

    double left = name.left();
    double right = icon.x + icon.width;
    assertEquals(boxWidth(scene) / 2, (left + right) / 2, 0.01);
  }

  @Test
  void noIconWithoutIconOrFreeTextOrUnknownConfiguration() throws ClassDiagramParseException {
    assertTrue(all(layout("class A at(0, 0) <<:No_Icon>>"), SceneVectorImage.class).isEmpty());
    assertTrue(all(layout("class A at(0, 0) <<Entity>>"), SceneVectorImage.class).isEmpty());
    assertTrue(all(layout("class A at(0, 0) <<:Gone>>"), SceneVectorImage.class).isEmpty());
    assertTrue(all(layout("class A at(0, 0)"), SceneVectorImage.class).isEmpty());
  }

  private static boolean coversIconCenter(SceneRectangle rectangle, SceneVectorImage icon) {
    double cx = icon.x + icon.width / 2;
    double cy = icon.y + icon.height / 2;
    return rectangle.fill != null && rectangle.keepsVisible && rectangle.x <= cx && cx <= rectangle.x + rectangle.width
      && rectangle.y <= cy && cy <= rectangle.y + rectangle.height;
  }

  @Test
  void iconOnlyStereotypeHasAreaOfIcon() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) <<:Icon_Only>>");
    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);

    SceneTextArea area = scene.textAreas.stream().filter(text -> text.kind == TextKind.STEREOTYPE).findFirst()
      .orElseThrow();

    assertTrue(area.area.contains(icon.x + icon.width / 2, icon.y + icon.height / 2), area.area.toString());
  }

  @Test
  void stereotypeWithLabelHasAreaOfItsRow() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) <<:With_Icon>>");
    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);

    SceneTextArea area = scene.textAreas.stream().filter(text -> text.kind == TextKind.STEREOTYPE).findFirst()
      .orElseThrow();

    assertTrue(area.area.getMaxY() <= icon.y + 0.01, "row " + area.area + " icon " + icon.y);
  }

  @Test
  void addedStereotypeMarksIcon() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) <<:Icon_Only>> change(added, yellow)");
    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);

    assertTrue(all(scene, SceneRectangle.class).stream().anyMatch(rectangle -> coversIconCenter(rectangle, icon)));
  }

  @Test
  void changedStereotypeMarksIcon() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) <<:Icon_Only>> was(yellow, \":With_Icon\")");
    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);

    assertTrue(all(scene, SceneRectangle.class).stream().anyMatch(rectangle -> coversIconCenter(rectangle, icon)));
  }

  @Test
  void removedStereotypeStrikesIconThrough() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) <<:Icon_Only>> change(removed, yellow)");
    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);

    assertTrue(all(scene, SceneRectangle.class).stream().anyMatch(rectangle -> coversIconCenter(rectangle, icon)));
    assertTrue(all(scene, SceneLine.class).stream().anyMatch(line -> line.x1 <= icon.x && line.x2 >= icon.x + icon.width
      && line.y1 > icon.y && line.y1 < icon.y + icon.height));
  }

  @Test
  void unmarkedStereotypeLeavesIconUnmarked() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) <<:Icon_Only>>");
    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);

    assertTrue(all(scene, SceneRectangle.class).stream().noneMatch(rectangle -> coversIconCenter(rectangle, icon)));
  }

  @Test
  void interfaceAndEnumGetIconToo() throws ClassDiagramParseException {
    assertEquals(1, all(layout("interface A at(0, 0) <<:With_Icon>>"), SceneVectorImage.class).size());
    assertEquals(1, all(layout("enum A at(0, 0) <<:With_Icon>> {\n  RED\n}"), SceneVectorImage.class).size());
  }
}
