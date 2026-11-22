package classman.render;

/** The kinds of text which the editor lets the user change in place, and how each is drawn and typed. */
public enum TextKind {
    CLASS_NAME(true, true, false, false, 120, 0),
    /** Typed without the angle brackets, which the diagram draws around it. Empty removes the stereotype. */
    STEREOTYPE(false, true, false, true, 80, 0, "<<", ">>"),
    /** One tagged value, typed as KEY=value without the braces, which the diagram draws around it. Empty deletes it. */
    TAGGED_VALUE(false, true, false, true, 120, 0, "{", "}"),
    NOTE_TEXT(false, false, true, false, 120, 0),
    RELATION_LABEL(false, false, false, true, 80, DiagramStyle.TEXT_HIT_PADDING),
    FROM_MULTIPLICITY(false, false, false, true, 40, DiagramStyle.TEXT_HIT_PADDING),
    TO_MULTIPLICITY(false, false, false, true, 40, DiagramStyle.TEXT_HIT_PADDING),
    /** A whole line of an attribute, like +name : type [info]. The empty text deletes the attribute. */
    ATTRIBUTE_LINE(false, false, true, true, 120, 0);

    public final boolean bold;
    public final boolean centered;
    /** True if the text is wrapped at the width of its box, false if the box grows with the text. */
    public final boolean wrapped;
    /** True if the element can do without the text, so that the empty text removes it. */
    public final boolean optional;
    /** The width in pixels which the field for typing the text has at least. */
    public final int minFieldWidth;
    /** How far around the text a double click still counts as one on the text, in diagram units. */
    public final double hitPadding;
    /** What the diagram draws in front of and behind the text, which the user doesn't type. */
    public final String prefix;
    public final String suffix;

    TextKind(boolean bold, boolean centered, boolean wrapped, boolean optional, int minFieldWidth, double hitPadding) {
        this(bold, centered, wrapped, optional, minFieldWidth, hitPadding, "", "");
    }

    TextKind(boolean bold, boolean centered, boolean wrapped, boolean optional, int minFieldWidth, double hitPadding,
             String prefix, String suffix) {
        this.bold = bold;
        this.centered = centered;
        this.wrapped = wrapped;
        this.optional = optional;
        this.minFieldWidth = minFieldWidth;
        this.hitPadding = hitPadding;
        this.prefix = prefix;
        this.suffix = suffix;
    }
}
