package specman.editarea.image;

import org.junit.jupiter.api.Test;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.editarea.image.plantuml.PlantUmlImageEditArea;

import javax.swing.JRadioButton;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;

/** For a new diagram of a kind which can be linked, the dialog offers to link the file instead of taking its content. */
class SourceFileChooserTest {

  private static JRadioButton radioButton(SourceFileChooser chooser, String messageKey) {
    return find(chooser.storagePanel(), i18n(messageKey));
  }

  private static JRadioButton find(Component component, String text) {
    if (component instanceof JRadioButton button && text.equals(button.getText())) {
      return button;
    }
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) {
        JRadioButton found = find(child, text);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  @Test
  void storageDefaultsToEmbedded() {
    SourceFileChooser chooser = new SourceFileChooser(ClassmanImageEditArea.FILE_KIND, null, true);

    assertTrue(radioButton(chooser, "dialog.sourcefile.embedded").isSelected());
    assertFalse(chooser.isLinkedSelected());
    assertTrue(chooser.isEmptyDiagramButtonEnabled());
  }

  @Test
  void linkedStorageDisablesEmptyDiagram() {
    SourceFileChooser chooser = new SourceFileChooser(ClassmanImageEditArea.FILE_KIND, null, true);

    radioButton(chooser, "dialog.sourcefile.linked").doClick();

    assertTrue(chooser.isLinkedSelected());
    assertFalse(chooser.isEmptyDiagramButtonEnabled());

    radioButton(chooser, "dialog.sourcefile.embedded").doClick();

    assertTrue(chooser.isEmptyDiagramButtonEnabled());
  }

  @Test
  void importIntoExistingDiagramDoesNotOfferLinking() {
    SourceFileChooser chooser = new SourceFileChooser(ClassmanImageEditArea.FILE_KIND, null, false);

    assertNull(chooser.storagePanel());
    assertFalse(chooser.isLinkedSelected());
  }

  @Test
  void kindWhichCantBeLinkedDoesNotOfferIt() {
    SourceFileChooser chooser = new SourceFileChooser(PlantUmlImageEditArea.FILE_KIND, null, true);

    assertNull(chooser.storagePanel());
  }
}
