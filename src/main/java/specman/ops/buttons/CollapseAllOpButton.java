package specman.ops.buttons;

import specman.*;

public class CollapseAllOpButton extends AbstractSpecmanOpButton {

  public CollapseAllOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    alleSchritteKlappen(true);
  }

}
