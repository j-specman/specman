package classman.editor;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagramEditingTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String EMPTY = "classdiagram {\n\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final EditorTexts texts = new EditorTexts(host.locale());

  /** The class lands where it was dropped and is reported with ID and name. */
  @Test
  void droppedClass() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(120, 80);

    ClassModel_V001 box = onlyClassOf(editing.source());
    assertEquals("NewClass", box.name);
    assertEquals(120, box.position.x);
    assertEquals(80, box.position.y);
    assertTrue(box.id.matches("[0-9a-f]{8}"));
    assertEquals(List.of(editing.source()), host.reportedSources);
  }

  /** Several new classes can be told apart. */
  @Test
  void distinctClasses() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(0, 0);
    editing.addClass(200, 0);
    editing.addClass(400, 0);

    List<ClassModel_V001> boxes = modelOf(editing.source()).classes();
    Set<String> names = new HashSet<>();
    Set<String> ids = new HashSet<>();
    for (ClassModel_V001 box : boxes) {
      names.add(box.name);
      ids.add(box.id);
    }
    assertEquals(3, names.size());
    assertEquals(3, ids.size());
    assertEquals(3, host.reportedSources.size());
  }

  /** The picture follows the diagram after each operation. */
  @Test
  void sceneFollowsSource() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);
    double widthBefore = editing.scene().width;

    editing.addClass(300, 300);

    assertTrue(editing.scene().width > widthBefore);
  }

  /** While changes are tracked, a new class is marked as added by the change set. */
  @Test
  void trackedClass() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(0, 0);

    ClassModel_V001 box = onlyClassOf(editing.source());
    assertNotNull(box.change);
    assertEquals(ChangeType_V001.ADDED, box.change.type);
    assertEquals("yellow", box.change.changeSet);
  }

  /** Without tracking, a new class carries no mark. */
  @Test
  void untrackedClass() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(0, 0);

    assertNull(onlyClassOf(editing.source()).change);
  }

  /** What the diagram already has stays when a class is added. */
  @Test
  void existingElementsKept() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.addClass(300, 20);

    List<ClassModel_V001> boxes = modelOf(editing.source()).classes();
    assertEquals(2, boxes.size());
    assertEquals("Book", boxes.get(0).name);
    assertEquals("04de91aa", boxes.get(0).id);
  }

  /** A press inside a box selects it, a press outside deselects. */
  @Test
  void selectingBox() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n  class Page (11111111) at(400, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    assertFalse(editing.hasSelection());

    editing.selectAt(new Point2D.Double(15, 25));

    assertTrue(editing.hasSelection());
    assertTrue(editing.selectedArea().contains(15, 25));
    assertFalse(editing.selectedArea().contains(405, 25));

    editing.selectAt(new Point2D.Double(405, 25));

    assertTrue(editing.selectedArea().contains(405, 25));

    editing.selectAt(new Point2D.Double(250, 300));

    assertFalse(editing.hasSelection());
    assertNull(editing.selectedArea());
  }

  /** Where boxes overlap, the one drawn on top is selected. */
  @Test
  void selectingTopmostBox() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Below (04de91aa) at(10, 20)\n  class Above (11111111) at(12, 22)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(15, 25));

    assertEquals(12, editing.selectedArea().getX(), 0.001);
  }

  /** A class which has just been created is selected. */
  @Test
  void newClassSelected() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(100, 100);

    assertEquals(100, editing.selectedArea().getX(), 0.001);
    assertEquals(100, editing.selectedArea().getY(), 0.001);
  }

  /** Deselecting leaves nothing selected. */
  @Test
  void deselecting() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);
    editing.addClass(100, 100);

    editing.deselect();

    assertFalse(editing.hasSelection());
  }

  /** A box which a change set with a special selection color marked gets that color for its selection frame. */
  @Test
  void specialSelectionColor() throws ClassDiagramParseException {
    host.changeColors.selectionColors.put("blue", "#000000");
    String source = "classdiagram {\n  class Book (04de91aa) change(added, blue) at(10, 20)\n"
      + "  class Page (11111111) change(added, yellow) at(400, 20)\n  class Cover (22222222) at(10, 300)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(15, 25));
    assertEquals(Color.BLACK, editing.specialSelectionColor());

    editing.selectAt(new Point2D.Double(405, 25));
    assertNull(editing.specialSelectionColor());

    editing.selectAt(new Point2D.Double(15, 305));
    assertNull(editing.specialSelectionColor());

    editing.deselect();
    assertNull(editing.specialSelectionColor());
  }

  /** A note can be selected like a class. */
  @Test
  void selectingNote() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n  note (22222222) at(300, 20) \"Remember\"\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(305, 25));

    assertTrue(editing.hasSelection());
    assertEquals(300, editing.selectedArea().getX(), 0.001);
  }

  /** An association class can be selected wherever it stands, also where the layout has placed it. */
  @Test
  void selectingAssociationClass() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class A (aaaaaaaa) at(0, 0)\n  class B (bbbbbbbb) at(300, 0)\n"
      + "  association aaaaaaaa -- bbbbbbbb\n  assocclass link\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    Rectangle2D placed = editing.scene().elementAreas.get(2).area;

    editing.selectAt(new Point2D.Double(placed.getCenterX(), placed.getCenterY()));

    assertEquals(placed, editing.selectedArea());
  }

  /** The user's own position of an association class counts. */
  @Test
  void selectingPositionedAssociationClass() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class A (aaaaaaaa) at(0, 0)\n  class B (bbbbbbbb) at(300, 0)\n"
      + "  association aaaaaaaa -- bbbbbbbb\n  assocclass link at(100, 200)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(105, 205));

    assertEquals(100, editing.selectedArea().getX(), 0.001);
    assertEquals(200, editing.selectedArea().getY(), 0.001);
  }

  /** Deleting the selected class takes it out of the diagram, and nothing stays selected. */
  @Test
  void deletedClass() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n  class Page (11111111) at(400, 20)\n"
      + "  association 04de91aa -> 11111111\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));

    editing.deleteSelected();

    assertEquals(1, modelOf(editing.source()).classes().size());
    assertEquals("Page", modelOf(editing.source()).classes().get(0).name);
    assertTrue(modelOf(editing.source()).relations().isEmpty());
    assertFalse(editing.hasSelection());
    assertEquals(1, host.reportedSources.size());
  }

  /** While changes are tracked, the class stays, marked as removed, but is no longer selected. */
  @Test
  void deletedClassTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));

    editing.deleteSelected();

    ClassModel_V001 book = onlyClassOf(editing.source());
    assertEquals(ChangeType_V001.REMOVED, book.change.type);
    assertEquals("yellow", book.change.changeSet);
    assertFalse(editing.hasSelection());
  }

  /** Delete with nothing selected does nothing. */
  @Test
  void nothingToDelete() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.deleteSelected();

    assertTrue(host.reportedSources.isEmpty());
  }

  /** In change mode, a class which was added is gone at once. */
  @Test
  void addedClassGoneAtOnce() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);
    editing.addClass(10, 10);

    editing.deleteSelected();

    assertTrue(modelOf(editing.source()).classes().isEmpty());
    assertFalse(editing.hasSelection());
  }

  /** The name area of a class lies in the header of its box, across its width. */
  @Test
  void nameAreaInHeader() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20) { -title : String }\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(15, 25));

    Rectangle2D box = editing.selectedArea();
    Rectangle2D name = editing.selectedNameArea();
    assertTrue(box.contains(name));
    assertTrue(name.getHeight() > 0);
    assertTrue(name.getMaxY() < box.getCenterY() + box.getHeight() / 2 - 10);
    assertEquals(editing.selectedClass().name, "Book");
  }

  /** A note has no name area, and no class is selected. */
  @Test
  void noNameAreaForNote() throws ClassDiagramParseException {
    String source = "classdiagram {\n  note (22222222) at(300, 20) \"Remember\"\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(305, 25));

    assertTrue(editing.hasSelection());
    assertNull(editing.selectedNameArea());
    assertNull(editing.selectedClass());
  }

  /** The new name is reported, once. */
  @Test
  void renamedClass() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));

    editing.rename(editing.selectedClass(), "Volume");

    assertEquals("Volume", onlyClassOf(editing.source()).name);
    assertNull(onlyClassOf(editing.source()).nameChange);
    assertEquals(1, host.reportedSources.size());
  }

  /** While changes are tracked, the old name is kept as backup of the change set. */
  @Test
  void renamedClassTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));

    editing.rename(editing.selectedClass(), "Volume");

    ClassModel_V001 box = onlyClassOf(editing.source());
    assertEquals("Volume", box.name);
    assertEquals("yellow", box.nameChange.changeSet);
    assertEquals("Book", box.nameChange.backup);
  }

  /** A line break in a name stays in the name, in the source and in the picture. */
  @Test
  void renamedWithLineBreak() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));
    double oneLine = editing.selectedNameArea().getHeight();

    editing.rename(editing.selectedClass(), "Book\nVolume");

    assertEquals("Book\nVolume", onlyClassOf(editing.source()).name);
    assertTrue(editing.selectedNameArea().getHeight() > 1.5 * oneLine);
  }

  /** The same name is no operation. */
  @Test
  void sameNameIsNoStep() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));

    editing.rename(editing.selectedClass(), "Book");

    assertFalse(editing.canUndo());
  }

  /** A diagram which can't be read can't be edited. */
  @Test
  void unreadableSource() {
    assertThrows(ClassDiagramParseException.class, () -> new DiagramEditing("classdiagram {", host, texts));
  }

  /** A diagram which refers to a class it doesn't have can't be edited. */
  @Test
  void danglingReference() {
    String source = "classdiagram {\n  extends 11111111 -> 22222222\n}";
    assertThrows(IllegalArgumentException.class, () -> new DiagramEditing(source, host, texts));
  }

  private static ClassDiagramModel_V001 modelOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source);
  }

  private static ClassModel_V001 onlyClassOf(String source) throws ClassDiagramParseException {
    List<ClassModel_V001> boxes = modelOf(source).classes();
    assertEquals(1, boxes.size());
    return boxes.get(0);
  }
}
