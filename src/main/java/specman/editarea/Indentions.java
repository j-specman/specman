package specman.editarea;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import org.jetbrains.annotations.Nullable;
import specman.Specman;
import specman.view.RoundedBorderDecorationStyle;

import static specman.view.RoundedBorderDecorationStyle.None;
import static specman.Specman.editor;

public class Indentions {
    public static final int JEDITORPANE_DEFAULT_BORDER_THICKNESS = 3;
    private static final int LEFTRIGHT_INSET_FOR_DECORATION = 10;
    // Originally sized to avoid colliding with RoundedBorderDecorator's antialiased border line
    // during text-field focus repaints, for both top and bottom edges - confirmed no longer
    // needed for the top edge now that EditContainer's meta-strip row always provides generous
    // clearance there (see topInset()/topBorder()). Still used for the bottom edge, which has no
    // such strip: a bit of breathing room below the content in "abgesetzte" (rounded-border) steps.
    private static final int BOTTOM_INSET_FOR_DECORATION = 1;

    final boolean top, left, bottom, right;
    final int individualLeft;

    public Indentions() { this(None); }

    public Indentions(RoundedBorderDecorationStyle style) {
        top = left = bottom = right = (style != None);
        this.individualLeft = 0;
    }

    public Indentions(boolean top, boolean left, boolean bottom, boolean right, int individualLeft) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        this.individualLeft = individualLeft;
    }

    public Indentions(boolean top, boolean left, boolean bottom, boolean right) {
        this(top, left, bottom, right, 0);
    }

    public Indentions(int individualLeft) {
        this(false, false, false, false, individualLeft);
    }

    private RowSpec toRowSpec(boolean indent) {
        return RowSpec.decode(pxFor(indent) + "px");
    }

    private int pxFor(boolean indent) {
        return indent ? (BOTTOM_INSET_FOR_DECORATION * zoomPercent() / 100) : 0;
    }

    private ColumnSpec toColumnSpec(boolean indent, int additional) {
        int px = indent ? ((LEFTRIGHT_INSET_FOR_DECORATION + additional) * zoomPercent() / 100) : 0;
        return ColumnSpec.decode(px + "px");
    }

    public RowSpec topInset() {
        // Was toRowSpec(top) - reserved breathing room above the content in "abgesetzte"
        // (rounded-border) steps. Always 0 now: that role moved entirely to EditContainer's
        // meta-strip row, which exists precisely when a step number is present - and "abgesetzt"
        // only ever applies to steps, which always have one. top is still used for other things
        // (see withTop() call sites for branches/substeps not at the top of their compound step).
        return toRowSpec(false);
    }
    public RowSpec bottomInset() { return toRowSpec(bottom); }
    public ColumnSpec leftInset() { return toColumnSpec(left, individualLeft); }
    public ColumnSpec rightInset() { return toColumnSpec(right, 0); }

    public int leftBorder() { return left ? individualLeft : JEDITORPANE_DEFAULT_BORDER_THICKNESS + individualLeft; }
    public int rightBorder() { return right ? 0 : JEDITORPANE_DEFAULT_BORDER_THICKNESS; }
    public int bottomBorder() {
        return JEDITORPANE_DEFAULT_BORDER_THICKNESS - (bottom ? BOTTOM_INSET_FOR_DECORATION : 0);
    }

    /** No compensation required at the top even for "abgesetzte" steps, because at the top of steps there is
     * always the meta-strip row of the EditContainer, which provides the necessary breathing room. */
    public int topBorder() { return JEDITORPANE_DEFAULT_BORDER_THICKNESS; }
    public boolean hasTopIndention() { return top; }

    public Indentions withTop(boolean top) { return new Indentions(top, left, bottom, right); }
    public Indentions withLeft(boolean left) { return new Indentions(top, left, bottom, right); }
    public Indentions withBottom(boolean bottom) { return new Indentions(top, left, bottom, right); }
    public Indentions withRight(boolean right) { return new Indentions(top, left, bottom, right); }
    public Indentions withIndividuals(@Nullable Indentions from) {
        return (from != null && individualLeft == 0)
          ? new Indentions(top, left, bottom, right, from.individualLeft)
          : this;
    }

    private int zoomPercent() { return editor().getZoomFactor(); }
}
