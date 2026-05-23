package specman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import specman.model.ModelEnvelope;
import specman.model.v001.StruktogrammModel_V001;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class SaveDiagrammSpecmanOp extends AbstractSpecmanOp {

  private static final String PROJEKTDATEI_EXTENSION = ".nsd";

  SaveDiagrammSpecmanOp(Specman specman) {
    super(specman);
  }

  void speichern(boolean dateiauswahlErzwingen) {
    try (ScrollPause sp = specman.pauseScrolling()) {
      if (specman.diagrammDatei == null || dateiauswahlErzwingen) {
        File verzeichnis = (specman.diagrammDatei != null) ? specman.diagrammDatei.getParentFile() : null;
        JFileChooser fileChooser = new JFileChooser(verzeichnis);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Nassi Diagramme", "nsd"));
        if (fileChooser.showSaveDialog(specman) != JFileChooser.APPROVE_OPTION)
          return;
        String ausgewaehlterDateiname = fileChooser.getSelectedFile().getAbsolutePath();
        if (!ausgewaehlterDateiname.endsWith(PROJEKTDATEI_EXTENSION))
          ausgewaehlterDateiname += PROJEKTDATEI_EXTENSION;
        File ausgewaehlteDatei = new File(ausgewaehlterDateiname);
        if (!ausgewaehlteDatei.equals(specman.diagrammDatei) && ausgewaehlteDatei.exists()) {
          int confirmErgebnis = JOptionPane.showConfirmDialog(specman,
              "Die ausgewählte Datei existiert bereits.\nSoll die Datei überschrieben werden?",
              "Datei überschreiben?", JOptionPane.OK_CANCEL_OPTION);
          if (confirmErgebnis == JOptionPane.CANCEL_OPTION)
            return;
        }
        specman.setDiagrammDatei(new File(ausgewaehlterDateiname));
      }
      // Generating the model includes cleaning up text edit areas which in turn runs setText which
      // in turn causes the scroll position to be changed. Therefore, temporarily pause scrolling.
      StruktogrammModel_V001 model = generiereStruktogrammModel(true);
      ModelEnvelope wrappedModel = wrapModel(model);

      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.enableDefaultTyping();
      byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(wrappedModel);
      FileOutputStream fos = new FileOutputStream(specman.diagrammDatei);
      fos.write(json);
      fos.close();

      specman.recentFiles.add(specman.diagrammDatei);
      specman.undoManager.discardAllEdits();
    }
    catch (JsonProcessingException jpx) {
      specman.displayException(jpx);
    }
    catch (IOException e) {
      specman.displayException(e);
    }
  }

  private StruktogrammModel_V001 generiereStruktogrammModel(boolean formatierterText) {
    return new StruktogrammModel_V001(
        specman.getName(),
        specman.diagrammbreite,
        specman.zoomFaktor,
        specman.aenderungenVerfolgen(),
        specman.hauptSequenz.generiereSchrittSequenzModel(formatierterText),
        specman.intro.editorContent2Model(formatierterText),
        specman.outro.editorContent2Model(formatierterText),
        specman.pdfExportOptions);
  }

  private ModelEnvelope wrapModel(StruktogrammModel_V001 model) {
    ModelEnvelope envelope = new ModelEnvelope();
    envelope.model = model;
    envelope.modelType = model.getClass().getName();
    envelope.specmanVersion = SpecmanVersion.getVersion();
    return envelope;
  }

}
