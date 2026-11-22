package classman.model.v001.change;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.Visibility_V001;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The line of an attribute as the user types it, like the diagram shows it: +name : type [info, info]. The parts are
 * recognized without quotes: the name is what stands before the first colon, the type what follows it up to the
 * brackets, and the infos are separated by commas. An info with a comma or a quote in it is put in double quotes.
 */
public class AttributeLineFormat_V001 {
    private static final String VISIBILITY_SIGNS = "+-#~";
    private static final String INFOS_START = " [";

    /** @return the line which the attribute is shown as, and edited in */
    public String textOf(AttributeModel_V001 attribute) {
        StringBuilder line = new StringBuilder();
        if (attribute.visibility != null) {
            line.append(attribute.visibility.sign);
        }
        line.append(attribute.name);
        if (attribute.type != null && !attribute.type.isEmpty()) {
            line.append(" : ").append(attribute.type);
        }
        List<String> infos = shownInfos(attribute);
        if (!infos.isEmpty()) {
            line.append(INFOS_START).append(String.join(", ", infos)).append(']');
        }
        return line.toString();
    }

    private List<String> shownInfos(AttributeModel_V001 attribute) {
        List<String> infos = new ArrayList<>();
        for (InfoModel_V001 info : AttributeInfosChanger_V001.currentInfos(attribute)) {
            infos.add(needsQuotes(info.text) ? quoted(info.text) : info.text);
        }
        return infos;
    }

    private boolean needsQuotes(String info) {
        return info.contains(",") || info.contains("\"") || info.contains("]") || info.contains("\\");
    }

    private String quoted(String info) {
        return '"' + info.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    /** @return the parts of the line, none if it has no name, brackets which aren't closed, or line breaks */
    public Optional<AttributeLine_V001> parse(String text) {
        return parse(text, true);
    }

    /** @param hasType false for the value of an enum, which has no type: a colon makes the line invalid
     * @return the parts of the line, none if it has no name, brackets which aren't closed, or line breaks */
    public Optional<AttributeLine_V001> parse(String text, boolean hasType) {
        String rest = text.strip();
        if (rest.isEmpty() || rest.contains("\n") || rest.equals("...")) {
            return Optional.empty();
        }
        Visibility_V001 visibility = null;
        if (VISIBILITY_SIGNS.indexOf(rest.charAt(0)) >= 0) {
            visibility = Visibility_V001.fromSign(rest.charAt(0));
            rest = rest.substring(1).strip();
        }
        List<String> infos = new ArrayList<>();
        int infosStart = rest.indexOf(INFOS_START);
        if (infosStart >= 0) {
            if (!rest.endsWith("]")) {
                return Optional.empty();
            }
            infos = infosOf(rest.substring(infosStart + INFOS_START.length(), rest.length() - 1));
            rest = rest.substring(0, infosStart);
        }
        int colon = rest.indexOf(':');
        if (colon >= 0 && !hasType) {
            return Optional.empty();
        }
        String name = (colon < 0 ? rest : rest.substring(0, colon)).strip();
        String type = colon < 0 ? "" : rest.substring(colon + 1).strip();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AttributeLine_V001(visibility, name, type.isEmpty() ? null : type, infos));
    }

    /** The items between the commas which aren't inside double quotes. Empty items are left out. */
    private List<String> infosOf(String infosText) {
        List<String> infos = new ArrayList<>();
        StringBuilder item = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < infosText.length(); i++) {
            char c = infosText.charAt(i);
            if (c == '\\' && inQuotes && i + 1 < infosText.length()) {
                item.append(infosText.charAt(++i));
            }
            else if (c == '"') {
                inQuotes = !inQuotes;
            }
            else if (c == ',' && !inQuotes) {
                addItem(infos, item);
            }
            else {
                item.append(c);
            }
        }
        addItem(infos, item);
        return infos;
    }

    private void addItem(List<String> infos, StringBuilder item) {
        String text = item.toString().strip();
        if (!text.isEmpty()) {
            infos.add(text);
        }
        item.setLength(0);
    }
}
