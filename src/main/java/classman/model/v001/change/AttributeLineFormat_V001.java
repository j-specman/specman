package classman.model.v001.change;

import classman.model.v001.AttributeModel_V001;
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
        return line.append(EntryListFormat_V001.infosText(attribute.infos)).toString();
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
        int infosStart = rest.indexOf(EntryListFormat_V001.INFOS_START);
        if (infosStart >= 0) {
            if (!rest.endsWith("]")) {
                return Optional.empty();
            }
            infos = EntryListFormat_V001.splitInfos(
                rest.substring(infosStart + EntryListFormat_V001.INFOS_START.length(), rest.length() - 1));
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
}
