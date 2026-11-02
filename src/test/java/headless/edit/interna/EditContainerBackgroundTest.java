package headless.edit.interna;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.EditArea;
import specman.editarea.EditContainer;

import java.awt.Color;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Internals of the Swing integration, not the user-visible editing: An edit container reports the background of its
 * first edit area. Swing's Component#getBackground() falls back to the parent's background if a component has none
 * of its own, and the parent of the first edit area is the container itself.
 */
@ExtendWith(HeadlessExtension.class)
class EditContainerBackgroundTest {

  @Test
  void testContainerReportsBackgroundOfItsFirstEditArea() {
    EditContainer container = specman.clickNewSimpleStep().getTextShef();
    container.getFirstEditArea().asComponent().setBackground(Color.MAGENTA);

    assertEquals(Color.MAGENTA, container.getBackground());
  }

  @Test
  void testContainerSurvivesFirstEditAreaWithoutBackgroundOfItsOwn() {
    EditContainer container = specman.clickNewSimpleStep().getTextShef();
    EditArea<?> firstArea = container.getFirstEditArea();
    firstArea.asComponent().setBackground(null);
    assertFalse(firstArea.isBackgroundSet());

    assertDoesNotThrow(container::getBackground);
  }
}
