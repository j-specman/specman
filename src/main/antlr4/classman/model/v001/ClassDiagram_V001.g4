grammar ClassDiagram_V001;
// ============================================================
// Classman class diagram text format, version 001 (draft) — maps 1:1 to ClassDiagramModel_V001
//
// First line of a file: JSON envelope comment, skipped like any comment, read separately:
//   // {"classmanVersion":"0.1","modelType":"classman.model.v001.ClassDiagramModel_V001"}
//
// Names: unquoted if made of letters, digits, '_' and '-' or '/' between letters, else in backticks.
// This holds for class names, attribute names and attribute types alike: a name or type with spaces or
// other special characters must be written in backticks, e.g.  -`In Baum anzeigen` : J/N
// Keywords can't be unquoted class names. As attribute names, types and info texts they work unquoted.
//
// Relation types and how they are drawn:
//   association    line, open arrowhead at the target with ->, no arrowhead with --
//   aggregation    like association plus a hollow diamond at the source
//   composition    like association plus a filled diamond at the source
//   extends    hollow triangle at the target (the superclass), always ->, no multiplicities
//   implements     dashed line, hollow triangle at the target (the interface), always ->, no multiplicities
// Only association, aggregation and composition take multiplicities and an assocclass.
//
// Layout rules the renderer applies, not expressed in the grammar:
//   * A class (or note) without at(x,y) is placed in the vertical stack: source order, top to bottom,
//     centered on a common axis, default gap between them. Mixing placed and stacked elements is allowed,
//     collisions are not resolved.
//   * An association class without at(x,y) is placed next to its relation (rule open).
//   * A class with width=N is N pixels wide; text wraps at spaces. Without width the content decides.
//     The height is always computed.
//   * A relation without route is a straight line from center to center, clipped at the box borders.
//   * Route anchors: Class.side@offset, side = top|right|bottom|left, offset in pixels from the start of the
//     side (left end of top/bottom, upper end of left/right), clamped to the side's length. Waypoints are
//     absolute pixel points.
//   * Class names are unique within a diagram; relations and notes refer to classes by name.
//   * An assocclass nested in a relation belongs to that relation.
//   * Labels and note texts: backtick strings; the two characters \n stand for a line break.
//   * Attribute info items (inside [ ]) are shown exactly as written, the renderer doesn't interpret them.
//     The attribute ... says: the model has more attributes which this diagram doesn't show.
// ============================================================

// ============================================================
// Parser rules
// ============================================================

classDiagram
    : CLASSDIAGRAM diagramName? LBRACE element* RBRACE EOF
    ;

diagramName
    : NAME
    | BACKTICK_STRING
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

// Fixed order.
classHead
    : name ELEMENT_ID? position? widthSpec? STEREOTYPE? taggedValues?
    ;

// {TABLENAME=helpversion} or {A=1, B=2}. A '{' followed by word '=' starts tagged values,
// any other '{' starts the body.
taggedValues
    : LBRACE taggedValue (COMMA taggedValue)* RBRACE
    ;

taggedValue
    : word EQUALS (word | NUMBER | BACKTICK_STRING)
    ;

classBody
    : LBRACE attribute* RBRACE
    ;

// One attribute: [visibility] name [: type] [[info, info, ...]]. A name or type is one word or a backtick string.
attribute
    : ELLIPSIS
    | visibility? wordOrString (COLON wordOrString)? infos?
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
info
    : (word | NUMBER | DOTDOT | STAR | EQUALS | BACKTICK_STRING)+
    ;

wordOrString
    : word
    | BACKTICK_STRING
    ;

name
    : NAME
    | BACKTICK_STRING
    ;

position
    : AT LPAREN NUMBER COMMA NUMBER RPAREN
    ;

widthSpec
    : WIDTH EQUALS NUMBER
    ;

// ------------------------------------------------------------ relations

relation
    : associationRelation
    | extendsRelation
    | implementsRelation
    ;

associationRelation
    : type=(ASSOCIATION | AGGREGATION | COMPOSITION) ELEMENT_ID? from=name fromMultiplicity=multiplicity?
      (ARROW | LINE) toMultiplicity=multiplicity? to=name label=BACKTICK_STRING? route? assocClass?
    ;

extendsRelation
    : EXTENDS ELEMENT_ID? from=name ARROW to=name label=BACKTICK_STRING? route?
    ;

implementsRelation
    : IMPLEMENTS ELEMENT_ID? from=name ARROW to=name label=BACKTICK_STRING? route?
    ;

multiplicity
    : NUMBER (DOTDOT (NUMBER | STAR))?
    | STAR
    ;

route
    : ROUTE anchor waypoint* anchor
    ;

anchor
    : name DOT side AT_SIGN NUMBER
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
    : NOTE ELEMENT_ID? position? widthSpec? BACKTICK_STRING (ATTACHED name)?
    ;

// A plain word, including the keywords: they are only reserved where a class name is expected.
word
    : NAME
    | CLASSDIAGRAM | CLASS | ASSOCCLASS | ASSOCIATION | AGGREGATION | COMPOSITION | EXTENDS | IMPLEMENTS
    | NOTE | ROUTE | ATTACHED | AT | WIDTH | TOP | RIGHT | BOTTOM | LEFT
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
ATTACHED     : 'attached' ;
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

// Signed integer: coordinates, widths, offsets, and the numbers of multiplicities (1, 0..1, 1..*).
NUMBER       : '-'? [0-9]+ ;

NAME         : [\p{L}_] [\p{L}\p{N}_]* (('-' | '/') [\p{L}\p{N}_]+)* ;

// Names and types with spaces, relation labels, note texts. '\n' inside stands for a line break.
BACKTICK_STRING : '`' ~'`'* '`' ;

WS           : [ \t\r\n]+ -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;

fragment HEX : [0-9a-f] ;
