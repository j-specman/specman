package classman.model.v001.change;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.MethodModel_V001;
import classman.model.v001.Visibility_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The line of a method as the user types it and the diagram shows it. */
class MethodLineFormatTest {
  private final MethodLineFormat_V001 format = new MethodLineFormat_V001();

  private MethodLine_V001 parsed(String text) {
    return format.parse(text).orElseThrow();
  }

  private MethodModel_V001 methodOf(String text) throws ClassDiagramParseException {
    ClassModel_V001 box = new ClassDiagramParser_V001().parse("classdiagram {\nclass A {\n" + text + "\n}\n}").classes().get(0);
    return box.methods.get(0);
  }

  /** The line shows the parameters in parentheses and the return type behind a colon. */
  @Test
  void shownLine() throws ClassDiagramParseException {
    assertEquals("+save(file: File, count) : boolean [final]",
      format.textOf(methodOf("+save(file: File, count) : boolean [final]")));
    assertEquals("run()", format.textOf(methodOf("run()")));
  }

  /** All parts of a line are recognized. */
  @Test
  void allParts() {
    MethodLine_V001 line = parsed("-save(file: File, count) : boolean [final, 2..3]");

    assertEquals(Visibility_V001.PRIVATE, line.visibility);
    assertEquals("save", line.name);
    assertEquals(List.of("file: File", "count"), line.parameters);
    assertEquals("boolean", line.type);
    assertEquals(List.of("final", "2..3"), line.infos);
  }

  /** The parentheses may be left out for a method without parameters. */
  @Test
  void withoutParentheses() {
    assertEquals(List.of(), parsed("run").parameters);
    assertEquals("void", parsed("run: void").type);
    assertEquals("run", parsed("run [final]").name);
    assertEquals(List.of("final"), parsed("run [final]").infos);
  }

  /** The blanks around a colon of a parameter are made one behind it, and the colon alone leaves the name. */
  @Test
  void parameterColons() {
    assertEquals(List.of("file: File", "String", "x"), parsed("f(file :File,  : String, x:)").parameters);
  }

  /** A comma inside brackets doesn't separate parameters, a quoted one is a parameter of its own. */
  @Test
  void commasInParameters() {
    assertEquals(List.of("m: Map<String, int>", "a, b"), parsed("f(m: Map<String, int>, \"a, b\")").parameters);
  }

  /** What is shown for the method is read back as the same parts, also for parameters which need quotes. */
  @Test
  void shownLineReadBack() {
    List<String> parameters = List.of("m: Map<String, int>", "a, b", "say \"hi\"", "x)");
    MethodLine_V001 line = new MethodLine_V001(null, "f", parameters, "List<String>", List.of("y, z"));
    MethodModel_V001 method = new MethodChanger_V001().create(new ClassModel_V001("A"), line, null);

    MethodLine_V001 again = parsed(format.textOf(method));

    assertEquals(parameters, again.parameters);
    assertEquals("List<String>", again.type);
    assertEquals(List.of("y, z"), again.infos);
  }

  /** Lines which are no methods: no name, open parentheses or brackets, text between the parentheses and the type. */
  @Test
  void invalidLines() {
    for (String text : new String[] {"", "()", "(a)", ": int", "f(a", "f(a) int", "f(a) : int [x", "f [x", "...", "f()\ng()"}) {
      assertTrue(format.parse(text).isEmpty(), "invalid: " + text);
    }
  }

  /** A parameter in double quotes is taken as it is, also with a colon in it. */
  @Test
  void quotedParameterKept() {
    assertEquals(List.of("a:b", "c: d"), parsed("f(\"a:b\", c:d)").parameters);
  }
}
