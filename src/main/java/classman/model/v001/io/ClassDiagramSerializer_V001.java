package classman.model.v001.io;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.AssocClassLinkModel_V001;
import classman.model.v001.AttachLineModel_V001;
import classman.model.v001.AnchorModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassDiagram_V001Lexer;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.Multiplicity_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RouteModel_V001;
import classman.model.v001.TaggedValueModel_V001;
import classman.model.v001.TextChangeModel_V001;
import classman.model.v001.TextPlaceModel_V001;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Writes a class diagram model as text in the canonical form: reading it again gives the same model. */
public class ClassDiagramSerializer_V001 {
    private static final String INDENT = "    ";
    private static final String ENVELOPE =
        "// {\"classmanVersion\":\"0.1\",\"modelType\":\"classman.model.v001.ClassDiagramModel_V001\"}";
    private static final Pattern NAME = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]*(?:[-/][\\p{L}\\p{N}_]+)*");
    private static final Pattern INTEGER = Pattern.compile("-?[0-9]+");

    private final Set<Integer> keywordTypes = new HashSet<>();
    private final Set<String> keywords = new HashSet<>();

    public ClassDiagramSerializer_V001() {
        for (int type = 1; type <= ClassDiagram_V001Lexer.VOCABULARY.getMaxTokenType(); type++) {
            String literal = ClassDiagram_V001Lexer.VOCABULARY.getLiteralName(type);
            if (literal != null && literal.matches("'[a-z]+'")) {
                keywordTypes.add(type);
                keywords.add(literal.substring(1, literal.length() - 1));
            }
        }
    }

    public String serialize(ClassDiagramModel_V001 model) {
        StringBuilder out = new StringBuilder(ENVELOPE).append('\n');
        out.append("classdiagram");
        if (model.name != null) {
            out.append(' ').append(name(model.name));
            appendTextChange(out, model.nameChange);
        }
        out.append(" {\n");
        boolean first = true;
        for (AbstractDiagramElementModel_V001 element : model.elements) {
            if (!first) {
                out.append('\n');
            }
            first = false;
            appendElement(out, element);
        }
        return out.append("}\n").toString();
    }

    private void appendElement(StringBuilder out, AbstractDiagramElementModel_V001 element) {
        if (element instanceof ClassModel_V001 classModel) {
            appendClass(out, INDENT, classModel.kind.keyword, classModel);
        } else if (element instanceof AbstractRelationModel_V001 relation) {
            appendRelation(out, relation);
        } else if (element instanceof NoteModel_V001 note) {
            appendNote(out, note);
        } else if (element instanceof AnchorLineModel_V001 line) {
            appendAttachLine(out, "anchor", line);
        } else if (element instanceof AssocClassLinkModel_V001 link) {
            appendAttachLine(out, "assocclass", link);
        } else {
            throw new IllegalArgumentException("Unknown diagram element: " + element.getClass().getName());
        }
    }

    // ---- classes ----

    private void appendClass(StringBuilder out, String indent, String keyword, ClassModel_V001 classModel) {
        out.append(indent).append(keyword).append(' ').append(name(classModel.name));
        appendTextChange(out, classModel.nameChange);
        appendId(out, classModel.id);
        appendChange(out, classModel.change);
        appendPosition(out, classModel.position);
        appendWidth(out, classModel.width);
        appendStyle(out, classModel.style);
        if (classModel.attributesHidden) {
            out.append(" hideattributes");
        }
        if (classModel.stereotype != null) {
            out.append(" <<").append(classModel.stereotype).append(">>");
            appendTextChange(out, classModel.stereotypeChange);
            appendId(out, classModel.stereotypeId);
            appendChange(out, classModel.stereotypeElementChange);
        }
        appendTaggedValues(out, classModel.taggedValues);
        if (!classModel.attributes.isEmpty()) {
            out.append(" {\n");
            for (AttributeModel_V001 attribute : classModel.attributes) {
                out.append(indent).append(INDENT).append(attribute(attribute)).append('\n');
            }
            out.append(indent).append('}');
        }
        out.append('\n');
    }

    private void appendTaggedValues(StringBuilder out, List<TaggedValueModel_V001> taggedValues) {
        if (taggedValues.isEmpty()) {
            return;
        }
        out.append(" {");
        for (int i = 0; i < taggedValues.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            appendTaggedValue(out, taggedValues.get(i));
        }
        out.append('}');
    }

    private void appendTaggedValue(StringBuilder out, TaggedValueModel_V001 tag) {
        out.append(word(tag.key));
        if (tag.change == null) {
            appendTextChange(out, tag.keyChange);
        }
        out.append('=');
        if (tag.value != null && !tag.value.isEmpty()) {
            out.append(INTEGER.matcher(tag.value).matches() ? tag.value : word(tag.value));
        }
        if (tag.change != null) {
            appendChange(out, tag.change);
        }
        else {
            appendTextChange(out, tag.valueChange);
        }
    }

    private String attribute(AttributeModel_V001 attribute) {
        if (attribute.hiddenAttributesMarker) {
            return "...";
        }
        StringBuilder line = new StringBuilder();
        if (attribute.visibility != null) {
            line.append(attribute.visibility.sign);
            if (attribute.visibilityChange != null) {
                appendTextChange(line, attribute.visibilityChange);
                line.append(' ');
            }
        }
        line.append(word(attribute.name));
        appendTextChange(line, attribute.nameChange);
        appendId(line, attribute.id);
        appendChange(line, attribute.change);
        if (attribute.type != null) {
            line.append(" : ").append(word(attribute.type));
            appendTextChange(line, attribute.typeChange);
        }
        if (!attribute.infos.isEmpty()) {
            List<String> infos = new ArrayList<>();
            for (InfoModel_V001 info : attribute.infos) {
                StringBuilder item = new StringBuilder(isBareInfo(info.text) ? info.text : quoted(info.text));
                if (info.change != null) {
                    appendChange(item, info.change);
                }
                else {
                    appendTextChange(item, info.textChange);
                }
                infos.add(item.toString());
            }
            line.append(" [").append(String.join(", ", infos)).append(']');
        }
        if (attribute.hidden) {
            line.append(" (hidden)");
        }
        return line.toString();
    }

    // ---- relations ----

    private static String arrowToken(AssociationRelationModel_V001 association) {
        if (association.fromArrow) {
            return association.toArrow ? " <->" : " <-";
        }
        return association.toArrow ? " ->" : " --";
    }

    private void appendRelation(StringBuilder out, AbstractRelationModel_V001 relation) {
        out.append(INDENT).append(relation.type.name().toLowerCase(Locale.ROOT));
        appendId(out, relation.id);
        appendChange(out, relation.change);
        appendStyle(out, relation.style);
        out.append(' ').append(relation.fromId);
        if (relation instanceof AssociationRelationModel_V001 association) {
            appendMultiplicity(out, association.fromMultiplicity, association.fromMultiplicityChange,
                association.fromMultiplicityPlace);
            out.append(arrowToken(association));
            appendMultiplicity(out, association.toMultiplicity, association.toMultiplicityChange,
                association.toMultiplicityPlace);
        } else {
            out.append(" ->");
        }
        out.append(' ').append(relation.toId);
        if (relation.label != null || relation.labelChange != null) {
            out.append(' ').append(quoted(relation.label == null ? "" : relation.label));
            appendTextChange(out, relation.labelChange);
            if (relation.labelPlace != null) {
                out.append(" at(").append(relation.labelPlace.along).append("%, ").append(relation.labelPlace.across)
                    .append(')');
            }
        }
        out.append('\n');
        if (relation.route != null) {
            appendRoute(out, relation.route);
        }
    }

    private void appendMultiplicity(StringBuilder out, String multiplicity, TextChangeModel_V001 change,
                                    TextPlaceModel_V001 place) {
        if (multiplicity != null || change != null) {
            String text = multiplicity == null ? "" : multiplicity;
            out.append(' ').append(Multiplicity_V001.isBare(text) ? text : quoted(text));
            appendTextChange(out, change);
            if (place != null) {
                out.append(" at(").append(place.along).append(", ").append(place.across).append(')');
            }
        }
    }

    private void appendRoute(StringBuilder out, RouteModel_V001 route) {
        out.append(INDENT).append(INDENT).append("route ").append(anchor(route.start));
        appendWaypoints(out, route.waypoints);
        out.append(' ').append(anchor(route.end)).append('\n');
    }

    private void appendWaypoints(StringBuilder out, List<PositionModel_V001> waypoints) {
        for (PositionModel_V001 waypoint : waypoints) {
            out.append(" (").append(waypoint.x).append(", ").append(waypoint.y).append(')');
        }
    }

    private String anchor(AnchorModel_V001 anchor) {
        if (anchor.isOnLine()) {
            return anchor.classId + "@" + anchor.offset + "%";
        }
        if (anchor.center) {
            return anchor.classId + ".center";
        }
        return anchor.classId + "." + anchor.side.name().toLowerCase(Locale.ROOT) + "@" + anchor.offset;
    }

    // ---- notes ----

    private void appendNote(StringBuilder out, NoteModel_V001 note) {
        out.append(INDENT).append("note");
        appendId(out, note.id);
        appendChange(out, note.change);
        appendPosition(out, note.position);
        appendWidth(out, note.width);
        appendStyle(out, note.style);
        out.append(' ').append(quoted(note.text));
        appendTextChange(out, note.textChange);
        out.append('\n');
    }

    // ---- anchor lines and association class links ----

    private void appendAttachLine(StringBuilder out, String keyword, AttachLineModel_V001 line) {
        out.append(INDENT).append(keyword);
        appendId(out, line.id);
        appendChange(out, line.change);
        out.append(' ').append(line.startId).append(" -- ").append(line.targetId);
        if (line.start != null || !line.waypoints.isEmpty() || line.end != null) {
            out.append(" route");
            if (line.start != null) {
                out.append(' ').append(anchor(line.start));
            }
            appendWaypoints(out, line.waypoints);
            if (line.end != null) {
                out.append(' ').append(anchor(line.end));
            }
        }
        out.append('\n');
    }

    // ---- small parts ----

    private void appendId(StringBuilder out, String id) {
        if (id != null) {
            out.append(" (").append(id).append(')');
        }
    }

    private void appendTextChange(StringBuilder out, TextChangeModel_V001 change) {
        if (change != null) {
            out.append(" was(").append(word(change.changeSet));
            if (change.backup != null) {
                out.append(", ").append(backupText(change.backup));
            }
            out.append(')');
        }
    }

    /** Bare if it is a multiplicity or a simple word, else in quotes. */
    private String backupText(String backup) {
        return Multiplicity_V001.isBare(backup) ? backup : word(backup);
    }

    private void appendChange(StringBuilder out, ChangeModel_V001 change) {
        if (change != null) {
            out.append(" change(").append(change.type.keyword()).append(", ").append(word(change.changeSet)).append(')');
        }
    }

    private void appendPosition(StringBuilder out, PositionModel_V001 position) {
        if (position != null) {
            out.append(" at(").append(position.x).append(", ").append(position.y).append(')');
        }
    }

    private void appendWidth(StringBuilder out, Integer width) {
        if (width != null) {
            out.append(" width=").append(width);
        }
    }

    private void appendStyle(StringBuilder out, ElementStyleModel_V001 style) {
        List<String> entries = new ArrayList<>();
        if (style.fill != null) {
            entries.add("fill=" + quoted(style.fill));
        }
        if (style.textColor != null) {
            entries.add("textcolor=" + quoted(style.textColor));
        }
        if (style.lineColor != null) {
            entries.add("linecolor=" + quoted(style.lineColor));
        }
        if (style.font != null) {
            entries.add("font=" + style.font.sourceName);
        }
        if (style.fontSize != null) {
            entries.add("fontsize=" + style.fontSize);
        }
        if (!entries.isEmpty()) {
            out.append(" style(").append(String.join(", ", entries)).append(')');
        }
    }

    /** A class or diagram name: quoted if it has special characters or is a keyword. */
    private String name(String name) {
        return NAME.matcher(name).matches() && !keywords.contains(name) ? name : quoted(name);
    }

    /** An attribute name or type, tagged value key or value: keywords are fine here. */
    private String word(String word) {
        return NAME.matcher(word).matches() ? word : quoted(word);
    }

    /**
     * A string in double quotes with the escapes the grammar knows. A backtick is written as an escape too,
     * because a diagram source in a Specman model file must not contain one.
     */
    private String quoted(String text) {
        StringBuilder quoted = new StringBuilder("\"");
        for (char c : text.replace("\r\n", "\n").toCharArray()) {
            switch (c) {
                case '"':
                    quoted.append("\\\"");
                    break;
                case '\\':
                    quoted.append("\\\\");
                    break;
                case '\n':
                case '\r':
                    quoted.append("\\n");
                    break;
                case '`':
                    quoted.append("\\u0060");
                    break;
                default:
                    quoted.append(c);
            }
        }
        return quoted.append('"').toString();
    }

    /** True if the info reads back as itself when written without quotes. */
    private boolean isBareInfo(String info) {
        if (info.isBlank() || !info.equals(info.strip())) {
            return false;
        }
        ClassDiagramSyntaxErrorCollector errors = new ClassDiagramSyntaxErrorCollector();
        ClassDiagram_V001Lexer lexer = new ClassDiagram_V001Lexer(CharStreams.fromString(info));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errors);
        StringBuilder tokenTexts = new StringBuilder();
        for (Token token = lexer.nextToken(); token.getType() != Token.EOF; token = lexer.nextToken()) {
            if (!isInfoToken(token.getType())) {
                return false;
            }
            tokenTexts.append(token.getText());
        }
        return !errors.hasError() && withoutSpaces(tokenTexts.toString()).equals(withoutSpaces(info));
    }

    private boolean isInfoToken(int type) {
        return type == ClassDiagram_V001Lexer.NAME || type == ClassDiagram_V001Lexer.HEX_ID || type == ClassDiagram_V001Lexer.NUMBER
            || type == ClassDiagram_V001Lexer.DOTDOT || type == ClassDiagram_V001Lexer.STAR
            || type == ClassDiagram_V001Lexer.EQUALS || type == ClassDiagram_V001Lexer.QUOTED_STRING
            || keywordTypes.contains(type);
    }

    private String withoutSpaces(String text) {
        return text.replaceAll("\\s", "");
    }
}
