package specman;

class ReviewOpButton extends AbstractSpecmanOpButton {

  ReviewOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    specman.hauptSequenz.zusammenklappenFuerReview();
  }

}
