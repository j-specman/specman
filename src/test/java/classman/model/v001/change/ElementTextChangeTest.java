package classman.model.v001.change;

import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ElementTextChangeTest {
  private final ElementTextChanger_V001 changer = new ElementTextChanger_V001();

  /** Without tracking, the class has the new name and no mark. */
  @Test
  void untracked() {
    ClassModel_V001 box = new ClassModel_V001("Old");

    changer.setClassName(box, "New");

    assertEquals("New", box.name);
    assertNull(box.nameChange);
  }

  /** The change set which renames a class gets the old name as backup. */
  @Test
  void trackedFirstRename() {
    ClassModel_V001 box = new ClassModel_V001("Old");

    changer.setClassName(box, "New", "blue");

    assertEquals("New", box.name);
    assertEquals("blue", box.nameChange.changeSet);
    assertEquals("Old", box.nameChange.backup);
  }

  /** Renaming again keeps the name from before the change set as backup. */
  @Test
  void trackedRenameAgain() {
    ClassModel_V001 box = new ClassModel_V001("Old");
    changer.setClassName(box, "Middle", "blue");

    changer.setClassName(box, "New", "blue");

    assertEquals("New", box.name);
    assertEquals("Old", box.nameChange.backup);
  }

  /** Setting the name back to the one from before the change set leaves nothing to track. */
  @Test
  void trackedRenameBack() {
    ClassModel_V001 box = new ClassModel_V001("Old");
    changer.setClassName(box, "New", "blue");

    changer.setClassName(box, "Old", "blue");

    assertEquals("Old", box.name);
    assertNull(box.nameChange);
  }

  /** The same name is no change. */
  @Test
  void trackedSameName() {
    ClassModel_V001 box = new ClassModel_V001("Old");

    changer.setClassName(box, "Old", "blue");

    assertNull(box.nameChange);
  }

  /** A change set which renames what another one has renamed takes the change over, backup is the name before. */
  @Test
  void trackedTakeOver() {
    ClassModel_V001 box = new ClassModel_V001("Old");
    changer.setClassName(box, "Middle", "blue");

    changer.setClassName(box, "New", "pink");

    assertEquals("New", box.name);
    assertEquals("pink", box.nameChange.changeSet);
    assertEquals("Middle", box.nameChange.backup);
  }

  /** An added or removed class is renamed without a mark. */
  @Test
  void trackedAddedClass() {
    ClassModel_V001 box = new ClassModel_V001("Old");
    box.change = new ChangeModel_V001(ChangeType_V001.ADDED, "blue");

    changer.setClassName(box, "New", "blue");

    assertEquals("New", box.name);
    assertNull(box.nameChange);
  }

  /** The text of a note is changed like a name: without tracking no mark, with it the old text as backup. */
  @Test
  void noteText() {
    NoteModel_V001 note = new NoteModel_V001("Old text");

    changer.setNoteText(note, "Newer text");
    assertEquals("Newer text", note.text);
    assertNull(note.textChange);

    changer.setNoteText(note, "New text", "blue");
    assertEquals("New text", note.text);
    assertEquals("blue", note.textChange.changeSet);
    assertEquals("Newer text", note.textChange.backup);

    changer.setNoteText(note, "Newer text", "blue");
    assertNull(note.textChange);
  }

  /** The text of an added note is changed without a mark. */
  @Test
  void addedNoteText() {
    NoteModel_V001 note = new NoteModel_V001("Old text");
    note.change = new ChangeModel_V001(ChangeType_V001.ADDED, "blue");

    changer.setNoteText(note, "New text", "blue");

    assertEquals("New text", note.text);
    assertNull(note.textChange);
  }
}
