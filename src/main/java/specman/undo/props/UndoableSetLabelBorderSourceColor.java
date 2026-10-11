package specman.undo.props;

import specman.metatag.stepnumberlabel.StepnumberLabel;

import java.awt.Color;

public class UndoableSetLabelBorderSourceColor extends UndoableSetProperty<Color> {

  public UndoableSetLabelBorderSourceColor(StepnumberLabel label, Color undoColor) {
    super(undoColor, label::setBorderSourceColor, label::getBorderSourceColor);
  }
}
