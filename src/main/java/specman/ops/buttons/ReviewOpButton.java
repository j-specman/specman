package specman.ops.buttons;

import specman.*;

public class ReviewOpButton extends AbstractSpecmanOpButton {

  public ReviewOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    zusammenklappenFuerReview();
  }

}
