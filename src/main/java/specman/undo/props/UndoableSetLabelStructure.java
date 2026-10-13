package specman.undo.props;

import specman.metatag.stepnumberlabel.StepnumberLabel;
import specman.metatag.stepnumberlabel.StepnumberLabel.LabelStructure;

public class UndoableSetLabelStructure extends UndoableSetProperty<LabelStructure> {

  public UndoableSetLabelStructure(StepnumberLabel label, LabelStructure structure) {
    super(structure, label::setStructure, label::getStructure);
  }
}
