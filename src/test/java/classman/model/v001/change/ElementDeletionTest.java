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
        assocclass L (eeeeeeee)
      association (22222222) bbbbbbbb -- cccccccc
      note (a0000001) "on A" attach(aaaaaaaa)
      note (a0000002) "on relation of A" attach(11111111)
      note (a0000003) "on survivor" attach(22222222)
      note (a0000004) "free"
      """;

  /** Deleting a class takes its relations with their association classes and the notes attached to them along. */
  @Test
  void deletedClass() throws ClassDiagramParseException {
    String expected = """
        class B (bbbbbbbb)
        class C (cccccccc)
        association (22222222) bbbbbbbb -- cccccccc
        note (a0000003) "on survivor" attach(22222222)
        note (a0000004) "free"
        """;
    assertEquals(canonical(expected), deleted(SOURCE, "aaaaaaaa"));
  }

  /** Deleting a note leaves everything else. */
  @Test
  void deletedNote() throws ClassDiagramParseException {
    assertEquals(canonical(SOURCE.replace("note (a0000004) \"free\"\n", "")), deleted(SOURCE, "a0000004"));
  }

  /** Deleting an association class leaves its relation. */
  @Test
  void deletedAssociationClass() throws ClassDiagramParseException {
    assertEquals(canonical(SOURCE.replace("  assocclass L (eeeeeeee)\n", "")), deleted(SOURCE, "eeeeeeee"));
  }

  /** While changes are tracked, a class is marked as removed, and so is what hangs on it. */
  @Test
  void trackedClassMarkedRemoved() throws ClassDiagramParseException {
    String expected = """
        class A (aaaaaaaa) change(removed, blue)
        class B (bbbbbbbb)
        class C (cccccccc)
        association (11111111) change(removed, blue) aaaaaaaa -- bbbbbbbb
          assocclass L (eeeeeeee) change(removed, blue)
        association (22222222) bbbbbbbb -- cccccccc
        note (a0000001) change(removed, blue) "on A" attach(aaaaaaaa)
        note (a0000002) change(removed, blue) "on relation of A" attach(11111111)
        note (a0000003) "on survivor" attach(22222222)
        note (a0000004) "free"
        """;
    assertEquals(canonical(expected), deleted(SOURCE, "aaaaaaaa", "blue"));
  }

  /** While changes are tracked, what has been added, by whichever change set, is removed right away. */
  @Test
  void trackedAddedClassRemoved() throws ClassDiagramParseException {
    String source = SOURCE.replace("class A (aaaaaaaa)", "class A (aaaaaaaa) change(added, yellow)");
    String expected = """
        class B (bbbbbbbb)
        class C (cccccccc)
        association (22222222) bbbbbbbb -- cccccccc
        note (a0000003) "on survivor" attach(22222222)
        note (a0000004) "free"
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
