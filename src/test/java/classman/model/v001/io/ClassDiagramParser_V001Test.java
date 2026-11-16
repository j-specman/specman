package classman.model.v001.io;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ExtendsRelationModel_V001;
import classman.model.v001.ImplementsRelationModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.RelationType_V001;
import classman.model.v001.Side_V001;
import classman.model.v001.Visibility_V001;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassDiagramParser_V001Test {

  private final ClassDiagramParser_V001 parser = new ClassDiagramParser_V001();

  private static <T> T assertInstanceOf(Class<T> type, Object value) {
    assertTrue(type.isInstance(value), "expected " + type.getSimpleName() + " but was " + value);
    return type.cast(value);
  }

  private ClassDiagramModel_V001 parse(String elements) throws ClassDiagramParseException {
    return parser.parse("classdiagram {\n" + elements + "\n}");
  }

  private ClassModel_V001 onlyClass(String elements) throws ClassDiagramParseException {
    return parse(elements).classes().get(0);
  }

  /** Id, position, width, stereotype, tagged values and attributes of a class are read. */
  @Test
  void classHeaderParts() throws ClassDiagramParseException {
    ClassModel_V001 c = onlyClass("""
        class `Hilfe Eintrag` (9a31c4e0) at(-20, 30) width=140 <<Entity>> {TABLENAME=helpversion, SCHEMA=`x y`} {
          -Name : String [Pflicht, 2..50, TN=key]
          ...
        }""");
    assertEquals("Hilfe Eintrag", c.name);
    assertEquals("9a31c4e0", c.id);
    assertEquals(-20, c.position.x);
    assertEquals(30, c.position.y);
    assertEquals(140, c.width);
    assertEquals("Entity", c.stereotype);
    assertEquals(2, c.taggedValues.size());
    assertEquals("TABLENAME", c.taggedValues.get(0).key);
    assertEquals("helpversion", c.taggedValues.get(0).value);
    assertEquals("x y", c.taggedValues.get(1).value);
    AttributeModel_V001 name = c.attributes.get(0);
    assertEquals(Visibility_V001.PRIVATE, name.visibility);
    assertEquals("Name", name.name);
    assertEquals("String", name.type);
    assertEquals(List.of("Pflicht", "2..50", "TN=key"), name.infos);
    assertTrue(c.attributes.get(1).hiddenAttributesMarker);
  }

  /** Whatever the source leaves out is null or empty in the model, never a default. */
  @Test
  void absentPartsAreNull() throws ClassDiagramParseException {
    ClassModel_V001 c = onlyClass("class A");
    assertEquals("A", c.name);
    assertNull(c.id);
    assertNull(c.position);
    assertNull(c.width);
    assertNull(c.stereotype);
    assertTrue(c.taggedValues.isEmpty());
    assertTrue(c.attributes.isEmpty());
  }

  /** Backticks around an attribute name or type are dropped, the text inside is kept. */
  @Test
  void quotedAttributeNameAndType() throws ClassDiagramParseException {
    ClassModel_V001 c = onlyClass("class A { `In Baum anzeigen` : `J/N ja` }");
    AttributeModel_V001 attribute = c.attributes.get(0);
    assertNull(attribute.visibility);
    assertEquals("In Baum anzeigen", attribute.name);
    assertEquals("J/N ja", attribute.type);
  }

  /** An attribute may be called width or left although these are keywords. */
  @Test
  void keywordAsAttributeName() throws ClassDiagramParseException {
    AttributeModel_V001 attribute = onlyClass("class A { width : left }").attributes.get(0);
    assertEquals("width", attribute.name);
    assertEquals("left", attribute.type);
  }

  /** A class written in one line with a single attribute is fine. */
  @Test
  void oneLineClass() throws ClassDiagramParseException {
    assertEquals(1, onlyClass("class A { x }").attributes.size());
  }

  /** Two attributes in one line mean a forgotten backtick. The parser reports it with the line number. */
  @Test
  void unquotedSpacesRejected() {
    ClassDiagramParseException e = assertThrows(ClassDiagramParseException.class,
        () -> parse("class A {\n  In Baum anzeigen : J/N\n}"));
    assertTrue(e.getMessage().startsWith("line 3"), e.getMessage());
    assertTrue(e.getMessage().contains("backticks"), e.getMessage());
  }

  /** Attributes in separate lines are separate attributes. */
  @Test
  void attributePerLine() throws ClassDiagramParseException {
    List<AttributeModel_V001> attributes = onlyClass("class A {\n  Sprache\n  Beschreibung : CLOB\n}").attributes;
    assertEquals(2, attributes.size());
    assertEquals("Sprache", attributes.get(0).name);
    assertEquals("Beschreibung", attributes.get(1).name);
  }

  /** A syntax error is reported with its line. */
  @Test
  void syntaxErrorLine() {
    ClassDiagramParseException e = assertThrows(ClassDiagramParseException.class, () -> parse("class A\nclass"));
    assertTrue(e.getMessage().startsWith("line 4"), e.getMessage());
  }

  /** The diagram name is read, and it may be missing. */
  @Test
  void diagramName() throws ClassDiagramParseException {
    assertEquals("Hilfe Versionierung", parser.parse("classdiagram `Hilfe Versionierung` { }").name);
    assertNull(parser.parse("classdiagram { }").name);
  }

  /** An association with id, multiplicities, direction and a label with a line break. */
  @Test
  void associationParts() throws ClassDiagramParseException {
    AssociationRelationModel_V001 r = assertInstanceOf(AssociationRelationModel_V001.class,
        parse("association (0123abcd) A 1 -> 0..* B `first\\nsecond`").relations().get(0));
    assertEquals(RelationType_V001.ASSOCIATION, r.type);
    assertEquals("0123abcd", r.id);
    assertEquals("A", r.from);
    assertEquals("B", r.to);
    assertEquals("1", r.fromMultiplicity);
    assertEquals("0..*", r.toMultiplicity);
    assertTrue(r.directed);
    assertEquals("first\nsecond", r.label);
    assertNull(r.route);
    assertNull(r.assocClass);
  }

  /** Aggregation and composition are associations too. A -- instead of -> makes the relation undirected. */
  @Test
  void aggregationCompositionUndirected() throws ClassDiagramParseException {
    List<AbstractRelationModel_V001> relations = parse("aggregation A -- B\ncomposition A -> B").relations();
    assertEquals(RelationType_V001.AGGREGATION, relations.get(0).type);
    assertFalse(assertInstanceOf(AssociationRelationModel_V001.class, relations.get(0)).directed);
    assertEquals(RelationType_V001.COMPOSITION, relations.get(1).type);
    assertTrue(assertInstanceOf(AssociationRelationModel_V001.class, relations.get(1)).directed);
  }

  /** Extends and implements become their own model classes. */
  @Test
  void extendsAndImplements() throws ClassDiagramParseException {
    List<AbstractRelationModel_V001> relations = parse("extends A -> B `is a`\nimplements A -> C").relations();
    assertInstanceOf(ExtendsRelationModel_V001.class, relations.get(0));
    assertEquals("is a", relations.get(0).label);
    assertInstanceOf(ImplementsRelationModel_V001.class, relations.get(1));
    assertEquals("C", relations.get(1).to);
  }

  /** A route has a start anchor, waypoints and an end anchor. Names in backticks work in anchors. */
  @Test
  void routeAnchorsAndWaypoints() throws ClassDiagramParseException {
    AbstractRelationModel_V001 r = parse("association A -> B\n  route A.top@5 (10, 20) (30, -40) `B C`.left@7").relations().get(0);
    assertEquals("A", r.route.start.className);
    assertEquals(Side_V001.TOP, r.route.start.side);
    assertEquals(5, r.route.start.offset);
    assertEquals(2, r.route.waypoints.size());
    assertEquals(30, r.route.waypoints.get(1).x);
    assertEquals(-40, r.route.waypoints.get(1).y);
    assertEquals("B C", r.route.end.className);
    assertEquals(Side_V001.LEFT, r.route.end.side);
    assertEquals(7, r.route.end.offset);
  }

  /** An association class nested in a relation belongs to that relation. */
  @Test
  void nestedAssocClass() throws ClassDiagramParseException {
    AssociationRelationModel_V001 r = assertInstanceOf(AssociationRelationModel_V001.class,
        parse("association A -> B\n  assocclass link at(5, 6) {\n    since\n  }").relations().get(0));
    assertEquals("link", r.assocClass.name);
    assertEquals(5, r.assocClass.position.x);
    assertEquals("since", r.assocClass.attributes.get(0).name);
  }

  /** A note with id, position, width, text with a line break and the ID of the element it is attached to. */
  @Test
  void noteParts() throws ClassDiagramParseException {
    NoteModel_V001 note = parse("note (aabbccdd) at(1, 2) width=90 `one\\ntwo` attach(11223344)").notes().get(0);
    assertEquals("aabbccdd", note.id);
    assertEquals(1, note.position.x);
    assertEquals(90, note.width);
    assertEquals("one\ntwo", note.text);
    assertEquals("11223344", note.attachedToId);
    assertNull(parse("note `free`").notes().get(0).attachedToId);
  }

  /** The dashed line of a note can lead over waypoints. */
  @Test
  void noteRoute() throws ClassDiagramParseException {
    NoteModel_V001 note = parse("note `n` attach(11223344) route (10, 20) (30, -40)").notes().get(0);
    assertEquals(2, note.attachWaypoints.size());
    assertEquals(10, note.attachWaypoints.get(0).x);
    assertEquals(-40, note.attachWaypoints.get(1).y);
    assertTrue(parse("note `n` attach(11223344)").notes().get(0).attachWaypoints.isEmpty());
  }

  /** An attribute can have an ID after its name, so that notes can be attached to it. */
  @Test
  void attributeId() throws ClassDiagramParseException {
    ClassModel_V001 classModel = parse("class A {\n  -name (0a1b2c3d) : String [x]\n  other\n}").classes().get(0);
    assertEquals("0a1b2c3d", classModel.attributes.get(0).id);
    assertEquals("String", classModel.attributes.get(0).type);
    assertNull(classModel.attributes.get(1).id);
  }

  /** Classes, relations and notes stay in the order of the source. */
  @Test
  void elementSourceOrder() throws ClassDiagramParseException {
    List<AbstractDiagramElementModel_V001> elements =
        parse("class A\nnote `n`\nclass B\nextends A -> B\nnote `m`").elements;
    assertEquals(List.of(ClassModel_V001.class, NoteModel_V001.class, ClassModel_V001.class,
        ExtendsRelationModel_V001.class, NoteModel_V001.class),
        elements.stream().<Class<?>>map(Object::getClass).toList());
  }
}
