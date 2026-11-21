package classman.model.v001;

import java.util.regex.Pattern;

/** The multiplicity of a relation end as the diagram text can hold it: a number, a range like 0..1 or 1..*, or *. */
public final class Multiplicity_V001 {
    private static final Pattern PATTERN = Pattern.compile("-?[0-9]+(?:\\.\\.(?:-?[0-9]+|\\*))?|\\*");

    private Multiplicity_V001() {
    }

    public static boolean isValid(String text) {
        return PATTERN.matcher(text).matches();
    }
}
