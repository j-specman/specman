package specman.editarea;

import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.view.KlappButton;

import javax.swing.*;
import java.awt.*;

import static specman.Specman.editor;

/**
 * Transparent overlay panel for the step number, the fold/collapse button, and future meta
 * widgets, placed on top of the first edit area via a FormLayout row span in
 * {@link EditContainer}. Positioning of its children is done externally
 * (EditContainer#updateBounds(), {@link #repositionKlappButton(Rectangle)}), not by a
 * LayoutManager - hence the null layout and the fixed (0,0) preferred/minimum size, which keeps
 * this panel from freezing the row height of the FormLayout row it spans.
 * <p>
 * {@link #contains(int, int)} is overridden so the panel only ever claims mouse events at points
 * actually covered by one of its children's bounds (visible or not, e.g. KlappButton while it is
 * hidden but already positioned). Everywhere else it is transparent to clicks, even once a child
 * (like KlappButton, for its hover-to-reveal behaviour) registers a listener on this panel itself -
 * which would otherwise make the panel an eligible mouse-event target across its whole bounds.
 * <p>
 * Only holds constants/logic about how widgets are arranged *within* the strip (gap between them,
 * margin to the strip's own right edge) - how an individual widget renders itself (corner arc,
 * border) is {@link specman.graphics.HangingTab}'s concern, not this panel's.
 */
public class MetaStripPanel extends JPanel {
  // All still in flux for finetuning the "hanging tab" look - at 100% zoom, scaled from there
  // the same way the widgets' own sizes are (see StepnumberLabel/KlappButton paint()/updateLocation()).
  public static final int WIDGET_GAP = 4;
  public static final int RIGHT_MARGIN = 3;
  // Pixels trimmed off a widget's natural preferred height for the final "stingy" box height -
  // larger than HangingTab.CONTENT_LIFT so the box actually gets shorter, not just internally
  // re-centered.
  public static final int HEIGHT_TRIM = 4;
  // Compensates for RoundedBorderDecorator painting its antialiased border line on top of the
  // step number label in "abgesetzte" (rounded-border) steps, once the label bridges the topInset
  // row to reach the very top (see EditContainer#initLayoutAndEditAreas()). Deliberately NOT
  // zoom-scaled: the border's own stroke width (RoundedBorderDecorator#drawInnerBorderLine uses a
  // fixed BasicStroke(2)) does not scale with zoom either, so neither should this correction.
  public static final int BORDER_OVERDRAW_COMPENSATION = 1;

  private StepnumberLabel stepNumber;

  public MetaStripPanel() {
    setLayout(null);
    setOpaque(false);
  }

  public void add(StepnumberLabel stepNumber) {
    this.stepNumber = stepNumber;
    super.add(stepNumber);
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

  /** Repositions this strip's KlappButton (if it has one among its children - not every step has
   * a fold/collapse button) relative to the step number's own just-updated bounds. Called from
   * EditContainer#updateBounds() so every caller that resizes a step gets both meta-strip widgets
   * kept in sync for free, instead of each KlappButton-owning view (SchleifenSchrittView,
   * SubsequenzSchrittView, VerzweigungSchrittView) having to separately remember to call
   * klappen.updateLocation(editContainer.getStepNumberBounds()) after every resize. */
  private void repositionKlappButton(Rectangle stepnumberBounds) {
    for (Component child : getComponents()) {
      if (child instanceof KlappButton) {
        ((KlappButton) child).updateLocation(stepnumberBounds);
      }
    }
  }

  public void updateBounds(int maxEditWidth, Indentions indentions, boolean schrittNummerSichtbar) {
    if (stepNumber != null) {
      if (schrittNummerSichtbar) {
        Dimension schrittnummerGroesse = stepNumber.getPreferredSize();
        int rightMargin = (int) editor().scale(MetaStripPanel.RIGHT_MARGIN);
        int heightTrim = (int) editor().scale(MetaStripPanel.HEIGHT_TRIM);
        // schrittNummer already reaches the very top of the step via row 2 alone (see
        // initLayoutAndEditAreas()) - but RoundedBorderDecorator paints its
        // antialiased border line on top of everything inside it, overdrawing a few
        // pixels off the label's top edge in "abgesetzte" (rounded-border) steps.
        // Making the label taller by that same (fixed, not zoom-scaled - see
        // MetaStripPanel.BORDER_OVERDRAW_COMPENSATION) amount keeps its visible
        // (non-overdrawn) height identical to a step without that border style.
        int topInsetBridge = indentions != null && indentions.top ? MetaStripPanel.BORDER_OVERDRAW_COMPENSATION : 0;
        stepNumber.setBounds(maxEditWidth - schrittnummerGroesse.width - rightMargin,
          0,
          schrittnummerGroesse.width,
          schrittnummerGroesse.height - heightTrim + topInsetBridge);
      } else {
        stepNumber.setBounds(0, 0, 0, 0);
      }
      // Keeps a KlappButton living in metaPanel (if any - not every step has one) positioned
      // relative to the step number's bounds just set above, so views owning a KlappButton
      // (SchleifenSchrittView, SubsequenzSchrittView, VerzweigungSchrittView) don't each need to
      // separately call klappen.updateLocation(...) after every resize.
      repositionKlappButton(stepNumber.getBounds());
    }
  }
}
