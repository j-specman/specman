package classman.model.v001.change;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.Visibility_V001;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeLineFormatTest {
  private final AttributeLineFormat_V001 format = new AttributeLineFormat_V001();

  private AttributeLine_V001 parsed(String text) {
    return format.parse(text).orElseThrow();
  }

  /** The line shows the attribute as the diagram does, an info with a comma or a quote in quotes. */
  @Test
  void textOfAttribute() {
    List<InfoModel_V001> infos = new ArrayList<>(List.of(new InfoModel_V001("Pflicht"), new InfoModel_V001("a, b"),
      new InfoModel_V001("say \"hi\""), new InfoModel_V001("")));
    AttributeModel_V001 attribute = new AttributeModel_V001(Visibility_V001.PRIVATE, "title", "String", infos);
    assertEquals("-title : String [Pflicht, \"a, b\", \"say \\\"hi\\\"\"]", format.textOf(attribute));
    assertEquals("x", format.textOf(new AttributeModel_V001(null, "x", null, new ArrayList<>())));
    assertEquals("x", format.textOf(new AttributeModel_V001(null, "x", "", new ArrayList<>())), "a removed type has no colon");
  }

  /** All parts are found. */
  @Test
  void parseAllParts() {
    AttributeLine_V001 line = parsed("+title : String [Pflicht, 2..50]");
    assertEquals(Visibility_V001.PUBLIC, line.visibility);
    assertEquals("title", line.name);
    assertEquals("String", line.type);
    assertEquals(List.of("Pflicht", "2..50"), line.infos);
  }

  /** Visibility, type and infos are optional, blanks around the parts don't matter. */
  @Test
  void parseOptionalParts() {
    AttributeLine_V001 plain = parsed("  title  ");
    assertNull(plain.visibility);
    assertEquals("title", plain.name);
    assertNull(plain.type);
    assertTrue(plain.infos.isEmpty());
    assertEquals("String", parsed("title:String").type);
    assertNull(parsed("title :").type);
    assertEquals(Visibility_V001.PROTECTED, parsed("# title").visibility);
    assertEquals(Visibility_V001.PACKAGE, parsed("~title").visibility);
  }

  /** A name with spaces needs no quotes. */
  @Test
  void nameWithSpaces() {
    AttributeLine_V001 line = parsed("-In Baum anzeigen : J/N");
    assertEquals("In Baum anzeigen", line.name);
    assertEquals("J/N", line.type);
  }

  /** Infos are separated by commas, but not inside quotes, and empty ones are dropped. */
  @Test
  void parseInfos() {
    assertEquals(List.of("a, b", "say \"hi\"", "c"), parsed("x [\"a, b\", \"say \\\"hi\\\"\", ,c]").infos);
  }

  /** What can't be an attribute is not parsed. */
  @Test
  void invalidLines() {
    for (String invalid : List.of("", "   ", ": String", "+", "x [a, b", "two\nlines", "...")) {
      assertEquals(Optional.empty(), format.parse(invalid), "line: " + invalid.replace("\n", "\\n"));
    }
  }

  /** The line which an attribute is shown as gives the same attribute when it is parsed. */
  @Test
  void roundTrip() {
    List<InfoModel_V001> infos = new ArrayList<>(List.of(new InfoModel_V001("a, b"), new InfoModel_V001("2..50")));
    AttributeModel_V001 attribute = new AttributeModel_V001(Visibility_V001.PACKAGE, "name x", "List<String>", infos);

    AttributeLine_V001 line = parsed(format.textOf(attribute));

    assertEquals(Visibility_V001.PACKAGE, line.visibility);
    assertEquals("name x", line.name);
    assertEquals("List<String>", line.type);
    assertEquals(List.of("a, b", "2..50"), line.infos);
  }
}
