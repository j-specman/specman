package classman.model.v001.io;

import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.CompartmentVisibility_V001;
import classman.model.v001.MethodModel_V001;
import classman.model.v001.Visibility_V001;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodFormatTest {

  private final ClassDiagramParser_V001 parser = new ClassDiagramParser_V001();
  private final ClassDiagramSerializer_V001 serializer = new ClassDiagramSerializer_V001();

  private ClassModel_V001 onlyClass(String elements) throws ClassDiagramParseException {
    return parser.parse("classdiagram {\n" + elements + "\n}").classes().get(0);
  }

  private String roundTripOfBody(String body) throws ClassDiagramParseException {
    String source = "classdiagram {\n    class A (aaaaaaaa) {\n        " + body + "\n    }\n}\n";
    String written = serializer.serialize(parser.parse(source));
    assertEquals(written, serializer.serialize(parser.parse(written)), "the written text reads back as itself");
    return written;
  }

  /** A method with all parts is read into the method list, apart from the attributes. */
  @Test
  void methodWithAllParts() throws ClassDiagramParseException {
    ClassModel_V001 box = onlyClass("""
        class A {
          -id : int
          +save(file: File, count) (0a0a0a0a) : boolean [final]
        }""");

    MethodModel_V001 method = box.methods.get(0);
    assertEquals(1, box.attributes.size());
    assertEquals(Visibility_V001.PUBLIC, method.visibility);
    assertEquals("save", method.name);
    assertEquals("0a0a0a0a", method.id);
    assertEquals(2, method.parameters.size());
    assertEquals("file: File", method.parameters.get(0).text);
    assertEquals("count", method.parameters.get(1).text);
    assertEquals("boolean", method.type);
    assertEquals("final", method.infos.get(0).text);
  }

  /** The parentheses stand also without parameters, and the return type is optional. */
  @Test
  void methodWithoutParameters() throws ClassDiagramParseException {
    MethodModel_V001 method = onlyClass("class A { run() }").methods.get(0);

    assertTrue(method.parameters.isEmpty());
    assertNull(method.type);
  }

  /** Marks on the method, a parameter, the return type, and the hidden mark are read and written back. */
  @Test
  void marksOfMethodParts() throws ClassDiagramParseException {
    String body = "+save(file: File was(red, \"f: File\"),"
        + " count change(removed, red)) was(yellow, store) (0a0a0a0a) change(added, blue) : boolean was(yellow, void)"
        + " [final change(added, red)] (hidden)";

    MethodModel_V001 method = onlyClass("class A { " + body + " }").methods.get(0);

    assertEquals("store", method.nameChange.backup);
    assertEquals(ChangeType_V001.ADDED, method.change.type);
    assertEquals("f: File", method.parameters.get(0).textChange.backup);
    assertEquals(ChangeType_V001.REMOVED, method.parameters.get(1).change.type);
    assertEquals("void", method.typeChange.backup);
    assertEquals("red", method.infos.get(0).change.changeSet);
    assertTrue(method.hidden);
    assertEquals("blue", method.firstChangeSet());
    assertTrue(roundTripOfBody(body).contains(body));
  }

  /** An interface has methods, its attributes are refused. */
  @Test
  void interfaceWithMethodsOnly() throws ClassDiagramParseException {
    ClassModel_V001 box = onlyClass("interface Runnable { run() }");

    assertEquals(ClassKind_V001.INTERFACE, box.kind);
    assertEquals(1, box.methods.size());
    assertThrows(ClassDiagramParseException.class, () -> onlyClass("interface Runnable { x : int }"));
  }

  /** An enum has values and methods. */
  @Test
  void enumWithValuesAndMethods() throws ClassDiagramParseException {
    ClassModel_V001 box = onlyClass("enum Color {\n RED\n GREEN\n label(): String\n}");

    assertEquals(2, box.attributes.size());
    assertEquals("label", box.methods.get(0).name);
  }

  /** hideattributes and hidemethods hide each compartment on its own, showmethods shows the empty method compartment. */
  @Test
  void compartmentWords() throws ClassDiagramParseException {
    ClassModel_V001 plain = onlyClass("class A");
    ClassModel_V001 hidden = onlyClass("class A hidemethods");
    ClassModel_V001 shown = onlyClass("class A showmethods");
    ClassModel_V001 both = onlyClass("class A hidemethods hideattributes");

    assertEquals(CompartmentVisibility_V001.AUTOMATIC, plain.methodsVisibility);
    assertEquals(CompartmentVisibility_V001.HIDDEN, hidden.methodsVisibility);
    assertEquals(CompartmentVisibility_V001.SHOWN, shown.methodsVisibility);
    assertFalse(hidden.attributesHidden);
    assertTrue(both.attributesHidden);
    assertTrue(serializer.serialize(parser.parse("classdiagram {\nclass A hidemethods hideattributes\n}"))
        .contains("class A hideattributes hidemethods\n"));
    assertTrue(serializer.serialize(parser.parse("classdiagram {\nclass A showmethods\n}"))
        .contains("class A showmethods\n"));
  }

  /** A word which stands twice, or hidemethods together with showmethods, is a mistake. */
  @Test
  void compartmentWordsOnce() {
    assertThrows(ClassDiagramParseException.class, () -> onlyClass("class A hidemethods hidemethods"));
    assertThrows(ClassDiagramParseException.class, () -> onlyClass("class A hidemethods showmethods"));
  }

  /** An attribute can be followed by a method, with or without type and ID, and a body may mix both. */
  @Test
  void attributesAndMethodsInOneBody() throws ClassDiagramParseException {
    ClassModel_V001 box = onlyClass("""
        class A {
          a
          b : int
          c (0c0c0c0c)
          d()
          e: int
          f (0f0f0f0f) change(added, yellow)
          +g(h)
          i
        }""");

    assertEquals(List.of("a", "b", "c", "e", "f", "i"), box.attributes.stream().map(attribute -> attribute.name).toList());
    assertEquals(List.of("d", "g"), box.methods.stream().map(method -> method.name).toList());
    assertEquals("0c0c0c0c", box.attributes.get(2).id);
    assertEquals("yellow", box.attributes.get(4).change.changeSet);
  }

  /** Parameters which the grammar can't read as they are, or which would read as something else, are quoted. */
  @Test
  void parametersNeedingQuotes() throws ClassDiagramParseException {
    assertTrue(roundTripOfBody("find(\"Map<String, Integer>\", name: String)").contains(
        "find(\"Map<String, Integer>\", name: String)"));
    assertTrue(roundTripOfBody("a(\"deadbeef\")").contains("a(\"deadbeef\")"));
    assertTrue(roundTripOfBody("b(\"hidden\")").contains("b(\"hidden\")"));
    assertEquals("deadbeef", onlyClass("class A { a(\"deadbeef\") }").methods.get(0).parameters.get(0).text);
  }

  /** Names of methods which are words of the marks are quoted: change(a, b) would be read as a mark. */
  @Test
  void methodNamedChange() throws ClassDiagramParseException {
    String written = roundTripOfBody("x : int\n        \"change\"(a, b)");

    assertTrue(written.contains("\"change\"(a, b)"));
    assertEquals("change", onlyClass("class A {\n x : int\n \"change\"(a, b)\n}").methods.get(0).name);
  }

  /** A method and an attribute can't start in one line: names with blanks need quotes. */
  @Test
  void oneMemberPerLine() {
    assertThrows(ClassDiagramParseException.class, () -> onlyClass("class A { run() stop() }"));
  }
}
