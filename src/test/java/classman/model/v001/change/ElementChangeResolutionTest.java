package classman.model.v001.change;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import static classman.model.v001.change.ResolvedDiagrams.accepted;
import static classman.model.v001.change.ResolvedDiagrams.canonical;
import static classman.model.v001.change.ResolvedDiagrams.rejected;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementChangeResolutionTest {

  /** Accepting an added class keeps it and drops the mark. */
  @Test
  void acceptedAddedClass() throws ClassDiagramParseException {
    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb)"),
        accepted("class A (aaaaaaaa) change(added, yellow)\nclass B (bbbbbbbb)", "yellow"));
  }

  /** Rejecting an added class removes it. */
  @Test
  void rejectedAddedClass() throws ClassDiagramParseException {
    assertEquals(canonical("class B (bbbbbbbb)"),
        rejected("class A (aaaaaaaa) change(added, yellow)\nclass B (bbbbbbbb)", "yellow"));
  }

  /** Accepting a removed class removes it. */
  @Test
  void acceptedRemovedClass() throws ClassDiagramParseException {
    assertEquals(canonical("class B (bbbbbbbb)"),
        accepted("class A (aaaaaaaa) change(removed, yellow)\nclass B (bbbbbbbb)", "yellow"));
  }

  /** Rejecting a removed class keeps it and drops the mark. */
  @Test
  void rejectedRemovedClass() throws ClassDiagramParseException {
    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb)"),
        rejected("class A (aaaaaaaa) change(removed, yellow)\nclass B (bbbbbbbb)", "yellow"));
  }

  /** Attributes follow the same rules as classes, the other attributes stay. */
  @Test
  void attributeMarks() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {\n -x change(added, yellow)\n -y change(removed, yellow)\n -z\n}";
    assertEquals(canonical("class A (aaaaaaaa) {\n -x\n -z\n}"), accepted(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa) {\n -y\n -z\n}"), rejected(source, "yellow"));
  }


  /** A stereotype is a list entry like an info: accepting or rejecting its mark keeps it or takes it away with its ID. */
  @Test
  void stereotypeMarks() throws ClassDiagramParseException {
    String added = "class A (aaaaaaaa) <<Entity>> (bbbbbbbb) change(added, yellow)";
    assertEquals(canonical("class A (aaaaaaaa) <<Entity>> (bbbbbbbb)"), accepted(added, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa)"), rejected(added, "yellow"));
    String removed = "class A (aaaaaaaa) <<Entity>> (bbbbbbbb) change(removed, yellow)";
    assertEquals(canonical("class A (aaaaaaaa)"), accepted(removed, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa) <<Entity>> (bbbbbbbb)"), rejected(removed, "yellow"));
  }

  /** Relations follow the same rules as classes. */
  @Test
  void relationMarks() throws ClassDiagramParseException {
    String classes = "class A (aaaaaaaa)\nclass B (bbbbbbbb)\n";
    assertEquals(canonical(classes + "association aaaaaaaa -> bbbbbbbb"),
        accepted(classes + "association change(added, yellow) aaaaaaaa -> bbbbbbbb", "yellow"));
    assertEquals(canonical(classes),
        rejected(classes + "association change(added, yellow) aaaaaaaa -> bbbbbbbb", "yellow"));
    assertEquals(canonical(classes),
        accepted(classes + "extends change(removed, yellow) aaaaaaaa -> bbbbbbbb", "yellow"));
  }

  /** A removed or rejected association class link leaves the class and the relation without it, both stay. */
  @Test
  void assocClassLinkMarks() throws ClassDiagramParseException {
    String base = "class A (aaaaaaaa)\nclass B (bbbbbbbb)\nclass L (cccccccc)\nassociation (11111111) aaaaaaaa -- bbbbbbbb\n";
    String source = base + "assocclass change(added, yellow) cccccccc -- 11111111";
    assertEquals(canonical(base + "assocclass cccccccc -- 11111111"), accepted(source, "yellow"));
    assertEquals(canonical(base), rejected(source, "yellow"));
  }

  /** Notes follow the same rules as classes. */
  @Test
  void noteMarks() throws ClassDiagramParseException {
    assertEquals(canonical("note \"n\""), accepted("note change(added, yellow) \"n\"", "yellow"));
    assertEquals(canonical(""), accepted("note change(removed, yellow) \"n\"", "yellow"));
  }

  /** Only the marks of the resolved change set change, the marks of other change sets stay. */
  @Test
  void otherChangeSetsStay() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) change(added, yellow)\nclass B (bbbbbbbb) change(added, blue)";
    assertEquals(canonical("class B (bbbbbbbb) change(added, blue)"), rejected(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb) change(added, blue)"), accepted(source, "yellow"));
  }
}
