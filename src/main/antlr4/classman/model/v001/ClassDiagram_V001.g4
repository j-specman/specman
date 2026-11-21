grammar ClassDiagram_V001;
// ============================================================
// Classman class diagram text format, version 001 (draft) — maps 1:1 to ClassDiagramModel_V001
//
// First line of a file: JSON envelope comment, skipped like any comment, read separately:
//   // {"classmanVersion":"0.1","modelType":"classman.model.v001.ClassDiagramModel_V001"}
//
// Names: unquoted if made of letters, digits, '_' and '-' or '/' between letters, else in double quotes.
// This holds for class names, attribute names and attribute types alike: a name or type with spaces or
// other special characters must be written in double quotes, e.g.  -"In Baum anzeigen" : J/N
// Keywords can't be unquoted class names. As attribute names, types and info texts they work unquoted.
//
// Relation types and how they are drawn:
//   association    line, open arrowhead at the target with ->, no arrowhead with --
//   aggregation    like association plus a hollow diamond at the source
//   composition    like association plus a filled diamond at the source
//   extends    hollow triangle at the target (the superclass), always ->, no multiplicities
//   implements     dashed line, hollow triangle at the target (the interface), always ->, no multiplicities
// Only association, aggregation and composition take multiplicities and an assocclass.
// A multiplicity is free text: written bare if it is a number, a range like 0..1 or 1..*, or *, else in double
// quotes, for example "1..<maximum capacity>".
// A relation refers to its two classes by their IDs, written bare (without parentheses, unlike the ID of the
// element itself):  composition (11223344) 04de91aa 1 -> * b7c20f13 "label"
// The classes need an ID for that. Class names are free text and need not be unique.
//
// Layout rules the renderer applies, not expressed in the grammar:
//   * A class (or note) without at(x,y) is placed in the vertical stack: source order, top to bottom,
//     centered on a common axis, default gap between them. Mixing placed and stacked elements is allowed,
//     collisions are not resolved.
//   * An association class without at(x,y) is placed next to its relation (rule open).
//   * A class with width=N is N pixels wide; text wraps at spaces. Without width the content decides.
//     The height is always computed.
//   * A relation without route is a straight line from center to center, clipped at the box borders.
//   * Route anchors: classId.side@offset, side = top|right|bottom|left, offset in pixels from the start of the
//     side (left end of top/bottom, upper end of left/right), clamped to the side's length. Waypoints are
//     absolute pixel points.
//   * A note is attached to a class, an attribute or a relation by the ID of that element: attach(04de91aa).
//     Whoever wants to attach a note must give the target an ID. The dashed line of a note on a class ends at the
//     class border, on an attribute at the border of its class on the side of the note, on a relation at the
//     middle of the longest part of the relation.
//   * IDs of classes, attributes, relations and stereotypes (class Hilfeversion (04de91aa) <<Entity>> (7b2f90c1))
//     are optional, except that a class needs one to take part in a relation. Created in a class model, a diagram refers with them to the elements of the model: the
//     stereotype is defined there once and assigned to the classes. A diagram which stands for itself needs none.
//   * The dashed line of a note can lead over waypoints (absolute pixel points): attach(04de91aa) route (120, 40) (120, 90).
//     The line starts at the note border facing the first waypoint and ends at the target facing the last one.
//   * A change mark change(added, yellow) or change(removed, blue) follows the ID of a class, attribute, relation,
//     association class or note. Moving an element is never marked.
//   * A text change mark was(yellow, "old text") follows the text it belongs to: the diagram name, class name,
//     stereotype, key or value of a tagged value, attribute name or type, an info item, a multiplicity, a relation
//     label, a note text. The first value names the change set, the optional second one is the text before the
//     change (bare if it is a word or multiplicity, else a string). Without the second value there was no text
//     before, an empty string says it was empty. The text itself always stands for the new state, the box is laid
//     out for it. A removed text stays as an empty string with the mark: "" was(yellow, "old label"). That holds
//     for a multiplicity too. Marks of the name come before the ID.
//   * Text marks inside an element which is marked added or removed are accepted and ignored by the renderer:
//     accepting or rejecting the element decides about everything in it.
//   * An assocclass nested in a relation belongs to that relation.
//   * Strings (names, labels, note texts) are in double quotes. Escapes: backslash-n is a line break, backslash-quote
//     a quote, two backslashes one backslash, backslash-u0060 a backtick. The latter is the only way to write
//     a backtick: a diagram source embedded in a Specman model file must not contain one.
//   * Attribute info items (inside [ ]) are shown exactly as written, the renderer doesn't interpret them.
//     The attribute ... says: the model has more attributes which this diagram doesn't show.
// ============================================================

