package classman.model.v001;

import java.util.regex.Pattern;

/** The multiplicity of a relation end is free text. Only a number, a range like 0..1 or 1..*, or * can stand in the
 * diagram text without quotes. */
public final class Multiplicity_V001 {
    private static final Pattern BARE = Pattern.compile("-?[0-9]+(?:\\.\\.(?:-?[0-9]+|\\*))?|\\*");

    private Multiplicity_V001() {
    }

    /** @return true if the text can be written without quotes: a number, a range or a star */
    public static boolean isBare(String text) {
        return BARE.matcher(text).matches();
    }
}
