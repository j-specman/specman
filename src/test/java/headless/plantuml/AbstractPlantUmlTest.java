package headless.plantuml;

import headless.HeadlessExtension;
import net.atlanticbb.tantlinger.ui.text.actions.PlantUmlAction;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.EditContainer;
import specman.editarea.plantuml.PlantUmlImageEditArea;
import specman.view.EinfacherSchrittView;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(HeadlessExtension.class)
abstract class AbstractPlantUmlTest {

  static final String TWO_CLASSES = "@startuml\nclass Hello\nclass World\nHello --> World\n@enduml\n";
  static final String THREE_CLASSES =
    "@startuml\nclass Hello\nclass World\nclass Universe\nHello --> World\nWorld --> Universe\n@enduml\n";

  static EditContainer containerOfNewStep() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    specman.focus(step);
    return step.getFirstEditArea().asTextArea().getParent();
  }

  static PlantUmlImageEditArea insertDiagramByToolbarButton(EditContainer container) {
    specman.doEdit(PlantUmlAction.class);
    specman.layoutComponents();
    assertTrue(hasDiagram(container), "A PlantUML diagram must follow the text");
    return (PlantUmlImageEditArea) container.getEditAreaAt(1);
  }

  static boolean hasDiagram(EditContainer container) {
    try {
      return container.getEditAreaAt(1) instanceof PlantUmlImageEditArea;
    }
    catch (IndexOutOfBoundsException noSecondArea) {
      return false;
    }
  }
}
