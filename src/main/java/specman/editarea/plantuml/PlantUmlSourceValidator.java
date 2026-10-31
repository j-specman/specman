package specman.editarea.plantuml;

import net.sourceforge.plantuml.syntax.SyntaxChecker;
import net.sourceforge.plantuml.syntax.SyntaxResult;

import java.util.regex.Pattern;

import static specman.i18n.Messages.i18n;

/** The rules PlantUML source must follow to be accepted by Specman. */
public class PlantUmlSourceValidator {
  public static final String BACKTICK_MESSAGE_KEY = "msg.plantuml.backtick";
  public static final String PRAGMA_MESSAGE_KEY = "msg.plantuml.pragma";

  private static final Pattern PRAGMA = Pattern.compile("^\\s*!pragma\\b", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

  /** @return a message telling the user what is wrong with the source, or null if it is fine */
  public String problemOf(String source) {
    String violationKey = violationMessageKey(source);
    return violationKey != null ? i18n(violationKey) : syntaxProblem(source);
  }

  /** Backticks delimit the source in the model file, and the layout engine is fixed by Specman. */
  public String violationMessageKey(String source) {
    if (source.indexOf('`') >= 0) {
      return BACKTICK_MESSAGE_KEY;
    }
    if (PRAGMA.matcher(source).find()) {
      return PRAGMA_MESSAGE_KEY;
    }
    return null;
  }

  /** @return PlantUML's description of the syntax error with its line number, or null if the source is fine */
  public String syntaxProblem(String source) {
    SyntaxResult result = SyntaxChecker.checkSyntax(source);
    if (!result.isError()) {
      return null;
    }
    String errors = String.join(" ", result.getErrors());
    // PlantUML counts lines from zero, starting with the line containing @startuml
    int line = result.getLineLocation() != null ? result.getLineLocation().getPosition() + 1 : 1;
    return i18n("msg.plantuml.syntax", line, errors);
  }
}
