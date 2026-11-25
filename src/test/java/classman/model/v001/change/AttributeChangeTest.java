package classman.model.v001.change;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import org.junit.jupiter.api.Test;

import static classman.model.v001.change.ResolvedDiagrams.canonical;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Changing, creating and deleting attributes of a class, with and without change tracking. */
class AttributeChangeTest {
  private static final String CLASS = "class A (aaaaaaaa) {\n  -title : String [Pflicht, 2..50]\n}\n";

  private final ClassDiagramParser_V001 parser = new ClassDiagramParser_V001();
  private final ClassDiagramSerializer_V001 serializer = new ClassDiagramSerializer_V001();
  private final AttributeLineFormat_V001 format = new AttributeLineFormat_V001();
  private final AttributeChanger_V001 changer = new AttributeChanger_V001();

  private ClassDiagramModel_V001 parse(String elements) throws ClassDiagramParseException {
    return parser.parse("classdiagram {\n" + elements + "\n}");
  }

  private AttributeLine_V001 line(String text) {
    return format.parse(text).orElseThrow();
  }

  /** Gives the first attribute of the first class the line and returns the canonical text of the diagram. */
  private String applied(String elements, String line, String changeSet) throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(elements);
    ClassModel_V001 box = model.classes().get(0);
    changer.apply(box, box.attributes.get(0), line(line), changeSet);
    return serializer.serialize(model);
  }

  /** Without tracking all parts of the line are taken over, with no marks. */
  @Test
  void untracked() throws ClassDiagramParseException {
    String expected = "class A (aaaaaaaa) {\n  +headline : Text [Pflicht, 2..60, unique]\n}\n";
    assertEquals(canonical(expected), applied(CLASS, "+headline : Text [Pflicht, 2..60, unique]", null));
  }

  /** The line as it is now changes nothing, tracked or not. */
  @Test
  void unchangedLine() throws ClassDiagramParseException {
    assertEquals(canonical(CLASS), applied(CLASS, "-title : String [Pflicht, 2..50]", "yellow"));
    assertEquals(canonical(CLASS), applied(CLASS, "-title : String [Pflicht, 2..50]", null));
  }

  /** While changes are tracked, each part which differs gets its mark with the old text as backup. */
  @Test
  void tracked() throws ClassDiagramParseException {
    String expected = """
        class A (aaaaaaaa) {
          + was(yellow, "-") headline was(yellow, title) : Text was(yellow, String) [Pflicht, 2..60 was(yellow, "2..50"), unique change(added, yellow)]
        }
        """;
    assertEquals(canonical(expected), applied(CLASS, "+headline : Text [Pflicht, 2..60, unique]", "yellow"));
  }

  /** Only the part which differs is marked. */
  @Test
  void trackedTypeOnly() throws ClassDiagramParseException {
    String expected = "class A (aaaaaaaa) {\n  -title : Text was(yellow, String) [Pflicht, 2..50]\n}\n";
    assertEquals(canonical(expected), applied(CLASS, "-title : Text [Pflicht, 2..50]", "yellow"));
  }

  /** An info which is left out stays with its text and is marked as removed. */
  @Test
  void trackedRemovedInfo() throws ClassDiagramParseException {
    String expected = "class A (aaaaaaaa) {\n  -title : String [Pflicht, 2..50 change(removed, yellow)]\n}\n";
    assertEquals(canonical(expected), applied(CLASS, "-title : String [Pflicht]", "yellow"));
  }

  /** An info which the change set added is gone completely when it is removed again. */
  @Test
  void addedInfoRemovedAgain() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(CLASS);
    ClassModel_V001 box = model.classes().get(0);
    AttributeModel_V001 attribute = box.attributes.get(0);
    changer.apply(box, attribute, line("-title : String [Pflicht, 2..50, unique]"), "yellow");

    changer.apply(box, attribute, line("-title : String [Pflicht, 2..50]"), "yellow");

    assertEquals(canonical(CLASS), serializer.serialize(model));
  }

  /** An info in the middle is told apart from the others, and new infos come after the one before them. */
  @Test
  void infosInTheMiddle() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {\n  x [a, b, c]\n}\n";
    String expected = "class A (aaaaaaaa) {\n  x [a, b change(removed, yellow), c, d change(added, yellow)]\n}\n";
    assertEquals(canonical(expected), applied(source, "x [a, c, d]", "yellow"));
    String inserted = "class A (aaaaaaaa) {\n  x [a, n change(added, yellow), b, c]\n}\n";
    assertEquals(canonical(inserted), applied(source, "x [a, n, b, c]", "yellow"));
  }

  /** An info which the change set removed and which is typed again is not removed any more. */
  @Test
  void removedInfoTypedAgain() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {\n  x [a, b, c]\n}\n";
    ClassDiagramModel_V001 model = parse(source);
    ClassModel_V001 box = model.classes().get(0);
    AttributeModel_V001 attribute = box.attributes.get(0);
    changer.apply(box, attribute, line("x [a, c]"), "yellow");
    assertEquals(canonical("class A (aaaaaaaa) {\n  x [a, b change(removed, yellow), c]\n}\n"), serializer.serialize(model));

    changer.apply(box, attribute, line("x [a, b, c]"), "yellow");

    assertEquals(canonical(source), serializer.serialize(model));
  }

  /** An info which another change set has added is gone at once if it is removed, whoever removes it. */
  @Test
  void infoOfOtherChangeSetRemoved() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {\n  x [a, b change(added, pink), c]\n}\n";
    assertEquals(canonical("class A (aaaaaaaa) {\n  x [a, c]\n}\n"), applied(source, "x [a, c]", "yellow"));
  }

  /** An info which was changed and is removed afterwards keeps the mark of its text, so that rejecting brings back
   * the old text. */
  @Test
  void changedInfoRemoved() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse("class A (aaaaaaaa) {\n  x [a, b]\n}\n");
    ClassModel_V001 box = model.classes().get(0);
    AttributeModel_V001 attribute = box.attributes.get(0);
    changer.apply(box, attribute, line("x [a, B]"), "yellow");

    changer.apply(box, attribute, line("x [a]"), "yellow");

    String expected = "class A (aaaaaaaa) {\n  x [a, B change(removed, yellow)]\n}\n";
    assertEquals(canonical(expected), serializer.serialize(model).replace(" was(yellow, b)", ""));
    assertEquals("b", attribute.infos.get(1).textChange.backup);
  }

  /** A visibility sign which was there can't be removed under tracking, unless the change set has added it. */
  @Test
  void visibilityRemoval() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(CLASS);
    ClassModel_V001 box = model.classes().get(0);
    AttributeModel_V001 attribute = box.attributes.get(0);

    assertFalse(changer.canApply(box, attribute, line("title : String [Pflicht, 2..50]"), "yellow"));
    assertTrue(changer.canApply(box, attribute, line("title : String [Pflicht, 2..50]"), null));
    assertTrue(changer.canApply(box, attribute, line("+title : String [Pflicht, 2..50]"), "yellow"));

    ClassDiagramModel_V001 added = parse("class A (aaaaaaaa) {\n  title\n}\n");
    ClassModel_V001 addedBox = added.classes().get(0);
    changer.apply(addedBox, addedBox.attributes.get(0), line("+title"), "yellow");
    assertTrue(changer.canApply(addedBox, addedBox.attributes.get(0), line("title"), "yellow"));
    changer.apply(addedBox, addedBox.attributes.get(0), line("title"), "yellow");
    assertEquals(canonical("class A (aaaaaaaa) {\n  title\n}\n"), serializer.serialize(added));
  }

  /** In a class which a change set has added, attributes are changed without marks. */
  @Test
  void inAddedClass() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) change(added, yellow) {\n  title : String\n}\n";
    String expected = "class A (aaaaaaaa) change(added, yellow) {\n  headline : Text\n}\n";
    assertEquals(canonical(expected), applied(source, "headline : Text", "yellow"));
  }

  /** A new attribute is marked as added while changes are tracked, in an added class it carries no mark. */
  @Test
  void created() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(CLASS);
    ClassModel_V001 box = model.classes().get(0);

    AttributeModel_V001 tracked = changer.create(box, line("+isbn : String [unique]"), "yellow");
    AttributeModel_V001 untracked = changer.create(box, line("pages"), null);

    assertEquals("yellow", tracked.change.changeSet);
    assertEquals("isbn", tracked.name);
    assertEquals("unique", tracked.infos.get(0).text);
    assertNull(untracked.change);
    ClassDiagramModel_V001 addedModel = parse("class B (bbbbbbbb) change(added, yellow)\n");
    assertNull(changer.create(addedModel.classes().get(0), line("pages"), "yellow").change);
  }

  private static final String WITH_NOTES = """
      class A (aaaaaaaa) {
        -title (a1a1a1a1) : String
        -pages
      }
      note (a0000001) "on title"
      note (a0000002) "free"
      anchor (f0000001) a0000001 -- a1a1a1a1
      """;

  /** Deleting an attribute without tracking takes the notes attached to it along. */
  @Test
  void deleted() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(WITH_NOTES);
    new ElementDeleter_V001().delete(model, model.classes().get(0).attributes.get(0));
    String expected = "class A (aaaaaaaa) {\n  -pages\n}\nnote (a0000002) \"free\"\n";
    assertEquals(canonical(expected), serializer.serialize(model));
  }

  /** While changes are tracked the attribute and the notes attached to it are marked as removed. */
  @Test
  void deletedTracked() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(WITH_NOTES);
    new ElementDeleter_V001().delete(model, model.classes().get(0).attributes.get(0), "yellow");
    String expected = """
        class A (aaaaaaaa) {
          -title (a1a1a1a1) change(removed, yellow) : String
          -pages
        }
        note (a0000001) change(removed, yellow) "on title"
        note (a0000002) "free"
        anchor (f0000001) change(removed, yellow) a0000001 -- a1a1a1a1
        """;
    assertEquals(canonical(expected), serializer.serialize(model));
  }

  /** An attribute which a change set has added goes at once, whoever deletes it. */
  @Test
  void addedAttributeDeletedTracked() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {\n  -title (a1a1a1a1) change(added, pink) : String\n  -pages\n}\n";
    ClassDiagramModel_V001 model = parse(source);
    new ElementDeleter_V001().delete(model, model.classes().get(0).attributes.get(0), "yellow");
    assertEquals(canonical("class A (aaaaaaaa) {\n  -pages\n}\n"), serializer.serialize(model));
  }
}
