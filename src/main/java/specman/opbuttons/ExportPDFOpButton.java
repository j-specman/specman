package specman.opbuttons;

import specman.*;

public class ExportPDFOpButton extends AbstractSpecmanOpButton {

  public ExportPDFOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    exportAsPDF();
  }

}
