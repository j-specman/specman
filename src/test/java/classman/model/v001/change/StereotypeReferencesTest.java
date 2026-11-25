package classman.model.v001.change;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which configurations of the host the stereotypes of a diagram refer to, and taking them away from the classes. */
class StereotypeReferencesTest {
  private static final String SOURCE = """
      classdiagram {
          class A (aaaaaaaa) <<@Domain_Value>>
          class B (bbbbbbbb) <<Entity>>
          interface C (cccccccc) <<@Other>> change(removed, yellow)
          class D (dddddddd) <<@Other>> was(yellow, "@Domain_Value")
          class E (eeeeeeee)
      }
      """;

  private final StereotypeReferences_V001 references = new StereotypeReferences_V001();

  @Test
  void usedConfigsAreTheReferencedOnes() {
    assertEquals(Set.of("Domain_Value", "Other"), references.configsUsedIn(SOURCE));
  }

  @Test
  void classesUsingConfigAreNamedInSourceOrder() {
    assertEquals(java.util.List.of("A"), references.classesUsing(SOURCE, "Domain_Value"));
    assertEquals(java.util.List.of("C", "D"), references.classesUsing(SOURCE, "Other"));
    assertTrue(references.classesUsing(SOURCE, "Gone").isEmpty());
  }

  @Test
  void classesUsingConfigOfSourceWhichCanNotBeReadAreNone() {
    assertTrue(references.classesUsing("classdiagram {", "Other").isEmpty());
  }

  @Test
  void referenceInBackupOfChangeMarkIsNoUse() {
    String source = "classdiagram {\n  class D <<Entity>> was(yellow, \"@Domain_Value\")\n}";

    assertTrue(references.configsUsedIn(source).isEmpty());
  }

  @Test
  void configInStringOfNoteIsNoUse() {
    String source = "classdiagram {\n  note \"see <<@Domain_Value>>\"\n}";

    assertTrue(references.configsUsedIn(source).isEmpty());
  }

  @Test
  void sourceWhichCanNotBeReadUsesNothing() {
    assertTrue(references.configsUsedIn("classdiagram {").isEmpty());
  }

  @Test
  void removingConfigTakesStereotypeFromAllItsClasses() {
    String rewritten = references.withoutConfigs(SOURCE, Set.of("Other"));

    assertFalse(rewritten.contains("@Other"), rewritten);
    assertFalse(rewritten.contains("change(removed"), rewritten);
    assertTrue(rewritten.contains("<<@Domain_Value>>"), rewritten);
    assertTrue(rewritten.contains("<<Entity>>"), rewritten);
  }

  @Test
  void removingConfigTakesWholeStereotypeWithItsMarks() {
    String source = "classdiagram {\n  class D (dddddddd) <<@Other>> was(yellow, \"Entity\")\n}";

    String rewritten = references.withoutConfigs(source, Set.of("Other"));

    assertFalse(rewritten.contains("<<"), rewritten);
    assertFalse(rewritten.contains("was("), rewritten);
  }

  @Test
  void sourceWithoutUseStaysAsItIs() {
    String formatted = "classdiagram {\n      class A    <<Entity>>\n}";

    assertSame(formatted, references.withoutConfigs(formatted, Set.of("Other")));
  }

  @Test
  void removingNoConfigsChangesNothing() {
    assertSame(SOURCE, references.withoutConfigs(SOURCE, Set.of()));
  }
}
