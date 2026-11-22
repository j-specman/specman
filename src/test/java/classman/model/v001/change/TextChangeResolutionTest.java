package classman.model.v001.change;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import static classman.model.v001.change.ResolvedDiagrams.accepted;
import static classman.model.v001.change.ResolvedDiagrams.canonical;
import static classman.model.v001.change.ResolvedDiagrams.rejected;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TextChangeResolutionTest {

  /** An info is a list entry: added and removed ones are decided as a whole, those of other change sets stay. */
  @Test
  void infoElementMarks() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {\n x [a, b change(removed, yellow), c change(added, yellow), "
      + "d change(removed, pink)]\n}";
    assertEquals(canonical("class A (aaaaaaaa) {\n x [a, c, d change(removed, pink)]\n}"), accepted(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa) {\n x [a, b, d change(removed, pink)]\n}"), rejected(source, "yellow"));
  }

  /** Accepting keeps the new visibility sign, rejecting brings back the old one or none. */
  @Test
  void visibilityChange() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {\n + was(yellow, \"-\") x\n - was(yellow) y\n}";
    assertEquals(canonical("class A (aaaaaaaa) {\n +x\n -y\n}"), accepted(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa) {\n -x\n y\n}"), rejected(source, "yellow"));
  }

  /** Accepting keeps the new text, rejecting brings back the backup. */
  @Test
  void classNameChange() throws ClassDiagramParseException {
    String source = "class Order was(yellow, Bestellung) (aaaaaaaa)";
    assertEquals(canonical("class Order (aaaaaaaa)"), accepted(source, "yellow"));
    assertEquals(canonical("class Bestellung (aaaaaaaa)"), rejected(source, "yellow"));
  }

  /** Attribute type, multiplicities and relation label. */
  @Test
  void relationAndAttributeTexts() throws ClassDiagramParseException {
    String classes = "class A (aaaaaaaa) {\n -x : String was(yellow, Integer)\n}\nclass B (bbbbbbbb)\n";
    String source = classes
        + "association aaaaaaaa 1 was(yellow, \"0..1\") -> * bbbbbbbb \"places\" was(yellow, \"orders\")";
    String accepted = "class A (aaaaaaaa) {\n -x : String\n}\nclass B (bbbbbbbb)\n"
        + "association aaaaaaaa 1 -> * bbbbbbbb \"places\"";
    String rejected = "class A (aaaaaaaa) {\n -x : Integer\n}\nclass B (bbbbbbbb)\n"
        + "association aaaaaaaa 0..1 -> * bbbbbbbb \"orders\"";
    assertEquals(canonical(accepted), accepted(source, "yellow"));
    assertEquals(canonical(rejected), rejected(source, "yellow"));
  }

  /** A removed multiplicity (empty string with mark) is gone when accepted and back when rejected. */
  @Test
  void removedMultiplicity() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa)\nclass B (bbbbbbbb)\n"
        + "association aaaaaaaa \"\" was(yellow, \"1..<max capacity>\") -> * bbbbbbbb";
    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb)\nassociation aaaaaaaa -> * bbbbbbbb"),
        accepted(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb)\n"
        + "association aaaaaaaa \"1..<max capacity>\" -> * bbbbbbbb"), rejected(source, "yellow"));
  }

  /** The text of a note and an info item of an attribute. */
  @Test
  void noteTextAndInfoItem() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {\n -x [Pflicht was(yellow, optional), 2..50]\n}\n"
        + "note \"new\" was(yellow, \"old\")";
    assertEquals(canonical("class A (aaaaaaaa) {\n -x [Pflicht, 2..50]\n}\nnote \"new\""), accepted(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa) {\n -x [optional, 2..50]\n}\nnote \"old\""), rejected(source, "yellow"));
  }

  /** Stereotype and tagged value: the marks of the other change set stay. */
  @Test
  void stereotypeAndTaggedValue() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) <<Entity>> was(yellow, Table) (e3a1d7c2) {T was(blue, S)=orders was(yellow, old)}";
    assertEquals(canonical("class A (aaaaaaaa) <<Entity>> (e3a1d7c2) {T was(blue, S)=orders}"),
        accepted(source, "yellow"));
    assertEquals(canonical("class A (aaaaaaaa) <<Table>> (e3a1d7c2) {T was(blue, S)=old}"),
        rejected(source, "yellow"));
  }

  /** A text which the change set removed (empty string with the mark) is gone after accepting. */
  @Test
  void removedTextAccepted() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) {K=\"\" was(yellow, v), L=w} {\n"
        + " -x : \"\" was(yellow, Integer) [\"\" was(yellow, a), b]\n}\nclass B (bbbbbbbb)\n"
        + "association aaaaaaaa -- bbbbbbbb \"\" was(yellow, \"gone\")";
    assertEquals(canonical("class A (aaaaaaaa) {K=, L=w} {\n -x [b]\n}\nclass B (bbbbbbbb)\nassociation aaaaaaaa -- bbbbbbbb"),
        accepted(source, "yellow"));
  }


  /** Text which didn't exist before (mark without backup) is gone after rejecting. */
  @Test
  void addedTextRejected() throws ClassDiagramParseException {
    String source = "class A (aaaaaaaa) <<Entity>> was(yellow) (e3a1d7c2) {K=v was(yellow), L=w} {\n"
        + " -x : String was(yellow) [a was(yellow), b]\n}\nclass B (bbbbbbbb)\n"
        + "association aaaaaaaa 1 was(yellow) -> bbbbbbbb \"new\" was(yellow)";
    assertEquals(canonical("class A (aaaaaaaa) {K=, L=w} {\n -x [b]\n}\nclass B (bbbbbbbb)\nassociation aaaaaaaa -> bbbbbbbb"),
        rejected(source, "yellow"));
  }

  /** The marks of other change sets stay where they are. */
  @Test
  void otherChangeSetsStay() throws ClassDiagramParseException {
    String source = "class A was(yellow, X) (aaaaaaaa)\nclass B was(blue, Y) (bbbbbbbb)";
    assertEquals(canonical("class X (aaaaaaaa)\nclass B was(blue, Y) (bbbbbbbb)"), rejected(source, "yellow"));
  }

  /** Text marks inside a class that stays are resolved too, inside a class that goes they go with it. */
  @Test
  void marksInsideAddedClass() throws ClassDiagramParseException {
    String source = "class A was(yellow, X) (aaaaaaaa) change(added, yellow)\n"
        + "class B (bbbbbbbb) change(added, yellow) {\n -y was(yellow, z)\n}";
    assertEquals(canonical("class A (aaaaaaaa)\nclass B (bbbbbbbb) {\n -y\n}"), accepted(source, "yellow"));
    assertEquals(canonical(""), rejected(source, "yellow"));
  }
}