// ============================================================
// Parser rules
// ============================================================

classDiagram
    : CLASSDIAGRAM (diagramName nameChange=textChange?)? LBRACE element* RBRACE EOF
    ;

diagramName
    : NAME
    | HEX_ID
    | QUOTED_STRING
    ;

element
    : classDecl
    | relation
    | note
    ;

// ------------------------------------------------------------ classes

// The body is optional, class Hello is a complete class.
classDecl
    : CLASS classHead classBody?
    ;

// Fixed order. The ID after the name belongs to the class, the one after the stereotype to the stereotype.
classHead
    : name nameChange=textChange? classId=ELEMENT_ID? changeMark? position? widthSpec?
      (STEREOTYPE stereotypeChange=textChange? stereotypeId=ELEMENT_ID?)? taggedValues?
    ;

// {TABLENAME=helpversion} or {A=1, B=2}. A '{' followed by word '=' starts tagged values,
// any other '{' starts the body.
taggedValues
    : LBRACE taggedValue (COMMA taggedValue)* RBRACE
    ;

taggedValue
    : word keyChange=textChange? EQUALS (word | NUMBER | QUOTED_STRING) valueChange=textChange?
    ;

classBody
    : LBRACE attribute* RBRACE
    ;

// One attribute: [visibility] name [(id)] [: type] [[info, info, ...]]. A name or type is one word or a quoted string.
attribute
    : ELLIPSIS
    | visibility? wordOrString nameChange=textChange? ELEMENT_ID? changeMark?
      (COLON wordOrString typeChange=textChange?)? infos?
    ;

visibility
    : PLUS
    | MINUS
    | HASH
    | TILDE
    ;

infos
    : LBRACK info (COMMA info)* RBRACK
    ;

// Any run of tokens up to the next comma or closing bracket, e.g.  Pflicht   2..50   TN=versionkey
// The word was ends the run if a parenthesis follows: that is the mark of the info.
info
    : infoText textChange?
    ;

infoText
    : (word | NUMBER | DOTDOT | STAR | EQUALS | QUOTED_STRING)+
    ;

wordOrString
    : word
    | QUOTED_STRING
    ;

name
    : NAME
    | HEX_ID
    | QUOTED_STRING
    ;

position
    : AT LPAREN NUMBER COMMA NUMBER RPAREN
    ;

widthSpec
    : WIDTH EQUALS NUMBER
    ;

// change(added, yellow): a change set added the element, or change(removed, blue): it removed the element, which
// stays in the diagram until the change is accepted. The second word (or string) names the change set. The type is checked by
// the parser and not by the grammar, so that added and removed stay usable as names. The mark follows the ID.
changeMark
    : CHANGE LPAREN changeType=word COMMA changeSet=wordOrString RPAREN
    ;

// was(yellow, Old): the text in front of the mark is the new one, Old the text before. The backup is optional,
// see the header. Multiplicities may be written bare like 1..*, anything else which isn't a word needs a string.
textChange
    : WAS LPAREN changeSet=wordOrString (COMMA backup=backupText)? RPAREN
    ;

backupText
    : wordOrString
    | bareMultiplicity
    ;

// ------------------------------------------------------------ relations

relation
    : associationRelation
    | extendsRelation
    | implementsRelation
    ;

associationRelation
    : type=(ASSOCIATION | AGGREGATION | COMPOSITION) ELEMENT_ID? changeMark? from=HEX_ID (fromMultiplicity=multiplicity fromMultiplicityChange=textChange?)?
      (ARROW | LINE) (toMultiplicity=multiplicity toMultiplicityChange=textChange?)? to=HEX_ID
      (label=QUOTED_STRING labelChange=textChange?)? route? assocClass?
    ;

extendsRelation
    : EXTENDS ELEMENT_ID? changeMark? from=HEX_ID ARROW to=HEX_ID (label=QUOTED_STRING labelChange=textChange?)? route?
    ;

