package headless.changemode;

import headless.HeadlessExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.ChangeSet;
import specman.editarea.TextEditArea;
import specman.editarea.markups.MarkupType;
import specman.model.v002.Markup_V002;
import specman.view.AbstractSchrittView;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.function.Consumer;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.editarea.markups.MarkupSearchPurpose.All;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;
import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;
import static specman.graphics.Styles.Hintergrundfarbe_Deviderbar;

/** Base class for tests about the change mode. It provides the headless Specman, switching the change mode
 * on and the checks for what a change set's coloring is supposed to look like. */
@ExtendWith(HeadlessExtension.class)
abstract class AbstractChangemodeTest {

  /** Switches the change mode on and chooses the given change set, like the user would. */
  static void enableChangeMode(ChangeSet changeSet) {
    specman.toggleChangeMode();
    assertTrue(specman.aenderungenVerfolgen(), "change mode must be on");
    specman.selectChangeSet(changeSet);
  }

  /** The text must be marked as changed by the given change set from its first to its last character. An empty
   * text has no character to mark, so it must not be marked at all. Markups covering no character at all,
   * i.e. ending before they start, are not visible and don't count. */
  static void assertWholeTextMarkedAsChangedBy(TextEditArea textArea, ChangeSet changeSet) {
    List<Markup_V002> markups = visibleMarkups(textArea);
    if (textArea.getPlainText().isEmpty()) {
      assertEquals(List.of(), markups, "text markups in empty " + describe(textArea));
      return;
    }
    assertEquals(1, markups.size(), "text markups in " + describe(textArea));
    Markup_V002 markup = markups.get(0);
    assertEquals(MarkupType.Changed, markup.type, "markup type in " + describe(textArea));
    assertEquals(changeSet.name, markup.changeset, "markup change set in " + describe(textArea));
    assertEquals(0, markup.from, "markup start in " + describe(textArea));
    assertEquals(textArea.getPlainText().length() - 1, markup.to, "markup end in " + describe(textArea));
  }

  /** The text must not be marked as changed anywhere. Markups covering no character at all
   * are not visible and don't count. */
  static void assertNoChangeMarkups(TextEditArea textArea) {
    assertEquals(List.of(), visibleMarkups(textArea), "text markups in " + describe(textArea));
  }

  private static List<Markup_V002> visibleMarkups(TextEditArea textArea) {
    return textArea.findMarkups(All).stream().filter(markup -> markup.to >= markup.from).toList();
  }

  /**
   * Checks the coloring of the given step, i.e. of everything below its component, which includes its substeps.
   * All texts must be marked as changed by the given change set. All text areas and the panels forming the area
   * of the step must have its panel color. A panel is also fine if it has the diagram line color or the one of
   * the divider bars of catch areas. Those are the thin lines, gaps and bars between the colored areas, which are
   * independent of any change set. Invisible components and panels without opaque background are not painted and
   * therefore don't count. A panel which is neither colored nor a line or bar is most likely one which has been
   * forgotten when the step's color was switched.
   * <p>
   * A catch area is no part of the step it belongs to. It hangs in the surrounding sequence, so it has to be
   * checked on its own: {@code assertColoredBy(step.getParent().getCatchBereich(), changeSet)}.
   */
  static void assertColoredBy(AbstractSchrittView step, ChangeSet changeSet) {
    assertAreasBelow(step, changeSet.panelColor(), "change set " + changeSet,
      textArea -> assertWholeTextMarkedAsChangedBy(textArea, changeSet));
  }

  /** The opposite of {@link #assertColoredBy}: No text is marked as changed and the areas have the standard background. */
  static void assertUncolored(AbstractSchrittView step) {
    assertAreasBelow(step, BACKGROUND_COLOR_STANDARD, "standard background", AbstractChangemodeTest::assertNoChangeMarkups);
  }

  private static void assertAreasBelow(AbstractSchrittView step, Color areaColor, String areaColorName, Consumer<TextEditArea> textCheck) {
    Component root = step.getDecoratedComponent();
    assertAreasBelow(root, root, areaColor, areaColorName, textCheck);
  }

  private static void assertAreasBelow(Component root, Component component, Color areaColor, String areaColorName,
      Consumer<TextEditArea> textCheck) {
    if (!component.isVisible()) {
      return;
    }
    if (component instanceof TextEditArea textArea) {
      textCheck.accept(textArea);
      assertEquals(areaColor, textArea.getBackground(), "background of " + describe(root, textArea));
    }
    else if (component instanceof JPanel panel && panel.isOpaque()) {
      Color background = panel.getBackground();
      assertTrue(
        background.equals(areaColor) || background.equals(DIAGRAMM_LINE_COLOR) || background.equals(Hintergrundfarbe_Deviderbar),
        "background of " + describe(root, panel) + " must be the " + areaColorName + " " + areaColor
          + ", the line or the divider bar color, but is " + background);
    }
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) {
        assertAreasBelow(root, child, areaColor, areaColorName, textCheck);
      }
    }
  }

  /** The component's class, name and index in its parent, preceded by those of its parents up to the given root,
   * to find it in the component hierarchy. */
  private static String describe(Component root, Component component) {
    String description = component.getClass().getSimpleName();
    if (component.getName() != null) {
      description += "(" + component.getName() + ")";
    }
    Container parent = component.getParent();
    if (component == root || parent == null) {
      return description;
    }
    return describe(root, parent) + " > " + description + "[" + List.of(parent.getComponents()).indexOf(component) + "]";
  }

  private static String describe(Component component) {
    return describe(null, component);
  }
}
