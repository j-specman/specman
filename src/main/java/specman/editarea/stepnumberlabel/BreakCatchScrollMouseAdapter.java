package specman.editarea.stepnumberlabel;

import specman.Specman;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CatchBereich;
import static specman.Specman.editor;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class BreakCatchScrollMouseAdapter extends MouseAdapter {
  public static final String SCROLL_TOOLTIP = "STRG+Klicken um Link zu folgen";
  public static final Cursor SCROLL_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

  static BreakCatchScrollMouseAdapter instance = new BreakCatchScrollMouseAdapter();

  public static boolean userWantsToScroll(MouseEvent e) {
    return e.isControlDown() && e.getSource() instanceof StepnumberLabel;
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    if (userWantsToScroll(e) && stepRefersToOtherStep(e)) {
      setTooltipAndCursor(e, SCROLL_TOOLTIP, SCROLL_CURSOR);
    }
  }

  private boolean stepRefersToOtherStep(MouseEvent e) {
    return editor().findStep(source(e)).refersToOtherStep();
  }

  @Override
  public void mouseExited(MouseEvent e) {
    setTooltipAndCursor(e, null, Cursor.getDefaultCursor());
  }

  private StepnumberLabel source(MouseEvent e) {
    return (StepnumberLabel) e.getSource();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (userWantsToScroll(e)) {
      StepnumberLabel stepnumberLabel = source(e);
      AbstractSchrittView step = editor().findStep(stepnumberLabel);
      if (step instanceof BreakSchrittView) {
        ((BreakSchrittView) step).scrollToCatch();
      }
      else if (step instanceof CatchBereich) {
        ((CatchBereich) step).scrollToBreak(stepnumberLabel);
      }
    }
  }

  private void setTooltipAndCursor(MouseEvent e, String tooltip, Cursor cursor) {
    StepnumberLabel stepnumberLabel = source(e);
    if (!Objects.equals(stepnumberLabel.getToolTipText(), tooltip)) {
      stepnumberLabel.setToolTipText(tooltip);
      editor().setCursor(cursor);
    }
  }

}
