package headless.image.plantuml;

import org.junit.jupiter.api.Test;
import specman.editarea.image.plantuml.PlantUmlSourceValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantUmlSourceValidatorTest {

  private final PlantUmlSourceValidator validator = new PlantUmlSourceValidator();

  @Test
  void testPlainSourceIsAccepted() {
    assertNull(validator.violationMessageKey("@startuml\nclass Hello\n@enduml\n"));
  }

  @Test
  void testBacktickIsRejected() {
    assertEquals(PlantUmlSourceValidator.BACKTICK_MESSAGE_KEY,
      validator.violationMessageKey("@startuml\nnote: `code`\n@enduml\n"));
  }

  @Test
  void testPragmaIsRejectedRegardlessOfCaseAndIndentation() {
    assertEquals(PlantUmlSourceValidator.PRAGMA_MESSAGE_KEY,
      validator.violationMessageKey("@startuml\n  !PRAGMA layout smetana\n@enduml\n"));
  }

  @Test
  void testWordStartingWithPragmaInsideTextIsAccepted() {
    assertNull(validator.violationMessageKey("@startuml\nclass Pragmatic\nnote: use !pragma never\n@enduml\n"));
  }

  @Test
  void testSourceWithoutContentIsRejected() {
    assertEquals(PlantUmlSourceValidator.EMPTY_MESSAGE_KEY,
      validator.violationMessageKey("@startuml\n@enduml\n"));
    assertEquals(PlantUmlSourceValidator.EMPTY_MESSAGE_KEY,
      validator.violationMessageKey("@startuml\n\n   \n@enduml\n"));
    assertEquals(PlantUmlSourceValidator.EMPTY_MESSAGE_KEY,
      validator.violationMessageKey(""));
  }

  @Test
  void testCommentsDoNotCountAsContent() {
    assertEquals(PlantUmlSourceValidator.EMPTY_MESSAGE_KEY,
      validator.violationMessageKey("@startuml\n' just a note\n/' a\nblock comment '/\n@enduml\n"));
  }

  @Test
  void testSingleClassIsContent() {
    assertNull(validator.violationMessageKey("@startuml\n' with a comment\nclass A\n@enduml\n"));
  }

  @Test
  void testSeveralDiagramsAreRejected() {
    assertEquals(PlantUmlSourceValidator.MULTIPLE_DIAGRAMS_MESSAGE_KEY,
      validator.violationMessageKey("@startuml\nclass A\n@enduml\n@startuml\nclass B\n@enduml\n"));
    assertEquals(PlantUmlSourceValidator.MULTIPLE_DIAGRAMS_MESSAGE_KEY,
      validator.violationMessageKey("@startuml\nclass A\n@enduml\n  @STARTUML\nclass B\n@enduml\n"));
  }

  @Test
  void testStartMarkerInsideCommentOrTextIsNoSecondDiagram() {
    assertNull(validator.violationMessageKey("@startuml\n' @startuml\n/' @startuml\n'/\nclass A\nnote: @startuml\n@enduml\n"));
  }

  @Test
  void testSyntaxErrorIsReportedWithLine() {
    String problem = validator.syntaxProblem("@startuml\nclass A\nA --> \n@enduml\n");

    assertNotNull(problem);
    assertTrue(problem.contains("3"), "The problem must name the line, but was: " + problem);
  }

  @Test
  void testMissingEnddumlIsASyntaxProblem() {
    assertNotNull(validator.syntaxProblem("@startuml\nclass A\n"));
  }

  @Test
  void testValidDiagramHasNoProblem() {
    assertNull(validator.problemOf("@startuml\nclass A\nclass B\nA --> B\n@enduml\n"));
  }
}
