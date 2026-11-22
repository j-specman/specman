package classman.editor;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Changing, adding and deleting the attributes of a class in the diagram editing, line by line. */
class AttributeEditingTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String BOOK = "classdiagram {\n  class Book (04de91aa) at(10, 20) {\n"
    + "    -title : String [Pflicht]\n    -pages\n  }\n}";
  private static final String EMPTY_BOOK = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final EditorTexts texts = new EditorTexts(host.locale());

  private DiagramEditing open(String source) throws ClassDiagramParseException {
    return new DiagramEditing(source, host, texts);
  }

  private static List<SceneTextArea> lines(DiagramEditing editing) {
    return editing.scene().textAreas.stream().filter(text -> text.kind == TextKind.ATTRIBUTE_LINE).toList();
  }

  private static ClassModel_V001 bookOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source).classes().get(0);
  }

  /** Each attribute has a line of its own below the name, with the text which the diagram shows. */
  @Test
  void attributeLines() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);
    List<SceneTextArea> lines = lines(editing);

    assertEquals(2, lines.size());
    assertEquals("-title : String [Pflicht]", editing.textOf(lines.get(0)));
    assertEquals("-pages", editing.textOf(lines.get(1)));
    Rectangle2D name = editing.scene().textAreas.get(0).area;
    assertTrue(lines.get(0).area.getMinY() > name.getMaxY());
    assertTrue(lines.get(1).area.getMinY() >= lines.get(0).area.getMaxY());
  }

  /** The lines of a class can be changed when the class is selected, not before. */
  @Test
  void editableWhenClassSelected() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);
    Rectangle2D line = lines(editing).get(0).area;
    Point2D onLine = new Point2D.Double(line.getCenterX(), line.getCenterY());

    assertNull(editing.editableTextAt(onLine));
    editing.selectAt(onLine);

    assertEquals(TextKind.ATTRIBUTE_LINE, editing.editableTextAt(onLine).kind);
  }

  /** A changed line changes the parts of the attribute, as one operation. */
  @Test
  void changedLine() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);

    editing.setText(lines(editing).get(0), "+headline : Text [Pflicht, unique]");

    AttributeModel_V001 attribute = bookOf(editing.source()).attributes.get(0);
    assertEquals("headline", attribute.name);
    assertEquals("Text", attribute.type);
    assertEquals(2, attribute.infos.size());
    assertEquals(1, host.reportedSources.size());
    assertNull(attribute.nameChange);
  }

  /** While changes are tracked, the parts which changed get their marks. */
  @Test
  void changedLineTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = open(BOOK);

    editing.setText(lines(editing).get(0), "-title : Text [Pflicht]");

    AttributeModel_V001 attribute = bookOf(editing.source()).attributes.get(0);
    assertEquals("String", attribute.typeChange.backup);
    assertNull(attribute.nameChange);
  }

  /** The same line is no operation. */
  @Test
  void sameLine() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);

    editing.setText(lines(editing).get(0), "-title : String [Pflicht]");

    assertFalse(editing.canUndo());
  }

  /** A line without a name, or with brackets which aren't closed, is not accepted. */
  @Test
  void invalidLines() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);
    SceneTextArea line = lines(editing).get(0);

    assertTrue(editing.accepts(line, "+x : y [a]"));
    assertTrue(editing.accepts(line, ""));
    assertFalse(editing.accepts(line, ": String"));
    assertFalse(editing.accepts(line, "x [a"));
    assertFalse(editing.accepts(line, "..."));
  }

  /** A visibility sign which was there can't be taken away while changes are tracked. */
  @Test
  void visibilitySignStays() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);
    SceneTextArea line = lines(editing).get(0);
    assertTrue(editing.accepts(line, "title : String [Pflicht]"));

    host.trackedChangeSet = "yellow";

    assertFalse(editing.accepts(line, "title : String [Pflicht]"));
    assertTrue(editing.accepts(line, "+title : String [Pflicht]"));
  }

  /** The empty text deletes the attribute. */
  @Test
  void deletedByEmptyText() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);

    editing.setText(lines(editing).get(0), "");

    List<AttributeModel_V001> attributes = bookOf(editing.source()).attributes;
    assertEquals(1, attributes.size());
    assertEquals("pages", attributes.get(0).name);
    assertEquals(1, host.reportedSources.size());
  }

  /** While changes are tracked, the attribute stays and is marked as removed. */
  @Test
  void deletedTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = open(BOOK);

    editing.setText(lines(editing).get(0), "");

    List<AttributeModel_V001> attributes = bookOf(editing.source()).attributes;
    assertEquals(2, attributes.size());
    assertEquals("yellow", attributes.get(0).change.changeSet);
    assertEquals("removed", attributes.get(0).change.type.keyword());
  }

  /** A class without attributes has a blank line, in which the first attribute is typed. */
  @Test
  void blankLineOfEmptyClass() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = open(EMPTY_BOOK);
    List<SceneTextArea> lines = lines(editing);

    assertEquals(1, lines.size());
    assertNull(lines.get(0).attribute);
    assertEquals("", editing.textOf(lines.get(0)));
    assertTrue(editing.isLastAttributeLine(lines.get(0)));

    editing.setText(lines.get(0), "name : String");

    AttributeModel_V001 attribute = bookOf(editing.source()).attributes.get(0);
    assertEquals("name", attribute.name);
    assertEquals("added", attribute.change.type.keyword());
  }

  /** Only the last line of a class is followed by a new line. */
  @Test
  void lastLine() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);

    assertFalse(editing.isLastAttributeLine(lines(editing).get(0)));
    assertTrue(editing.isLastAttributeLine(lines(editing).get(1)));
  }

  /** The blank line for a new attribute is in the picture, but not in the diagram, until it gets a text. */
  @Test
  void newAttributeAtEnd() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);
    double heightBefore = editing.scene().height;
    ClassModel_V001 book = (ClassModel_V001) editing.scene().textAreas.get(0).element;

    SceneTextArea blank = editing.newAttributeAtEnd(book);

    assertNotNull(blank.attribute);
    assertEquals("", editing.textOf(blank));
    assertTrue(editing.scene().height > heightBefore);
    assertTrue(blank.area.getMinY() >= lines(editing).get(1).area.getMaxY());
    assertTrue(host.reportedSources.isEmpty());

    editing.setText(blank, "isbn : String");

    List<AttributeModel_V001> attributes = bookOf(editing.source()).attributes;
    assertEquals(3, attributes.size());
    assertEquals("isbn", attributes.get(2).name);
    assertEquals(1, host.reportedSources.size());
  }

  /** A blank line which is dropped, or committed empty, leaves the diagram as it was and is no operation. */
  @Test
  void newAttributeDropped() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);
    double heightBefore = editing.scene().height;
    ClassModel_V001 book = (ClassModel_V001) editing.scene().textAreas.get(0).element;

    editing.newAttributeAtEnd(book);
    editing.discardNewPart();

    assertEquals(heightBefore, editing.scene().height);
    assertEquals(2, lines(editing).size());

    SceneTextArea blank = editing.newAttributeAtEnd(book);
    editing.setText(blank, "x");
    assertEquals(3, bookOf(editing.source()).attributes.size());

    SceneTextArea another = editing.newAttributeAtEnd(book);
    editing.setText(another, "");

    assertEquals(3, lines(editing).size());
    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());
  }
}
