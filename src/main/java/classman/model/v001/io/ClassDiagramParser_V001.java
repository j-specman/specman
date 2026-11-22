package classman.model.v001.io;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.AnchorModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ClassKind_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassDiagram_V001Lexer;
import classman.model.v001.ClassDiagram_V001Parser;
import classman.model.v001.ClassDiagram_V001Parser.AnchorContext;
import classman.model.v001.ClassDiagram_V001Parser.AnchorLineContext;
import classman.model.v001.ClassDiagram_V001Parser.AssociationRelationContext;
import classman.model.v001.ClassDiagram_V001Parser.AttributeContext;
import classman.model.v001.ClassDiagram_V001Parser.BackupTextContext;
import classman.model.v001.ClassDiagram_V001Parser.ChangeMarkContext;
import classman.model.v001.ClassDiagram_V001Parser.ClassBodyContext;
import classman.model.v001.ClassDiagram_V001Parser.ClassDeclContext;
import classman.model.v001.ClassDiagram_V001Parser.ClassDiagramContext;
import classman.model.v001.ClassDiagram_V001Parser.ClassHeadContext;
import classman.model.v001.ClassDiagram_V001Parser.DiagramNameContext;
import classman.model.v001.ClassDiagram_V001Parser.ElementContext;
import classman.model.v001.ClassDiagram_V001Parser.ExtendsRelationContext;
import classman.model.v001.ClassDiagram_V001Parser.ImplementsRelationContext;
import classman.model.v001.ClassDiagram_V001Parser.InfoContext;
import classman.model.v001.ClassDiagram_V001Parser.InfoTextContext;
import classman.model.v001.ClassDiagram_V001Parser.InfosContext;
import classman.model.v001.ClassDiagram_V001Parser.LinePointContext;
import classman.model.v001.ClassDiagram_V001Parser.MultiplicityContext;
import classman.model.v001.ClassDiagram_V001Parser.NameContext;
import classman.model.v001.ClassDiagram_V001Parser.NoteAnchorContext;
import classman.model.v001.ClassDiagram_V001Parser.NoteContext;
import classman.model.v001.ClassDiagram_V001Parser.PositionContext;
import classman.model.v001.ClassDiagram_V001Parser.RelationContext;
import classman.model.v001.ClassDiagram_V001Parser.NoteRouteContext;
import classman.model.v001.ClassDiagram_V001Parser.RouteContext;
import classman.model.v001.ClassDiagram_V001Parser.TaggedValueContext;
import classman.model.v001.ClassDiagram_V001Parser.TextChangeContext;
import classman.model.v001.ClassDiagram_V001Parser.WaypointContext;
import classman.model.v001.ClassDiagram_V001Parser.WidthSpecContext;
import classman.model.v001.ClassDiagram_V001Parser.WordOrStringContext;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ExtendsRelationModel_V001;
import classman.model.v001.ImplementsRelationModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RelationType_V001;
import classman.model.v001.RouteModel_V001;
import classman.model.v001.Side_V001;
import classman.model.v001.TaggedValueModel_V001;
import classman.model.v001.TextChangeModel_V001;
import classman.model.v001.Visibility_V001;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads the text of a class diagram, see ClassDiagram_V001.g4, into the model. */
public class ClassDiagramParser_V001 {

