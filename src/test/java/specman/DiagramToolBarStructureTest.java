package specman;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.graphics.SpecmanIcons;
import specman.toolbar.ToolbarGroup;
import specman.ops.buttons.AcceptChangesADBLOpButton;
import specman.ops.buttons.BirdsViewSpecmanOpButton;
import specman.ops.buttons.CollapseAllOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.ops.buttons.ExpandAllOpButton;
import specman.ops.buttons.ExportPDFOpButton;
import specman.ops.buttons.MergeChangeSetsADBLOpButton;
import specman.ops.buttons.RejectChangesADBLOpButton;
import specman.ops.buttons.ReviewOpButton;
import specman.ops.buttons.ToggleBorderTypeOpButton;
import specman.ops.buttons.ToneOpButton;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;

/**
 * Characterizes the single icon toolbar: the {@link DiagramToolBar} holds the step, change, text,
 * insert, view and export groups; the text and insert controls come from the SHEF editor pane.
 */
@ExtendWith(HeadlessExtension.class)
class DiagramToolBarStructureTest {

  /** The buttons of the toolbar in display order, without the SHEF text and insert buttons. */
  private static final List<Class<? extends AbstractButton>> DIAGRAM_TOOLBAR_BUTTONS = List.of(
    // file
    JButton.class, ToolbarToggleButton.class,
    // step
    ToneOpButton.class, DeleteStepOpButton.class, ToggleBorderTypeOpButton.class,
    // changes
    ToolbarToggleButton.class, AcceptChangesADBLOpButton.class, RejectChangesADBLOpButton.class,
    MergeChangeSetsADBLOpButton.class, ReviewOpButton.class,
    // view
    CollapseAllOpButton.class, ExpandAllOpButton.class, BirdsViewSpecmanOpButton.class,
    // export
    ExportPDFOpButton.class);

  @Test
  void diagramToolBarHasAllStepChangeViewAndExportButtons() {
    // the SHEF text and insert buttons are part of the same toolbar, but not of this expectation
    List<AbstractButton> ownButtons = new ArrayList<>(buttons(specman.findComponent(DiagramToolBar.class)));
    ownButtons.removeAll(shefButtons());
    assertEquals(DIAGRAM_TOOLBAR_BUTTONS, ownButtons.stream().map(AbstractButton::getClass).toList());
  }

  @Test
  void diagramToolBarHasSevenTitledGroupsInOrder() {
    DiagramToolBar toolBar = specman.findComponent(DiagramToolBar.class);
    assertEquals(7, toolBar.getComponentCount());
    assertTrue(List.of(toolBar.getComponents()).stream().allMatch(ToolbarGroup.class::isInstance));
  }

  @Test
  void fileGroupComesFirstWithSaveButtonAutoSaveLabelAndCombo() {
    DiagramToolBar toolBar = specman.findComponent(DiagramToolBar.class);
    List<Component> rows = rows((ToolbarGroup) toolBar.getComponent(0));
    assertEquals(2, rows.size());
    List<Component> firstRow = components((Container) rows.get(0));
    assertTrue(firstRow.stream().anyMatch(c -> c instanceof JButton b && b.getIcon() != null));
    assertTrue(firstRow.stream().anyMatch(ToolbarToggleButton.class::isInstance));
    List<Component> secondRow = components((Container) rows.get(1));
    assertTrue(secondRow.stream().anyMatch(c -> c instanceof JLabel l && i18n("toolbar.autosave").equals(l.getText())));
    assertTrue(secondRow.stream().anyMatch(JComboBox.class::isInstance));
  }

  @Test
  void everyGroupExceptExportUsesTwoRows() {
    DiagramToolBar toolBar = specman.findComponent(DiagramToolBar.class);
    for (int i = 0; i < toolBar.getComponentCount() - 1; i++) {
      assertEquals(2, rows((ToolbarGroup) toolBar.getComponent(i)).size());
    }
    assertEquals(1, rows((ToolbarGroup) toolBar.getComponent(toolBar.getComponentCount() - 1)).size());
  }

  @Test
  void diagramToolBarHasTheZoomCombo() {
    assertTrue(components(specman.findComponent(DiagramToolBar.class)).stream().anyMatch(ZoomComboBox.class::isInstance));
  }

  @Test
  void diagramToolBarButtonsHave24PixelIconsAndTooltips() {
    for (AbstractButton button : buttons(specman.findComponent(DiagramToolBar.class))) {
      assertToolbarIcon(button);
      if (!shefButtons().contains(button)) {
        assertNotNull(button.getToolTipText());
      }
    }
  }

  @Test
  void textAndInsertGroupsHaveButtonsAndTheParagraphAndFontCombos() {
    assertTrue(shefButtons().size() > 10);
    long combos = shefComponents().stream().filter(JComboBox.class::isInstance).count();
    assertTrue(combos >= 2);
  }

  private static void assertToolbarIcon(AbstractButton button) {
    assertNotNull(button.getIcon());
    assertEquals(SpecmanIcons.SIZE_TOOLBAR, button.getIcon().getIconWidth());
    assertEquals(SpecmanIcons.SIZE_TOOLBAR, button.getIcon().getIconHeight());
  }

  /**
   * shefEditorPane is package-private, so it is only reachable through the Specman type, not SpecmanFT
   */
  private static List<Component> shefComponents() {
    List<Component> result = new ArrayList<>(((Specman) specman).shefEditorPane.getTextGroupComponents());
    result.addAll(((Specman) specman).shefEditorPane.getInsertGroupComponents());
    return result;
  }

  private static List<AbstractButton> shefButtons() {
    return shefComponents().stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).toList();
  }

  /** The row panels of a group: its content is the first child, the title the second. */
  private static List<Component> rows(ToolbarGroup group) {
    return List.of(((Container) group.getComponent(0)).getComponents());
  }

  private static List<AbstractButton> buttons(Container container) {
    return components(container).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).toList();
  }

  private static List<Component> components(Container container) {
    List<Component> result = new ArrayList<>();
    for (Component component : container.getComponents()) {
      result.add(component);
      if (component instanceof Container child && !(component instanceof JComboBox)) {
        result.addAll(components(child));
      }
    }
    return result;
  }

}
