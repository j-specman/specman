package classman.model.v001.change;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static classman.model.v001.change.ResolvedDiagrams.renamed;
import static classman.model.v001.change.ResolvedDiagrams.renamedWhole;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChangeSetRenamingTest {

  /** Element marks and text marks of the renamed change set get the new name. */
  @Test
  void marksOfOneChangeSet() throws ClassDiagramParseException {
    String source = """
        class A (aaaaaaaa) change(added, yellow) {
          -x (a1a1a1a1) change(removed, yellow) : String was(yellow, Integer)
        }
        note "n" was(yellow, "old")
        """;
    String expected = """
        class A (aaaaaaaa) change(added, pink) {
          -x (a1a1a1a1) change(removed, pink) : String was(pink, Integer)
        }
        note "n" was(pink, "old")
        """;
    assertEquals(ResolvedDiagrams.canonical(expected), renamed(source, "yellow", "pink"));
  }

  /** Marks of other change sets stay as they are. */
  @Test
  void otherChangeSetsStay() throws ClassDiagramParseException {
    String source = """
        class A (aaaaaaaa) change(added, blue)
        class B was(blue, Old) (bbbbbbbb) change(added, yellow)
        """;
    String expected = """
        class A (aaaaaaaa) change(added, blue)
        class B was(blue, Old) (bbbbbbbb) change(added, pink)
        """;
    assertEquals(ResolvedDiagrams.canonical(expected), renamed(source, "yellow", "pink"));
  }

  /** Every kind of element and text mark is found: the examples which carry all of them have no yellow left. */
  @ParameterizedTest
  @ValueSource(strings = {"changes", "textchanges"})
  void everyKindOfMark(String exampleName) throws IOException, ClassDiagramParseException {
    String source = example(exampleName);
    String renamed = renamedWhole(source, "yellow", "pink");
    assertFalse(renamed.contains("yellow"), renamed);
    assertEquals(count(ResolvedDiagrams.canonicalWhole(source), "yellow"), count(renamed, "pink"));
  }

  private static String example(String name) throws IOException {
    try (InputStream in = ChangeSetRenamingTest.class.getResourceAsStream("/classman/model/v001/" + name + ".cld")) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static int count(String text, String word) {
    int count = 0;
    for (int at = text.indexOf(word); at >= 0; at = text.indexOf(word, at + 1)) {
      count++;
    }
    return count;
  }
}
