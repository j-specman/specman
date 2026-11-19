package classman.render;

import static classman.render.DiagramStyle.PALE_SHARE;
import static classman.render.DiagramStyle.UNKNOWN_CHANGE_COLOR;
import static classman.render.DiagramStyle.WHITE;

/** The colors of the change marks: the strong color of a change set, and its pale variant. */
public class ChangePalette {
    private final ChangeColors colors;

    public ChangePalette(ChangeColors colors) {
        this.colors = colors;
    }

    public String strong(String changeSet) {
        String color = colors.strongColorOf(changeSet);
        return color == null ? UNKNOWN_CHANGE_COLOR : color;
    }

    public String pale(String changeSet) {
        return ColorMix.blend(strong(changeSet), WHITE, PALE_SHARE);
    }
}
