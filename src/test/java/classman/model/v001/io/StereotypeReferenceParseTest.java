package classman.model.v001.io;

import classman.model.v001.ClassModel_V001;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A stereotype which begins with : refers to a configuration of the host, whose name has to be one word. */
class StereotypeReferenceParseTest {

  private static ClassModel_V001 classOf(String stereotype) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse("classdiagram {\n  class A <<" + stereotype + ">>\n}").classes().get(0);
  }

  @Test
  void referenceKeepsItsName() throws ClassDiagramParseException {
    assertEquals(":Domain_Value2", classOf(":Domain_Value2").stereotype);
  }

  @Test
  void referenceRoundTrips() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class A <<:Domain_Value>>\n}";
    String written = new ClassDiagramSerializer_V001().serialize(new ClassDiagramParser_V001().parse(source));

    assertEquals(":Domain_Value", new ClassDiagramParser_V001().parse(written).classes().get(0).stereotype);
  }

  @Test
  void referenceWithoutNameIsRejected() {
    assertThrows(ClassDiagramParseException.class, () -> classOf(":"));
  }

  @Test
  void referenceWithBlankOrSpecialCharactersIsRejected() {
    assertThrows(ClassDiagramParseException.class, () -> classOf(":Domain Value"));
    assertThrows(ClassDiagramParseException.class, () -> classOf(":Dömain"));
  }

  @Test
  void oldTextOfChangeMarkMayBeReference() throws ClassDiagramParseException {
    ClassModel_V001 model = new ClassDiagramParser_V001()
      .parse("classdiagram {\n  class A <<Entity>> was(yellow, \":Domain_Value\")\n}").classes().get(0);

    assertEquals(":Domain_Value", model.stereotypeChange.backup);
  }
}
