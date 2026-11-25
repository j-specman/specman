package headless.image.classman;

import classman.render.DiagramStyle;
import classman.render.Scene;
import classman.render.SceneRectangle;
import classman.render.SceneText;
import org.junit.jupiter.api.Test;
import specman.ChangeSet;
import specman.editarea.EditContainer;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.editarea.image.classman.ClassmanRenderer;
import specman.metatag.generic.GenericMetaTag;
import specman.metatag.generic.config.MetaTagConfig;
import specman.metatag.generic.config.MetaTagConfigsEditor;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static headless.SpecmanFT.findByName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;
import static specman.metatag.generic.config.MetaTagConfigsEditor.DELETE_BUTTON;
import static specman.metatag.generic.config.MetaTagConfigsEditor.EDIT_BUTTON;
import static specman.metatag.generic.config.MetaTagConfigsEditor.LIST;
import static specman.metatag.generic.config.MetaTagConfigsEditor.NEW_BUTTON;

/** A diagram which uses a meta tag configuration as the stereotype of a class protects the configuration like a step
 * with a tag does: the user can't rename it, and deleting it takes the stereotype away, in one undo step. Editing the
 * configuration shows in the diagram, without a change mark. The user works in the editor for the configurations. */
class ClassmanMetaTagConfigTest extends AbstractClassmanTest {

  private static final MetaTagConfig DOMAIN = new MetaTagConfig("Domain", "Domain Value", null, null, null, null, true);
  private static final MetaTagConfig OTHER = new MetaTagConfig("Other", "Other", null, null, null, null, true);
  private static final MetaTagConfig LONGER_DOMAIN = new MetaTagConfig("Domain", "A very much longer label than before",
    null, null, null, null, true);
  private static final String USING_DOMAIN = "classdiagram {\n    class A (aaaaaaaa) <<@Domain>>\n    class B (bbbbbbbb)\n}\n";

  private static ClassmanImageEditArea diagramUsingDomain() {
    specman.setMetaTagConfigs(List.of(DOMAIN, OTHER));
    return diagramWithSource(containerOfNewStep(), USING_DOMAIN);
  }

  private static void select(MetaTagConfigsEditor editor, MetaTagConfig config) {
    @SuppressWarnings("unchecked")
    JList<MetaTagConfig> list = findByName(editor, LIST, JList.class);
    list.setSelectedValue(config, false);
  }

  private static void click(MetaTagConfigsEditor editor, String buttonName) {
    findByName(editor, buttonName, JButton.class).doClick();
  }

  private static void deleteInEditor(MetaTagConfig config) {
    MetaTagConfigsEditor editor = specman.openMetaTagConfigs();
    select(editor, config);
    click(editor, DELETE_BUTTON);
  }

  /** Scenario: The configuration of a stereotype counts as used, so the dialog doesn't let the user rename it. */
  @Test
  void testConfigOfStereotypeIsInUse() {
    diagramUsingDomain();

    assertTrue(specman.queryMetaTagConfigsInUse().contains("Domain"));
    assertFalse(specman.queryMetaTagConfigsInUse().contains("Other"));
  }

  /** Scenario: A diagram without a stereotype of a configuration doesn't use it. */
  @Test
  void testDiagramWithoutStereotypeUsesNothing() {
    specman.setMetaTagConfigs(List.of(DOMAIN));
    diagramWithSource(containerOfNewStep(), TWO_CLASSES);

    assertTrue(specman.queryMetaTagConfigsInUse().isEmpty());
  }

  /** Scenario: The user deletes the configuration which the stereotype refers to and confirms. The class loses the
   * stereotype, and a single undo brings back the configuration and the stereotype. */
  @Test
  void testDeletingConfigRemovesStereotypeUndoably() {
    ClassmanImageEditArea diagram = diagramUsingDomain();

    deleteInEditor(DOMAIN);

    assertTrue(specman.shownMessages().stream().anyMatch(message -> message.contains("Domain")),
      "the user must be asked for confirmation: " + specman.shownMessages());
    assertFalse(diagram.getSource().contains("<<"), diagram.getSource());
    assertNull(specman.findMetaTagConfig("Domain"));
    assertFalse(specman.queryMetaTagConfigsInUse().contains("Domain"));

    specman.undo();

    assertEquals(USING_DOMAIN, diagram.getSource());
    assertNotNull(specman.findMetaTagConfig("Domain"));
  }

