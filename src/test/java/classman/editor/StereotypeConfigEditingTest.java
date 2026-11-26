package classman.editor;

import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
import classman.render.StereotypeLook;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A stereotype is a free text or refers to a configuration of the host, which the user chooses from a menu. */
class StereotypeConfigEditingTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String BARE = "classdiagram {\n  class A (11111111) at(10, 60)\n}";
  private static final String WITH_CONFIG = "classdiagram {\n  class A (11111111) at(10, 60) <<:Domain_Value>>\n}";
  private static final String WITH_FREE_TEXT = "classdiagram {\n  class A (11111111) at(10, 60) <<Entity>>\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void open(String source) throws ClassDiagramParseException {
    host.stereotypeLooks.put("Domain_Value", new StereotypeLook("Domain Value", null));
    host.stereotypeLooks.put("Other", new StereotypeLook("Other", null));
    editing = new DiagramEditing(source, host);
    Rectangle2D area = editing.scene().elementAreas.get(0).area;
    editing.selectAt(new Point2D.Double(area.getCenterX(), area.getMaxY() - 2));
  }

  private ClassModel_V001 box() {
    return editing.selectedClass();
  }

  private ClassModel_V001 written() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).classes().get(0);
  }

  private SceneTextArea stereotypeArea() {
    return editing.scene().textAreas.stream().filter(area -> area.kind == TextKind.STEREOTYPE).findFirst()
      .orElseThrow();
  }

  @Test
  void chosenConfigIsWrittenAsReference() throws ClassDiagramParseException {
    open(BARE);

    editing.setStereotypeConfig(box(), "Domain_Value");

    assertEquals(":Domain_Value", written().stereotype);
    assertNull(written().stereotypeElementChange);
    assertEquals("<<Domain Value>>", stereotypeAreaLabel());
  }

  private String stereotypeAreaLabel() {
    return editing.scene().elements.stream().filter(element -> element instanceof classman.render.SceneText)
      .map(element -> ((classman.render.SceneText) element).text).filter(text -> text.startsWith("<<")).findFirst()
      .orElse(null);
  }

  @Test
  void chosenConfigIsMarkedAsAddedWhileTracking() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(BARE);

    editing.setStereotypeConfig(box(), "Domain_Value");

    assertEquals(ChangeType_V001.ADDED, written().stereotypeElementChange.type);
  }

  @Test
  void otherConfigKeepsReferenceOfFormerOneAsBackup() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(WITH_CONFIG);

    editing.setStereotypeConfig(box(), "Other");

    assertEquals(":Other", written().stereotype);
    assertEquals(":Domain_Value", written().stereotypeChange.backup);
  }

  @Test
  void configInsteadOfFreeTextKeepsTextAsBackup() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(WITH_FREE_TEXT);

    editing.setStereotypeConfig(box(), "Domain_Value");

    assertEquals("Entity", written().stereotypeChange.backup);
  }

  @Test
  void choosingTheSameConfigAgainIsNoOperation() throws ClassDiagramParseException {
    open(WITH_CONFIG);

    editing.setStereotypeConfig(box(), "Domain_Value");

    assertFalse(editing.canUndo());
  }

  @Test
  void invalidConfigNameIsRejected() throws ClassDiagramParseException {
    open(BARE);

    assertThrows(IllegalArgumentException.class, () -> editing.setStereotypeConfig(box(), "Domain Value"));
  }

  @Test
  void referenceIsNoTextToEdit() throws ClassDiagramParseException {
    open(WITH_CONFIG);

    assertNull(editing.editableTextAt(new Point2D.Double(stereotypeArea().area.getCenterX(), stereotypeArea().area.getCenterY())));
  }

  @Test
  void freeTextCanNotBeginWithReferencePrefix() throws ClassDiagramParseException {
    open(WITH_FREE_TEXT);

    assertFalse(editing.accepts(stereotypeArea(), ":Domain_Value"));
  }

  @Test
  void freeTextReplacesReferenceAndKeepsItAsBackup() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(WITH_CONFIG);

    SceneTextArea row = editing.stereotypeRowForFreeText(box());
    assertEquals("", editing.textOf(row));
    editing.setText(row, "Entity");

    assertEquals("Entity", written().stereotype);
    assertEquals(":Domain_Value", written().stereotypeChange.backup);
  }

  @Test
  void droppedFreeTextRowKeepsReference() throws ClassDiagramParseException {
    open(WITH_CONFIG);
    String before = editing.source();

    editing.stereotypeRowForFreeText(box());
    editing.discardNewPart();

    assertEquals(":Domain_Value", box().stereotype);
    assertEquals(before, editing.source());
  }

  @Test
  void emptyFreeTextKeepsReference() throws ClassDiagramParseException {
    open(WITH_CONFIG);

    editing.setText(editing.stereotypeRowForFreeText(box()), "");

    assertEquals(":Domain_Value", written().stereotype);
    assertFalse(editing.canUndo());
  }

  @Test
  void freeTextRowOfFreeTextIsTheTextItself() throws ClassDiagramParseException {
    open(WITH_FREE_TEXT);

    SceneTextArea row = editing.stereotypeRowForFreeText(box());

    assertEquals("Entity", editing.textOf(row));
  }

  @Test
  void freeTextRowOfClassWithoutStereotypeIsBlank() throws ClassDiagramParseException {
    open(BARE);

    SceneTextArea row = editing.stereotypeRowForFreeText(box());

    assertEquals("", editing.textOf(row));
    assertNull(written().stereotype);
  }

  @Test
  void removedStereotypeIsGone() throws ClassDiagramParseException {
    open(WITH_CONFIG);

    editing.removeStereotype(box());

    assertNull(written().stereotype);
  }

  @Test
  void removedStereotypeKeepsReferenceWhileTracking() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(WITH_CONFIG);

    editing.removeStereotype(box());

    assertEquals(":Domain_Value", written().stereotype);
    assertEquals(ChangeType_V001.REMOVED, written().stereotypeElementChange.type);
  }

  @Test
  void removalIsOnlyPossibleWithStereotype() throws ClassDiagramParseException {
    open(BARE);
    assertFalse(editing.canRemoveStereotypeOf(box()).can());

    open(WITH_CONFIG);
    assertTrue(editing.canRemoveStereotypeOf(box()).can());
  }

  @Test
  void configOfStereotypeIsKnown() throws ClassDiagramParseException {
    open(WITH_CONFIG);
    assertEquals("Domain_Value", editing.stereotypeConfigOf(box()));

    open(WITH_FREE_TEXT);
    assertNull(editing.stereotypeConfigOf(box()));
    assertTrue(editing.hasFreeTextStereotype(box()));
  }
}
