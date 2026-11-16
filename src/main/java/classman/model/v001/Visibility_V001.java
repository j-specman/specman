package classman.model.v001;

/** The visibility sign in front of an attribute. */
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
}
