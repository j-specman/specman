package headless.plantuml;

import org.junit.jupiter.api.Test;
import specman.editarea.EditContainer;
import specman.editarea.plantuml.PlantUmlImageEditArea;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.ops.AddImageSpecmanOp.EMPTY_PLANTUML;

class PlantUmlImageTest extends AbstractPlantUmlTest {

  @Test
  void testToolbarButtonInsertsEmptyDiagramBehindText() {
    EditContainer container = containerOfNewStep();

    PlantUmlImageEditArea diagram = insertDiagramByToolbarButton(container);

    assertEquals(EMPTY_PLANTUML, diagram.getSource());
    assertTrue(diagram.getFullSizeImage().getWidth() > 0, "Even an empty diagram must have an extent");
    assertTrue(diagram.getTotalScalePercent() > 0, "The diagram must be shown on screen");
  }

  @Test
  void testFirstContentBelongsToTheInsertionSoOneUndoTakesTheWholeDiagramOut() {
    EditContainer container = containerOfNewStep();
    PlantUmlImageEditArea diagram = insertDiagramByToolbarButton(container);

    diagram.commitSource(TWO_CLASSES);
    assertEquals(TWO_CLASSES, diagram.getSource());

    specman.undo();
    assertFalse(hasDiagram(container), "A single undo must take the diagram out");

    specman.redo();
    assertTrue(hasDiagram(container), "Redo must bring the diagram back");
    assertEquals(TWO_CLASSES, diagram.getSource(), "...with its content");
  }

  @Test
  void testLeavingTheEditorWithoutContentTakesTheDiagramOutWithoutTrace() {
    EditContainer container = containerOfNewStep();
    PlantUmlImageEditArea diagram = insertDiagramByToolbarButton(container);

    diagram.editorClosed();

    assertFalse(hasDiagram(container), "The diagram must be gone");
    assertFalse(specman.getUndoManager().canRedo(), "The insertion must not be redoable");
    assertTrue(specman.getUndoManager().canUndo(), "The step created before must still be undoable");
  }

  @Test
  void testChangedSourceCanBeUndoneAndRedone() {
    PlantUmlImageEditArea diagram = insertDiagramByToolbarButton(containerOfNewStep());
    diagram.commitSource(TWO_CLASSES);
    String originalSize = sizeOf(diagram);

    diagram.changeSource(THREE_CLASSES);
    assertEquals(THREE_CLASSES, diagram.getSource());
    assertNotEquals(originalSize, sizeOf(diagram), "The diagram must be rendered anew");

    specman.undo();
    assertEquals(TWO_CLASSES, diagram.getSource());
    assertEquals(originalSize, sizeOf(diagram));

    specman.redo();
    assertEquals(THREE_CLASSES, diagram.getSource());
  }

  private static String sizeOf(PlantUmlImageEditArea diagram) {
    return diagram.getFullSizeImage().getWidth() + "x" + diagram.getFullSizeImage().getHeight();
  }
}
