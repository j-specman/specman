package classman.editor;

import classman.render.DiagramStyle;
import classman.render.StereotypeLook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The colors which an element has without a color of its own, which the style dialog shows for "default". A class
 * whose stereotype refers to a configuration has the colors of the configuration where it has some. */
class StandardColorsTest {

  @Test
  void classWithoutConfigurationHasTheColorsOfTheClass() {
    StandardColors colors = StandardColors.forClass(null);

    assertEquals(DiagramStyle.CLASS_FILL, colors.fill);
    assertEquals(DiagramStyle.TEXT_COLOR, colors.text);
    assertEquals(DiagramStyle.STROKE, colors.line);
  }

  @Test
  void configurationGivesItsColors() {
    StandardColors colors = StandardColors.forClass(new StereotypeLook("x", null).withColors("#112233", "#445566", "#778899"));

    assertEquals("#112233", colors.fill);
    assertEquals("#778899", colors.text);
    assertEquals("#445566", colors.line);
  }

  @Test
  void configurationLeavesColorsAtTheDefaultOfTheClass() {
    StandardColors colors = StandardColors.forClass(new StereotypeLook("x", null).withColors(null, "#445566", null));

    assertEquals(DiagramStyle.CLASS_FILL, colors.fill);
    assertEquals(DiagramStyle.TEXT_COLOR, colors.text);
    assertEquals("#445566", colors.line);
  }

  @Test
  void noteAndRelationHaveTheirOwn() {
    assertEquals(DiagramStyle.NOTE_FILL, StandardColors.NOTE.fill);
    assertNull(StandardColors.RELATION.fill);
    assertEquals(DiagramStyle.STROKE, StandardColors.RELATION.line);
  }
}
