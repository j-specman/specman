grammar ClassDiagram_V001;
// ============================================================
// Classman class diagram text format, version 001 (draft) — maps 1:1 to ClassDiagramModel_V001
//
// First line of a file: JSON envelope comment, skipped like any comment, read separately:
//   // {"classmanVersion":"0.1","modelType":"ClassDiagramModel_V001"}
// The model type is the simple class name without package, so a move of the class doesn't invalidate the files.
//
// The diagram itself may have a name and an ID, both optional and in this order:  classdiagram "Name" (0a1b2c3d) { ... }
// The ID is eight hex digits in parentheses like the ID of an element, also for a diagram without a name. It is there
// for whoever refers to the diagram (a link from Specman, later a class model); Specman gives a new diagram one.
//
// Names: unquoted if made of letters, digits, '_' and '-' or '/' between letters, else in double quotes.
// This holds for class names, attribute names and attribute types alike: a name or type with spaces or
// other special characters must be written in double quotes, e.g.  -"In Baum anzeigen" : J/N
// Keywords can't be unquoted class names. As attribute names, types and info texts they work unquoted.
//
// Relation types and how they are drawn:
//   association    line, open arrowhead at the target with ->, at the source with <-, at both with <->, none with --
//   aggregation    like association plus a hollow diamond at the source (so no <- or <->)
//   composition    like association plus a filled diamond at the source (so no <- or <->)
//   extends    hollow triangle at the target (the superclass), always ->, no multiplicities
//   implements     dashed line, hollow triangle at the target (the interface), always ->, no multiplicities
//   dependency     dashed line, open arrowhead at the target, always ->, no multiplicities. Its ends can also be attributes
//                  (enum values included), named by the ID of the attribute: dependency 04de91aa -> 0a0a0a0a "uses"
// Only association, aggregation and composition take multiplicities and can have an association class.
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
//   * A class with width=N is N pixels wide; text wraps at spaces. Without width the content decides.
//   * style(fill="#rrggbb", textcolor="#rrggbb", linecolor="#rrggbb", font=Roboto, fontsize=14) in the head gives the
//     class box a look of its own: background, text and line color, font (SitkaDisplay, Roboto or CourierPrime) and
//     font size. A color is "#rrggbb", or "#rrggbbaa" if it is not opaque (aa is the alpha, ff is opaque, 00 invisible).
//     The entries come in any order, each is optional and may stand once. The words hideattributes and
//     hidemethods behind the style hide the attribute compartment (the values of an enum) and the method compartment;
//     showmethods shows the method compartment of a class which has no methods. The attributes and methods stay in the
//     source. Without a word the attribute compartment is shown, the method compartment if the class has methods.
//     The height is always computed.
//   * A note has the same style(...), behind width= and before its text. A relation has it behind its change mark,
//     before the classes it joins, and no fill: association style(linecolor="#cc0000") 04de91aa -> b7c20f13.
//   * An attribute which ends with (hidden) is not shown: the attribute compartment ends with a line "..." instead,
//     in the color of the first change set which marked one of the hidden attributes. The same holds for a method.
//     A method is a line of the body with parentheses behind its name: +save(file: File, "Map<A, B>") : boolean [info].
//   * A relation without route is a straight line from center to center, clipped at the box borders.
//   * The label and the multiplicities of a relation stand where the layout puts them, unless a position is given:
//     label "owns" at(40%, -12) and composition 04de91aa 1 at(14, -8) -> * b7c20f13. The label is at 40 percent of the
//     length of the route, the multiplicity 14 units along the route from its end, both 12 or 8 units to the left
//     of the line (negative) seen from the source to the target; the middle of the text stands there.
//   * Route anchors: classId.side@offset, side = top|right|bottom|left, offset in pixels from the start of the
//     side (left end of top/bottom, upper end of left/right), clamped to the side's length. Waypoints are
//     absolute pixel points.
//   * A note is attached to a class, an attribute or a relation by an anchor line, an element of its own like a
//     relation which names the note and the target by their IDs: anchor 11223344 -- 04de91aa. The note and the
//     target need an ID. Like a relation it can have an ID and a change mark: anchor (5a5a5a5a) change(added, yellow)
//     11223344 -- 04de91aa. The line on a class ends at the class border, on an attribute at the border of the box
//     around its text on the side of the note, on a relation at the middle of the longest part of the relation.
//   * IDs of classes, attributes, relations and stereotypes (class Hilfeversion (04de91aa) <<Entity>> (7b2f90c1))
//     are optional, except that a class needs one to take part in a relation. Created in a class model, a diagram refers with them to the elements of the model: the
//     stereotype is defined there once and assigned to the classes. A diagram which stands for itself needs none.
//   * A diagram can have an ID of its own too, behind its name (and the change mark of the name) and before the opening
//     brace, also if it has no name: classdiagram "Hilfe" (04de91aa) { ... } or classdiagram (04de91aa) { ... }.
//     The ID prepares the diagram to be one of several in a class model.
//   * A stereotype is a free text, <<Entity>>, or refers to a meta tag configuration of the host (in Specman: of the
//     document): <<:Domain_Value>>. The name consists of the letters A-Z and a-z, digits and underscores, the parser
//     refuses anything else. The host gives the reference its label, icon and colors, so the source carries none of
//     them, and a free text never begins with a colon. A reference to a configuration which doesn't exist is a mistake, which
//     Specman's sanitizer reports; the old text of a change mark, was(yellow, ":Domain_Value"), may refer to one which
//     has gone. There is no empty stereotype <<>>: a configuration without a label shows only its icon.
//   * An anchor line can lead over waypoints (absolute pixel points): anchor 11223344 -- 04de91aa route (120, 40) (120, 90).
//     The line starts at the note border facing the first waypoint and ends at the target facing the last one.
//     Either end can be fixed at a point of the border like the ends of a route of a relation, with an anchor
//     classId.side@offset: the start on the note itself, the end on the box it leads to:
//     anchor 11223344 -- 04de91aa route 11223344.right@10 (120, 40) 04de91aa.top@25.
//     The end on an attribute is a point of the border of the text of the attribute, an anchor like the one on a class.
//     The end on a relation is a point of its line, in percent of the length of the line from its start:
//     anchor 11223344 -- 04de91aa route (120, 40) 04de91aa@40%. An end without anchor is computed as without waypoints.
//     An anchor which doesn't fit the target (a side anchor on a relation, a percent on a box) is ignored by the renderer.
//   * A change mark change(added, yellow) or change(removed, blue) follows the ID of a class, attribute, relation,
//     note, anchor line or association class link, and the text (and ID) of a stereotype or an info. Moving an element is never marked.
//   * A text change mark was(yellow, "old text") follows the text it belongs to: the diagram name, class name,
//     stereotype, key or value of a tagged value, attribute name or type, the visibility sign of an attribute, an info
//     item, a multiplicity, a relation label, a note text. The first value names the change set, the optional second
//     one is the text before the
//     change (bare if it is a word or multiplicity, else a string). Without the second value there was no text
//     before, an empty string says it was empty. The text itself always stands for the new state, the box is laid
//     out for it. A removed text stays as an empty string with the mark: "" was(yellow, "old label"). That holds
//     for a multiplicity too, but not for a visibility sign, which has no text left to carry a mark: a sign which
//     was there can't be removed under change tracking. Nor for a stereotype: it is a list entry like an info and
//     keeps its text when removed, <<Entity>> change(removed, yellow). Marks of the name come before the ID.
//   * Text marks inside an element which is marked added or removed are accepted and ignored by the renderer:
//     accepting or rejecting the element decides about everything in it.
//   * A class is the association class of a relation by an association class link, an element of its own which looks
//     like an anchor line: assocclass 11223344 -- 04de91aa (the class, then the association, aggregation or
//     composition). It has an optional ID, a change mark and a route like an anchor line. A relation has at most one
//     link and a class belongs to at most one relation, which the editor takes care of.
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
    : CLASSDIAGRAM (diagramName nameChange=textChange?)? diagramId=ELEMENT_ID? LBRACE element* RBRACE EOF
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
    | anchorLine
    | assocClassLink
    ;

