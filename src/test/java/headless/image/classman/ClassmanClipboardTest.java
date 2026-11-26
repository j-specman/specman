package headless.image.classman;

import classman.clipboard.ClassDiagramClipboardContent;
import classman.clipboard.ClassDiagramTransferable;
import org.junit.jupiter.api.Test;
import specman.editarea.EditContainer;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.metatag.generic.config.MetaTagConfig;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static java.awt.event.KeyEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_C;
import static java.awt.event.KeyEvent.VK_V;
import static java.awt.event.KeyEvent.VK_X;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A diagram which is copied or cut can be pasted into Specman as a diagram and into other programs as a picture. */
class ClassmanClipboardTest extends AbstractClassmanTest {

  private static final MetaTagConfig DOMAIN = new MetaTagConfig("Domain", "Domain Value", null, null, null, null, true);
  private static final String USING_DOMAIN = "classdiagram {\n    class A (aaaaaaaa) <<:Domain>>\n    class B (bbbbbbbb) <<Entity>>\n}\n";

  private static void pressOnDiagram(ClassmanImageEditArea diagram, int keyCode) {
    specman.focus(diagram);
    specman.press(keyCode, CTRL_DOWN_MASK);
  }

  /** Pastes into the text of a new step and returns its container. */
  private static EditContainer pasteIntoNewStep() {
    EditContainer target = containerOfNewStep();
    specman.press(VK_V, CTRL_DOWN_MASK);
    specman.layoutComponents();
    return target;
  }

  private static Transferable clipboardContent() {
    return specman.clipboard().getContents(null);
  }

  /** Scenario: Ctrl+C offers the source for Specman and the picture for other programs. */
  @Test
  void testBothFlavorsOnCopy() throws Exception {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());

    pressOnDiagram(diagram, VK_C);

    Transferable content = clipboardContent();
    assertTrue(content.isDataFlavorSupported(ClassDiagramTransferable.CLASS_DIAGRAM_FLAVOR));
    assertTrue(content.isDataFlavorSupported(DataFlavor.imageFlavor));
    assertEquals(TWO_CLASSES,
      ((ClassDiagramClipboardContent) content.getTransferData(ClassDiagramTransferable.CLASS_DIAGRAM_FLAVOR)).source);
    assertTrue(((BufferedImage) content.getTransferData(DataFlavor.imageFlavor)).getWidth() > 0);
  }

  /** Scenario: The copied diagram is pasted as diagram, not as picture. */
  @Test
  void testPasteAsDiagram() {
    pressOnDiagram(existingDiagram(containerOfNewStep()), VK_C);

    EditContainer target = pasteIntoNewStep();

    assertEquals(TWO_CLASSES, ((ClassmanImageEditArea) target.getEditAreaAt(1)).getSource());
  }

  /** Scenario: Ctrl+X takes the diagram out and puts the same content into the clipboard. Undo brings it back. */
  @Test
  void testCutKeepsFlavors() {
    EditContainer container = containerOfNewStep();
    ClassmanImageEditArea diagram = existingDiagram(container);

    pressOnDiagram(diagram, VK_X);

    assertFalse(hasDiagram(container));
    assertTrue(clipboardContent().isDataFlavorSupported(ClassDiagramTransferable.CLASS_DIAGRAM_FLAVOR));
    assertTrue(clipboardContent().isDataFlavorSupported(DataFlavor.imageFlavor));
    specman.undo();
    specman.layoutComponents();
    assertTrue(hasDiagram(container));
  }

  /** Scenario: The stereotype of a configuration which the document doesn't have stays, and nothing is said. */
  @Test
  void testPasteKeepsUnknownConfig() {
    specman.setMetaTagConfigs(List.of(DOMAIN));
    pressOnDiagram(diagramWithSource(containerOfNewStep(), USING_DOMAIN), VK_C);
    specman.setMetaTagConfigs(List.of());

    EditContainer target = pasteIntoNewStep();

    assertEquals(USING_DOMAIN, ((ClassmanImageEditArea) target.getEditAreaAt(1)).getSource());
    assertEquals(List.of(), specman.shownMessages());
  }
}
