package specman.editarea.focusmover;

import specman.editarea.image.AbstractImageEditArea;

public class CrossEditAreaFocusMoverFromImage extends AbstractCrossEditAreaFocusMover<AbstractImageEditArea> {

  public CrossEditAreaFocusMoverFromImage(AbstractImageEditArea currentFocusArea) {
    super(currentFocusArea);
  }

  @Override
  protected boolean caretAtBottom() {
    return true;
  }

  @Override
  protected boolean caretAtTop() {
    return true;
  }

  @Override
  protected int desiredLocalX() {
    return currentFocusArea.getNavigationX();
  }
}
