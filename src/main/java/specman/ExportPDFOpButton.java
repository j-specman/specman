package specman;

class ExportPDFOpButton extends AbstractSpecmanOpButton {

  ExportPDFOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    context.exportAsPDF();
  }

}