  /** Scenario: The warning names every use of the configuration in the order of the document: the diagram in the
   * intro, the step with a tag, the diagram in the step behind it and the diagram in the outro. */
  @Test
  void testDeletionWarningListsTheUses() {
    specman.setMetaTagConfigs(List.of(DOMAIN));
    addDiagramTo(specman.getIntro());
    specman.clickNewSimpleStep().addTag(new GenericMetaTag(DOMAIN, null));
    diagramWithSource(containerOfNewStep(), USING_DOMAIN);
    addDiagramTo(specman.getOutro());

    deleteInEditor(DOMAIN);

    String warning = specman.shownMessages().get(0);
    List<String> expected = List.of(
      i18n("dialog.metatag.configs.use.element", "A", i18n("dialog.metatag.configs.use.intro")),
      i18n("dialog.metatag.configs.use.step", "1"),
      i18n("dialog.metatag.configs.use.element", "A", i18n("dialog.metatag.configs.use.step", "2")),
      i18n("dialog.metatag.configs.use.element", "A", i18n("dialog.metatag.configs.use.outro")));
    int position = -1;
    for (String use : expected) {
      int found = warning.indexOf(use, position + 1);
      assertTrue(found > position, "'" + use + "' must follow in the warning:\n" + warning);
      position = found;
    }
  }

  /** Scenario: A configuration which is used very often is listed with the first ten uses only, the rest is counted. */
  @Test
  void testDeletionWarningStopsAtTenUses() {
    specman.setMetaTagConfigs(List.of(DOMAIN));
    StringBuilder source = new StringBuilder("classdiagram {\n");
    for (int i = 1; i <= 13; i++) {
      source.append("    class C").append(i).append(" <<@Domain>>\n");
    }
    diagramWithSource(containerOfNewStep(), source.append("}\n").toString());

    deleteInEditor(DOMAIN);

    String warning = specman.shownMessages().get(0);
    assertTrue(warning.contains(i18n("dialog.metatag.configs.use.element", "C10",
      i18n("dialog.metatag.configs.use.step", "1"))), warning);
    assertFalse(warning.contains("C11"), warning);
    assertTrue(warning.contains(i18n("dialog.metatag.configs.use.more", 3)), warning);
  }

  /** Scenario: With exactly ten uses, nothing is left over to count. */
  @Test
  void testDeletionWarningWithTenUsesHasNoRest() {
    specman.setMetaTagConfigs(List.of(DOMAIN));
    StringBuilder source = new StringBuilder("classdiagram {\n");
    for (int i = 1; i <= 10; i++) {
      source.append("    class C").append(i).append(" <<@Domain>>\n");
    }
    diagramWithSource(containerOfNewStep(), source.append("}\n").toString());

    deleteInEditor(DOMAIN);

    assertFalse(specman.shownMessages().get(0).contains(i18n("dialog.metatag.configs.use.more", 0)));
  }

  private static void addDiagramTo(EditContainer container) {
    container.getEditAreaAt(0).asTextArea().addClassmanImage(USING_DOMAIN);
  }

  /** Scenario: The user doesn't confirm the deletion. Nothing changes. */
  @Test
  void testDeclinedDeletionChangesNothing() {
    ClassmanImageEditArea diagram = diagramUsingDomain();
    specman.answerConfirmDialogsWith(JOptionPane.NO_OPTION);

    deleteInEditor(DOMAIN);

    assertEquals(USING_DOMAIN, diagram.getSource());
    assertNotNull(specman.findMetaTagConfig("Domain"));
  }

  /** Scenario: Taking the stereotype away is not a change of the content, so no change mark is left even in change
   * mode. */
  @Test
  void testDeletingConfigLeavesNoChangeMark() {
    ClassmanImageEditArea diagram = diagramUsingDomain();
    specman.toggleChangeMode();
    specman.selectChangeSet(ChangeSet.ALL.get("blue"));

    deleteInEditor(DOMAIN);

    assertFalse(diagram.getSource().contains("change("), diagram.getSource());
    assertFalse(diagram.getSource().contains("was("), diagram.getSource());
  }

