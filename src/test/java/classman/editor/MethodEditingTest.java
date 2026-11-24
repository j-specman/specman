package classman.editor;

import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.Compartment_V001;
import classman.model.v001.CompartmentVisibility_V001;
import classman.model.v001.MethodModel_V001;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Changing, adding, deleting and hiding the methods of a class in the diagram editing, line by line. */
class MethodEditingTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String BOOK = "classdiagram {\n  class Book (04de91aa) at(10, 20) {\n    -pages\n"
    + "    +save(file: File, count) : boolean\n    +load()\n  }\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();

  private DiagramEditing open(String source) throws ClassDiagramParseException {
    return new DiagramEditing(source, host);
  }

  private static List<SceneTextArea> lines(DiagramEditing editing) {
    return editing.scene().textAreas.stream().filter(text -> text.kind == TextKind.METHOD_LINE).toList();
  }

  private static ClassModel_V001 currentBook(DiagramEditing editing) {
    return (ClassModel_V001) editing.scene().textAreas.get(0).element;
  }

  private static ClassModel_V001 bookOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source).classes().get(0);
  }

  /** Each method has a line of its own, with the text which the diagram shows, and can be changed when the class is
   * selected. */
  @Test
  void methodLines() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);
    List<SceneTextArea> lines = lines(editing);
    Rectangle2D line = lines.get(0).area;
    Point2D onLine = new Point2D.Double(line.getCenterX(), line.getCenterY());

    assertEquals(2, lines.size());
    assertEquals("+save(file: File, count) : boolean", editing.textOf(lines.get(0)));
    assertEquals("+load()", editing.textOf(lines.get(1)));
    assertNull(editing.editableTextAt(onLine));
    editing.selectAt(onLine);
    assertEquals(TextKind.METHOD_LINE, editing.editableTextAt(onLine).kind);
  }

  /** The parts of the line which differ change, the others stay, and while changes are tracked they get marks. */
  @Test
  void changedLineTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = open(BOOK);

    editing.setText(lines(editing).get(0), "+save(file: Path, count, force: boolean) : void");

    MethodModel_V001 method = bookOf(editing.source()).methods.get(0);
    assertEquals("file: File", method.parameters.get(0).textChange.backup);
    assertNull(method.parameters.get(1).change);
    assertEquals("added", method.parameters.get(2).change.type.keyword());
    assertEquals("boolean", method.typeChange.backup);
    assertNull(method.nameChange);
  }

  /** The parentheses of a method without parameters are put in, whether it has a return type or not. */
  @Test
  void parenthesesAdded() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);

    editing.setText(lines(editing).get(1), "reload: void");
    editing.setText(lines(editing).get(0), "close");

    List<MethodModel_V001> methods = bookOf(editing.source()).methods;
    assertEquals("close", methods.get(0).name);
    assertTrue(methods.get(0).parameters.isEmpty());
    assertEquals("reload", methods.get(1).name);
    assertEquals("void", methods.get(1).type);
    assertEquals("reload() : void", editing.textOf(lines(editing).get(1)));
  }

  /** A line which has no name, or open parentheses, can't be committed; the empty text can, it deletes the method. */
  @Test
  void acceptedLines() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);
    SceneTextArea line = lines(editing).get(0);

    assertFalse(editing.accepts(line, "(a)"));
    assertFalse(editing.accepts(line, "f(a"));
    assertTrue(editing.accepts(line, "f(a: Map<K, V>)"));
    assertTrue(editing.accepts(line, ""));
  }

  /** A class without methods has a blank line, in which the first method is typed, and the next one is typed in a new
   * blank line below it. */
  @Test
  void blankLinesForNewMethods() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = open("classdiagram {\n  class Book (04de91aa) at(10, 20) showmethods\n}");

    SceneTextArea first = lines(editing).get(0);
    assertNull(first.member);
    assertTrue(editing.isLastMemberLine(first));
    editing.setText(first, "run");
    SceneTextArea blank = editing.newMemberAtEnd(currentBook(editing), Compartment_V001.METHODS);
    assertEquals("", editing.textOf(blank));
    assertEquals(1, host.reportedSources.size());
    editing.setText(blank, "stop()");

    List<MethodModel_V001> methods = bookOf(editing.source()).methods;
    assertEquals(2, methods.size());
    assertEquals("added", methods.get(1).change.type.keyword());
  }

  /** The empty text deletes the method, and with it the notes and dependencies which hang on it. */
  @Test
  void methodDeleted() throws ClassDiagramParseException {
    DiagramEditing editing = open("classdiagram {\n  class Book (04de91aa) at(10, 20) {\n    +save() (0a0a0a0a)\n  }\n"
      + "  note (22222222) at(300, 20) \"n\"\n  anchor 22222222 -- 0a0a0a0a\n}");

    editing.setText(lines(editing).get(0), "");

    assertTrue(bookOf(editing.source()).methods.isEmpty());
    assertFalse(editing.source().contains("note"), editing.source());
  }

  /** The compartment is hidden and shown like the attribute compartment: the methods stay in the source. */
  @Test
  void compartmentHiddenAndShown() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);
    ClassModel_V001 book = currentBook(editing);

    editing.setCompartmentHidden(book, Compartment_V001.METHODS, true);

    assertEquals(CompartmentVisibility_V001.HIDDEN, bookOf(editing.source()).methodsVisibility);
    assertFalse(bookOf(editing.source()).attributesHidden);
    assertEquals(2, bookOf(editing.source()).methods.size());
    assertTrue(lines(editing).isEmpty());
    assertFalse(editing.canAddMemberTo(book, Compartment_V001.METHODS).can());

    editing.setCompartmentHidden(currentBook(editing), Compartment_V001.METHODS, false);

    assertEquals(CompartmentVisibility_V001.SHOWN, bookOf(editing.source()).methodsVisibility);
    assertEquals(2, lines(editing).size());
  }

  /** Methods with marks of a change set can't be hidden. */
  @Test
  void markedMethodsCannotBeHidden() throws ClassDiagramParseException {
    DiagramEditing editing = open("classdiagram {\n  class Book (04de91aa) at(10, 20) {\n"
      + "    +save() change(added, yellow)\n  }\n}");
    ClassModel_V001 book = currentBook(editing);

    assertFalse(editing.canHideCompartmentOf(book, Compartment_V001.METHODS).can());
    assertTrue(editing.canHideCompartmentOf(book, Compartment_V001.ATTRIBUTES).can());
  }

  /** A single method is hidden and shown on its own, and stays in the source with (hidden). */
  @Test
  void singleMethodHidden() throws ClassDiagramParseException {
    DiagramEditing editing = open(BOOK);
    ClassModel_V001 book = currentBook(editing);

    editing.hideMember(book, book.methods.get(1));

    assertTrue(editing.source().contains("+load() (hidden)"), editing.source());
    assertEquals(1, lines(editing).size());
  }

  /** A new class and a new enum start without methods and so without the compartment, a new interface shows it. */
  @Test
  void methodCompartmentOfNewBoxes() throws ClassDiagramParseException {
    DiagramEditing editing = open("classdiagram {\n}");

    for (ClassKind_V001 kind : ClassKind_V001.values()) {
      SceneTextArea name = editing.newClass(10, 10, kind);
      editing.setText(name, kind.keyword + "Box");
    }

    List<ClassModel_V001> boxes = new ClassDiagramParser_V001().parse(editing.source()).classes();
    assertEquals(CompartmentVisibility_V001.AUTOMATIC, boxes.get(0).methodsVisibility);
    assertEquals(CompartmentVisibility_V001.SHOWN, boxes.get(1).methodsVisibility);
    assertEquals(CompartmentVisibility_V001.AUTOMATIC, boxes.get(2).methodsVisibility);
    assertEquals(1, editing.source().split("showmethods", -1).length - 1);
  }
}
