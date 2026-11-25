package classman.model.v001;

import java.util.EnumSet;
import java.util.Set;

/** The parts of an {@link ElementStyleModel_V001}. Which of them an element has depends on what it is: a relation has
 * no background. */
public enum StyleProperty_V001 {
    FILL,
    TEXT_COLOR,
    LINE_COLOR,
    FONT,
    FONT_SIZE;

    /** What classes and notes have. */
    public static final Set<StyleProperty_V001> ALL = EnumSet.allOf(StyleProperty_V001.class);
    /** What relations have. */
    public static final Set<StyleProperty_V001> WITHOUT_FILL = EnumSet.complementOf(EnumSet.of(FILL));
}
