package classman.model.v001;

import java.util.regex.Pattern;

/** The text of a stereotype which refers to a configuration of the host instead of being a text of its own:
 * {@code :name}, written {@code <<:name>>} in the source. The host knows what the configuration looks like (label,
 * icon, colors), so the diagram doesn't carry any of it. A free text never begins with the prefix, so the two can't be
 * mixed up, not even in the old text of a change mark. The name is what a configuration of the host is called:
 * letters A-Z and a-z, digits and underscores. */
public final class StereotypeReference_V001 {
    public static final String PREFIX = ":";
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]+");

    private StereotypeReference_V001() {
    }

    /** @return true if the text of a stereotype refers to a configuration */
    public static boolean isReference(String stereotype) {
        return stereotype != null && stereotype.startsWith(PREFIX);
    }

    /** @return true if the text can be given to a stereotype which is a free text: one which doesn't begin like a
     * reference */
    public static boolean isFreeText(String stereotype) {
        return !isReference(stereotype);
    }

    /** @return true if a configuration of the host can have the name */
    public static boolean isValidName(String name) {
        return NAME.matcher(name).matches();
    }

    /** @return the text of a stereotype which refers to the configuration */
    public static String textOf(String name) {
        return PREFIX + name;
    }

    /** @param stereotype the text of a stereotype which {@link #isReference(String) is} a reference
     * @return the name of the configuration, which may not be valid in a source which was written by hand */
    public static String nameOf(String stereotype) {
        return stereotype.substring(PREFIX.length());
    }
}
