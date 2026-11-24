package classman.model.v001.change;

import classman.model.v001.MethodModel_V001;
import classman.model.v001.Visibility_V001;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The line of a method as the user types it, like the diagram shows it: +name(name: Type, Type) : type [info, info].
 * The name is what stands before the first parenthesis, colon or bracket, so it can't contain these. A line without
 * parentheses is a method without parameters, the parentheses are added when it is shown: <code>run</code> is
 * <code>run()</code>. The parameters are separated by commas, a comma inside brackets, as in a generic type, doesn't
 * count, and a parameter with a comma or a quote in it elsewhere is put in double quotes. A parameter is shown as
 * typed, only the blanks around its colon are made one behind the colon and none before it. The return type is what
 * follows the colon behind the parentheses, and the infos are in brackets at the end.
 */
public class MethodLineFormat_V001 {
    private static final String VISIBILITY_SIGNS = "+-#~";

    /** @return the line which the method is shown as, and edited in */
    public String textOf(MethodModel_V001 method) {
        StringBuilder line = new StringBuilder();
        if (method.visibility != null) {
            line.append(method.visibility.sign);
        }
        line.append(method.name).append('(').append(EntryListFormat_V001.parametersText(method.parameters)).append(')');
        if (method.type != null && !method.type.isEmpty()) {
            line.append(" : ").append(method.type);
        }
        return line.append(EntryListFormat_V001.infosText(method.infos)).toString();
    }

    /** @return the parts of the line, none if it has no name, parentheses or brackets which aren't closed, something
     * between the parentheses and the type, or line breaks */
    public Optional<MethodLine_V001> parse(String text) {
        String rest = text.strip();
        if (rest.isEmpty() || rest.contains("\n") || rest.equals("...")) {
            return Optional.empty();
        }
        Visibility_V001 visibility = null;
        if (VISIBILITY_SIGNS.indexOf(rest.charAt(0)) >= 0) {
            visibility = Visibility_V001.fromSign(rest.charAt(0));
            rest = rest.substring(1).strip();
        }
        int nameEnd = nameEnd(rest);
        String name = rest.substring(0, nameEnd).strip();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        List<String> parameters = new ArrayList<>();
        String tail = rest.substring(nameEnd);
        if (tail.startsWith("(")) {
            int closing = EntryListFormat_V001.closingParenthesis(tail, 0);
            if (closing < 0) {
                return Optional.empty();
            }
            parameters.addAll(EntryListFormat_V001.splitParameters(tail.substring(1, closing)));
            tail = tail.substring(closing + 1);
        }
        return typeAndInfos(visibility, name, parameters, tail.strip());
    }

    /** The name ends at the first parenthesis, colon or brackets of the infos. */
    private int nameEnd(String rest) {
        int end = rest.length();
        for (String delimiter : new String[] {"(", ":", EntryListFormat_V001.INFOS_START}) {
            int index = rest.indexOf(delimiter);
            if (index >= 0) {
                end = Math.min(end, index);
            }
        }
        return end;
    }

    /** @param tail what follows the parentheses: an optional colon with the return type, and the optional infos */
    private Optional<MethodLine_V001> typeAndInfos(Visibility_V001 visibility, String name, List<String> parameters,
                                                   String tail) {
        List<String> infos = new ArrayList<>();
        String typePart = tail;
        int infosStart = tail.startsWith("[") ? 0 : tail.indexOf(EntryListFormat_V001.INFOS_START);
        if (infosStart >= 0) {
            if (!tail.endsWith("]")) {
                return Optional.empty();
            }
            infos = EntryListFormat_V001.splitInfos(tail.substring(tail.indexOf('[', infosStart) + 1, tail.length() - 1));
            typePart = tail.substring(0, infosStart).strip();
        }
        String returnType = null;
        if (typePart.startsWith(":")) {
            returnType = typePart.substring(1).strip();
        }
        else if (!typePart.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MethodLine_V001(visibility, name, parameters,
            returnType == null || returnType.isEmpty() ? null : returnType, infos));
    }
}