// ------------------------------------------------------------ classes

// The body is optional, class Hello is a complete class. An interface has no attributes, its body holds methods only.
// The body of an enum holds its values, which are attributes without a type, and methods.
classDecl
    : CLASS classHead classBody?
    | INTERFACE classHead classBody?
    | ENUM classHead classBody?
    ;

// Fixed order. The ID and the change mark after the name belong to the class, those after the stereotype to the
// stereotype. Like an info of an attribute, a stereotype is a list entry which can be added or removed as a whole,
// change(removed, yellow), and keeps its text then; or its text can be changed, was(yellow, Old).
classHead
    : name nameChange=textChange? classId=ELEMENT_ID? classMark=changeMark? position? widthSpec? styleSpec? compartmentSpec?
      (STEREOTYPE stereotypeChange=textChange? stereotypeId=ELEMENT_ID? stereotypeMark=changeMark?)? taggedValues?
    ;

// {TABLENAME=helpversion} or {A=1, B=2}. A '{' followed by word '=' starts tagged values,
// any other '{' starts the body. Like an info of an attribute, a tagged value is a list entry which can be added or
// removed as a whole, change(removed, yellow), and keeps its text then; or its key or value can be changed, was(...).
// The value may be missing, a tag which only has a key is written {persistent=}.
taggedValues
    : LBRACE taggedValue (COMMA taggedValue)* RBRACE
    ;

