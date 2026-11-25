package classman.model.v001.change;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import static classman.model.v001.change.ResolvedDiagrams.accepted;
import static classman.model.v001.change.ResolvedDiagrams.canonical;
import static classman.model.v001.change.ResolvedDiagrams.rejected;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChangeCascadeTest {
  private static final String SOURCE = """
      class A (aaaaaaaa) change(added, yellow) {
        -x (a1a1a1a1)
      }
      class B (bbbbbbbb)
      class C (cccccccc)
      association (11111111) aaaaaaaa -- bbbbbbbb
      class L (dddddddd) {
        -y
      }
      assocclass dddddddd -- 11111111
      extends change(added, blue) cccccccc -> aaaaaaaa
      association (22222222) bbbbbbbb -- cccccccc
      note (a0000001) "on class"
      note (a0000002) "on attribute"
      note (a0000003) "on cascaded relation"
      note (a0000004) "on survivor"
      note "free"
      anchor a0000001 -- aaaaaaaa
      anchor a0000002 -- a1a1a1a1
      anchor a0000003 -- 11111111
      anchor a0000004 -- 22222222
      """;

  /** A vanishing class takes its relations and the notes attached to them along, and the association class links
   * of the relations. The association classes stay, they are classes. */
  @Test
  void vanishingClass() throws ClassDiagramParseException {
    String expected = """
        class B (bbbbbbbb)
        class C (cccccccc)
        class L (dddddddd) {
          -y
        }
        association (22222222) bbbbbbbb -- cccccccc
        note (a0000004) "on survivor"
        note "free"
        anchor a0000004 -- 22222222
        """;
    assertEquals(canonical(expected), rejected(SOURCE, "yellow"));
  }

  /** A vanishing attribute takes the notes attached to it along, but not its class. */
  @Test
  void vanishingAttribute() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {\n -x (a1a1a1a1) change(removed, yellow)\n -y\n}\n"
        + "note (a0000001) \"on attribute\"\nnote (a0000002) \"on class\"\n"
        + "anchor a0000001 -- a1a1a1a1\nanchor a0000002 -- aaaaaaaa";
    assertEquals(canonical("class A (aaaaaaaa) {\n -y\n}\nnote (a0000002) \"on class\"\nanchor a0000002 -- aaaaaaaa"),
        accepted(source, "yellow"));
  }

  /** A vanishing relation takes the notes attached to it along. */
  @Test
  void vanishingRelation() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa)\nclass B (bbbbbbbb)\n"
        + "association (11111111) change(added, yellow) aaaaaaaa -> bbbbbbbb\nnote (a0000001) \"n\"\n"
        + "anchor a0000001 -- 11111111";
    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb)"), rejected(source, "yellow"));
  }

  /** A vanishing note takes its anchor lines along, which lead to something which stays. */
  @Test
  void vanishingNote() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa)\nnote (a0000001) change(added, yellow) \"n\"\nanchor a0000001 -- aaaaaaaa";
    assertEquals(canonical("class A (aaaaaaaa)"), rejected(source, "yellow"));
  }

  /** A vanishing anchor line leaves its note and its target. */
  @Test
  void vanishingAnchorLine() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa)\nnote (a0000001) \"n\"\nanchor change(added, yellow) a0000001 -- aaaaaaaa";
    assertEquals(canonical("class A (aaaaaaaa)\nnote (a0000001) \"n\""), rejected(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa)\nnote (a0000001) \"n\"\nanchor a0000001 -- aaaaaaaa"),
        accepted(source, "yellow"));
  }

  /** A note whose line was replaced: accepting drops the removed line, rejecting the added one. */
  @Test
  void replacedAnchorLine() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa)\nclass B (bbbbbbbb)\nnote (a0000001) \"n\"\n"
        + "anchor change(removed, yellow) a0000001 -- aaaaaaaa\nanchor change(added, yellow) a0000001 -- bbbbbbbb";
    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb)\nnote (a0000001) \"n\"\nanchor a0000001 -- bbbbbbbb"),
        accepted(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb)\nnote (a0000001) \"n\"\nanchor a0000001 -- aaaaaaaa"),
        rejected(source, "yellow"));
  }

  /** A removed anchor line doesn't attach its note: when its target goes, the note stays with its other line. */
  @Test
  void removedLineDoesNotTakeNoteAlong() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) change(added, blue)\nclass B (bbbbbbbb)\nnote (a0000001) \"n\"\n"
        + "anchor change(removed, yellow) a0000001 -- aaaaaaaa\nanchor change(added, yellow) a0000001 -- bbbbbbbb";
    assertEquals(canonical("class B (bbbbbbbb)\nnote (a0000001) \"n\"\nanchor change(added, yellow) a0000001 -- bbbbbbbb"),
        rejected(source, "blue"));
  }

  /** A note which hangs on a vanishing class and on a class which stays, stays with its other line. */
  @Test
  void noteWithOtherLineStaysWhenOneTargetVanishes() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) change(added, blue)\nclass B (bbbbbbbb)\nnote (a0000001) \"n\"\n"
        + "anchor a0000001 -- aaaaaaaa\nanchor a0000001 -- bbbbbbbb";
    assertEquals(canonical("class B (bbbbbbbb)\nnote (a0000001) \"n\"\nanchor a0000001 -- bbbbbbbb"),
        rejected(source, "blue"));
  }

  /** A note whose lines all lead to vanishing elements goes with them. */
  @Test
  void noteGoesWhenAllTargetsVanish() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) change(added, blue)\nclass B (bbbbbbbb) change(added, blue)\n"
        + "note (a0000001) \"n\"\nanchor a0000001 -- aaaaaaaa\nanchor a0000001 -- bbbbbbbb";
    assertEquals(canonical(""), rejected(source, "blue"));
  }

  /** What survives keeps the marks of other change sets: the added relation of blue stays marked. */
  @Test
  void survivorsKeepOtherMarks() throws ClassDiagramParseException {
    String expected = """
        class A (aaaaaaaa) {
          -x (a1a1a1a1)
        }
        class B (bbbbbbbb)
        class C (cccccccc)
        association (11111111) aaaaaaaa -- bbbbbbbb
        class L (dddddddd) {
          -y
        }
        assocclass dddddddd -- 11111111
        extends change(added, blue) cccccccc -> aaaaaaaa
        association (22222222) bbbbbbbb -- cccccccc
        note (a0000001) "on class"
        note (a0000002) "on attribute"
        note (a0000003) "on cascaded relation"
        note (a0000004) "on survivor"
        note "free"
        anchor a0000001 -- aaaaaaaa
        anchor a0000002 -- a1a1a1a1
        anchor a0000003 -- 11111111
        anchor a0000004 -- 22222222
        """;
    assertEquals(canonical(expected), accepted(SOURCE, "yellow"));
  }
}
