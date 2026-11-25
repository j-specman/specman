package classman.model.v001.change;

import classman.model.v001.TaggedValueModel_V001;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A tagged value as the user types it: KEY=value or only KEY, without the braces which the diagram draws around it.
 * The key is what stands before the first equals sign, written like a name of the diagram format without quotes. The
 * value is everything behind it and may contain anything but a line break, also blanks and equals signs. It may be
 * missing, but not if there is an equals sign: KEY= is no tagged value.
 */
public class TaggedValueFormat_V001 {
    private static final Pattern KEY = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]*(?:[-/][\\p{L}\\p{N}_]+)*");

    /** @return the text which shows the tagged value, key=value */
    public String textOf(TaggedValueModel_V001 tag) {
        return tag.value == null || tag.value.isEmpty() ? tag.key : tag.key + "=" + tag.value;
    }

    /** @return the key and value of the text, the value null if there is none, empty if it is no tagged value: no key,
     * an unfit key, an equals sign without a value or a line break */
    public Optional<TaggedValueLine_V001> parse(String text) {
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            return Optional.empty();
        }
        int equals = text.indexOf('=');
        String key = (equals < 0 ? text : text.substring(0, equals)).strip();
        String value = equals < 0 ? null : text.substring(equals + 1).strip();
        if (!KEY.matcher(key).matches() || (value != null && value.isEmpty())) {
            return Optional.empty();
        }
        return Optional.of(new TaggedValueLine_V001(key, value));
    }
}