taggedValue
    : word keyChange=textChange? EQUALS (word | NUMBER | QUOTED_STRING)? valueChange=textChange? changeMark?
    ;

classBody
    : LBRACE member* RBRACE
    ;

// The attributes and the methods of a class stand in one body, a method is told from an attribute by the parameters in
// parentheses after its name. The attributes are shown in their compartment, the methods in theirs.
member
    : attribute
    | method
    ;

// One attribute: [visibility] name [(id)] [: type] [[info, info, ...]] [(hidden)]. A name or type is one word or a
// quoted string. The diagram doesn't show an attribute which ends with (hidden); it stays in the model.
attribute
    : ELLIPSIS
    | (visibility visibilityChange=textChange?)? wordOrString nameChange=textChange? ELEMENT_ID? changeMark?
      (COLON wordOrString typeChange=textChange?)? infos? HIDDEN_MARK?
    ;

// One method: [visibility] name(parameter, ...) [was(...)] [(id)] [: return type] [[info, ...]] [(hidden)]. The parentheses
// stand even if there are no parameters. A parameter is a list entry like an info: its text is written as it is shown,
// name: Type or just Type, a text with characters which the grammar doesn't know (a generic type, for example) is in
// double quotes, and it can carry the mark of a change set. A method with a single parameter which is written as the
// ID of an element or as (hidden) has that parameter in double quotes, else it would be taken for an ID or the mark. A
// method called change or was has its name in double quotes, else its parentheses would be taken for a mark.
method
    : (visibility visibilityChange=textChange?)? methodName parameters nameChange=textChange? ELEMENT_ID? changeMark?
      (COLON wordOrString typeChange=textChange?)? infos? HIDDEN_MARK?
    ;

methodName
    : plainWord
    | QUOTED_STRING
    ;

parameters
    : LPAREN (parameter (COMMA parameter)*)? RPAREN
    ;

parameter
    : parameterText (changeMark | textChange)?
    ;

parameterText
    : (word | NUMBER | DOTDOT | STAR | EQUALS | COLON | QUOTED_STRING)+
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
// The words was and change end the run if a parenthesis follows: that is the mark of the info. An info is a list
// entry, so it can be added or removed as a whole, change(removed, yellow), and keeps its text then; or its text can
// be changed, was(yellow, "old").
info
    : infoText (changeMark | textChange)?
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

