package classman.model.v001.change;

import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.TaggedValueModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import org.junit.jupiter.api.Test;

import static classman.model.v001.change.ResolvedDiagrams.accepted;
import static classman.model.v001.change.ResolvedDiagrams.canonical;
import static classman.model.v001.change.ResolvedDiagrams.rejected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaggedValueChangeTest {
  private static final String TWO_TAGS = "class A (aaaaaaaa) {TABLE=books, author=Jan}";

  private final TaggedValueFormat_V001 format = new TaggedValueFormat_V001();
  private final TaggedValueChanger_V001 changer = new TaggedValueChanger_V001();

  private ClassModel_V001 classOf(String elements) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse("classdiagram {\n" + elements + "\n}").classes().get(0);
  }

  /** The mark of a tagged value is read and written behind its value. */
  @Test
  void elementMarkRoundTrip() throws ClassDiagramParseException {
    ClassModel_V001 box = classOf("class A (aaaaaaaa) {TABLE=books change(added, yellow), author=Jan change(removed, blue)}");

    assertEquals(ChangeType_V001.ADDED, box.taggedValues.get(0).change.type);
    assertEquals("blue", box.taggedValues.get(1).change.changeSet);
    String written = new ClassDiagramSerializer_V001().serialize(
      new ClassDiagramParser_V001().parse("classdiagram {\n  class A (aaaaaaaa) {TABLE=books change(added, yellow)}\n}"));
    assertTrue(written.contains("{TABLE=books change(added, yellow)}"), written);
  }

  /** Accepting keeps an added tagged value and drops a removed one; rejecting does the opposite. */
  @Test
  void acceptAndRejectMarks() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {TABLE=books change(added, yellow), author=Jan change(removed, yellow), v=1}";

    assertEquals(canonical("class A (aaaaaaaa) {TABLE=books, v=1}"), accepted(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa) {author=Jan, v=1}"), rejected(source, "yellow"));
  }

  /** The marks of other change sets stay. */
  @Test
  void otherChangeSetsStay() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {TABLE=books change(added, blue), v=1}";

    assertEquals(canonical(source), accepted(source, "yellow"));
  }

  /** What the user types is split at the first equals sign. The value may have blanks and equals signs. */
  @Test
  void parseLine() {
    assertEquals("TABLE", format.parse("TABLE=books").orElseThrow().key);
    assertEquals("a b=c", format.parse(" author = a b=c ").orElseThrow().value);
    assertEquals("author", format.parse(" author = a b=c ").orElseThrow().key);
  }

  /** A tagged value may have only a key. */
  @Test
  void keyOnly() {
    assertEquals("abc", format.parse(" abc ").orElseThrow().key);
    assertNull(format.parse("abc").orElseThrow().value);
  }

  /** Not a tagged value: no key, an unfit key, an equals sign without a value, a line break. */
  @Test
  void invalidLines() {
    for (String text : new String[] {"=x", "a b=x", "1=x", "a=", "a= ", "a=x\ny", "a b", ""}) {
      assertFalse(format.parse(text).isPresent(), text);
    }
  }

  /** Without tracking, a tagged value just gets the new key and value. */
  @Test
  void plainChange() throws ClassDiagramParseException {
    ClassModel_V001 box = classOf(TWO_TAGS);

    changer.apply(box, box.taggedValues.get(1), format.parse("writer=Anna").orElseThrow(), null);

    assertEquals("writer", box.taggedValues.get(1).key);
    assertEquals("Anna", box.taggedValues.get(1).value);
    assertNull(box.taggedValues.get(1).keyChange);
  }

  /** With tracking, key and value get a text mark with the old text, and only what changed. */
  @Test
  void trackedChange() throws ClassDiagramParseException {
    ClassModel_V001 box = classOf(TWO_TAGS);
    TaggedValueModel_V001 tag = box.taggedValues.get(1);

    changer.apply(box, tag, format.parse("author=Anna").orElseThrow(), "yellow");

    assertNull(tag.keyChange);
    assertEquals("Jan", tag.valueChange.backup);
    assertEquals("yellow", tag.valueChange.changeSet);

    changer.apply(box, tag, format.parse("author=Jan").orElseThrow(), "yellow");

    assertNull(tag.valueChange);
  }

  /** A tagged value which a change set has added is changed without marks. */
  @Test
  void addedIsChangedPlainly() throws ClassDiagramParseException {
    ClassModel_V001 box = classOf("class A (aaaaaaaa) {TABLE=books change(added, yellow)}");

    changer.apply(box, box.taggedValues.get(0), format.parse("TABLE=things").orElseThrow(), "yellow");

    assertEquals("things", box.taggedValues.get(0).value);
    assertNull(box.taggedValues.get(0).valueChange);
  }

  /** A tagged value without a value is written with an equals sign and nothing behind it, and read back. */
  @Test
  void keyOnlyRoundTrip() throws ClassDiagramParseException {
    ClassModel_V001 box = classOf("class A (aaaaaaaa) {persistent=, TABLE=books, flag= change(added, yellow)}");

    assertNull(box.taggedValues.get(0).value);
    assertEquals("books", box.taggedValues.get(1).value);
    assertEquals(ChangeType_V001.ADDED, box.taggedValues.get(2).change.type);
    assertEquals(canonical("class A (aaaaaaaa) {persistent=, TABLE=books, flag= change(added, yellow)}"),
      canonical("class A (aaaaaaaa) {persistent=, TABLE=books, flag=\"\" change(added, yellow)}"));
  }

  /** Taking the value away keeps the key, tracked with the old value as backup; rejecting brings the value back. */
  @Test
  void valueTakenAway() throws ClassDiagramParseException {
    ClassModel_V001 box = classOf(TWO_TAGS);
    TaggedValueModel_V001 tag = box.taggedValues.get(1);

    changer.apply(box, tag, format.parse("author").orElseThrow(), "yellow");

    assertEquals("Jan", tag.valueChange.backup);
    assertEquals("author", format.textOf(tag));
    String source = "class A (aaaaaaaa) {TABLE=books, author= was(yellow, Jan)}";
    assertEquals(canonical("class A (aaaaaaaa) {TABLE=books, author=Jan}"), rejected(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa) {TABLE=books, author=}"), accepted(source, "yellow"));
  }

  /** A new tagged value is marked as added while changes are tracked, and in a class which is added or removed it
   * is not. */
  @Test
  void createdTagged() throws ClassDiagramParseException {
    ClassModel_V001 box = classOf("class A (aaaaaaaa)");
    TaggedValueLine_V001 line = format.parse("k=v").orElseThrow();

    assertNull(changer.create(box, line, null).change);
    assertEquals(ChangeType_V001.ADDED, changer.create(box, line, "yellow").change.type);

    ClassModel_V001 added = classOf("class A (aaaaaaaa) change(added, yellow)");
    assertNull(changer.create(added, line, "yellow").change);
  }

  /** Deleting removes the tagged value, or marks it as removed while changes are tracked. One which was added is gone. */
  @Test
  void deleted() throws ClassDiagramParseException {
    ClassModel_V001 box = classOf("class A (aaaaaaaa) {a=1, b=2 change(added, blue), c=3}");

    changer.delete(box, box.taggedValues.get(0), null);
    assertEquals(2, box.taggedValues.size());

    changer.delete(box, box.taggedValues.get(0), "yellow");
    assertEquals(1, box.taggedValues.size());

    changer.delete(box, box.taggedValues.get(0), "yellow");
    assertEquals(ChangeType_V001.REMOVED, box.taggedValues.get(0).change.type);
    assertEquals("c", box.taggedValues.get(0).key);

    changer.delete(box, box.taggedValues.get(0), "blue");
    assertEquals("yellow", box.taggedValues.get(0).change.changeSet);
  }
}
