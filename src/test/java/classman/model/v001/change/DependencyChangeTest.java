package classman.model.v001.change;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import org.junit.jupiter.api.Test;

import static classman.model.v001.change.ResolvedDiagrams.accepted;
import static classman.model.v001.change.ResolvedDiagrams.canonical;
import static classman.model.v001.change.ResolvedDiagrams.deleted;
import static classman.model.v001.change.ResolvedDiagrams.rejected;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** A dependency goes with its class or attribute at either end. Its own marks are resolved like those of a relation. */
class DependencyChangeTest {
  private static final String BOXES = """
      class A (aaaaaaaa) {
        -x (a1a1a1a1)
        -y (a2a2a2a2)
      }
      class B (bbbbbbbb) {
        -z (b1b1b1b1)
      }
      class C (cccccccc)
      """;

  /** A vanishing class takes the dependencies of its attributes along, at the start and at the end. */
  @Test
  void vanishingClassTakesDependenciesOfItsAttributes() throws ClassDiagramParseException {
    String source = BOXES.replace("class A (aaaaaaaa)", "class A (aaaaaaaa) change(added, yellow)")
        + "dependency (d0000001) a1a1a1a1 -> cccccccc\ndependency (d0000002) bbbbbbbb -> a2a2a2a2\n"
        + "dependency (d0000003) b1b1b1b1 -> cccccccc\n";
    String expected = """
        class B (bbbbbbbb) {
          -z (b1b1b1b1)
        }
        class C (cccccccc)
        dependency (d0000003) b1b1b1b1 -> cccccccc
        """;
    assertEquals(canonical(expected), rejected(source, "yellow"));
  }

  /** A vanishing attribute takes the dependencies with it, not its class nor the other end. */
  @Test
  void vanishingAttributeTakesItsDependencies() throws ClassDiagramParseException {
    String source = BOXES.replace("-x (a1a1a1a1)", "-x (a1a1a1a1) change(removed, yellow)")
        + "dependency (d0000001) a1a1a1a1 -> cccccccc\ndependency (d0000002) bbbbbbbb -> a1a1a1a1\n"
        + "dependency (d0000003) a2a2a2a2 -> cccccccc\n";
    String expected = """
        class A (aaaaaaaa) {
          -y (a2a2a2a2)
        }
        class B (bbbbbbbb) {
          -z (b1b1b1b1)
        }
        class C (cccccccc)
        dependency (d0000003) a2a2a2a2 -> cccccccc
        """;
    assertEquals(canonical(expected), accepted(source, "yellow"));
  }

  /** The note which hangs only on a vanishing dependency goes with it. */
  @Test
  void vanishingDependencyTakesItsNote() throws ClassDiagramParseException {
    String source = BOXES + "dependency (d0000001) change(added, yellow) a1a1a1a1 -> cccccccc\n"
        + "note (e0000001) \"n\"\nanchor e0000001 -- d0000001\n";
    assertEquals(canonical(BOXES), rejected(source, "yellow"));
  }

  /** Accepting the addition of a dependency, or rejecting its removal, keeps it and drops the mark. */
  @Test
  void survivingDependencyLosesMark() throws ClassDiagramParseException {
    String marked = BOXES + "dependency (d0000001) change(added, yellow) a1a1a1a1 -> cccccccc \"uses\"\n";
    String plain = BOXES + "dependency (d0000001) a1a1a1a1 -> cccccccc \"uses\"\n";
    assertEquals(canonical(plain), accepted(marked, "yellow"));
    assertEquals(canonical(plain), rejected(marked.replace("added", "removed"), "yellow"));
  }

  /** The label of a dependency is a text which a change set can change, like that of any relation. */
  @Test
  void labelChangeResolved() throws ClassDiagramParseException {
    String source = BOXES + "dependency (d0000001) a1a1a1a1 -> cccccccc \"uses\" was(yellow, \"needs\")\n";
    assertEquals(canonical(BOXES + "dependency (d0000001) a1a1a1a1 -> cccccccc \"uses\"\n"), accepted(source, "yellow"));
    assertEquals(canonical(BOXES + "dependency (d0000001) a1a1a1a1 -> cccccccc \"needs\"\n"), rejected(source, "yellow"));
  }

  // ---- deleting ----

  private static final String WITH_DEPENDENCIES = BOXES + "dependency (d0000001) a1a1a1a1 -> cccccccc\n"
      + "dependency (d0000002) cccccccc -> b1b1b1b1\n";

  /** Deleting a class takes the dependencies of its attributes along. */
  @Test
  void deletedClass() throws ClassDiagramParseException {
    String expected = """
        class B (bbbbbbbb) {
          -z (b1b1b1b1)
        }
        class C (cccccccc)
        dependency (d0000002) cccccccc -> b1b1b1b1
        """;
    assertEquals(canonical(expected), deleted(WITH_DEPENDENCIES, "aaaaaaaa"));
  }

  /** While changes are tracked, deleting a class marks its dependencies as removed too. */
  @Test
  void deletedClassWhileTracking() throws ClassDiagramParseException {
    String expected = BOXES.replace("class C (cccccccc)", "class C (cccccccc) change(removed, yellow)")
        + "dependency (d0000001) change(removed, yellow) a1a1a1a1 -> cccccccc\n"
        + "dependency (d0000002) change(removed, yellow) cccccccc -> b1b1b1b1\n";
    assertEquals(canonical(expected), deleted(WITH_DEPENDENCIES, "cccccccc", "yellow"));
  }

  /** Deleting an attribute takes its dependencies along. */
  @Test
  void deletedAttribute() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(WITH_DEPENDENCIES);
    AttributeModel_V001 x = model.classes().get(0).attributes.get(0);

    new ElementDeleter_V001().delete(model, x);

    assertEquals(canonical(BOXES.replace("  -x (a1a1a1a1)\n", "") + "dependency (d0000002) cccccccc -> b1b1b1b1\n"),
        new ClassDiagramSerializer_V001().serialize(model));
  }

  /** While changes are tracked, deleting an attribute marks its dependencies as removed, unless a change set added them. */
  @Test
  void deletedAttributeWhileTracking() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(BOXES + "dependency (d0000001) a1a1a1a1 -> cccccccc\n"
        + "dependency (d0000002) change(added, yellow) cccccccc -> a1a1a1a1\n");
    AttributeModel_V001 x = model.classes().get(0).attributes.get(0);

    new ElementDeleter_V001().delete(model, x, "yellow");

    assertEquals(canonical(BOXES.replace("-x (a1a1a1a1)", "-x (a1a1a1a1) change(removed, yellow)")
        + "dependency (d0000001) change(removed, yellow) a1a1a1a1 -> cccccccc\n"),
        new ClassDiagramSerializer_V001().serialize(model));
  }

  private static ClassDiagramModel_V001 parse(String elements) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse("classdiagram {\n" + elements + "\n}");
  }
}