// The look of a class, note or relation. A color is a string "#rrggbb" or "#rrggbbaa", the font a word, the font size a number, which the parser
// checks. Each entry may stand once, and a relation has no fill, which the parser checks too.
styleSpec
    : STYLE LPAREN styleEntry (COMMA styleEntry)* RPAREN
    ;

styleEntry
    : colorKey=(FILL | TEXTCOLOR | LINECOLOR) EQUALS QUOTED_STRING
    | FONT EQUALS word
    | FONTSIZE EQUALS NUMBER
    ;

// The class box doesn't show its attribute compartment (the values of an enum) or its method compartment, or shows the
// method compartment although it has no methods. The attributes and methods stay in the model. Each word may stand
// once, and hidemethods and showmethods exclude each other. Without any of them the method compartment is shown if
// the class has methods.
compartmentSpec
    : (HIDEATTRIBUTES | HIDEMETHODS | SHOWMETHODS)+
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
    | dependencyRelation
    ;

associationRelation
    : type=(ASSOCIATION | AGGREGATION | COMPOSITION) ELEMENT_ID? changeMark? styleSpec? from=HEX_ID (fromMultiplicity=multiplicity fromMultiplicityChange=textChange? fromPlace=position?)?
      (ARROW | LINE | BACK_ARROW | BOTH_ARROW) (toMultiplicity=multiplicity toMultiplicityChange=textChange? toPlace=position?)? to=HEX_ID
      (label=QUOTED_STRING labelChange=textChange? labelPlace=labelPosition?)? route?
    ;

extendsRelation
    : EXTENDS ELEMENT_ID? changeMark? styleSpec? from=HEX_ID ARROW to=HEX_ID (label=QUOTED_STRING labelChange=textChange? labelPlace=labelPosition?)? route?
    ;

implementsRelation
    : IMPLEMENTS ELEMENT_ID? changeMark? styleSpec? from=HEX_ID ARROW to=HEX_ID (label=QUOTED_STRING labelChange=textChange? labelPlace=labelPosition?)? route?
    ;

// A dependency joins two classes, interfaces, enums or attributes (enum values included), named by their IDs. Dashed
// line with an arrowhead at the target, no multiplicities. Its route anchors are on the border of the class or attribute.
dependencyRelation
    : DEPENDENCY ELEMENT_ID? changeMark? styleSpec? from=HEX_ID ARROW to=HEX_ID (label=QUOTED_STRING labelChange=textChange? labelPlace=labelPosition?)? route?
    ;

// A relation label which the user has moved: at(40%, -12) is 40 percent of the length of the route from its start, 12
// units to the right of the line seen from the source to the target (a negative number is left of it). A multiplicity
// which the user has moved has an at(14, -8) behind its text (and its change mark): 14 units along the route from the
// end where it belongs, 8 units left of the line. Both give where the middle of the text stands. A text without it is
// placed by the layout.
labelPosition
    : AT LPAREN NUMBER PERCENT COMMA NUMBER RPAREN
    ;

multiplicity
    : bareMultiplicity
    | QUOTED_STRING
    ;

bareMultiplicity
    : NUMBER (DOTDOT (NUMBER | STAR))?
    | STAR
    ;

// An end of the route is a point of the border of its class, or classId.center: linked to the center of the class, so
// the line leaves the class where a line from the center to the next point crosses its border.
route
    : ROUTE routeAnchor waypoint* routeAnchor
    ;

routeAnchor
    : anchor
    | centerAnchor
    ;

centerAnchor
    : HEX_ID DOT CENTER
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

// ------------------------------------------------------------ notes

note
    : NOTE noteId=ELEMENT_ID? changeMark? position? widthSpec? styleSpec? QUOTED_STRING textChange?
    ;

