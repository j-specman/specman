package headless.image.classman;

import headless.HeadlessExtension;
import net.atlanticbb.tantlinger.ui.text.actions.ClassmanAction;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.EditContainer;
import specman.editarea.TextEditArea;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.view.EinfacherSchrittView;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(HeadlessExtension.class)
abstract class AbstractClassmanTest {

  static final String TWO_CLASSES = "classdiagram {\n    class Hello\n    class World\n}\n";
  static final String THREE_CLASSES = "classdiagram {\n    class Hello\n    class World\n    class Universe\n}\n";

  static EditContainer containerOfNewStep() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    specman.focus(step);
    return step.getFirstEditArea().asTextArea().getParent();
  }

  /** A new step whose text is followed by a diagram with the source {@link #TWO_CLASSES}. */
  static ClassmanImageEditArea existingDiagram(EditContainer container) {
    return diagramWithSource(container, TWO_CLASSES);
  }

  /** A new step whose text is followed by a diagram with the given source. */
  static ClassmanImageEditArea diagramWithSource(EditContainer container, String source) {
    TextEditArea text = container.getEditAreaAt(0).asTextArea();
    text.addClassmanImage(source);
    specman.layoutComponents();
    return (ClassmanImageEditArea) container.getEditAreaAt(1);
  }

  /** Clicks the toolbar button for a Classman diagram, which by default offers an empty diagram. */
  static ClassmanImageEditArea insertDiagramByToolbarButton(EditContainer container) {
    specman.doEdit(ClassmanAction.class);
    specman.layoutComponents();
    assertTrue(hasDiagram(container), "A Classman diagram must follow the text");
    return (ClassmanImageEditArea) container.getEditAreaAt(1);
  }

  static boolean hasDiagram(EditContainer container) {
    try {
      return container.getEditAreaAt(1) instanceof ClassmanImageEditArea;
    }
    catch (IndexOutOfBoundsException noSecondArea) {
      return false;
    }
  }
}
