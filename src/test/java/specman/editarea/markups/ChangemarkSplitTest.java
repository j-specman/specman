package specman.editarea.markups;

import org.junit.jupiter.api.Test;
import specman.model.v002.Markup_V002;

import java.util.List;

public class ChangemarkSplitTest extends AbstractChangemarkRecoveryTest {

  @Test
  void testSimpleSplit() {
    List<Markup_V002> marksAfterSplit = split(
      " 0 1|2 3 ",
      " x x x x "
    );
    assertMarked(marksAfterSplit,
      0, 2,
      3, 4);
  }

  @Test
  void testSplitAfterSingleBlank() {
    List<Markup_V002> marksAfterSplit = split(
      " 0 1 _|2 3 4 ",
      "   x x x x   "
    );
    assertMarked(marksAfterSplit,
      1, 3,
      4, 5);
  }

  @Test
  void testCompleteWhitespaceRemovalOfCaretSequence() {
    List<Markup_V002> marksAfterSplit = split(
      " 0 1 2 _|_ _ 6 7 8",
      "             x    "
    );
    assertMarked(marksAfterSplit, 4, 4);
  }

}
