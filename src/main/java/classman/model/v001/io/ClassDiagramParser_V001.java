package classman.model.v001.io;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.AssocClassLinkModel_V001;
import classman.model.v001.AttachLineModel_V001;
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
import classman.model.v001.ClassDiagram_V001Parser.RouteAnchorContext;
import classman.model.v001.ClassDiagram_V001Parser.ClassDiagramContext;
import classman.model.v001.ClassDiagram_V001Parser.ClassHeadContext;
import classman.model.v001.ClassDiagram_V001Parser.DependencyRelationContext;
import classman.model.v001.ClassDiagram_V001Parser.DiagramNameContext;
import classman.model.v001.ClassDiagram_V001Parser.ElementContext;
import classman.model.v001.ClassDiagram_V001Parser.CompartmentSpecContext;
import classman.model.v001.ClassDiagram_V001Parser.MemberContext;
import classman.model.v001.ClassDiagram_V001Parser.MethodContext;
import classman.model.v001.ClassDiagram_V001Parser.MethodNameContext;
import classman.model.v001.ClassDiagram_V001Parser.ParameterContext;
import classman.model.v001.ClassDiagram_V001Parser.ParametersContext;
import classman.model.v001.ClassDiagram_V001Parser.ExtendsRelationContext;
import classman.model.v001.ClassDiagram_V001Parser.ImplementsRelationContext;
import classman.model.v001.ClassDiagram_V001Parser.InfoContext;
import classman.model.v001.ClassDiagram_V001Parser.VisibilityContext;
import classman.model.v001.ClassDiagram_V001Parser.InfosContext;
import classman.model.v001.ClassDiagram_V001Parser.LinePointContext;
import classman.model.v001.ClassDiagram_V001Parser.LabelPositionContext;
import classman.model.v001.ClassDiagram_V001Parser.MultiplicityContext;
import classman.model.v001.ClassDiagram_V001Parser.NameContext;
import classman.model.v001.ClassDiagram_V001Parser.AttachAnchorContext;
import classman.model.v001.ClassDiagram_V001Parser.NoteContext;
import classman.model.v001.ClassDiagram_V001Parser.PositionContext;
import classman.model.v001.ClassDiagram_V001Parser.RelationContext;
import classman.model.v001.ClassDiagram_V001Parser.AttachRouteContext;
import classman.model.v001.ClassDiagram_V001Parser.AssocClassLinkContext;
import classman.model.v001.ClassDiagram_V001Parser.RouteContext;
import classman.model.v001.ClassDiagram_V001Parser.StyleSpecContext;
import classman.model.v001.ClassDiagram_V001Parser.TaggedValueContext;
import classman.model.v001.ClassDiagram_V001Parser.TextChangeContext;
import classman.model.v001.ClassDiagram_V001Parser.WaypointContext;
import classman.model.v001.ClassDiagram_V001Parser.WidthSpecContext;
import classman.model.v001.ClassDiagram_V001Parser.WordOrStringContext;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.CompartmentVisibility_V001;
import classman.model.v001.DependencyRelationModel_V001;
import classman.model.v001.ExtendsRelationModel_V001;
import classman.model.v001.ImplementsRelationModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.MethodModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.TextPlaceModel_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.RelationType_V001;
import classman.model.v001.RouteModel_V001;
import classman.model.v001.StyleProperty_V001;
import classman.model.v001.Side_V001;
import classman.model.v001.TaggedValueModel_V001;
import classman.model.v001.TextChangeModel_V001;
import classman.model.v001.Visibility_V001;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads the text of a class diagram, see ClassDiagram_V001.g4, into the model. */
public class ClassDiagramParser_V001 {
    private final StyleReader_V001 classStyleReader = new StyleReader_V001("class", StyleProperty_V001.ALL);
    private final StyleReader_V001 noteStyleReader = new StyleReader_V001("note", StyleProperty_V001.ALL);
    private final StyleReader_V001 relationStyleReader =
        new StyleReader_V001("relation", StyleProperty_V001.WITHOUT_FILL);

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
        if (element.assocClassLink() != null) {
            return buildAssocClassLink(element.assocClassLink());
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
        model.style = classStyleReader.read(head.styleSpec());
        readCompartmentSpec(model, head.compartmentSpec());
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
            checkOneMemberPerLine(body);
            for (MemberContext member : body.member()) {
                if (member.method() != null) {
                    model.methods.add(buildMethod(member.method()));
                }
                else {
                    checkAttributeAllowed(kind, member.attribute());
                    model.attributes.add(buildAttribute(member.attribute()));
                }
            }
        }
        return model;
    }

    /** Each word of the compartment spec may stand once, and the method compartment is hidden or shown, not both. */
    private void readCompartmentSpec(ClassModel_V001 model, CompartmentSpecContext spec)
            throws ClassDiagramParseException {
        if (spec == null) {
            return;
        }
        int hideMethods = spec.HIDEMETHODS().size();
        int showMethods = spec.SHOWMETHODS().size();
        if (spec.HIDEATTRIBUTES().size() > 1 || hideMethods > 1 || showMethods > 1) {
            throw problem(spec.getStart(), "hideattributes, hidemethods and showmethods may stand once each.");
        }
        if (hideMethods > 0 && showMethods > 0) {
            throw problem(spec.getStart(), "hidemethods and showmethods exclude each other.");
        }
        model.attributesHidden = !spec.HIDEATTRIBUTES().isEmpty();
        if (hideMethods > 0) {
            model.methodsVisibility = CompartmentVisibility_V001.HIDDEN;
        }
        else if (showMethods > 0) {
            model.methodsVisibility = CompartmentVisibility_V001.SHOWN;
        }
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

    // ---- attributes and methods ----

    /** An enum value has no type, and an interface has no attributes. */
    private void checkAttributeAllowed(ClassKind_V001 kind, AttributeContext attribute)
            throws ClassDiagramParseException {
        if (!kind.hasAttributes()) {
            throw problem(attribute.getStart(), "An interface has no attributes.");
        }
        if (!kind.hasTypedAttributes() && attribute.COLON() != null) {
            throw problem(attribute.getStart(), "The value of an enum has no type.");
        }
    }

    /** Names and types with spaces must be in double quotes, else one member would silently become several. */
    private void checkOneMemberPerLine(ClassBodyContext body) throws ClassDiagramParseException {
        List<MemberContext> members = body.member();
        for (int i = 1; i < members.size(); i++) {
            Token previousEnd = members.get(i - 1).getStop();
            Token start = members.get(i).getStart();
            if (start.getLine() == previousEnd.getLine()) {
                throw problem(start, "Two attributes or methods start in one line. Names and types with spaces need"
                    + " double quotes, for example \"In Baum anzeigen\".");
            }
        }
    }

    private AttributeModel_V001 buildAttribute(AttributeContext attribute) throws ClassDiagramParseException {
        if (attribute.ELLIPSIS() != null) {
            return AttributeModel_V001.hiddenAttributesMarker();
        }
        List<WordOrStringContext> nameAndType = attribute.wordOrString();
        String type = attribute.COLON() == null ? null : text(nameAndType.get(1));
        AttributeModel_V001 model = new AttributeModel_V001(visibility(attribute.visibility()),
            text(nameAndType.get(0)), type, infos(attribute.infos()));
        model.id = id(attribute.ELEMENT_ID());
        model.visibilityChange = textChange(attribute.visibilityChange);
        model.nameChange = textChange(attribute.nameChange);
        model.typeChange = textChange(attribute.typeChange);
        model.change = change(attribute.changeMark());
        model.hidden = attribute.HIDDEN_MARK() != null;
        return model;
    }

    private MethodModel_V001 buildMethod(MethodContext method) throws ClassDiagramParseException {
        String returnType = method.COLON() == null ? null : text(method.wordOrString());
        MethodModel_V001 model = new MethodModel_V001(visibility(method.visibility()), text(method.methodName()),
            parameters(method.parameters()), returnType, infos(method.infos()));
        model.id = id(method.ELEMENT_ID());
        model.visibilityChange = textChange(method.visibilityChange);
        model.nameChange = textChange(method.nameChange);
        model.typeChange = textChange(method.typeChange);
        model.change = change(method.changeMark());
        model.hidden = method.HIDDEN_MARK() != null;
        return model;
    }

    private Visibility_V001 visibility(VisibilityContext visibility) {
        return visibility == null ? null : Visibility_V001.fromSign(visibility.getText().charAt(0));
    }

    private List<InfoModel_V001> infos(InfosContext infos) throws ClassDiagramParseException {
        List<InfoModel_V001> result = new ArrayList<>();
        if (infos != null) {
            for (InfoContext info : infos.info()) {
                result.add(listEntry(info.infoText(), info.changeMark(), info.textChange()));
            }
        }
        return result;
    }

    private List<InfoModel_V001> parameters(ParametersContext parameters) throws ClassDiagramParseException {
        List<InfoModel_V001> result = new ArrayList<>();
        for (ParameterContext parameter : parameters.parameter()) {
            result.add(listEntry(parameter.parameterText(), parameter.changeMark(), parameter.textChange()));
        }
        return result;
    }

    private InfoModel_V001 listEntry(ParserRuleContext text, ChangeMarkContext changeMark,
                                     TextChangeContext textChange) throws ClassDiagramParseException {
        InfoModel_V001 model = new InfoModel_V001(entryText(text));
        model.change = change(changeMark);
        model.textChange = textChange(textChange);
        return model;
    }

    /** The text of an info or parameter as written, except that a single quoted string loses its quotes. */
    private String entryText(ParserRuleContext entry) {
        if (entry.getChildCount() == 1 && entry.getStart().getType() == ClassDiagram_V001Lexer.QUOTED_STRING) {
            return unquote(entry.getText());
        }
        return entry.getStart().getInputStream().getText(
            Interval.of(entry.getStart().getStartIndex(), entry.getStop().getStopIndex()));
    }

    private ClassDiagramParseException problem(Token at, String message) {
        return new ClassDiagramParseException("line " + at.getLine() + ":" + at.getCharPositionInLine() + " " + message);
    }

    // ---- relations ----

    private AbstractRelationModel_V001 buildRelation(RelationContext relation) throws ClassDiagramParseException {
        if (relation.associationRelation() != null) {
            return buildAssociation(relation.associationRelation());
        }
        if (relation.extendsRelation() != null) {
            return buildExtends(relation.extendsRelation());
        }
        if (relation.dependencyRelation() != null) {
            return buildDependency(relation.dependencyRelation());
        }
        return buildImplements(relation.implementsRelation());
    }

    private DependencyRelationModel_V001 buildDependency(DependencyRelationContext ctx)
            throws ClassDiagramParseException {
        DependencyRelationModel_V001 model = new DependencyRelationModel_V001(ctx.from.getText(), ctx.to.getText());
        fillCommon(model, ctx.ELEMENT_ID(), ctx.changeMark(), ctx.styleSpec(), ctx.label, ctx.labelChange,
            ctx.labelPlace, ctx.route());
        return model;
    }

    private AssociationRelationModel_V001 buildAssociation(AssociationRelationContext ctx) throws ClassDiagramParseException {
        RelationType_V001 type = RelationType_V001.valueOf(ctx.type.getText().toUpperCase(Locale.ROOT));
        AssociationRelationModel_V001 model = new AssociationRelationModel_V001(type, ctx.from.getText(), ctx.to.getText());
        model.fromMultiplicity = multiplicity(ctx.fromMultiplicity);
        model.toMultiplicity = multiplicity(ctx.toMultiplicity);
        model.fromMultiplicityChange = textChange(ctx.fromMultiplicityChange);
        model.toMultiplicityChange = textChange(ctx.toMultiplicityChange);
        model.fromMultiplicityPlace = place(ctx.fromPlace);
        model.toMultiplicityPlace = place(ctx.toPlace);
        model.fromArrow = ctx.BACK_ARROW() != null || ctx.BOTH_ARROW() != null;
        model.toArrow = ctx.ARROW() != null || ctx.BOTH_ARROW() != null;
        if (model.fromArrow && !type.canHaveArrowAt(RelationEnd_V001.FROM)) {
            Token token = ctx.BACK_ARROW() != null ? ctx.BACK_ARROW().getSymbol() : ctx.BOTH_ARROW().getSymbol();
            throw new ClassDiagramParseException("line " + token.getLine() + ":" + token.getCharPositionInLine()
                + " The source of an " + type.name().toLowerCase(Locale.ROOT) + " has the diamond and no arrowhead.");
        }
        fillCommon(model, ctx.ELEMENT_ID(), ctx.changeMark(), ctx.styleSpec(), ctx.label, ctx.labelChange,
            ctx.labelPlace, ctx.route());
        return model;
    }

    private ExtendsRelationModel_V001 buildExtends(ExtendsRelationContext ctx) throws ClassDiagramParseException {
        ExtendsRelationModel_V001 model = new ExtendsRelationModel_V001(ctx.from.getText(), ctx.to.getText());
        fillCommon(model, ctx.ELEMENT_ID(), ctx.changeMark(), ctx.styleSpec(), ctx.label, ctx.labelChange,
            ctx.labelPlace, ctx.route());
        return model;
    }

    private ImplementsRelationModel_V001 buildImplements(ImplementsRelationContext ctx)
            throws ClassDiagramParseException {
        ImplementsRelationModel_V001 model = new ImplementsRelationModel_V001(ctx.from.getText(), ctx.to.getText());
        fillCommon(model, ctx.ELEMENT_ID(), ctx.changeMark(), ctx.styleSpec(), ctx.label, ctx.labelChange,
            ctx.labelPlace, ctx.route());
        return model;
    }

    private void fillCommon(AbstractRelationModel_V001 model, TerminalNode id, ChangeMarkContext changeMark,
                            StyleSpecContext style, Token label, TextChangeContext labelChange,
                            LabelPositionContext labelPlace, RouteContext route) throws ClassDiagramParseException {
        model.id = id(id);
        model.style = relationStyleReader.read(style);
        model.change = change(changeMark);
        model.label = label == null ? null : unquote(label.getText());
        model.labelChange = textChange(labelChange);
        model.labelPlace = labelPlace == null ? null
            : new TextPlaceModel_V001(Integer.parseInt(labelPlace.NUMBER(0).getText()),
                Integer.parseInt(labelPlace.NUMBER(1).getText()));
        model.route = route == null ? null : buildRoute(route);
    }

    private TextPlaceModel_V001 place(PositionContext position) {
        return position == null ? null : new TextPlaceModel_V001(Integer.parseInt(position.NUMBER(0).getText()),
            Integer.parseInt(position.NUMBER(1).getText()));
    }

    private String multiplicity(MultiplicityContext multiplicity) {
        if (multiplicity == null) {
            return null;
        }
        return multiplicity.QUOTED_STRING() != null ? unquote(multiplicity.QUOTED_STRING().getText()) : multiplicity.getText();
    }

    private RouteModel_V001 buildRoute(RouteContext route) {
        List<RouteAnchorContext> anchors = route.routeAnchor();
        List<PositionModel_V001> waypoints = new ArrayList<>();
        for (WaypointContext waypoint : route.waypoint()) {
            waypoints.add(position(waypoint.NUMBER(0), waypoint.NUMBER(1)));
        }
        return new RouteModel_V001(buildRouteAnchor(anchors.get(0)), waypoints, buildRouteAnchor(anchors.get(1)));
    }

    private AnchorModel_V001 buildRouteAnchor(RouteAnchorContext anchor) {
        if (anchor.centerAnchor() != null) {
            return AnchorModel_V001.center(anchor.centerAnchor().HEX_ID().getText());
        }
        return buildAnchor(anchor.anchor());
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
        model.style = noteStyleReader.read(ctx.styleSpec());
        model.textChange = textChange(ctx.textChange());
        model.position = position(ctx.position());
        model.width = width(ctx.widthSpec());
        return model;
    }

    // ---- dashed lines of notes and classes ----

    private AnchorLineModel_V001 buildAnchorLine(AnchorLineContext ctx) throws ClassDiagramParseException {
        AnchorLineModel_V001 model = new AnchorLineModel_V001(ctx.noteRef.getText(), ctx.targetRef.getText());
        model.id = id(ctx.ELEMENT_ID());
        model.change = change(ctx.changeMark());
        if (ctx.attachRoute() != null) {
            buildRoute(model, ctx.attachRoute());
        }
        return model;
    }

    private AssocClassLinkModel_V001 buildAssocClassLink(AssocClassLinkContext ctx) throws ClassDiagramParseException {
        AssocClassLinkModel_V001 model = new AssocClassLinkModel_V001(ctx.classRef.getText(), ctx.relationRef.getText());
        model.id = id(ctx.ELEMENT_ID());
        model.change = change(ctx.changeMark());
        if (ctx.attachRoute() != null) {
            buildRoute(model, ctx.attachRoute());
        }
        return model;
    }

    /** An anchor on the box is the start of the line, one on the target the end. A point on a line is only an end. */
    private void buildRoute(AttachLineModel_V001 model, AttachRouteContext route) throws ClassDiagramParseException {
        for (WaypointContext waypoint : route.waypoint()) {
            model.waypoints.add(position(waypoint.NUMBER(0), waypoint.NUMBER(1)));
        }
        for (AttachAnchorContext anchor : route.attachAnchor()) {
            AnchorModel_V001 built = anchor.anchor() != null ? buildAnchor(anchor.anchor())
                : buildLinePoint(anchor.linePoint());
            if (!built.isOnLine() && built.classId.equals(model.startId) && model.start == null) {
                model.start = built;
            }
            else if (built.classId.equals(model.targetId) && model.end == null) {
                model.end = built;
            }
            else {
                Token start = anchor.getStart();
                throw new ClassDiagramParseException("line " + start.getLine() + ":" + start.getCharPositionInLine()
                    + " An anchor in the route of a dashed line must name the box it starts at (for the start of the line) or"
                    + " the element it leads to (for the end), and each of them only once.");
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

    private String text(MethodNameContext name) {
        return name.plainWord() != null ? name.plainWord().getText() : unquote(name.QUOTED_STRING().getText());
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
