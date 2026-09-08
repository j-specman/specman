package specman.ops;

import specman.ScrollPause;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.io.ModelSerializer_V002;
import static specman.ChangeSet.changeset;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SaveDiagrammSpecmanOp extends AbstractSpecmanOp {

  private static final String PROJEKTDATEI_EXTENSION = ".nsd";

  public SaveDiagrammSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  public void speichern(boolean dateiauswahlErzwingen) {
    try (ScrollPause sp = pauseScrolling()) {
      if (getDiagrammDatei() == null || dateiauswahlErzwingen) {
        File verzeichnis = (getDiagrammDatei() != null) ? getDiagrammDatei().getParentFile() : null;
        JFileChooser fileChooser = new JFileChooser(verzeichnis);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Nassi Diagramme", "nsd"));
        if (fileChooser.showSaveDialog(getScrollPane()) != JFileChooser.APPROVE_OPTION) {
          return;
        }
        String ausgewaehlterDateiname = fileChooser.getSelectedFile().getAbsolutePath();
        if (!ausgewaehlterDateiname.endsWith(PROJEKTDATEI_EXTENSION)) {
          ausgewaehlterDateiname += PROJEKTDATEI_EXTENSION;
        }
        File ausgewaehlteDatei = new File(ausgewaehlterDateiname);
        if (!ausgewaehlteDatei.equals(getDiagrammDatei()) && ausgewaehlteDatei.exists()) {
          int confirmErgebnis = showConfirmDialog(
              "Die ausgewählte Datei existiert bereits.\nSoll die Datei überschrieben werden?",
              "Datei überschreiben?", JOptionPane.OK_CANCEL_OPTION);
          if (confirmErgebnis == JOptionPane.CANCEL_OPTION) {
            return;
          }
        }
        setDiagrammDatei(new File(ausgewaehlterDateiname));
      }
      saveToFile(getDiagrammDatei());
      long wcTimestamp = AutoSaveOp.createWorkingCopyFor(getDiagrammDatei());
      context().notifyWorkingCopyInitialized(wcTimestamp);
      addRecentFile(getDiagrammDatei());
      discardAllUndoEdits();
    }
    catch (IOException e) {
      displayException(e);
    }
  }

  // Generating the model includes cleaning up text edit areas which in turn runs setText which
  // in turn causes the scroll position to be changed — callers must hold a ScrollPause.
  void saveToFile(File targetFile) throws IOException {
    writeToFile(targetFile, generateBytes());
  }

  byte[] generateBytes() throws IOException {
    DiagramModel_V002 model = generiereStruktogrammModel(true);
    String text = new ModelSerializer_V002().serialize(model);
    return text.getBytes(StandardCharsets.UTF_8);
  }

  void writeToFile(File targetFile, byte[] content) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
      fos.write(content);
    }
  }

  private DiagramModel_V002 generiereStruktogrammModel(boolean formatierterText) {
    DiagramModel_V002 model = new DiagramModel_V002(
        getDiagrammName(),
        getDiagrammbreite(),
        getZoomFactor(),
        aenderungenVerfolgen(),
        getHauptSequenz().generiereSchrittSequenzModel(formatierterText),
        getIntro().editorContent2Model(formatierterText),
        getOutro().editorContent2Model(formatierterText),
        getPdfExportOptions(),
        changeset().name);
    return model;
  }

}
