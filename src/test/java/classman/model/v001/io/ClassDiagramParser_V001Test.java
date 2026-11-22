package classman.model.v001.io;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ExtendsRelationModel_V001;
import classman.model.v001.ImplementsRelationModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.RelationType_V001;
import classman.model.v001.TextChangeModel_V001;
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
        class \"Hilfe Eintrag\" (9a31c4e0) at(-20, 30) width=140 <<Entity>> (e3a1d7c2) {TABLENAME=helpversion, SCHEMA=\"x y\"} {
          -Name : String [Pflicht, 2..50, TN=key]
          ...
        }""");
    assertEquals("Hilfe Eintrag", c.name);
    assertEquals("9a31c4e0", c.id);
    assertEquals(-20, c.position.x);
    assertEquals(30, c.position.y);
    assertEquals(140, c.width);
    assertEquals("Entity", c.stereotype);
    assertEquals("e3a1d7c2", c.stereotypeId);
    assertEquals(2, c.taggedValues.size());
    assertEquals("TABLENAME", c.taggedValues.get(0).key);
    assertEquals("helpversion", c.taggedValues.get(0).value);
    assertEquals("x y", c.taggedValues.get(1).value);
    AttributeModel_V001 name = c.attributes.get(0);
    assertEquals(Visibility_V001.PRIVATE, name.visibility);
    assertEquals("Name", name.name);
    assertEquals("String", name.type);
    assertEquals(List.of("Pflicht", "2..50", "TN=key"), infoTexts(name));
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
    assertNull(c.stereotypeId);
    assertTrue(c.taggedValues.isEmpty());
    assertTrue(c.attributes.isEmpty());
  }

  /** Quotes around an attribute name or type are dropped, the text inside is kept. */
  @Test
  void quotedAttributeNameAndType() throws ClassDiagramParseException {
    ClassModel_V001 c = onlyClass("class A { \"In Baum anzeigen\" : \"J/N ja\" }");
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

  /** Two attributes in one line mean forgotten quotes. The parser reports it with the line number. */
  @Test
  void unquotedSpacesRejected() {
    ClassDiagramParseException e = assertThrows(ClassDiagramParseException.class,
        () -> parse("class A {\n  In Baum anzeigen : J/N\n}"));
    assertTrue(e.getMessage().startsWith("line 3"), e.getMessage());
    assertTrue(e.getMessage().contains("double quotes"), e.getMessage());
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
    assertEquals("Hilfe Versionierung", parser.parse("classdiagram \"Hilfe Versionierung\" { }").name);
    assertNull(parser.parse("classdiagram { }").name);
  }

  /** An association with id, multiplicities, direction and a label with a line break. */
  @Test
  void associationParts() throws ClassDiagramParseException {
    AssociationRelationModel_V001 r = assertInstanceOf(AssociationRelationModel_V001.class,
        parse("association (0123abcd) aaaaaaaa 1 -> 0..* bbbbbbbb \"first\\nsecond\"").relations().get(0));
    assertEquals(RelationType_V001.ASSOCIATION, r.type);
    assertEquals("0123abcd", r.id);
    assertEquals("aaaaaaaa", r.fromId);
    assertEquals("bbbbbbbb", r.toId);
    assertEquals("1", r.fromMultiplicity);
    assertEquals("0..*", r.toMultiplicity);
    assertTrue(r.toArrow);
    assertFalse(r.fromArrow);
    assertEquals("first\nsecond", r.label);
    assertNull(r.route);
    assertNull(r.assocClass);
  }

  /** A multiplicity is free text in quotes, an empty one with a mark is a removed multiplicity. */
  @Test
  void freeTextMultiplicity() throws ClassDiagramParseException {
    AssociationRelationModel_V001 r = assertInstanceOf(AssociationRelationModel_V001.class,
        parse("association aaaaaaaa \"1..<maximum capacity>\" -> \"\" was(yellow, 1) bbbbbbbb").relations().get(0));
    assertEquals("1..<maximum capacity>", r.fromMultiplicity);
    assertEquals("", r.toMultiplicity);
    assertTextChange("yellow", "1", r.toMultiplicityChange);
  }

  /** Aggregation and composition are associations too. -- instead of -> makes the relation undirected. */
  @Test
  void aggregationCompositionUndirected() throws ClassDiagramParseException {
    List<AbstractRelationModel_V001> relations = parse("aggregation aaaaaaaa -- bbbbbbbb\ncomposition aaaaaaaa -> bbbbbbbb").relations();
    assertEquals(RelationType_V001.AGGREGATION, relations.get(0).type);
    assertFalse(assertInstanceOf(AssociationRelationModel_V001.class, relations.get(0)).toArrow);
    assertEquals(RelationType_V001.COMPOSITION, relations.get(1).type);
    assertTrue(assertInstanceOf(AssociationRelationModel_V001.class, relations.get(1)).toArrow);
  }

  /** Extends and implements become their own model classes. */
  @Test
  void extendsAndImplements() throws ClassDiagramParseException {
    List<AbstractRelationModel_V001> relations = parse("extends aaaaaaaa -> bbbbbbbb \"is a\"\nimplements aaaaaaaa -> cccccccc").relations();
    assertInstanceOf(ExtendsRelationModel_V001.class, relations.get(0));
    assertEquals("is a", relations.get(0).label);
    assertInstanceOf(ImplementsRelationModel_V001.class, relations.get(1));
    assertEquals("cccccccc", relations.get(1).toId);
  }

  /** The route of an anchor line can fix its ends: an anchor on the note is the start, one on the target the end. */
  @Test
  void anchorLineRouteAnchors() throws ClassDiagramParseException {
    AnchorLineModel_V001 both = parse("class A (aaaaaaaa)\nnote (22222222) \"n\"\n"
      + "anchor 22222222 -- aaaaaaaa route 22222222.right@3 (10, 20) aaaaaaaa.top@5").anchorLines().get(0);
    assertEquals(Side_V001.RIGHT, both.start.side);
    assertEquals(3, both.start.offset);
    assertEquals("aaaaaaaa", both.end.classId);
    assertEquals(1, both.waypoints.size());

    AnchorLineModel_V001 endOnly = parse("class A (aaaaaaaa)\nnote (22222222) \"n\"\n"
      + "anchor 22222222 -- aaaaaaaa route (10, 20) aaaaaaaa.left@4").anchorLines().get(0);
    assertNull(endOnly.start);
    assertEquals(Side_V001.LEFT, endOnly.end.side);

    assertThrows(ClassDiagramParseException.class,
      () -> parse("class A (aaaaaaaa)\nnote (22222222) \"n\"\nanchor 22222222 -- aaaaaaaa route 33333333.top@1"));
  }

  /** The end of an anchor line on a relation is a point of the relation line in percent, and written back so. */
  @Test
  void anchorLineLinePoint() throws ClassDiagramParseException {
    String source = "association (11110000) aaaaaaaa -- bbbbbbbb\nnote (22222222) \"n\"\n"
      + "anchor 22222222 -- 11110000 route (10, 20) 11110000@40%";
    AnchorLineModel_V001 anchorLine = parse(source).anchorLines().get(0);
    assertTrue(anchorLine.end.isOnLine());
    assertEquals(40, anchorLine.end.offset);
    assertEquals("11110000", anchorLine.end.classId);
    assertTrue(new ClassDiagramSerializer_V001().serialize(parse(source)).contains("route (10, 20) 11110000@40%"));

    assertThrows(ClassDiagramParseException.class,
      () -> parse("class A (aaaaaaaa)\nnote (22222222) \"n\"\nanchor 22222222 -- aaaaaaaa route 22222222@40%"));
  }

  /** A route has a start anchor, waypoints and an end anchor. The anchors name the classes by ID. */
  @Test
  void routeAnchorsAndWaypoints() throws ClassDiagramParseException {
    AbstractRelationModel_V001 r = parse("association aaaaaaaa -> bbbbbbbb\n  route aaaaaaaa.top@5 (10, 20) (30, -40) bbbbbbbb.left@7").relations().get(0);
    assertEquals("aaaaaaaa", r.route.start.classId);
    assertEquals(Side_V001.TOP, r.route.start.side);
    assertEquals(5, r.route.start.offset);
    assertEquals(2, r.route.waypoints.size());
    assertEquals(30, r.route.waypoints.get(1).x);
    assertEquals(-40, r.route.waypoints.get(1).y);
    assertEquals("bbbbbbbb", r.route.end.classId);
    assertEquals(Side_V001.LEFT, r.route.end.side);
    assertEquals(7, r.route.end.offset);
  }

  /** A word of 8 hex characters is an ID reference in a relation, but still a normal word as name, type or info. */
  @Test
  void hexWordAsName() throws ClassDiagramParseException {
    ClassDiagramModel_V001 diagram = parse("""
        class deadbeef (12345678) {KEY=12345678} {
          deadbeef : deadbeef [deadbeef, 12345678]
        }
        class Other (aaaaaaaa)
        extends 12345678 -> aaaaaaaa""");
    ClassModel_V001 c = diagram.classes().get(0);
    assertEquals("deadbeef", c.name);
    assertEquals("12345678", c.id);
    assertEquals("12345678", c.taggedValues.get(0).value);
    assertEquals("deadbeef", c.attributes.get(0).name);
    assertEquals("deadbeef", c.attributes.get(0).type);
    assertEquals(List.of("deadbeef", "12345678"), infoTexts(c.attributes.get(0)));
    assertEquals("12345678", diagram.relations().get(0).fromId);
  }

  /** Two classes with the same name are told apart by their IDs in a relation. */
  @Test
  void relationBetweenSameNamedClasses() throws ClassDiagramParseException {
    AbstractRelationModel_V001 r = parse("class Same (aaaaaaaa)\nclass Same (bbbbbbbb)\nextends bbbbbbbb -> aaaaaaaa")
        .relations().get(0);
    assertEquals("bbbbbbbb", r.fromId);
    assertEquals("aaaaaaaa", r.toId);
  }

  /** A class name in place of an ID is a syntax error. */
  @Test
  void classNameAsEndpointRejected() {
    assertThrows(ClassDiagramParseException.class, () -> parse("class A (aaaaaaaa)\nclass B (bbbbbbbb)\nextends A -> B"));
  }

  /** An association class nested in a relation belongs to that relation. */
  @Test
  void nestedAssocClass() throws ClassDiagramParseException {
    AssociationRelationModel_V001 r = assertInstanceOf(AssociationRelationModel_V001.class,
        parse("association aaaaaaaa -> bbbbbbbb\n  assocclass link at(5, 6) {\n    since\n  }").relations().get(0));
    assertEquals("link", r.assocClass.name);
    assertEquals(5, r.assocClass.position.x);
    assertEquals("since", r.assocClass.attributes.get(0).name);
  }

  /** Strings know escapes for a line break, a quote, a backslash and a backtick, which is a unicode escape. */
  @Test
  void stringEscapes() throws ClassDiagramParseException {
    NoteModel_V001 note = parse("note \"a\\\"b\\\\c\\u0060d\\ne\"").notes().get(0);
    assertEquals("a\"b\\c`d\ne", note.text);
  }

  /** A backslash which starts no known escape stays in the text. */
  @Test
  void unknownEscapeKept() throws ClassDiagramParseException {
    assertEquals("C:\\temp", parse("note \"C:\\temp\"").notes().get(0).text);
  }

  /** A backtick in a string is refused, it has to be written as the escape, so that the source never has one. */
  @Test
  void backtickInStringRejected() {
    assertThrows(ClassDiagramParseException.class, () -> parse("note \"a`b\""));
  }

  /** A note with id, position, width and a text with a line break. */
  @Test
  void noteParts() throws ClassDiagramParseException {
    NoteModel_V001 note = parse("note (aabbccdd) at(1, 2) width=90 \"one\\ntwo\"").notes().get(0);
    assertEquals("aabbccdd", note.id);
    assertEquals(1, note.position.x);
    assertEquals(90, note.width);
    assertEquals("one\ntwo", note.text);
  }

  /** An anchor line names the note and its target by their IDs, can have an ID and a change mark, and leads over waypoints. */
  @Test
  void anchorLineParts() throws ClassDiagramParseException {
    AnchorLineModel_V001 line = parse("anchor (5a5a5a5a) change(added, yellow) aabbccdd -- 11223344 route (10, 20) (30, -40)")
      .anchorLines().get(0);
    assertEquals("5a5a5a5a", line.id);
    assertEquals(ChangeType_V001.ADDED, line.change.type);
    assertEquals("yellow", line.change.changeSet);
    assertEquals("aabbccdd", line.noteId);
    assertEquals("11223344", line.targetId);
    assertEquals(2, line.waypoints.size());
    assertEquals(10, line.waypoints.get(0).x);
    assertEquals(-40, line.waypoints.get(1).y);
    AnchorLineModel_V001 plain = parse("anchor aabbccdd -- 11223344").anchorLines().get(0);
    assertNull(plain.id);
    assertNull(plain.change);
    assertTrue(plain.waypoints.isEmpty());
  }

  /** An attribute can have an ID after its name, so that notes can be attached to it. */
  @Test
  void attributeId() throws ClassDiagramParseException {
    ClassModel_V001 classModel = parse("class A {\n  -name (0a1b2c3d) : String [x]\n  other\n}").classes().get(0);
    assertEquals("0a1b2c3d", classModel.attributes.get(0).id);
    assertEquals("String", classModel.attributes.get(0).type);
    assertNull(classModel.attributes.get(1).id);
  }

  /** A change mark follows the ID and gives the type and the change set. */
  @Test
  void changeMarks() throws ClassDiagramParseException {
    ClassDiagramModel_V001 diagram = parse("class A (aaaaaaaa) change(added, yellow) at(1, 2) {\n"
        + "  -x (a1a1a1a1) change(removed, blue) : String\n  -y\n}\n"
        + "class B (bbbbbbbb)\n"
        + "composition (cccccccc) change(removed, lilac) aaaaaaaa -> bbbbbbbb\n"
        + "  assocclass C change(added, green) {}\n"
        + "note (dddddddd) change(added, red) \"n\"");
    ClassModel_V001 a = diagram.classes().get(0);
    assertEquals(ChangeType_V001.ADDED, a.change.type);
    assertEquals("yellow", a.change.changeSet);
    assertEquals(1, a.position.x);
    assertEquals(ChangeType_V001.REMOVED, a.attributes.get(0).change.type);
    assertEquals("blue", a.attributes.get(0).change.changeSet);
    assertEquals("String", a.attributes.get(0).type);
    assertNull(a.attributes.get(1).change);
    assertNull(diagram.classes().get(1).change);
    AssociationRelationModel_V001 relation = (AssociationRelationModel_V001) diagram.relations().get(0);
    assertEquals(ChangeType_V001.REMOVED, relation.change.type);
    assertEquals("lilac", relation.change.changeSet);
    assertEquals("green", relation.assocClass.change.changeSet);
    assertEquals("red", diagram.notes().get(0).change.changeSet);
  }

  /** The word change is still fine as an attribute name, with or without a mark. */
  @Test
  void changeAsAttributeName() throws ClassDiagramParseException {
    ClassModel_V001 classModel = onlyClass("class A {\n  -change\n  change change(added, yellow)\n  other\n}");
    assertEquals("change", classModel.attributes.get(0).name);
    assertNull(classModel.attributes.get(0).change);
    assertEquals("change", classModel.attributes.get(1).name);
    assertEquals("yellow", classModel.attributes.get(1).change.changeSet);
    assertEquals("other", classModel.attributes.get(2).name);
  }

  /** Only added and removed exist as change types. */
  @Test
  void unknownChangeTypeRejected() {
    ClassDiagramParseException e = assertThrows(ClassDiagramParseException.class,
        () -> parse("class A (aaaaaaaa) change(moved, yellow)"));
    assertTrue(e.getMessage().contains("moved"), e.getMessage());
  }

  /** Classes, relations and notes stay in the order of the source. */
  @Test
  void elementSourceOrder() throws ClassDiagramParseException {
    List<AbstractDiagramElementModel_V001> elements =
        parse("class A (aaaaaaaa)\nnote \"n\"\nclass B (bbbbbbbb)\nextends aaaaaaaa -> bbbbbbbb\nnote \"m\"").elements;
    assertEquals(List.of(ClassModel_V001.class, NoteModel_V001.class, ClassModel_V001.class,
        ExtendsRelationModel_V001.class, NoteModel_V001.class),
        elements.stream().<Class<?>>map(Object::getClass).toList());
  }

  private static List<String> infoTexts(AttributeModel_V001 attribute) {
    return attribute.infos.stream().map(info -> info.text).toList();
  }

  private static void assertTextChange(String changeSet, String backup, TextChangeModel_V001 change) {
    assertEquals(changeSet, change.changeSet);
    assertEquals(backup, change.backup);
  }

  /** Every kind of text takes a was mark right behind it. */
  @Test
  void textChangeKinds() throws ClassDiagramParseException {
    ClassDiagramModel_V001 diagram = parser.parse("""
        classdiagram "T" was(red, "Old") {
          class Order was(yellow, Bestellung) (aaaaaaaa) <<Entity>> was(blue, Table) (e3a1d7c2) {TABLE was(green, T0)=orders was(lilac, o0)} {
            -nr was(yellow, number) : String was(blue, Text) [Pflicht was(green, optional), 2..50]
          }
          class Line (bbbbbbbb)
          association aaaaaaaa 1 was(yellow, "0..1") -> * was(blue, 1..*) bbbbbbbb "gehört zu" was(green, enthält)
          note "Neu" was(red, "Alt")
        }""");
    assertTextChange("red", "Old", diagram.nameChange);
    ClassModel_V001 order = diagram.classes().get(0);
    assertTextChange("yellow", "Bestellung", order.nameChange);
    assertEquals("aaaaaaaa", order.id);
    assertTextChange("blue", "Table", order.stereotypeChange);
    assertEquals("e3a1d7c2", order.stereotypeId);
    assertTextChange("green", "T0", order.taggedValues.get(0).keyChange);
    assertTextChange("lilac", "o0", order.taggedValues.get(0).valueChange);
    AttributeModel_V001 number = order.attributes.get(0);
    assertTextChange("yellow", "number", number.nameChange);
    assertTextChange("blue", "Text", number.typeChange);
    assertTextChange("green", "optional", number.infos.get(0).textChange);
    assertEquals("Pflicht", number.infos.get(0).text);
    assertNull(number.infos.get(1).textChange);
    AssociationRelationModel_V001 relation = (AssociationRelationModel_V001) diagram.relations().get(0);
    assertTextChange("yellow", "0..1", relation.fromMultiplicityChange);
    assertTextChange("blue", "1..*", relation.toMultiplicityChange);
    assertTextChange("green", "enthält", relation.labelChange);
    assertTextChange("red", "Alt", diagram.notes().get(0).textChange);
  }

  /** The visibility sign of an attribute takes a was mark right behind it, the sign before is the backup. */
  @Test
  void visibilityChange() throws ClassDiagramParseException {
    ClassModel_V001 box = parse("""
        class A (aaaaaaaa) {
          + was(yellow, "-") x
          - was(yellow) y
          z
        }""").classes().get(0);
    AttributeModel_V001 changed = box.attributes.get(0);
    assertEquals(Visibility_V001.PUBLIC, changed.visibility);
    assertTextChange("yellow", "-", changed.visibilityChange);
    assertEquals("x", changed.name);
    assertTextChange("yellow", null, box.attributes.get(1).visibilityChange);
    assertNull(box.attributes.get(2).visibility);
    assertNull(box.attributes.get(2).visibilityChange);
  }

  /** An info is a list entry and takes element marks: added or removed. Its text mark is a different thing. */
  @Test
  void infoElementMarks() throws ClassDiagramParseException {
    AttributeModel_V001 attribute = onlyClass("class A {\n  x [a, b change(removed, yellow), c change(added, blue), "
      + "d was(red, e)]\n}").attributes.get(0);
    assertEquals(List.of("a", "b", "c", "d"), infoTexts(attribute));
    assertNull(attribute.infos.get(0).change);
    assertEquals(ChangeType_V001.REMOVED, attribute.infos.get(1).change.type);
    assertEquals("yellow", attribute.infos.get(1).change.changeSet);
    assertEquals(ChangeType_V001.ADDED, attribute.infos.get(2).change.type);
    assertNull(attribute.infos.get(2).textChange);
    assertNull(attribute.infos.get(3).change);
    assertTextChange("red", "e", attribute.infos.get(3).textChange);
  }

  /** Without backup there was no text before, an empty backup says the text was empty. */
  @Test
  void absentAndEmptyBackup() throws ClassDiagramParseException {
    ClassDiagramModel_V001 diagram = parse("class A was(yellow) (aaaaaaaa)\nclass B was(yellow, \"\") (bbbbbbbb)");
    assertTextChange("yellow", null, diagram.classes().get(0).nameChange);
    assertTextChange("yellow", "", diagram.classes().get(1).nameChange);
  }

  /** A removed text stays as an empty string with the mark. */
  @Test
  void removedLabel() throws ClassDiagramParseException {
    AbstractRelationModel_V001 relation = parse("association aaaaaaaa -- bbbbbbbb \"\" was(yellow, \"old\")")
        .relations().get(0);
    assertEquals("", relation.label);
    assertTextChange("yellow", "old", relation.labelChange);
  }

  /** Texts without a mark have no change. */
  @Test
  void unmarkedTextsHaveNoChange() throws ClassDiagramParseException {
    ClassDiagramModel_V001 diagram = parse("class A (aaaaaaaa) <<E>> {K=v} {\n  x : T [i]\n}\nnote \"n\"");
    ClassModel_V001 classModel = diagram.classes().get(0);
    assertNull(classModel.nameChange);
    assertNull(classModel.stereotypeChange);
    assertNull(classModel.taggedValues.get(0).keyChange);
    assertNull(classModel.taggedValues.get(0).valueChange);
    assertNull(classModel.attributes.get(0).nameChange);
    assertNull(classModel.attributes.get(0).typeChange);
    assertNull(classModel.attributes.get(0).infos.get(0).textChange);
    assertNull(diagram.notes().get(0).textChange);
    assertNull(diagram.nameChange);
  }

  /** The word was is still fine as an attribute name, type and info, with or without a mark. */
  @Test
  void wasAsWord() throws ClassDiagramParseException {
    List<AttributeModel_V001> attributes = onlyClass(
        "class A {\n  was\n  was was(yellow, x) : was [was, was was(blue), a was]\n  other\n}").attributes;
    assertEquals("was", attributes.get(0).name);
    assertNull(attributes.get(0).nameChange);
    assertEquals("was", attributes.get(1).name);
    assertTextChange("yellow", "x", attributes.get(1).nameChange);
    assertEquals("was", attributes.get(1).type);
    assertNull(attributes.get(1).typeChange);
    assertEquals(List.of("was", "was", "a was"), infoTexts(attributes.get(1)));
    assertNull(attributes.get(1).infos.get(0).textChange);
    assertTextChange("blue", null, attributes.get(1).infos.get(1).textChange);
    assertEquals("other", attributes.get(2).name);
  }

  /** Marks inside tagged values don't hide whether a brace starts tagged values or the body. */
  @Test
  void taggedValuesBesideBody() throws ClassDiagramParseException {
    ClassModel_V001 withTags = onlyClass("class A {K was(yellow, J)=v was(blue, w), B=2} {\n  x\n}");
    assertEquals(2, withTags.taggedValues.size());
    assertEquals(1, withTags.attributes.size());
    ClassModel_V001 bodyOnly = onlyClass("class A {\n  K was(yellow, J)\n  B\n}");
    assertTrue(bodyOnly.taggedValues.isEmpty());
    assertEquals(2, bodyOnly.attributes.size());
    assertTextChange("yellow", "J", bodyOnly.attributes.get(0).nameChange);
  }

  /** A quoted backup keeps its line breaks. */
  @Test
  void multiLineBackup() throws ClassDiagramParseException {
    assertTextChange("yellow", "one\ntwo", parse("note \"n\" was(yellow, \"one\\ntwo\")").notes().get(0).textChange);
  }

  /** An interface is a class box of its own kind, with the head of a class and no body. */
  @Test
  void interfaceDeclaration() throws ClassDiagramParseException {
    ClassModel_V001 box = onlyClass("interface Runnable (aaaaaaaa) at(10, 20) <<Service>>");
    assertEquals(ClassKind_V001.INTERFACE, box.kind);
    assertEquals("Runnable", box.name);
    assertEquals("Service", box.stereotype);
    assertEquals(ClassKind_V001.CLASS, onlyClass("class Runnable").kind);
    assertThrows(ClassDiagramParseException.class, () -> parse("interface I { x }"));
  }

  /** An enum has a body with values, which are attributes without a type, and may have infos. */
  @Test
  void enumDeclaration() throws ClassDiagramParseException {
    ClassModel_V001 box = onlyClass("""
        enum Color (aaaaaaaa) {
          RED [primary]
          GREEN
        }""");
    assertEquals(ClassKind_V001.ENUM, box.kind);
    assertEquals(2, box.attributes.size());
    assertEquals("RED", box.attributes.get(0).name);
    assertEquals("primary", box.attributes.get(0).infos.get(0).text);
  }

  /** The value of an enum has no type. */
  @Test
  void enumValueWithTypeRejected() {
    ClassDiagramParseException e = assertThrows(ClassDiagramParseException.class,
        () -> parse("enum Color {\n  RED : int\n}"));
    assertTrue(e.getMessage().contains("no type"), e.getMessage());
  }

  /** An association can have an arrowhead at the source (<-), at both ends (<->), at the target (->) or none (--). */
  @Test
  void arrowheadsAtBothEnds() throws ClassDiagramParseException {
    List<AbstractRelationModel_V001> relations = parse("""
        class A (aaaaaaaa)
        class B (bbbbbbbb)
        association aaaaaaaa <- bbbbbbbb
        association aaaaaaaa <-> bbbbbbbb
        association aaaaaaaa -> bbbbbbbb
        association aaaaaaaa -- bbbbbbbb
        """).relations();
    boolean[][] expected = {{true, false}, {true, true}, {false, true}, {false, false}};
    for (int i = 0; i < expected.length; i++) {
      AssociationRelationModel_V001 relation = assertInstanceOf(AssociationRelationModel_V001.class, relations.get(i));
      assertEquals(expected[i][0], relation.fromArrow, "from of " + i);
      assertEquals(expected[i][1], relation.toArrow, "to of " + i);
    }
  }

  /** The source of an aggregation or composition has the diamond: no arrowhead there. */
  @Test
  void noArrowheadAtTheDiamond() {
    ClassDiagramParseException e = assertThrows(ClassDiagramParseException.class,
        () -> parse("class A (aaaaaaaa)\nclass B (bbbbbbbb)\ncomposition aaaaaaaa <-> bbbbbbbb"));
    assertTrue(e.getMessage().contains("diamond"), e.getMessage());
  }

  /** An end of a route can be linked to the center of its class: classId.center. */
  @Test
  void centerAnchorInRoute() throws ClassDiagramParseException {
    AbstractRelationModel_V001 relation = parse("""
        class A (aaaaaaaa)
        class B (bbbbbbbb)
        association aaaaaaaa -- bbbbbbbb
          route aaaaaaaa.center (10, 20) bbbbbbbb.top@5
        """).relations().get(0);
    assertTrue(relation.route.start.center);
    assertEquals("aaaaaaaa", relation.route.start.classId);
    assertFalse(relation.route.end.center);
    assertEquals(5, relation.route.end.offset);
  }
}
