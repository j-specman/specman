package specman.ops;

import specman.GraphvizExporter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;

public class ExportGraphvizSpecmanOp extends AbstractSpecmanOp {

  public ExportGraphvizSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  public void export() {
    File verzeichnis = getDiagrammDatei() != null ? getDiagrammDatei().getParentFile() : null;
    JFileChooser fileChooser = new JFileChooser(verzeichnis);
    fileChooser.setFileFilter(new FileNameExtensionFilter("Graphviz Files", "gv"));
    if (fileChooser.showSaveDialog(getScrollPane()) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    String filename = fileChooser.getSelectedFile().getAbsolutePath();
    if (!filename.toLowerCase().endsWith(".gv")) {
      filename += ".gv";
    }
    try {
      new GraphvizExporter(filename).export(getHauptSequenz().generiereSchrittSequenzModel(false));
    }
    catch (IOException e) {
      displayException(e);
    }
  }

}
