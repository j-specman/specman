package specman.ops;

import specman.link.SpecmanLink;
import specman.view.AbstractSchrittView;

import java.awt.datatransfer.StringSelection;

import static specman.Specman.editor;

public class CopyStepLinkOp {

  private final AbstractSchrittView step;

  public CopyStepLinkOp(AbstractSchrittView step) {
    this.step = step;
  }

  public void run() {
    String link = SpecmanLink.of(editor().getDiagrammDatei(), step);
    editor().clipboard().setContents(new StringSelection(link), null);
  }
}
