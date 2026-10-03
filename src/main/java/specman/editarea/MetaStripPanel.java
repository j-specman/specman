package specman.editarea;

import javax.swing.*;
import java.awt.*;

/**
 * Transparent overlay panel for the step number, the fold/collapse button, and future meta
 * widgets, placed on top of the first edit area via a FormLayout row span in
 * {@link EditContainer}. Positioning of its children is done externally
 * (EditContainer#updateBounds(), KlappButton#updateLocation()), not by a LayoutManager - hence
 * the null layout and the fixed (0,0) preferred/minimum size, which keeps this panel from
 * freezing the row height of the FormLayout row it spans.
 * <p>
 * {@link #contains(int, int)} is overridden so the panel only ever claims mouse events at points
 * actually covered by one of its children's bounds (visible or not, e.g. KlappButton while it is
 * hidden but already positioned). Everywhere else it is transparent to clicks, even once a child
 * (like KlappButton, for its hover-to-reveal behaviour) registers a listener on this panel itself -
 * which would otherwise make the panel an eligible mouse-event target across its whole bounds.
 */
public class MetaStripPanel extends JPanel {
  // All still in flux for finetuning the "hanging tab" look - at 100% zoom, scaled from there
  // the same way the widgets' own sizes are (see StepnumberLabel/KlappButton paint()/updateLocation()).
  public static final int CORNER_ARC = 8;
  public static final int WIDGET_GAP = 4;
  public static final int RIGHT_MARGIN = 3;
  // Extra width left/right of a widget's own content (text or icon), so the rounded corners
  // have room to live in without biting into the content itself. Not zoom-scaled, matching the
  // existing (pre-meta-strip) border insets on StepnumberLabel, which weren't either.
  public static final int SIDE_PADDING = 5;
  // Border bottom inset that nudges Swing's centered content upward within its natural
  // preferred height (a larger bottom inset than top shifts the centering point up).
  public static final int CONTENT_LIFT = 2;
  // Pixels trimmed off a widget's natural preferred height for the final "stingy" box height -
  // larger than CONTENT_LIFT so the box actually gets shorter, not just internally re-centered.
  public static final int HEIGHT_TRIM = 4;
  // Compensates for RoundedBorderDecorator painting its antialiased border line on top of the
  // step number label in "abgesetzte" (rounded-border) steps, once the label bridges the topInset
  // row to reach the very top (see EditContainer#initLayoutAndEditAreasV2()). Deliberately NOT
  // zoom-scaled: the border's own stroke width (RoundedBorderDecorator#drawInnerBorderLine uses a
  // fixed BasicStroke(2)) does not scale with zoom either, so neither should this correction.
  public static final int BORDER_OVERDRAW_COMPENSATION = 1;

  public MetaStripPanel() {
    setLayout(null);
    setOpaque(false);
  }

  @Override
  public boolean contains(int x, int y) {
    for (Component child : getComponents()) {
      if (child.getBounds().contains(x, y)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(0, 0);
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(0, 0);
  }
}
