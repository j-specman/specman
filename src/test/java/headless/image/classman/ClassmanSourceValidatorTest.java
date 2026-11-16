package headless.image.classman;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.image.classman.ClassmanSourceValidator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rules for the source of a Classman diagram in Specman. The messages come from the i18n bundle, which needs the
 * headless environment. */
@ExtendWith(HeadlessExtension.class)
class ClassmanSourceValidatorTest {

  private final ClassmanSourceValidator validator = new ClassmanSourceValidator();

  @Test
  void validSourceHasNoProblem() {
    assertNull(validator.problemOf("classdiagram {\n  class A\n  class B\n  association A -> B\n}"));
  }

  @Test
  void syntaxErrorIsReportedWithItsLine() {
    String problem = validator.problemOf("classdiagram {\n  class A\n  class\n}");

    assertNotNull(problem);
    assertTrue(problem.startsWith("line 4"), problem);
  }

  @Test
  void unknownClassIsReported() {
    String problem = validator.problemOf("classdiagram {\n  class A\n  association A -> Missing\n}");

    assertNotNull(problem);
    assertTrue(problem.contains("Missing"), problem);
  }

  @Test
  void backtickIsRefusedEvenInAComment() {
    assertNotNull(validator.problemOf("classdiagram {\n  // don't `do` this\n  class A\n}"));
  }

  @Test
  void diagramWithoutContentIsRefused() {
    assertNotNull(validator.problemOf("classdiagram {\n\n}"));
  }
}
