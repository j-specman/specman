package classman.editor;

import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteAndRelationAppearanceTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String DIAGRAM = "classdiagram {\n  class A (aaaaaaaa) at(10, 10)\n  class B (bbbbbbbb) at(10, 300)\n"
    + "  association (cccccccc) aaaaaaaa -> bbbbbbbb \"label\"\n  note (dddddddd) at(300, 10) \"n\"\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final List<Point2D> menusRequestedAt = new ArrayList<>();
  private DiagramEditing editing;
  private PointerControl control;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    control = new PointerControl(editing, () -> { }, text -> { }, menusRequestedAt::add, pixels -> pixels);
  }

  private Point2D onNote() {
    Rectangle2D area = editing.scene().elementAreas.get(2).area;
    return new Point2D.Double(area.getCenterX(), area.getMaxY() - 2);
  }

  /** A point on the line of the relation, away from its label. */
  private Point2D onRelationLine() {
    List<Point2D> points = editing.scene().relationLines.get(0).points();
    return new Point2D.Double(points.get(0).getX(), points.get(0).getY() + 6);
  }

  private static ElementStyleModel_V001 style() {
    ElementStyleModel_V001 style = new ElementStyleModel_V001();
    style.lineColor = "#cc0000";
    style.fontSize = 16;
    return style;
  }

  /** The style of a note is one operation without a mark, even while changes are tracked. */
  @Test
  void noteStyleIsOneUnmarkedOperation() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(DIAGRAM);
    editing.selectAt(onNote());

    editing.setStyle(editing.selectedNote(), style());

    ElementStyleModel_V001 written = new ClassDiagramParser_V001().parse(editing.source()).notes().get(0).style;
    assertEquals("#cc0000", written.lineColor);
    assertEquals(16, written.fontSize);
    assertEquals(1, host.reportedSources.size());
    assertFalse(editing.source().contains("change("), editing.source());
  }

  /** The style of a relation is one operation too, and the standard style takes it away. */
  @Test
  void relationStyleSetAndReset() throws ClassDiagramParseException {
    open(DIAGRAM);
    editing.selectAt(onRelationLine(), 4);
    assertNotNull(editing.selectedRelation());

    editing.setStyle(editing.selectedRelation(), style());

    assertEquals("#cc0000", new ClassDiagramParser_V001().parse(editing.source()).relations().get(0).style.lineColor);
    assertEquals(1, host.reportedSources.size());

    editing.setStyle(editing.selectedRelation(), new ElementStyleModel_V001());

    assertTrue(new ClassDiagramParser_V001().parse(editing.source()).relations().get(0).style.isStandard());
  }

  /** What a change set has removed can't get a style, and says why. */
  @Test
  void removedRelationAndNoteHaveNoStyle() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (aaaaaaaa) at(10, 10)\n  class B (bbbbbbbb) at(10, 300)\n"
      + "  association change(removed, yellow) aaaaaaaa -> bbbbbbbb\n  note change(removed, yellow) at(300, 10) \"n\"\n}");

    assertEquals("A change set has removed the relation.",
      editing.canChangeStyle(editing.scene().relationLines.get(0).relation).whyNot());
    assertEquals("A change set has removed the note.",
      editing.canChangeStyle(new ClassDiagramParser_V001().parse(editing.source()).notes().get(0)).whyNot());
  }

  /** A relation has no menu of its own: the menu of the handle at its end has the style entry, below the others. An
   * anchor line, which has no style yet, doesn't get it. */
  @Test
  void endHandleMenuOfRelationHasStyleEntry() throws ClassDiagramParseException {
    open(DIAGRAM);
    editing.selectAt(onRelationLine(), 4);
    EditorTexts texts = new EditorTexts(host.locale());

    LineEndContextMenu menu = new LineEndContextMenu(editing, RelationEnd_V001.FROM, texts, start -> null, () -> { });

    List<String> entries = new ArrayList<>();
    for (java.awt.Component component : menu.getComponents()) {
      if (component instanceof JMenuItem item) {
        entries.add(item.getText());
      }
    }
    assertEquals(List.of("Type", "Link to center", "Style..."), entries);
  }

  /** A right click on the line of a relation, away from its handles, opens no menu: the relation has none. */
  @Test
  void rightClickOnRelationLineOpensNoMenu() throws ClassDiagramParseException {
    open(DIAGRAM);

    control.contextMenuRequested(onRelationLine());

    assertNotNull(editing.selectedRelation());
    assertTrue(menusRequestedAt.isEmpty());
  }

  /** The style entry gives the element the style which the picker returns, and nothing when the user cancels. */
  @Test
  void styleEntryAppliesTheChosenStyle() throws ClassDiagramParseException {
    open(DIAGRAM);
    editing.selectAt(onRelationLine(), 4);
    EditorTexts texts = new EditorTexts(host.locale());

    new StyleMenuEntry(editing, editing.selectedRelation(), texts, start -> null, () -> { }).doClick(0);
    assertTrue(host.reportedSources.isEmpty());

    new StyleMenuEntry(editing, editing.selectedRelation(), texts, start -> style(), () -> { }).doClick(0);
    assertEquals("#cc0000", new ClassDiagramParser_V001().parse(editing.source()).relations().get(0).style.lineColor);
    assertNull(new ClassDiagramParser_V001().parse(editing.source()).relations().get(0).style.font);
  }
}