    /** @throws ClassDiagramParseException if the content has a syntax error, or two attributes in one line. */
    public ClassDiagramModel_V001 parse(String content) throws ClassDiagramParseException {
        ClassDiagramSyntaxErrorCollector errors = new ClassDiagramSyntaxErrorCollector();
        ClassDiagram_V001Lexer lexer = new ClassDiagram_V001Lexer(CharStreams.fromString(content));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errors);
        ClassDiagram_V001Parser parser = new ClassDiagram_V001Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errors);
        ClassDiagramContext tree = parser.classDiagram();
        if (errors.hasError()) {
            throw new ClassDiagramParseException(errors.firstError());
        }
        return buildDiagram(tree);
    }

    private ClassDiagramModel_V001 buildDiagram(ClassDiagramContext tree) throws ClassDiagramParseException {
        DiagramNameContext name = tree.diagramName();
        ClassDiagramModel_V001 model = new ClassDiagramModel_V001(name == null ? null : text(name));
        model.nameChange = textChange(tree.nameChange);
        for (ElementContext element : tree.element()) {
            model.elements.add(buildElement(element));
        }
        return model;
    }

    private AbstractDiagramElementModel_V001 buildElement(ElementContext element) throws ClassDiagramParseException {
        if (element.classDecl() != null) {
            ClassKind_V001 kind = kindOf(element.classDecl());
            return buildClass(kind, element.classDecl().classHead(), element.classDecl().classBody());
        }
        if (element.relation() != null) {
            return buildRelation(element.relation());
        }
        if (element.anchorLine() != null) {
            return buildAnchorLine(element.anchorLine());
        }
        return buildNote(element.note());
    }

    // ---- classes ----

    private ClassModel_V001 buildClass(ClassKind_V001 kind, ClassHeadContext head, ClassBodyContext body)
            throws ClassDiagramParseException {
        ClassModel_V001 model = new ClassModel_V001(text(head.name()), kind);
        model.id = id(head.classId);
        model.nameChange = textChange(head.nameChange);
        model.change = change(head.classMark);
        model.position = position(head.position());
        model.width = width(head.widthSpec());
        model.stereotype = stereotype(head.STEREOTYPE());
        model.stereotypeId = id(head.stereotypeId);
        model.stereotypeChange = textChange(head.stereotypeChange);
        model.stereotypeElementChange = change(head.stereotypeMark);
        if (head.taggedValues() != null) {
            for (TaggedValueContext tag : head.taggedValues().taggedValue()) {
                model.taggedValues.add(buildTaggedValue(tag));
            }
        }
        if (body != null) {
            checkOneAttributePerLine(body);
            for (AttributeContext attribute : body.attribute()) {
                checkTypeAllowed(kind, attribute);
                model.attributes.add(buildAttribute(attribute));
            }
        }
        return model;
    }

    private ClassKind_V001 kindOf(ClassDeclContext declaration) {
        if (declaration.INTERFACE() != null) {
            return ClassKind_V001.INTERFACE;
        }
        return declaration.ENUM() != null ? ClassKind_V001.ENUM : ClassKind_V001.CLASS;
    }

    private String stereotype(TerminalNode stereotype) {
        if (stereotype == null) {
            return null;
        }
        String text = stereotype.getText();
        return text.substring(2, text.length() - 2).trim();
    }

    private TaggedValueModel_V001 buildTaggedValue(TaggedValueContext tag) throws ClassDiagramParseException {
        TaggedValueModel_V001 model = new TaggedValueModel_V001(tag.word(0).getText(), taggedValueText(tag));
        model.keyChange = textChange(tag.keyChange);
        model.valueChange = textChange(tag.valueChange);
        model.change = change(tag.changeMark());
        return model;
    }

    private String taggedValueText(TaggedValueContext tag) {
        if (tag.word().size() == 2) {
            return tag.word(1).getText();
        }
        if (tag.NUMBER() != null) {
            return tag.NUMBER().getText();
        }
        return tag.QUOTED_STRING() != null ? unquote(tag.QUOTED_STRING().getText()) : null;
    }

    // ---- attributes ----

    /** Names and types with spaces must be in double quotes, else one attribute would silently become several. */
    private void checkTypeAllowed(ClassKind_V001 kind, AttributeContext attribute) throws ClassDiagramParseException {
        if (!kind.hasTypedAttributes() && attribute.COLON() != null) {
            Token start = attribute.getStart();
            throw new ClassDiagramParseException("line " + start.getLine() + ":" + start.getCharPositionInLine()
                + " The value of an enum has no type.");
        }
    }

    private void checkOneAttributePerLine(ClassBodyContext body) throws ClassDiagramParseException {
        List<AttributeContext> attributes = body.attribute();
        for (int i = 1; i < attributes.size(); i++) {
            Token previousEnd = attributes.get(i - 1).getStop();
            Token start = attributes.get(i).getStart();
            if (start.getLine() == previousEnd.getLine()) {
                throw new ClassDiagramParseException("line " + start.getLine() + ":" + start.getCharPositionInLine()
                    + " Two attributes start in one line. Attribute names and types with spaces need double quotes,"
                    + " for example \"In Baum anzeigen\".");
            }
        }
    }

    private AttributeModel_V001 buildAttribute(AttributeContext attribute) throws ClassDiagramParseException {
        if (attribute.ELLIPSIS() != null) {
            return AttributeModel_V001.hiddenAttributesMarker();
        }
        Visibility_V001 visibility = attribute.visibility() == null ? null
            : Visibility_V001.fromSign(attribute.visibility().getText().charAt(0));
        List<WordOrStringContext> nameAndType = attribute.wordOrString();
        String name = text(nameAndType.get(0));
        String type = attribute.COLON() == null ? null : text(nameAndType.get(1));
        AttributeModel_V001 model = new AttributeModel_V001(visibility, name, type, infos(attribute.infos()));
        model.id = id(attribute.ELEMENT_ID());
        model.visibilityChange = textChange(attribute.visibilityChange);
        model.nameChange = textChange(attribute.nameChange);
        model.typeChange = textChange(attribute.typeChange);
        model.change = change(attribute.changeMark());
        return model;
    }

    private List<InfoModel_V001> infos(InfosContext infos) throws ClassDiagramParseException {
        List<InfoModel_V001> result = new ArrayList<>();
        if (infos != null) {
            for (InfoContext info : infos.info()) {
                InfoModel_V001 model = new InfoModel_V001(infoText(info.infoText()));
                model.change = change(info.changeMark());
                model.textChange = textChange(info.textChange());
                result.add(model);
            }
        }
        return result;
    }

    /** The info as written, except that a single quoted string loses its quotes. */
    private String infoText(InfoTextContext info) {
        if (info.getChildCount() == 1 && info.QUOTED_STRING().size() == 1) {
            return unquote(info.getText());
        }
        return info.getStart().getInputStream().getText(
            Interval.of(info.getStart().getStartIndex(), info.getStop().getStopIndex()));
    }

    // ---- relations ----

    private AbstractRelationModel_V001 buildRelation(RelationContext relation) throws ClassDiagramParseException {
        if (relation.associationRelation() != null) {
            return buildAssociation(relation.associationRelation());
        }
        if (relation.extendsRelation() != null) {
            return buildExtends(relation.extendsRelation());
        }
        return buildImplements(relation.implementsRelation());
    }

    private AssociationRelationModel_V001 buildAssociation(AssociationRelationContext ctx) throws ClassDiagramParseException {
        RelationType_V001 type = RelationType_V001.valueOf(ctx.type.getText().toUpperCase(Locale.ROOT));
        AssociationRelationModel_V001 model = new AssociationRelationModel_V001(type, ctx.from.getText(), ctx.to.getText());
        model.fromMultiplicity = multiplicity(ctx.fromMultiplicity);
        model.toMultiplicity = multiplicity(ctx.toMultiplicity);
        model.fromMultiplicityChange = textChange(ctx.fromMultiplicityChange);
        model.toMultiplicityChange = textChange(ctx.toMultiplicityChange);
        model.directed = ctx.ARROW() != null;
        fillCommon(model, ctx.ELEMENT_ID(), ctx.changeMark(), ctx.label, ctx.labelChange, ctx.route());
        if (ctx.assocClass() != null) {
            model.assocClass = buildClass(ClassKind_V001.CLASS, ctx.assocClass().classHead(), ctx.assocClass().classBody());
        }
        return model;
    }

    private ExtendsRelationModel_V001 buildExtends(ExtendsRelationContext ctx) throws ClassDiagramParseException {
        ExtendsRelationModel_V001 model = new ExtendsRelationModel_V001(ctx.from.getText(), ctx.to.getText());
        fillCommon(model, ctx.ELEMENT_ID(), ctx.changeMark(), ctx.label, ctx.labelChange, ctx.route());
        return model;
    }

    private ImplementsRelationModel_V001 buildImplements(ImplementsRelationContext ctx)
            throws ClassDiagramParseException {
        ImplementsRelationModel_V001 model = new ImplementsRelationModel_V001(ctx.from.getText(), ctx.to.getText());
        fillCommon(model, ctx.ELEMENT_ID(), ctx.changeMark(), ctx.label, ctx.labelChange, ctx.route());
        return model;
    }

    private void fillCommon(AbstractRelationModel_V001 model, TerminalNode id, ChangeMarkContext changeMark, Token label,
                            TextChangeContext labelChange,
                            RouteContext route) throws ClassDiagramParseException {
        model.id = id(id);
        model.change = change(changeMark);
        model.label = label == null ? null : unquote(label.getText());
        model.labelChange = textChange(labelChange);
        model.route = route == null ? null : buildRoute(route);
    }

    private String multiplicity(MultiplicityContext multiplicity) {
        if (multiplicity == null) {
            return null;
        }
        return multiplicity.QUOTED_STRING() != null ? unquote(multiplicity.QUOTED_STRING().getText()) : multiplicity.getText();
    }

    private RouteModel_V001 buildRoute(RouteContext route) {
        List<AnchorContext> anchors = route.anchor();
        List<PositionModel_V001> waypoints = new ArrayList<>();
        for (WaypointContext waypoint : route.waypoint()) {
            waypoints.add(position(waypoint.NUMBER(0), waypoint.NUMBER(1)));
        }
        return new RouteModel_V001(buildAnchor(anchors.get(0)), waypoints, buildAnchor(anchors.get(1)));
    }

    private AnchorModel_V001 buildAnchor(AnchorContext anchor) {
        Side_V001 side = Side_V001.valueOf(anchor.side().getText().toUpperCase(Locale.ROOT));
        return new AnchorModel_V001(anchor.HEX_ID().getText(), side, Integer.parseInt(anchor.NUMBER().getText()));
    }

    // ---- notes ----

    private NoteModel_V001 buildNote(NoteContext ctx) throws ClassDiagramParseException {
        NoteModel_V001 model = new NoteModel_V001(unquote(ctx.QUOTED_STRING().getText()));
        model.id = id(ctx.noteId);
        model.change = change(ctx.changeMark());
        model.textChange = textChange(ctx.textChange());
        model.position = position(ctx.position());
        model.width = width(ctx.widthSpec());
        return model;
    }

    // ---- anchor lines ----

    private AnchorLineModel_V001 buildAnchorLine(AnchorLineContext ctx) throws ClassDiagramParseException {
        AnchorLineModel_V001 model = new AnchorLineModel_V001(ctx.noteRef.getText(), ctx.targetRef.getText());
        model.id = id(ctx.ELEMENT_ID());
        model.change = change(ctx.changeMark());
        if (ctx.noteRoute() != null) {
            buildRoute(model, ctx.noteRoute());
        }
        return model;
    }

    /** An anchor on the note is the start of the line, one on the target the end. A point on a line is only an end. */
    private void buildRoute(AnchorLineModel_V001 model, NoteRouteContext route) throws ClassDiagramParseException {
        for (WaypointContext waypoint : route.waypoint()) {
            model.waypoints.add(position(waypoint.NUMBER(0), waypoint.NUMBER(1)));
        }
        for (NoteAnchorContext anchor : route.noteAnchor()) {
            AnchorModel_V001 built = anchor.anchor() != null ? buildAnchor(anchor.anchor())
                : buildLinePoint(anchor.linePoint());
            if (!built.isOnLine() && built.classId.equals(model.noteId) && model.start == null) {
                model.start = built;
            }
            else if (built.classId.equals(model.targetId) && model.end == null) {
                model.end = built;
            }
            else {
                Token start = anchor.getStart();
                throw new ClassDiagramParseException("line " + start.getLine() + ":" + start.getCharPositionInLine()
                    + " An anchor in the route of an anchor line must name the note (for the start of the line) or the"
                    + " element it leads to (for the end), and each of them only once.");
            }
        }
    }

    private AnchorModel_V001 buildLinePoint(LinePointContext linePoint) {
        return AnchorModel_V001.onLine(linePoint.HEX_ID().getText(), Integer.parseInt(linePoint.NUMBER().getText()));
    }

    // ---- small parts ----

    private ChangeModel_V001 change(ChangeMarkContext changeMark) throws ClassDiagramParseException {
        if (changeMark == null) {
            return null;
        }
        String typeWord = changeMark.changeType.getText();
        ChangeType_V001 type = ChangeType_V001.fromKeyword(typeWord);
        if (type == null) {
            Token start = changeMark.changeType.getStart();
            throw new ClassDiagramParseException("line " + start.getLine() + ":" + start.getCharPositionInLine()
                + " Unknown change type " + typeWord + ", expected added or removed.");
        }
        return new ChangeModel_V001(type, text(changeMark.changeSet));
    }

    private String id(TerminalNode elementId) {
        return elementId == null ? null : id(elementId.getSymbol());
    }

    private String id(Token elementId) {
        return elementId == null ? null : elementId.getText().substring(1, elementId.getText().length() - 1);
    }

    private TextChangeModel_V001 textChange(TextChangeContext textChange) {
        if (textChange == null) {
            return null;
        }
        String backup = textChange.backup == null ? null : backupText(textChange.backup);
        return new TextChangeModel_V001(text(textChange.changeSet), backup);
    }

    private String backupText(BackupTextContext backup) {
        return backup.wordOrString() != null ? text(backup.wordOrString()) : backup.getText();
    }

    private PositionModel_V001 position(PositionContext position) {
        return position == null ? null : position(position.NUMBER(0), position.NUMBER(1));
    }

    private PositionModel_V001 position(TerminalNode x, TerminalNode y) {
        return new PositionModel_V001(Integer.parseInt(x.getText()), Integer.parseInt(y.getText()));
    }

    private Integer width(WidthSpecContext width) {
        return width == null ? null : Integer.valueOf(width.NUMBER().getText());
    }

    private String text(NameContext name) {
        return name.QUOTED_STRING() != null ? unquote(name.QUOTED_STRING().getText()) : name.getText();
    }

    private String text(DiagramNameContext name) {
        return name.QUOTED_STRING() != null ? unquote(name.QUOTED_STRING().getText()) : name.getText();
    }

    private String text(WordOrStringContext wordOrString) {
        return wordOrString.word() != null ? wordOrString.word().getText()
            : unquote(wordOrString.QUOTED_STRING().getText());
    }

    /** The text of a quoted string without the quotes, with its escapes resolved. */
    private String unquote(String quotedString) {
        String body = quotedString.substring(1, quotedString.length() - 1);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c != '\\') {
                text.append(c);
                continue;
            }
            i++;
            char escaped = body.charAt(i);
            if (escaped == 'n') {
                text.append('\n');
            } else if (escaped == 'u' && isUnicodeEscape(body, i)) {
                text.append((char) Integer.parseInt(body.substring(i + 1, i + 5), 16));
                i += 4;
            } else if (escaped == '"' || escaped == '\\') {
                text.append(escaped);
            } else {
                text.append('\\').append(escaped);
            }
        }
        return text.toString();
    }

    /** True if the u at the given index starts a unicode escape: four hex digits follow. */
    private boolean isUnicodeEscape(String body, int indexOfU) {
        return indexOfU + 4 < body.length() && body.substring(indexOfU + 1, indexOfU + 5).matches("[0-9a-fA-F]{4}");
    }
}
