package classman.model.v001;

/** The visibility sign in front of an attribute or method. */
public enum Visibility_V001 {
    PUBLIC('+'),
    PRIVATE('-'),
    PROTECTED('#'),
    PACKAGE('~');

    public final char sign;

    Visibility_V001(char sign) {
        this.sign = sign;
    }

    public static Visibility_V001 fromSign(char sign) {
        for (Visibility_V001 visibility : values()) {
            if (visibility.sign == sign) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("No visibility sign: " + sign);
    }

    /** @return the visibility with the sign, none for no text or a text which isn't a sign */
    public static Visibility_V001 ofSign(String text) {
        if (text == null || text.length() != 1) {
            return null;
        }
        for (Visibility_V001 visibility : values()) {
            if (visibility.sign == text.charAt(0)) {
                return visibility;
            }
        }
        return null;
    }
}
