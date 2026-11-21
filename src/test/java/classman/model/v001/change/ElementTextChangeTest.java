package classman.model.v001.change;

import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.RelationType_V001;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  /** Without tracking a label is set without a mark, the empty text removes it. */
  @Test
  void labelUntracked() {
    AssociationRelationModel_V001 relation = association();

    changer.setLabel(relation, "owns");
    assertEquals("owns", relation.label);

    changer.setLabel(relation, "");
    assertNull(relation.label);
    assertNull(relation.labelChange);
  }

  /** A label which a change set adds has the mark without a backup, and goes away completely with the label. */
  @Test
  void labelTrackedAdded() {
    AssociationRelationModel_V001 relation = association();

    changer.setLabel(relation, "owns", "blue");
    assertEquals("owns", relation.label);
    assertEquals("blue", relation.labelChange.changeSet);
    assertNull(relation.labelChange.backup);

    changer.setLabel(relation, "", "blue");
    assertNull(relation.label);
    assertNull(relation.labelChange);
  }

  /** A removed label stays as the empty text with the mark, and is restored with the old text. */
  @Test
  void labelTrackedRemoved() {
    AssociationRelationModel_V001 relation = association();
    relation.label = "owns";

    changer.setLabel(relation, "has", "blue");
    assertEquals("owns", relation.labelChange.backup);

    changer.setLabel(relation, "", "blue");
    assertEquals("", relation.label);
    assertEquals("owns", relation.labelChange.backup);

    changer.setLabel(relation, "owns", "blue");
    assertEquals("owns", relation.label);
    assertNull(relation.labelChange);
  }

  /** The label of an added relation is changed without a mark. */
  @Test
  void labelOfAddedRelation() {
    AssociationRelationModel_V001 relation = association();
    relation.change = new ChangeModel_V001(ChangeType_V001.ADDED, "blue");

    changer.setLabel(relation, "owns", "blue");

    assertEquals("owns", relation.label);
    assertNull(relation.labelChange);
  }

  /** A change set which changes what another one has changed takes the change over. */
  @Test
  void labelTakeOver() {
    AssociationRelationModel_V001 relation = association();
    relation.label = "owns";
    changer.setLabel(relation, "has", "blue");

    changer.setLabel(relation, "keeps", "pink");

    assertEquals("keeps", relation.label);
    assertEquals("pink", relation.labelChange.changeSet);
    assertEquals("has", relation.labelChange.backup);
  }

  /** A multiplicity is set and removed like a label without tracking, at either end. */
  @Test
  void multiplicityUntracked() {
    AssociationRelationModel_V001 relation = association();

    changer.setMultiplicity(relation, RelationEnd_V001.FROM, "1");
    changer.setMultiplicity(relation, RelationEnd_V001.TO, "0..*");
    assertEquals("1", relation.fromMultiplicity);
    assertEquals("0..*", relation.toMultiplicity);

    changer.setMultiplicity(relation, RelationEnd_V001.TO, "");
    assertEquals("1", relation.fromMultiplicity);
    assertNull(relation.toMultiplicity);
  }

  /** A changed multiplicity keeps the old one as backup. */
  @Test
  void multiplicityTracked() {
    AssociationRelationModel_V001 relation = association();
    relation.toMultiplicity = "1";

    changer.setMultiplicity(relation, RelationEnd_V001.TO, "0..1", "blue");

    assertEquals("0..1", relation.toMultiplicity);
    assertEquals("1", relation.toMultiplicityChange.backup);
    assertNull(relation.fromMultiplicityChange);
  }

  /** A multiplicity which was there before can't be removed while changes are tracked: nothing could carry the mark. */
  @Test
  void multiplicityRemovalNeedsMark() {
    AssociationRelationModel_V001 relation = association();
    relation.fromMultiplicity = "1";

    assertFalse(changer.canSetMultiplicity(relation, RelationEnd_V001.FROM, "", "blue"));
    assertThrows(IllegalArgumentException.class,
      () -> changer.setMultiplicity(relation, RelationEnd_V001.FROM, "", "blue"));
    assertEquals("1", relation.fromMultiplicity);
    assertTrue(changer.canSetMultiplicity(relation, RelationEnd_V001.FROM, "*", "blue"));
    assertTrue(changer.canSetMultiplicity(relation, RelationEnd_V001.TO, "", "blue"), "there is none to remove");
  }

  /** A multiplicity which the change set has added itself can be removed again, and an added relation loses it. */
  @Test
  void multiplicityRemovalWhereNoMarkIsNeeded() {
    AssociationRelationModel_V001 relation = association();
    changer.setMultiplicity(relation, RelationEnd_V001.FROM, "1", "blue");

    assertTrue(changer.canSetMultiplicity(relation, RelationEnd_V001.FROM, "", "blue"));
    changer.setMultiplicity(relation, RelationEnd_V001.FROM, "", "blue");
    assertNull(relation.fromMultiplicity);
    assertNull(relation.fromMultiplicityChange);

    AssociationRelationModel_V001 added = association();
    added.toMultiplicity = "1";
    added.change = new ChangeModel_V001(ChangeType_V001.ADDED, "blue");
    changer.setMultiplicity(added, RelationEnd_V001.TO, "", "blue");
    assertNull(added.toMultiplicity);
  }

  private static AssociationRelationModel_V001 association() {
    return new AssociationRelationModel_V001(RelationType_V001.ASSOCIATION, "11111111", "22222222");
  }
}