  /** Scenario: The user deletes a configuration which no diagram uses. No question is asked, and the diagram stays as
   * it is. */
  @Test
  void testDeletingUnusedConfigNeedsNoConfirmation() {
    ClassmanImageEditArea diagram = diagramUsingDomain();

    deleteInEditor(OTHER);

    assertEquals(USING_DOMAIN, diagram.getSource());
    assertNull(specman.findMetaTagConfig("Other"));
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The user can't rename a configuration which a diagram uses, but one which nothing uses. */
  @Test
  void testNameOfUsedConfigIsLocked() {
    diagramUsingDomain();
    MetaTagConfigsEditor editor = specman.openMetaTagConfigs();

    select(editor, DOMAIN);
    click(editor, EDIT_BUTTON);
    assertFalse(specman.metaTagConfigNameWasEditable(), "Domain is used by the diagram");

    select(editor, OTHER);
    click(editor, EDIT_BUTTON);
    assertTrue(specman.metaTagConfigNameWasEditable(), "Other is not used");
  }

  /** Scenario: The user edits the label of the configuration. The diagram shows it, and is made wider by the longer
   * label. The source is not touched. Undo brings the old label back. */
  @Test
  void testEditedConfigShowsInDiagram() {
    ClassmanImageEditArea diagram = diagramUsingDomain();
    diagram.addNotify();
    int widthBefore = diagram.getFullSizeImage().getWidth();
    MetaTagConfigsEditor editor = specman.openMetaTagConfigs();
    select(editor, DOMAIN);
    specman.answerMetaTagConfigEditWith(LONGER_DOMAIN);

    click(editor, EDIT_BUTTON);

    assertTrue(diagram.getFullSizeImage().getWidth() > widthBefore);
    assertEquals(USING_DOMAIN, diagram.getSource());

    specman.undo();

    assertEquals(widthBefore, diagram.getFullSizeImage().getWidth());
  }

  /** Scenario: A diagram which has left the document doesn't look at the configurations any more. */
  @Test
  void testDetachedDiagramIgnoresConfigs() {
    ClassmanImageEditArea diagram = diagramUsingDomain();
    diagram.addNotify();
    diagram.removeNotify();
    int widthBefore = diagram.getFullSizeImage().getWidth();
    MetaTagConfigsEditor editor = specman.openMetaTagConfigs();
    select(editor, DOMAIN);
    specman.answerMetaTagConfigEditWith(LONGER_DOMAIN);

    click(editor, EDIT_BUTTON);

    assertEquals(widthBefore, diagram.getFullSizeImage().getWidth());
  }

  /** Scenario: The configuration has a background, a border and a text color, the last one a little transparent. The
   * class takes them over; a color which the configuration leaves at the default stays the one of the class. */
  @Test
  void testColorsOfConfigReachTheClass() {
    specman.setMetaTagConfigs(List.of(new MetaTagConfig("Domain", "Domain Value", null, new Color(0x11, 0x22, 0x33),
      null, new Color(1, 2, 3, 128), true)));

    Scene scene = ClassmanRenderer.renderScene(USING_DOMAIN);

    SceneRectangle box = scene.elements.stream().filter(element -> element instanceof SceneRectangle)
      .map(element -> (SceneRectangle) element).findFirst().orElseThrow();
    assertEquals("#112233", box.fill);
    assertEquals(DiagramStyle.STROKE, box.stroke);
    SceneText name = scene.elements.stream().filter(element -> element instanceof SceneText)
      .map(element -> (SceneText) element).filter(text -> text.text.equals("A")).findFirst().orElseThrow();
    assertEquals("#01020380", name.fill);
  }

  /** Scenario: The user creates a configuration, which is available at once, and undoes it. */
  @Test
  void testNewConfigIsUndoable() {
    specman.setMetaTagConfigs(List.of(DOMAIN));
    MetaTagConfigsEditor editor = specman.openMetaTagConfigs();
    specman.answerMetaTagConfigEditWith(OTHER);

    click(editor, NEW_BUTTON);

    assertNotNull(specman.findMetaTagConfig("Other"));

    specman.undo();

    assertNull(specman.findMetaTagConfig("Other"));
  }

  /** Scenario: The user cancels the dialog for the configuration. Nothing is created. */
  @Test
  void testCancelledNewConfigIsNotCreated() {
    specman.setMetaTagConfigs(List.of(DOMAIN));
    MetaTagConfigsEditor editor = specman.openMetaTagConfigs();
    specman.answerMetaTagConfigEditWith(null);

    click(editor, NEW_BUTTON);

    assertEquals(List.of(DOMAIN), specman.metaTagConfigs());
  }
}
