package classman.render;

/** The red exclamation mark which tells that something is missing or wrong, the same as elsewhere in Specman: for a
 * stereotype which refers to a configuration the host doesn't know, and for a linked diagram which can't be shown.
 * It is read once, on first use. */
final class ProblemIcon {
    private static final String RESOURCE = "images/svg/material/priority_high.svg";
    private static VectorImage icon;

    private ProblemIcon() {
    }

    static synchronized VectorImage get() {
        if (icon == null) {
            icon = VectorImage.ofResource(RESOURCE);
        }
        return icon;
    }
}