// The dashed line from a note to what it is about (a class, an attribute or a relation), an element of the diagram
// like a relation: it can carry an ID and a change mark. The note and the target are named by their IDs, the note
// needs one. A note can have several.
anchorLine
    : ANCHOR ELEMENT_ID? changeMark? noteRef=HEX_ID LINE targetRef=HEX_ID attachRoute?
    ;

// The dashed line from a class to the relation it is the association class of, an element of the diagram like an
// anchor line. The class and the relation are named by their IDs, both need one.
assocClassLink
    : ASSOCCLASS ELEMENT_ID? changeMark? classRef=HEX_ID LINE relationRef=HEX_ID attachRoute?
    ;

// Waypoints of a dashed line from a box (note or class) to its target, and optionally anchors which fix its start
// (on the box) and its end (on the target). An end without anchor is computed. A single anchor belongs to the start if it
// names the box, to the end if it names the target, which the parser checks. The end on a relation is a point of
// its line, given in percent of its length, the start is never one.
attachRoute
    : ROUTE attachAnchor waypoint* attachAnchor?
    | ROUTE waypoint+ attachAnchor?
    ;

attachAnchor
    : anchor
    | linePoint
    ;

linePoint
    : HEX_ID AT_SIGN NUMBER PERCENT
    ;

// A plain word, including the keywords: they are only reserved where a class name is expected.
word
    : plainWord
    | CHANGE
    | WAS
    ;

// A word which can't be taken for the start of a mark: the name of a method is followed by parentheses, which change(...)
// and was(...) also have, so a method with one of these names has it in double quotes.
plainWord
    : NAME
    | HEX_ID
    | CLASSDIAGRAM | CLASS | INTERFACE | ENUM | ASSOCCLASS | ASSOCIATION | AGGREGATION | COMPOSITION | EXTENDS | IMPLEMENTS | DEPENDENCY
    | NOTE | ROUTE | ANCHOR | AT | WIDTH | STYLE | FILL | TEXTCOLOR | LINECOLOR | FONT | FONTSIZE | HIDEATTRIBUTES | HIDEMETHODS | SHOWMETHODS | TOP | RIGHT | BOTTOM | LEFT | CENTER
    ;

// ============================================================
// Lexer rules
// ============================================================

CLASSDIAGRAM : 'classdiagram' ;
CLASS        : 'class' ;
INTERFACE    : 'interface' ;
ENUM         : 'enum' ;
ASSOCCLASS   : 'assocclass' ;
ASSOCIATION  : 'association' ;
AGGREGATION  : 'aggregation' ;
COMPOSITION  : 'composition' ;
EXTENDS      : 'extends' ;
IMPLEMENTS   : 'implements' ;
DEPENDENCY   : 'dependency' ;
NOTE         : 'note' ;
ROUTE        : 'route' ;
ANCHOR       : 'anchor' ;
WAS          : 'was' ;
CHANGE       : 'change' ;
AT           : 'at' ;
WIDTH        : 'width' ;
STYLE        : 'style' ;
FILL         : 'fill' ;
TEXTCOLOR    : 'textcolor' ;
LINECOLOR    : 'linecolor' ;
FONT         : 'font' ;
FONTSIZE     : 'fontsize' ;
HIDEATTRIBUTES : 'hideattributes' ;
HIDEMETHODS : 'hidemethods' ;
SHOWMETHODS : 'showmethods' ;
TOP          : 'top' ;
RIGHT        : 'right' ;
BOTTOM       : 'bottom' ;
LEFT         : 'left' ;
CENTER       : 'center' ;

ARROW        : '->' ;
BACK_ARROW   : '<-' ;
BOTH_ARROW   : '<->' ;
LINE         : '--' ;
ELLIPSIS     : '...' ;
DOTDOT       : '..' ;
DOT          : '.' ;
STAR         : '*' ;
AT_SIGN      : '@' ;
PERCENT      : '%' ;
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

// The mark of an attribute which the diagram doesn't show, at the end of its line.
HIDDEN_MARK  : '(hidden)' ;

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
