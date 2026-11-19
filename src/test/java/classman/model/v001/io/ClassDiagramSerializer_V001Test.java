package classman.model.v001.io;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassDiagramSerializer_V001Test {

  private final ClassDiagramParser_V001 parser = new ClassDiagramParser_V001();
  private final ClassDiagramSerializer_V001 serializer = new ClassDiagramSerializer_V001();

  private static String example(String name) throws IOException {
    try (InputStream in = ClassDiagramSerializer_V001Test.class.getResourceAsStream("/classman/model/v001/" + name + ".cld")) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String roundTrip(String source) throws ClassDiagramParseException {
    return serializer.serialize(parser.parse(source));
  }

  /** Writing a model and reading the text again gives the same model, checked for all examples. */
  @ParameterizedTest
  @ValueSource(strings = {"hello", "benchmark", "edgecases", "changes", "textchanges", "change-colors"})
  void roundTripKeepsModel(String exampleName) throws Exception {
    ClassDiagramModel_V001 original = parser.parse(example(exampleName));
    ClassDiagramModel_V001 again = parser.parse(serializer.serialize(original));
    assertEquals(ClassDiagramDump.of(original), ClassDiagramDump.of(again));
  }

  /** The written text is the canonical form: reading and writing it again changes nothing. */
  @ParameterizedTest
  @ValueSource(strings = {"hello", "benchmark", "edgecases", "changes", "textchanges", "change-colors"})
  void canonicalTextIsStable(String exampleName) throws Exception {
    String written = roundTrip(example(exampleName));
    assertEquals(written, roundTrip(written));
  }

  /** The first line is the JSON envelope comment with the version and the model type. */
  @Test
  void envelopeComment() throws ClassDiagramParseException {
    String written = roundTrip("classdiagram { }");
    assertTrue(written.startsWith(
        "// {\"classmanVersion\":\"0.1\",\"modelType\":\"classman.model.v001.ClassDiagramModel_V001\"}\n"), written);
  }

  /** Quote, backslash, line break and backtick in a text survive writing and reading, and no backtick is written. */
  @Test
  void specialCharactersEscaped() throws ClassDiagramParseException {
    String text = "say \"hi\" to C:\\temp`s\r\nnext line";
    ClassDiagramModel_V001 model = new ClassDiagramModel_V001(null);
    model.elements.add(new NoteModel_V001(text));
    String written = serializer.serialize(model);
    assertTrue(!written.contains("`"), written);
    assertEquals(text.replace("\r\n", "\n"), parser.parse(written).notes().get(0).text);
  }

  /** Relation endpoints and route anchors are written as bare IDs, the ID of the relation itself in parentheses. */
  @Test
  void bareIdsInRelations() throws ClassDiagramParseException {
    String written = roundTrip("""
        classdiagram {
          class Same (aaaaaaaa)
          class Same (bbbbbbbb)
          composition (11223344) aaaaaaaa 1 -> * bbbbbbbb "owns"
            route aaaaaaaa.right@5 (10, 20) bbbbbbbb.left@7
        }""");
    assertTrue(written.contains("    composition (11223344) aaaaaaaa 1 -> * bbbbbbbb \"owns\"\n"), written);
    assertTrue(written.contains("route aaaaaaaa.right@5 (10, 20) bbbbbbbb.left@7\n"), written);
  }

  /** Names get quotes only if they have special characters or are keywords. */
  @Test
  void quotingOnlyWhenNeeded() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = new ClassDiagramModel_V001("Hilfe Versionierung");
    model.elements.add(new ClassModel_V001("Plain-Name"));
    model.elements.add(new ClassModel_V001("Hilfe Eintrag"));
    model.elements.add(new ClassModel_V001("left"));
    String written = serializer.serialize(model);
    assertTrue(written.contains("classdiagram \"Hilfe Versionierung\" {"), written);
    assertTrue(written.contains("class Plain-Name\n"), written);
    assertTrue(written.contains("class \"Hilfe Eintrag\"\n"), written);
    assertTrue(written.contains("class \"left\"\n"), written);
  }

  /** A line break in a label or note text is written as the two characters backslash and n. */
  @Test
  void lineBreakEscaping() throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = new ClassDiagramModel_V001(null);
    model.elements.add(new NoteModel_V001("one\ntwo"));
    assertTrue(serializer.serialize(model).contains("note \"one\\ntwo\""));
  }

  /** Attribute infos with spaces, commas or ranges survive writing and reading again. */
  @Test
  void infosRoundTrip() throws Exception {
    String source = "classdiagram { class A { x [must be unique, \"a, b\", 2..50] } }";
    assertEquals(ClassDiagramDump.of(parser.parse(source)), ClassDiagramDump.of(parser.parse(roundTrip(source))));
  }

  /** A text mark is written right behind its text, the backup bare if it is a simple word or multiplicity. */
  @Test
  void textChangesWritten() throws ClassDiagramParseException {
    String written = roundTrip("""
        classdiagram {
          class A was(yellow, "Alte Klasse") (aaaaaaaa)
          class B (bbbbbbbb)
          association aaaaaaaa 1 was(yellow, "0..1") -> bbbbbbbb "" was(blue, "old\\nlabel")
        }""");
    assertTrue(written.contains("class A was(yellow, \"Alte Klasse\") (aaaaaaaa)\n"), written);
    assertTrue(written.contains("aaaaaaaa 1 was(yellow, 0..1) -> bbbbbbbb \"\" was(blue, \"old\\nlabel\")\n"), written);
  }

  /** A removed label (empty string with mark) is kept, a mark without backup stays without backup. */
  @Test
  void emptyTextWithMark() throws ClassDiagramParseException {
    String written = roundTrip("classdiagram { class A (aaaaaaaa)\n association aaaaaaaa -- aaaaaaaa \"\" was(yellow)\n}");
    assertTrue(written.contains("aaaaaaaa -- aaaaaaaa \"\" was(yellow)\n"), written);
  }
}
