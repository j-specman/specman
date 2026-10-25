package headless.changemode;

import headless.HeadlessExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.ChangeSet;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedElement;
import specman.editarea.document.WrappedPosition;
import specman.editarea.markups.MarkupType;
import specman.metatag.stepnumberlabel.StepnumberLabel;
import specman.model.v002.Markup_V002;
import specman.ops.buttons.AbstractCreateStepOpButton;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateCaseStepOpButton;
import specman.ops.buttons.CreateIfElseStepOpButton;
import specman.ops.buttons.CreateIfStepOpButton;
import specman.ops.buttons.CreateSimpleStepOpButton;
import specman.ops.buttons.CreateSubsequenceStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.ops.buttons.CreateWhileWhileStepOpButton;
import specman.view.AbstractSchrittView;

import javax.swing.JPanel;
import javax.swing.text.html.CSS;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.editarea.document.WrappedDocumentUtil.elementHatDurchgestrichenenText;
import static specman.editarea.markups.MarkupSearchPurpose.All;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;
import static specman.graphics.Styles.DELETED_BACKGROUND_COLOR;
import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;
import static specman.graphics.Styles.Hintergrundfarbe_Deviderbar;
import static specman.graphics.Styles.Schriftfarbe_Geloescht;
import static specman.graphics.ReadWriteColor.toHTMLColor;

/** Base class for tests about the change mode. It provides the headless Specman, switching the change mode
 * on and the checks for what a change set's coloring is supposed to look like. */
@ExtendWith(HeadlessExtension.class)
abstract class AbstractChangemodeTest {

  /** The buttons creating one step of each type, except catch sequences, which need a break step first. */
  static Stream<Class<? extends AbstractCreateStepOpButton>> stepCreationButtons() {
    return Stream.of(
      CreateSimpleStepOpButton.class,
      CreateWhileStepOpButton.class,
      CreateWhileWhileStepOpButton.class,
      CreateIfElseStepOpButton.class,
      CreateIfStepOpButton.class,
      CreateCaseStepOpButton.class,
      CreateSubsequenceStepOpButton.class,
      CreateBreakStepOpButton.class);
  }

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

  /** The step is marked as deleted by the given change set: Its areas have the change set's panel color, like those
   * of an added step, see {@link #assertColoredBy}. All its texts are deleted, see {@link #assertTextDeleted},
   * and so are the step number labels, which are gray on black like the texts. */
  static void assertDeletedBy(AbstractSchrittView step, ChangeSet changeSet) {
    assertTrue(step.getChangeInfo().isDeleted(), "step " + step.getNumber() + " must be marked as deleted");
    assertAreasBelow(step, changeSet.panelColor(), "change set " + changeSet, AbstractChangemodeTest::assertTextDeleted);
    for (StepnumberLabel label : visibleComponentsBelow(step.getDecoratedComponent(), StepnumberLabel.class)) {
      assertStepnumberLabelDeleted(label);
    }
  }

  /** The text can't be edited anymore and each of its characters is struck through, gray on black. */
  static void assertTextDeleted(TextEditArea textArea) {
    assertFalse(textArea.isEditable(), describe(textArea) + " must not be editable");
    assertWholeTextDeleted(textArea);
  }

  static void assertStepnumberLabelDeleted(StepnumberLabel label) {
    assertEquals(DELETED_BACKGROUND_COLOR.color, label.getBackground(), "background of " + describe(label));
    assertEquals(Schriftfarbe_Geloescht, label.getForeground(), "text color of " + describe(label));
  }

  /** Each character of a deleted text is struck through, gray on black. */
  static void assertWholeTextDeleted(TextEditArea textArea) {
    WrappedDocument document = textArea.getWrappedDocument();
    WrappedPosition position = document.start();
    for (int index = 0; index < textArea.getPlainText().length(); index++) {
      WrappedElement character = document.getCharacterElement(position);
      String characterName = "character " + index + " of " + describe(textArea);
      assertTrue(elementHatDurchgestrichenenText(character), characterName + " must be struck through");
      assertEquals(DELETED_BACKGROUND_COLOR.htmlColor, cssColorOf(character, CSS.Attribute.BACKGROUND_COLOR),
        "background of " + characterName);
      assertEquals(toHTMLColor(Schriftfarbe_Geloescht), cssColorOf(character, CSS.Attribute.COLOR),
        "text color of " + characterName);
      position = position.inc();
    }
  }

  private static String cssColorOf(WrappedElement character, CSS.Attribute attribute) {
    Object color = character.getAttributes().getAttribute(attribute);
    return color != null ? color.toString() : null;
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

  /** The visible components of the given type at or below the given component. Invisible components are not painted,
   * so neither they nor their children are of interest. */
  private static <T extends Component> List<T> visibleComponentsBelow(Component component, Class<T> type) {
    List<T> result = new ArrayList<>();
    if (component.isVisible()) {
      if (type.isInstance(component)) {
        result.add(type.cast(component));
      }
      if (component instanceof Container container) {
        for (Component child : container.getComponents()) {
          result.addAll(visibleComponentsBelow(child, type));
        }
      }
    }
    return result;
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
