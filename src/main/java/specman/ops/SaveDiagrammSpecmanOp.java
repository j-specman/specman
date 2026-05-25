package specman.ops;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import specman.ScrollPause;
import specman.SpecmanOpContext;
import specman.SpecmanVersion;
import specman.model.ModelEnvelope;
import specman.model.v001.StruktogrammModel_V001;

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
        if (fileChooser.showSaveDialog(getScrollPane()) != JFileChooser.APPROVE_OPTION)
          return;
        String ausgewaehlterDateiname = fileChooser.getSelectedFile().getAbsolutePath();
        if (!ausgewaehlterDateiname.endsWith(PROJEKTDATEI_EXTENSION))
          ausgewaehlterDateiname += PROJEKTDATEI_EXTENSION;
        File ausgewaehlteDatei = new File(ausgewaehlterDateiname);
        if (!ausgewaehlteDatei.equals(getDiagrammDatei()) && ausgewaehlteDatei.exists()) {
          int confirmErgebnis = showConfirmDialog(
              "Die ausgewählte Datei existiert bereits.\nSoll die Datei überschrieben werden?",
              "Datei überschreiben?", JOptionPane.OK_CANCEL_OPTION);
          if (confirmErgebnis == JOptionPane.CANCEL_OPTION)
            return;
        }
        setDiagrammDatei(new File(ausgewaehlterDateiname));
      }
      // Generating the model includes cleaning up text edit areas which in turn runs setText which
      // in turn causes the scroll position to be changed. Therefore, temporarily pause scrolling.
      StruktogrammModel_V001 model = generiereStruktogrammModel(true);
      ModelEnvelope wrappedModel = wrapModel(model);

      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.enableDefaultTyping();
      byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(wrappedModel);
      FileOutputStream fos = new FileOutputStream(getDiagrammDatei());
      fos.write(json);
      fos.close();

      addRecentFile(getDiagrammDatei());
      discardAllUndoEdits();
    }
    catch (JsonProcessingException jpx) {
      displayException(jpx);
    }
    catch (IOException e) {
      displayException(e);
    }
  }

  private StruktogrammModel_V001 generiereStruktogrammModel(boolean formatierterText) {
    return new StruktogrammModel_V001(
        getDiagrammName(),
        getDiagrammbreite(),
        getZoomFactor(),
        aenderungenVerfolgen(),
        getHauptSequenz().generiereSchrittSequenzModel(formatierterText),
        getIntro().editorContent2Model(formatierterText),
        getOutro().editorContent2Model(formatierterText),
        getPdfExportOptions());
  }

  private ModelEnvelope wrapModel(StruktogrammModel_V001 model) {
    ModelEnvelope envelope = new ModelEnvelope();
    envelope.model = model;
    envelope.modelType = model.getClass().getName();
    envelope.specmanVersion = SpecmanVersion.getVersion();
    return envelope;
  }

}
