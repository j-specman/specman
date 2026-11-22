package classman.model.v001.change;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import static classman.model.v001.change.ResolvedDiagrams.canonical;
import static classman.model.v001.change.ResolvedDiagrams.deleted;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementDeletionTest {
  private static final String SOURCE = """
      class A (aaaaaaaa)
      class B (bbbbbbbb)
      class C (cccccccc)
      association (11111111) aaaaaaaa -- bbbbbbbb
      class L (eeeeeeee)
      assocclass (f0000004) eeeeeeee -- 11111111
      association (22222222) bbbbbbbb -- cccccccc
      note (a0000001) "on A"
      note (a0000002) "on relation of A"
      note (a0000003) "on survivor"
      note (a0000004) "free"
      anchor (f0000001) a0000001 -- aaaaaaaa
      anchor (f0000002) a0000002 -- 11111111
      anchor (f0000003) a0000003 -- 22222222
      """;

  /** Deleting a class takes its relations, their association class links and the notes attached to them along. The
   * association class stays, it is a class. */
  @Test
  void deletedClass() throws ClassDiagramParseException {
    String expected = """
        class B (bbbbbbbb)
        class C (cccccccc)
        class L (eeeeeeee)
        association (22222222) bbbbbbbb -- cccccccc
        note (a0000003) "on survivor"
        note (a0000004) "free"
        anchor (f0000003) a0000003 -- 22222222
        """;
    assertEquals(canonical(expected), deleted(SOURCE, "aaaaaaaa"));
  }

  /** Deleting a note leaves everything else. */
  @Test
  void deletedNote() throws ClassDiagramParseException {
    assertEquals(canonical(SOURCE.replace("note (a0000004) \"free\"\n", "")), deleted(SOURCE, "a0000004"));
  }

  /** Deleting a note takes its anchor lines along. */
  @Test
  void deletedNoteTakesItsAnchorLines() throws ClassDiagramParseException {
    String expected = SOURCE.replace("note (a0000003) \"on survivor\"\n", "")
        .replace("anchor (f0000003) a0000003 -- 22222222\n", "");
    assertEquals(canonical(expected), deleted(SOURCE, "a0000003"));
  }

  /** Deleting an anchor line leaves its note and its target. */
  @Test
  void deletedAnchorLine() throws ClassDiagramParseException {
    assertEquals(canonical(SOURCE.replace("anchor (f0000003) a0000003 -- 22222222\n", "")),
        deleted(SOURCE, "f0000003"));
  }

  /** Deleting an association class takes its link along and leaves its relation. */
  @Test
  void deletedAssociationClass() throws ClassDiagramParseException {
    String expected = SOURCE.replace("class L (eeeeeeee)\n", "").replace("assocclass (f0000004) eeeeeeee -- 11111111\n", "");
    assertEquals(canonical(expected), deleted(SOURCE, "eeeeeeee"));
  }

  /** Deleting the link leaves the class and the relation. */
  @Test
  void deletedAssocClassLink() throws ClassDiagramParseException {
    assertEquals(canonical(SOURCE.replace("assocclass (f0000004) eeeeeeee -- 11111111\n", "")),
        deleted(SOURCE, "f0000004"));
  }

  /** While changes are tracked, a class is marked as removed, and so is what hangs on it. */
  @Test
  void trackedClassMarkedRemoved() throws ClassDiagramParseException {
    String expected = """
        class A (aaaaaaaa) change(removed, blue)
        class B (bbbbbbbb)
        class C (cccccccc)
        association (11111111) change(removed, blue) aaaaaaaa -- bbbbbbbb
        class L (eeeeeeee)
        assocclass (f0000004) change(removed, blue) eeeeeeee -- 11111111
        association (22222222) bbbbbbbb -- cccccccc
        note (a0000001) change(removed, blue) "on A"
        note (a0000002) change(removed, blue) "on relation of A"
        note (a0000003) "on survivor"
        note (a0000004) "free"
        anchor (f0000001) change(removed, blue) a0000001 -- aaaaaaaa
        anchor (f0000002) change(removed, blue) a0000002 -- 11111111
        anchor (f0000003) a0000003 -- 22222222
        """;
    assertEquals(canonical(expected), deleted(SOURCE, "aaaaaaaa", "blue"));
  }

  /** While changes are tracked, an anchor line is marked as removed, its note and its target stay as they are. */
  @Test
  void trackedAnchorLineMarkedRemoved() throws ClassDiagramParseException {
    String expected = SOURCE.replace("anchor (f0000003) a0000003", "anchor (f0000003) change(removed, blue) a0000003");
    assertEquals(canonical(expected), deleted(SOURCE, "f0000003", "blue"));
  }

  /** While changes are tracked, an anchor line which a change set has added is removed right away. */
  @Test
  void trackedAddedAnchorLineRemoved() throws ClassDiagramParseException {
    String source = SOURCE.replace("anchor (f0000003) a0000003", "anchor (f0000003) change(added, yellow) a0000003");
    assertEquals(canonical(SOURCE.replace("anchor (f0000003) a0000003 -- 22222222\n", "")),
        deleted(source, "f0000003", "blue"));
  }

  /** While changes are tracked, what has been added, by whichever change set, is removed right away. */
  @Test
  void trackedAddedClassRemoved() throws ClassDiagramParseException {
    String source = SOURCE.replace("class A (aaaaaaaa)", "class A (aaaaaaaa) change(added, yellow)");
    String expected = """
        class B (bbbbbbbb)
        class C (cccccccc)
        class L (eeeeeeee)
        association (22222222) bbbbbbbb -- cccccccc
        note (a0000003) "on survivor"
        note (a0000004) "free"
        anchor (f0000003) a0000003 -- 22222222
        """;
    assertEquals(canonical(expected), deleted(source, "aaaaaaaa", "blue"));
  }

  /** Deleting again something which is already removed changes nothing. */
  @Test
  void trackedRemovedStaysRemoved() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) change(removed, yellow)\nclass B (bbbbbbbb)";
    assertEquals(canonical(source), deleted(source, "aaaaaaaa", "blue"));
  }

  /** Relations which a change set has added go right away with a class which is only marked as removed. */
  @Test
  void trackedAddedRelationRemoved() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa)\nclass B (bbbbbbbb)\nassociation (11111111) change(added, yellow) aaaaaaaa -- bbbbbbbb";
    String expected = "class A (aaaaaaaa) change(removed, blue)\nclass B (bbbbbbbb)";
    assertEquals(canonical(expected), deleted(source, "aaaaaaaa", "blue"));
  }
}
