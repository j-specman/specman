package specman.ops.buttons;

import specman.*;

public class ExpandAllOpButton extends AbstractSpecmanOpButton {

  public ExpandAllOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    alleSchritteKlappen(false);
  }

}
