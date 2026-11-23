package classman.model.v001.io;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.DependencyRelationModel_V001;
import classman.model.v001.RelationType_V001;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The text of a dependency: like an implementation, with ends which may be attributes. */
class DependencyFormatTest {
  private static final ClassDiagramParser_V001 PARSER = new ClassDiagramParser_V001();
  private static final ClassDiagramSerializer_V001 SERIALIZER = new ClassDiagramSerializer_V001();

  private static String diagram(String elements) {
    return "classdiagram {\n" + elements + "\n}";
  }

  private static DependencyRelationModel_V001 parse(String element) throws ClassDiagramParseException {
    AbstractRelationModel_V001 relation = PARSER.parse(diagram(element)).relations().get(0);
    return (DependencyRelationModel_V001) relation;
  }

  /** The plainest dependency has two IDs and an arrow. */
  @Test
  void plain() throws ClassDiagramParseException {
    DependencyRelationModel_V001 dependency = parse("dependency 11111111 -> 22222222");

    assertEquals(RelationType_V001.DEPENDENCY, dependency.type);
    assertEquals("11111111", dependency.fromId);
    assertEquals("22222222", dependency.toId);
  }

  /** ID, change mark, style, label with its change mark and place, and a route on attributes are all kept. */
  @Test
  void fullRoundTrip() throws ClassDiagramParseException {
    String source = "class A (11111111) {\n    x (a0000001)\n}\nclass B (22222222)\n"
      + "dependency (d0000001) change(added, yellow) style(linecolor=\"#cc0000\") a0000001 -> 22222222 \"uses\" "
      + "was(yellow, \"needs\") at(40%, -12)\n  route a0000001.right@8 (200, 30) 22222222.left@10\n";
    DependencyRelationModel_V001 dependency = (DependencyRelationModel_V001) PARSER.parse(diagram(source)).relations().get(0);

    assertEquals("d0000001", dependency.id);
    assertEquals(ChangeType_V001.ADDED, dependency.change.type);
    assertEquals("uses", dependency.label);
    assertEquals("a0000001", dependency.route.start.classId);
    assertEquals(1, dependency.route.waypoints.size());
    String written = SERIALIZER.serialize(PARSER.parse(diagram(source)));
    assertTrue(written.contains("dependency (d0000001) change(added, yellow) style(linecolor=\"#cc0000\") a0000001 -> 22222222"),
      written);
    assertEquals(written, SERIALIZER.serialize(PARSER.parse(written)));
  }

  /** A dependency has no multiplicities and no other arrow than the one at its target. */
  @Test
  void onlyArrowToTarget() {
    assertThrows(ClassDiagramParseException.class, () -> parse("dependency 11111111 -- 22222222"));
    assertThrows(ClassDiagramParseException.class, () -> parse("dependency 11111111 <-> 22222222"));
    assertThrows(ClassDiagramParseException.class, () -> parse("dependency 11111111 1 -> * 22222222"));
  }

  /** The word dependency still works as a name where a word is expected. */
  @Test
  void keywordStillUsableAsAttributeName() throws ClassDiagramParseException {
    String source = "class A (11111111) {\n    dependency\n}";

    assertEquals("dependency", PARSER.parse(diagram(source)).classes().get(0).attributes.get(0).name);
  }
}
