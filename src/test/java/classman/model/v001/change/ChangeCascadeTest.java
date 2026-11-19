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
        assocclass L {
          -y
        }
      extends change(added, blue) cccccccc -> aaaaaaaa
      association (22222222) bbbbbbbb -- cccccccc
      note "on class" attach(aaaaaaaa)
      note "on attribute" attach(a1a1a1a1)
      note "on cascaded relation" attach(11111111)
      note "on survivor" attach(22222222)
      note "free"
      """;

  /** A vanishing class takes its relations, with their association classes, and the notes attached to them along. */
  @Test
  void vanishingClass() throws ClassDiagramParseException {
    String expected = """
        class B (bbbbbbbb)
        class C (cccccccc)
        association (22222222) bbbbbbbb -- cccccccc
        note "on survivor" attach(22222222)
        note "free"
        """;
    assertEquals(canonical(expected), rejected(SOURCE, "yellow"));
  }

  /** A vanishing attribute takes the notes attached to it along, but not its class. */
  @Test
  void vanishingAttribute() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {\n -x (a1a1a1a1) change(removed, yellow)\n -y\n}\n"
        + "note \"on attribute\" attach(a1a1a1a1)\nnote \"on class\" attach(aaaaaaaa)";
    assertEquals(canonical("class A (aaaaaaaa) {\n -y\n}\nnote \"on class\" attach(aaaaaaaa)"),
        accepted(source, "yellow"));
  }

  /** A vanishing relation takes the notes attached to it along. */
  @Test
  void vanishingRelation() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa)\nclass B (bbbbbbbb)\n"
        + "association (11111111) change(added, yellow) aaaaaaaa -> bbbbbbbb\nnote \"n\" attach(11111111)";
    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb)"), rejected(source, "yellow"));
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
          assocclass L {
            -y
          }
        extends change(added, blue) cccccccc -> aaaaaaaa
        association (22222222) bbbbbbbb -- cccccccc
        note "on class" attach(aaaaaaaa)
        note "on attribute" attach(a1a1a1a1)
        note "on cascaded relation" attach(11111111)
        note "on survivor" attach(22222222)
        note "free"
        """;
    assertEquals(canonical(expected), accepted(SOURCE, "yellow"));
  }
}
