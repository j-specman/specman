package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static classman.render.SceneTestHelper.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A stereotype which refers to a configuration of the host shows the label which the host gives, at the place of a
 * stereotype of its own. */
class StereotypeReferenceLayoutTest {
  private static final Map<String, StereotypeLook> LOOKS = Map.of(
    "Domain_Value", new StereotypeLook("Domain Value", null),
    "IconOnly", new StereotypeLook("", null));
  private static final ClassDiagramLayouter LAYOUTER =
    new ClassDiagramLayouter(new FixedTextMeasurer(), ChangeColors.NONE, LOOKS::get);

  private static List<String> textsOf(String elements) throws ClassDiagramParseException {
    return texts(LAYOUTER.layout(new ClassDiagramParser_V001().parse("classdiagram {\n" + elements + "\n}")));
  }

  @Test
  void labelOfConfigurationIsShown() throws ClassDiagramParseException {
    assertTrue(textsOf("class A <<@Domain_Value>>").contains("<<Domain Value>>"));
  }

  @Test
  void unknownConfigurationIsShownByItsName() throws ClassDiagramParseException {
    assertTrue(textsOf("class A <<@Gone>>").contains("<<Gone>>"));
  }

  @Test
  void configurationWithoutLabelShowsNoStereotypeText() throws ClassDiagramParseException {
    assertEquals(List.of("A"), textsOf("class A <<@IconOnly>>"));
  }

  @Test
  void interfaceKeepsDerivedStereotypeUntilLabelIsVisible() throws ClassDiagramParseException {
    assertTrue(textsOf("interface A <<@IconOnly>>").contains("<<interface>>"));

    List<String> withLabel = textsOf("interface A <<@Domain_Value>>");
    assertTrue(withLabel.contains("<<Domain Value>>"));
    assertFalse(withLabel.contains("<<interface>>"));
  }
}
