package classman.model.v001.io;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassDiagram_V001Lexer;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RouteModel_V001;
import classman.model.v001.TaggedValueModel_V001;
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
            appendClass(out, INDENT, "class", classModel);
        } else if (element instanceof AbstractRelationModel_V001 relation) {
            appendRelation(out, relation);
        } else if (element instanceof NoteModel_V001 note) {
            appendNote(out, note);
        } else {
            throw new IllegalArgumentException("Unknown diagram element: " + element.getClass().getName());
        }
    }

    // ---- classes ----

    private void appendClass(StringBuilder out, String indent, String keyword, ClassModel_V001 classModel) {
        out.append(indent).append(keyword).append(' ').append(name(classModel.name));
        appendId(out, classModel.id);
        appendPosition(out, classModel.position);
        appendWidth(out, classModel.width);
        if (classModel.stereotype != null) {
            out.append(" <<").append(classModel.stereotype).append(">>");
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
        List<String> pairs = new ArrayList<>();
        for (TaggedValueModel_V001 tag : taggedValues) {
            String value = INTEGER.matcher(tag.value).matches() ? tag.value : word(tag.value);
            pairs.add(word(tag.key) + "=" + value);
        }
        out.append(" {").append(String.join(", ", pairs)).append('}');
    }

    private String attribute(AttributeModel_V001 attribute) {
        if (attribute.hiddenAttributesMarker) {
            return "...";
        }
        StringBuilder line = new StringBuilder();
        if (attribute.visibility != null) {
            line.append(attribute.visibility.sign);
        }
        line.append(word(attribute.name));
        if (attribute.type != null) {
            line.append(" : ").append(word(attribute.type));
        }
        if (!attribute.infos.isEmpty()) {
            List<String> infos = new ArrayList<>();
            for (String info : attribute.infos) {
                infos.add(isBareInfo(info) ? info : quoted(info));
            }
            line.append(" [").append(String.join(", ", infos)).append(']');
        }
        return line.toString();
    }

    // ---- relations ----

    private void appendRelation(StringBuilder out, AbstractRelationModel_V001 relation) {
        out.append(INDENT).append(relation.type.name().toLowerCase(Locale.ROOT));
        appendId(out, relation.id);
        out.append(' ').append(name(relation.from));
        if (relation instanceof AssociationRelationModel_V001 association) {
            appendMultiplicity(out, association.fromMultiplicity);
            out.append(association.directed ? " ->" : " --");
            appendMultiplicity(out, association.toMultiplicity);
        } else {
            out.append(" ->");
        }
        out.append(' ').append(name(relation.to));
        if (relation.label != null) {
            out.append(' ').append(quoted(escapeLineBreaks(relation.label)));
        }
        out.append('\n');
        if (relation.route != null) {
            appendRoute(out, relation.route);
        }
        if (relation instanceof AssociationRelationModel_V001 association && association.assocClass != null) {
            appendClass(out, INDENT + INDENT, "assocclass", association.assocClass);
        }
    }

    private void appendMultiplicity(StringBuilder out, String multiplicity) {
        if (multiplicity != null) {
            out.append(' ').append(multiplicity);
        }
    }

    private void appendRoute(StringBuilder out, RouteModel_V001 route) {
        out.append(INDENT).append(INDENT).append("route ").append(anchor(route.start));
        for (PositionModel_V001 waypoint : route.waypoints) {
            out.append(" (").append(waypoint.x).append(", ").append(waypoint.y).append(')');
        }
        out.append(' ').append(anchor(route.end)).append('\n');
    }

    private String anchor(AnchorModel_V001 anchor) {
        return name(anchor.className) + "." + anchor.side.name().toLowerCase(Locale.ROOT) + "@" + anchor.offset;
    }

    // ---- notes ----

    private void appendNote(StringBuilder out, NoteModel_V001 note) {
        out.append(INDENT).append("note");
        appendId(out, note.id);
        appendPosition(out, note.position);
        appendWidth(out, note.width);
        out.append(' ').append(quoted(escapeLineBreaks(note.text)));
        if (note.attachedTo != null) {
            out.append(" attached ").append(name(note.attachedTo));
        }
        out.append('\n');
    }

    // ---- small parts ----

    private void appendId(StringBuilder out, String id) {
        if (id != null) {
            out.append(" (").append(id).append(')');
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

    /** A class or diagram name: quoted if it has special characters or is a keyword. */
    private String name(String name) {
        return NAME.matcher(name).matches() && !keywords.contains(name) ? name : quoted(name);
    }

    /** An attribute name or type, tagged value key or value: keywords are fine here. */
    private String word(String word) {
        return NAME.matcher(word).matches() ? word : quoted(word);
    }

    /** Backticks can't be escaped, so a backtick in the text becomes an apostrophe. */
    private String quoted(String text) {
        return "`" + text.replace('`', '\'') + "`";
    }

    private String escapeLineBreaks(String text) {
        return text.replace("\r\n", "\n").replace("\n", "\\n");
    }

    /** True if the info reads back as itself when written without backticks. */
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
        return type == ClassDiagram_V001Lexer.NAME || type == ClassDiagram_V001Lexer.NUMBER
            || type == ClassDiagram_V001Lexer.DOTDOT || type == ClassDiagram_V001Lexer.STAR
            || type == ClassDiagram_V001Lexer.EQUALS || type == ClassDiagram_V001Lexer.BACKTICK_STRING
            || keywordTypes.contains(type);
    }

    private String withoutSpaces(String text) {
        return text.replaceAll("\\s", "");
    }
}
