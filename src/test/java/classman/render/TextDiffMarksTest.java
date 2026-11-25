package classman.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Which characters of a changed text are marked. Numbers are character positions in the current text. */
class TextDiffMarksTest {
  private static final String LONG_TEXT = "The quick brown fox jumps over the lazy dog";

  private final TextDiffMarks diffMarks = new TextDiffMarks();

  private void assertMarks(List<TextMark> marks, int... startsAndEnds) {
    assertEquals(startsAndEnds.length / 2, marks.size(), "number of marks");
    for (int i = 0; i < marks.size(); i++) {
      assertEquals(startsAndEnds[2 * i], marks.get(i).start, "start of mark " + i);
      assertEquals(startsAndEnds[2 * i + 1], marks.get(i).end, "end of mark " + i);
      assertEquals("yellow", marks.get(i).changeSet);
    }
  }

  /** A text which is new, or has been empty, is marked as a whole. */
  @Test
  void newTextWhole() {
    assertMarks(diffMarks.of(null, LONG_TEXT, "yellow"), 0, LONG_TEXT.length());
    assertMarks(diffMarks.of("", LONG_TEXT, "yellow"), 0, LONG_TEXT.length());
  }

  /** A difference in a short text isn't worth a detailed view. */
  @Test
  void shortTextWhole() {
    assertMarks(diffMarks.of("cat", "hat", "yellow"), 0, 3);
  }

  /** Text which is added at the end, or at the start, is the only marked part. */
  @Test
  void addedPassage() {
    assertMarks(diffMarks.of("The quick brown fox", LONG_TEXT, "yellow"), 19, LONG_TEXT.length());
    assertMarks(diffMarks.of("quick brown fox jumps over the lazy dog", "The " + "quick brown fox jumps over the lazy dog",
      "yellow"), 0, 4);
  }

  /** Where text is gone inside the text, the characters next to the gap are marked. */
  @Test
  void removedPassageInside() {
    assertMarks(diffMarks.of("The quick brown fox jumps over", "The quick brown jumps over", "yellow"), 14, 17);
  }

  /** Text which is replaced is shown by the new text alone, without marking the characters next to it. */
  @Test
  void replacedPassage() {
    assertMarks(diffMarks.of("The quick brown fox jumps over", "The quick red fox jumps over", "yellow"), 10, 13);
  }

  /** Blanks next to the gap are skipped to the nearest characters, and the marked part includes the blanks. */
  @Test
  void removedWordBetweenBlanks() {
    assertMarks(diffMarks.of("The quick brown fox jumps", "The quick fox jumps", "yellow"), 8, 11);
  }

  /** Where text is gone at the start, only the first character of what is left is marked. */
  @Test
  void removedAtStart() {
    assertMarks(diffMarks.of("Very important note for all", "important note for all", "yellow"), 0, 1);
  }

  /** Where text is gone at the end, only the last character of what is left is marked. */
  @Test
  void removedAtEnd() {
    assertMarks(diffMarks.of("Remember to call the customer today", "Remember to call the customer", "yellow"), 28, 29);
  }

  /** Marks which are only a few characters apart are marked as one passage. */
  @Test
  void nearMarksMerged() {
    assertMarks(diffMarks.of("The quick brown fox jumps over the lazy dog", "The quick Brown fox jumps over the Lazy dog",
      "yellow"), 10, 11, 35, 36);
    List<TextMark> merged = diffMarks.of("The quick brown fox jumps over the lazy dog",
      "The quick BrOwn fox jumps over the lazy dog", "yellow");
    assertEquals(1, merged.size());
    assertEquals(10, merged.get(0).start);
  }

  /** A text which is changed in many places keeps at most {@link DiagramStyle#DIFF_MAX_RUNS} marked passages: the
   * closest ones grow together, the first and the last change stay the ends of the marks. */
  @Test
  void manyPassagesJoinedUntilFew() {
    String filler = " lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor ";
    StringBuilder backup = new StringBuilder();
    StringBuilder current = new StringBuilder();
    for (char marker = 'A'; marker <= 'J'; marker++) {
      backup.append(String.valueOf(marker).repeat(3)).append(filler);
      current.append(String.valueOf(Character.toLowerCase(marker)).repeat(3)).append(filler);
    }

    List<TextMark> marks = diffMarks.of(backup.toString(), current.toString(), "yellow");

    assertEquals(DiagramStyle.DIFF_MAX_RUNS, marks.size());
    assertEquals(0, marks.get(0).start);
    assertEquals(current.length() - filler.length(), marks.get(marks.size() - 1).end);
  }

  /** A text of which the marks would cover most is marked as a whole. */
  @Test
  void mostlyChangedTextWhole() {
    String current = "A completely different sentence appears";
    assertMarks(diffMarks.of(LONG_TEXT, current, "yellow"), 0, current.length());
  }

  /** A text with four changes which has one more character appended keeps its marks: the new one is one more. */
  @Test
  void appendedCharacterAddsOneMark() {
    String backup = "Aus Level und lfd. Nummer ergibt sich eine Baumstruktur aller Hilfekategorien. Kategorien und deren "
      + "Zuordnungen zu Handbuchseiten und Hilfeeinträgen werden nicht vererbt sondern bei Neuanlage einer "
      + "Version kopiert.";
    String current = "Level und lfd. Nummer ergibt sich eine Netzstruktur aller Hilfekategorien. Kategorien und deren "
      + "ZuoRdnÜngen zu Handbuchseiten und Hilfeeinträgen werden nicht vererbt sondern bei einer "
      + "Version kopiert.";

    assertMarks(diffMarks.of(backup, current, "yellow"), 0, 1, 39, 43, 99, 103, 176, 179);

    List<TextMark> withAppended = diffMarks.of(backup, current + "!", "yellow");

    assertEquals(5, withAppended.size());
    assertEquals(current.length(), withAppended.get(4).start);
    assertEquals(current.length() + 1, withAppended.get(4).end);
  }
}
