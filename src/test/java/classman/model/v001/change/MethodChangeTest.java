package classman.model.v001.change;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.MethodModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import org.junit.jupiter.api.Test;

import static classman.model.v001.change.ResolvedDiagrams.accepted;
import static classman.model.v001.change.ResolvedDiagrams.canonical;
import static classman.model.v001.change.ResolvedDiagrams.rejected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Changing, creating and deleting methods of a class, with and without change tracking. */
class MethodChangeTest {
  private static final String CLASS = "class A (aaaaaaaa) {\n  -save(file: File, count) : boolean [final]\n}\n";

  private final ClassDiagramParser_V001 parser = new ClassDiagramParser_V001();
  private final ClassDiagramSerializer_V001 serializer = new ClassDiagramSerializer_V001();
  private final MethodLineFormat_V001 format = new MethodLineFormat_V001();
  private final MethodChanger_V001 changer = new MethodChanger_V001();

  private ClassDiagramModel_V001 parse(String elements) throws ClassDiagramParseException {
    return parser.parse("classdiagram {\n" + elements + "\n}");
  }

  /** Gives the first method of the first class the line and returns the canonical text of the diagram. */
  private String applied(String elements, String line, String changeSet) throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(elements);
    ClassModel_V001 box = model.classes().get(0);
    changer.apply(box, box.methods.get(0), format.parse(line).orElseThrow(), changeSet);
    return serializer.serialize(model);
  }

  /** Without tracking all parts of the line are taken over, with no marks. */
  @Test
  void untracked() throws ClassDiagramParseException {
    String expected = "class A (aaaaaaaa) {\n  +store(path) : void\n}\n";
    assertEquals(canonical(expected), applied(CLASS, "+store(path) : void", null));
  }

  /** The line as it is now changes nothing, tracked or not. */
  @Test
  void unchangedLine() throws ClassDiagramParseException {
    assertEquals(canonical(CLASS), applied(CLASS, "-save(file: File, count) : boolean [final]", "yellow"));
  }

  /** A parameter which is changed or added gets its mark, like an info of an attribute. */
  @Test
  void trackedParameters() throws ClassDiagramParseException {
    String expected = """
        class A (aaaaaaaa) {
          -save(file: Path was(yellow, "file: File"), count, force: boolean change(added, yellow)) : boolean [final]
        }
        """;
    assertEquals(canonical(expected),
      applied(CLASS, "-save(file: Path, count, force: boolean) : boolean [final]", "yellow"));
  }

  /** A parameter which is left out stays with its text and is marked as removed. */
  @Test
  void trackedRemovedParameter() throws ClassDiagramParseException {
    String expected = """
        class A (aaaaaaaa) {
          -save(file: File, count change(removed, yellow)) : boolean [final]
        }
        """;
    assertEquals(canonical(expected), applied(CLASS, "-save(file: File) : boolean [final]", "yellow"));
  }

  /** The name, the return type and an info of a method get text marks with the old text. */
  @Test
  void trackedNameAndReturnType() throws ClassDiagramParseException {
    String expected = """
        class A (aaaaaaaa) {
          -store(file: File, count) was(yellow, save) : void was(yellow, boolean) [final]
        }
        """;
    assertEquals(canonical(expected), applied(CLASS, "-store(file: File, count) : void [final]", "yellow"));
  }

  /** A new method is marked as added while changes are tracked, in an added class it carries no mark. */
  @Test
  void created() throws ClassDiagramParseException {
    ClassModel_V001 box = parse(CLASS).classes().get(0);
    ClassModel_V001 addedBox = parse("class B (bbbbbbbb) change(added, yellow)\n").classes().get(0);
    MethodLine_V001 line = format.parse("+run(x)").orElseThrow();

    MethodModel_V001 tracked = changer.create(box, line, "yellow");

    assertEquals("yellow", tracked.change.changeSet);
    assertEquals("x", tracked.parameters.get(0).text);
    assertNull(changer.create(box, line, null).change);
    assertNull(changer.create(addedBox, line, "yellow").change);
  }

  private static final String WITH_REFERENCES = """
      class A (aaaaaaaa) {
        -save(file: File) (a1a1a1a1)
        -load()
      }
      class B (bbbbbbbb)
      note (a0000001) "on save"
      anchor (f0000001) a0000001 -- a1a1a1a1
      dependency (d0000001) bbbbbbbb -> a1a1a1a1
      """;

  /** Deleting a method takes the notes and dependencies which hang on it along. */
  @Test
  void deleted() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(WITH_REFERENCES);

    new ElementDeleter_V001().delete(model, model.classes().get(0).methods.get(0));

    String expected = "class A (aaaaaaaa) {\n  -load()\n}\nclass B (bbbbbbbb)\n";
    assertEquals(canonical(expected), serializer.serialize(model));
  }

  /** Deleting while changes are tracked marks the method and what hangs on it as removed. */
  @Test
  void deletedTracked() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(WITH_REFERENCES);

    new ElementDeleter_V001().delete(model, model.classes().get(0).methods.get(0), "yellow");

    String expected = """
        class A (aaaaaaaa) {
          -save(file: File) (a1a1a1a1) change(removed, yellow)
          -load()
        }
        class B (bbbbbbbb)
        note (a0000001) change(removed, yellow) "on save"
        anchor (f0000001) change(removed, yellow) a0000001 -- a1a1a1a1
        dependency (d0000001) change(removed, yellow) bbbbbbbb -> a1a1a1a1
        """;
    assertEquals(canonical(expected), serializer.serialize(model));
  }

  private static final String MARKED = """
      class A (aaaaaaaa) {
        -save(file: Path was(yellow, "file: File"), force change(added, yellow), count change(removed, yellow)) : void was(yellow, boolean)
        -load() change(added, yellow)
      }
      """;

  /** Accepting keeps the new parameters, return type and methods and drops what was removed. */
  @Test
  void acceptedMarks() throws ClassDiagramParseException {
    String expected = "class A (aaaaaaaa) {\n  -save(file: Path, force) : void\n  -load()\n}\n";
    assertEquals(canonical(expected), accepted(MARKED, "yellow"));
  }

  /** Rejecting brings the old parameters and return type back and drops what was added. */
  @Test
  void rejectedMarks() throws ClassDiagramParseException {
    String expected = "class A (aaaaaaaa) {\n  -save(file: File, count) : boolean\n}\n";
    assertEquals(canonical(expected), rejected(MARKED, "yellow"));
  }

  /** Accepting the removal of a method takes the dependency which ends at it along, rejecting it keeps both. */
  @Test
  void removedMethodWithDependency() throws ClassDiagramParseException {
    String source = """
        class A (aaaaaaaa) {
          -save(file: File) (a1a1a1a1) change(removed, yellow)
        }
        class B (bbbbbbbb)
        dependency (d0000001) bbbbbbbb -> a1a1a1a1
        """;

    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb)\n"), accepted(source, "yellow"));
    assertEquals(canonical(source.replace(" change(removed, yellow)", "")), rejected(source, "yellow"));
  }
}
