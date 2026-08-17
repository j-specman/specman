package specman.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import specman.ScrollPause;
import specman.SpecmanVersion;
import static specman.ChangeSet.changeset;
import specman.model.ModelEnvelope;
import specman.model.v001.StruktogrammModel_V001;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.PdfExportOptionsModel_V002;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
      AutoSaveOp.deleteWorkingCopyFor(getDiagrammDatei());
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
    ModelEnvelope wrappedModel = wrapModel(model);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(wrappedModel);
  }

  void writeToFile(File targetFile, byte[] json) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
      fos.write(json);
    }
  }

  private DiagramModel_V002 generiereStruktogrammModel(boolean formatierterText) {
    return new DiagramModel_V002(
        getDiagrammName(),
        getDiagrammbreite(),
        getZoomFactor(),
        aenderungenVerfolgen(),
        getHauptSequenz().generiereSchrittSequenzModel(formatierterText),
        getIntro().editorContent2Model(formatierterText),
        getOutro().editorContent2Model(formatierterText),
        PdfExportOptionsModel_V002.from(getPdfExportOptions()),
        changeset().name);
  }

  private ModelEnvelope wrapModel(DiagramModel_V002 model) {
    ModelEnvelope envelope = new ModelEnvelope();
    envelope.model = model;
    envelope.modelType = model.getClass().getName();
    envelope.specmanVersion = SpecmanVersion.getVersion();
    return envelope;
  }

}
