package specman.editarea.focusmover;

import specman.editarea.ImageEditArea;

public class CrossEditAreaFocusMoverFromImage extends AbstractCrossEditAreaFocusMover<ImageEditArea> {

  public CrossEditAreaFocusMoverFromImage(ImageEditArea currentFocusArea) {
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