implementsRelation
    : IMPLEMENTS ELEMENT_ID? changeMark? from=HEX_ID ARROW to=HEX_ID (label=QUOTED_STRING labelChange=textChange?)? route?
    ;

multiplicity
    : bareMultiplicity
    | QUOTED_STRING
    ;

bareMultiplicity
    : NUMBER (DOTDOT (NUMBER | STAR))?
    | STAR
    ;

route
    : ROUTE anchor waypoint* anchor
    ;

anchor
    : HEX_ID DOT side AT_SIGN NUMBER
    ;

side
    : TOP
    | RIGHT
    | BOTTOM
    | LEFT
    ;

waypoint
    : LPAREN NUMBER COMMA NUMBER RPAREN
    ;

assocClass
    : ASSOCCLASS classHead classBody?
    ;

// ------------------------------------------------------------ notes

note
    : NOTE noteId=ELEMENT_ID? changeMark? position? widthSpec? QUOTED_STRING textChange? (ATTACH target=ELEMENT_ID noteRoute?)?
    ;

// Waypoints of the dashed line from a note to its target. Start and end of the line are computed.
noteRoute
    : ROUTE waypoint+
    ;

// A plain word, including the keywords: they are only reserved where a class name is expected.
word
    : NAME
    | HEX_ID
    | CLASSDIAGRAM | CLASS | ASSOCCLASS | ASSOCIATION | AGGREGATION | COMPOSITION | EXTENDS | IMPLEMENTS
    | NOTE | ROUTE | ATTACH | CHANGE | WAS | AT | WIDTH | TOP | RIGHT | BOTTOM | LEFT
    ;

// ============================================================
// Lexer rules
// ============================================================

CLASSDIAGRAM : 'classdiagram' ;
CLASS        : 'class' ;
ASSOCCLASS   : 'assocclass' ;
ASSOCIATION  : 'association' ;
AGGREGATION  : 'aggregation' ;
COMPOSITION  : 'composition' ;
EXTENDS      : 'extends' ;
IMPLEMENTS   : 'implements' ;
NOTE         : 'note' ;
ROUTE        : 'route' ;
ATTACH       : 'attach' ;
WAS          : 'was' ;
CHANGE       : 'change' ;
AT           : 'at' ;
WIDTH        : 'width' ;
TOP          : 'top' ;
RIGHT        : 'right' ;
BOTTOM       : 'bottom' ;
LEFT         : 'left' ;

ARROW        : '->' ;
LINE         : '--' ;
ELLIPSIS     : '...' ;
DOTDOT       : '..' ;
DOT          : '.' ;
STAR         : '*' ;
AT_SIGN      : '@' ;
EQUALS       : '=' ;
COMMA        : ',' ;
COLON        : ':' ;
LPAREN       : '(' ;
RPAREN       : ')' ;
LBRACE       : '{' ;
RBRACE       : '}' ;
LBRACK       : '[' ;
RBRACK       : ']' ;
PLUS         : '+' ;
MINUS        : '-' ;
HASH         : '#' ;
TILDE        : '~' ;

// Element ID: 8 lower-case hex characters in parentheses, like Specman step IDs, e.g. (04de91aa).
ELEMENT_ID   : '(' HEX HEX HEX HEX HEX HEX HEX HEX ')' ;

// <<Entity>>
STEREOTYPE   : '<<' ~[>\r\n]+ '>>' ;

// Reference to the ID of an element, without parentheses. Defined before NUMBER and NAME so that it wins over
// them when a word of exactly 8 hex characters is read. Where a name or word is expected the parser accepts it
// as one (rules name, word, diagramName), so such a word still works as a class or attribute name.
HEX_ID       : HEX HEX HEX HEX HEX HEX HEX HEX ;

// Signed integer: coordinates, widths, offsets, and the numbers of multiplicities (1, 0..1, 1..*).
NUMBER       : '-'? [0-9]+ ;

NAME         : [\p{L}_] [\p{L}\p{N}_]* (('-' | '/') [\p{L}\p{N}_]+)* ;

// Names and types with spaces, relation labels, note texts. A quote or backslash inside is escaped by a backslash.
// A backtick can't be written as it is, see the escapes above.
QUOTED_STRING : '"' ( ~["\\`] | '\\' . )* '"' ;

WS           : [ \t\r\n]+ -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;

fragment HEX : [0-9a-f] ;
