package classman.model.v001.change;

import classman.model.v001.InfoModel_V001;

import java.util.ArrayList;
import java.util.List;

/**
 * The comma separated lists in the line of an attribute or method as the user types them: the infos in brackets, and
 * the parameters in parentheses. An entry which would be taken apart or cut off is put in double quotes, which hide
 * commas and brackets. The parameters of a method may have commas in generic types: the commas inside of brackets
 * (<code>Map&lt;String, int&gt;</code>) don't separate parameters.
 */
final class EntryListFormat_V001 {
    static final String INFOS_START = " [";
    private static final String OPENING = "(<[";
    private static final String CLOSING = ")>]";

    private EntryListFormat_V001() {
    }

    /** @return the infos as the line shows them, between the brackets, or the empty text if there are none */
    static String infosText(List<InfoModel_V001> infos) {
        List<String> shown = new ArrayList<>();
        for (InfoModel_V001 info : InfoListChanger_V001.currentEntries(infos)) {
            shown.add(needsInfoQuotes(info.text) ? quoted(info.text) : info.text);
        }
        return shown.isEmpty() ? "" : INFOS_START + String.join(", ", shown) + "]";
    }

    /** @return the parameters as the line shows them, joined by commas, between the parentheses */
    static String parametersText(List<InfoModel_V001> parameters) {
        List<String> shown = new ArrayList<>();
        for (InfoModel_V001 parameter : InfoListChanger_V001.currentEntries(parameters)) {
            shown.add(needsParameterQuotes(parameter.text) ? quoted(parameter.text) : parameter.text);
        }
        return String.join(", ", shown);
    }

    private static boolean needsInfoQuotes(String info) {
        return info.contains(",") || info.contains("\"") || info.contains("]") || info.contains("\\");
    }

    private static boolean needsParameterQuotes(String parameter) {
        return parameter.contains("\"") || parameter.contains("\\") || !parameter.equals(parameter.strip())
            || !isBalanced(parameter) || !split(parameter, true, false).equals(List.of(parameter));
    }

    private static boolean isBalanced(String text) {
        int depth = 0;
        for (char c : text.toCharArray()) {
            if (OPENING.indexOf(c) >= 0) {
                depth++;
            }
            else if (CLOSING.indexOf(c) >= 0 && --depth < 0) {
                return false;
            }
        }
        return depth == 0;
    }

    private static String quoted(String text) {
        return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    /** The infos between the brackets: the items between the commas which aren't inside double quotes. Empty items are
     * left out. */
    static List<String> splitInfos(String infosText) {
        return split(infosText, false, false);
    }

    /** The parameters between the parentheses: the items between the commas which aren't inside double quotes or
     * brackets. Empty items are left out. */
    static List<String> splitParameters(String parametersText) {
        return split(parametersText, true, true);
    }

    /** @param nested true if commas inside brackets don't separate
     * @param normalizeColons true to normalize the blanks around the colon of an item which isn't quoted */
    private static List<String> split(String text, boolean nested, boolean normalizeColons) {
        List<String> items = new ArrayList<>();
        StringBuilder item = new StringBuilder();
        boolean inQuotes = false;
        boolean quoted = false;
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && inQuotes && i + 1 < text.length()) {
                item.append(text.charAt(++i));
            }
            else if (c == '"') {
                inQuotes = !inQuotes;
                quoted = true;
            }
            else if (c == ',' && !inQuotes && depth == 0) {
                addItem(items, item, normalizeColons && !quoted);
                quoted = false;
            }
            else {
                if (nested && !inQuotes) {
                    depth = nesting(c, depth);
                }
                item.append(c);
            }
        }
        addItem(items, item, normalizeColons && !quoted);
        return items;
    }

    private static int nesting(char c, int depth) {
        if (OPENING.indexOf(c) >= 0) {
            return depth + 1;
        }
        return CLOSING.indexOf(c) >= 0 ? Math.max(0, depth - 1) : depth;
    }

    private static void addItem(List<String> items, StringBuilder item, boolean normalizeColon) {
        String text = item.toString().strip();
        if (!text.isEmpty()) {
            items.add(normalizeColon ? normalizedParameter(text) : text);
        }
        item.setLength(0);
    }

    /** @return the index of the parenthesis which closes the one at the given index, outside of double quotes, -1 if
     * there is none */
    static int closingParenthesis(String text, int opening) {
        boolean inQuotes = false;
        int depth = 0;
        for (int i = opening; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && inQuotes) {
                i++;
            }
            else if (c == '"') {
                inQuotes = !inQuotes;
            }
            else if (!inQuotes && c == '(') {
                depth++;
            }
            else if (!inQuotes && c == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    /** @return the parameter with one blank behind the colon between name and type and none before it */
    static String normalizedParameter(String parameter) {
        int colon = parameter.indexOf(':');
        if (colon < 0) {
            return parameter;
        }
        String name = parameter.substring(0, colon).strip();
        String type = parameter.substring(colon + 1).strip();
        if (type.isEmpty()) {
            return name;
        }
        return name.isEmpty() ? type : name + ": " + type;
    }
}
